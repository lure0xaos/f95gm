import java.security.MessageDigest

group = "com.github.lure0xaos"
version = "1.0.0"

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.i18n4k)
    alias(libs.plugins.detekt)
    jacoco
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
    parallel = true
    source.setFrom(files("src/commonMain/kotlin", "src/jsMain/kotlin", "src/jvmMain/kotlin", "packaging"))
    config.setFrom(files("config/detekt/detekt.yml"))
    baseline = file("config/detekt/baseline.xml")
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    jvmTarget = "22"
    baseline.set(layout.projectDirectory.file("config/detekt/baseline.xml"))
    onlyIf {
        JavaVersion.current() <= JavaVersion.VERSION_22
    }
    reports {
        html.required.set(true)
        sarif.required.set(true)
    }
}

tasks.withType<io.gitlab.arturbosch.detekt.DetektCreateBaselineTask>().configureEach {
    jvmTarget = "22"
}

tasks.named("detekt") {
    mustRunAfter("detektBaseline")
}

kotlin {
    jvmToolchain(25)
    js {
        browser {
            commonWebpackConfig {
                cssSupport {
                    enabled.set(true)
                }
                devServer = org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig.DevServer(
                    proxy = mutableListOf(
                        org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig.DevServer.Proxy(
                            context = mutableListOf("/api"),
                            target = "http://127.0.0.1:8081",
                            changeOrigin = true
                        )
                    )
                )
            }
        }
        binaries.executable()
    }
    jvm {
        @OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
        binaries {
            executable {
                mainClass.set("f95gm.server.entry.MainKt")
            }
        }
    }
    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.ktor.client.core)
                implementation(libs.i18n4k.core)
            }
            @OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
            generatedKotlin.srcDir(layout.buildDirectory.dir("generated/ksp/metadata/$name/kotlin"))
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        jsMain {
            dependencies {
                implementation(libs.fritz2.core)
                implementation(libs.fritz2.headless)
                implementation(libs.ktor.client.js)
                implementation(npm("bootstrap", "5.3.8"))
                implementation(npm("bootstrap-icons", "1.13.1"))
            }
        }
        jvmMain {
            dependencies {
                implementation(libs.kotlinLogging)
                implementation(libs.ktor.client.cio)
                implementation(libs.ktor.server.call.logging)
                implementation(libs.ktor.server.status.pages)
                implementation(libs.ktor.server.cio)
                implementation(libs.jsoup)
                implementation(libs.exposed.core)
                implementation(libs.exposed.jdbc)
                runtimeOnly(libs.h2)
                runtimeOnly(libs.log4j.api)
                runtimeOnly(libs.log4j.core)
                runtimeOnly(libs.log4j.slf4j2.impl)
            }
            resources.srcDir(layout.buildDirectory.dir("generated/jvmResources/main"))
        }
        jsTest {
            dependencies {
                implementation(libs.ktor.client.mock)
            }
        }
        jvmTest {
            dependencies {
                implementation(libs.ktor.server.test.host)
                implementation(libs.ktor.client.mock)
            }
        }
    }
}

i18n4k {
    inputDirectory = "src/commonMain/resources"
    sourceCodeLocales = listOf("en")
}


dependencies {
    kspCommonMainMetadata(libs.fritz2.lenses.annotation.processor)
}

val commonMetadataKspTasks: TaskCollection<com.google.devtools.ksp.gradle.KspAATask> =
    tasks.withType<com.google.devtools.ksp.gradle.KspAATask>().matching {
        it.kspConfig.platformType.orNull == org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType.common
    }

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask<*>>().configureEach {
    dependsOn(commonMetadataKspTasks)
}

val copyJsToJvm: TaskProvider<Sync> = tasks.register<Sync>("copyJsToJvm") {
    group = "application"
    description = "Copies the production Fritz2 browser bundle into JVM resources."
    dependsOn("jsBrowserDistribution")
    from(layout.buildDirectory.dir("dist/js/productionExecutable"))
    into(layout.buildDirectory.dir("generated/jvmResources/main/static"))
    doLast {
        val outputDirectory = destinationDir
        val manifestEntries = outputDirectory.walkTopDown()
            .filter { it.isFile && it.name != "asset-manifest.json" }
            .sortedBy { it.relativeTo(outputDirectory).invariantSeparatorsPath }
            .map { file ->
                val digest = MessageDigest.getInstance("SHA-256")
                    .digest(file.readBytes())
                    .joinToString("") { byte -> "%02x".format(byte) }
                "\"${file.relativeTo(outputDirectory).invariantSeparatorsPath}\":\"$digest\""
            }
            .toList()
        outputDirectory.resolve("asset-manifest.json").writeText("{${manifestEntries.joinToString(",")}}\n")
    }
}

tasks.named("installDist") {
    dependsOn(copyJsToJvm)
}

tasks.named("jvmProcessResources") {
    dependsOn(copyJsToJvm)
}

configurations.configureEach {
    exclude(group = "org.fusesource.jansi", module = "jansi")
}

tasks.named<JavaExec>("runJvm") {
    systemProperty("f95gm.port", providers.gradleProperty("f95gm.port").orElse("8080").get())
    providers.gradleProperty("f95gm.database").orNull?.let { database ->
        systemProperty("f95gm.database", database)
    }
    dependsOn(copyJsToJvm)
}

tasks.register("jvmRunApp") {
    group = "application"
    description = "Builds the Fritz2 browser bundle and runs the complete app on the JVM."
    dependsOn("runJvm")
}

tasks.register("jpackage") {
    group = "distribution"
    description = "Builds the F95GM Windows installer."
    dependsOn(":packaging:jpackage")
}

tasks.register("browserE2e") {
    group = "verification"
    description = "Runs the Playwright browser smoke test against a packaged JVM server."
    notCompatibleWithConfigurationCache("The smoke task owns external server and browser processes")
    dependsOn(":packaging:installDist")
    val launcherFile = layout.projectDirectory.file("packaging/build/install/packaging/bin/packaging.bat").asFile
    val e2eDirectory = layout.projectDirectory.dir("e2e")
    val databaseFile = layout.buildDirectory.dir("browser-e2e-data")
    doLast {
        val port = 18_080
        val launcher = launcherFile
        check(launcher.isFile) { "Missing packaged application launcher: $launcher" }
        val database = databaseFile.get().asFile.resolve("f95gm")
        val npm = if (org.gradle.internal.os.OperatingSystem.current().isWindows) "npx.cmd" else "npx"
        val browserTest = ProcessBuilder(npm, "--no-install", "playwright", "test")
            .directory(e2eDirectory.asFile)
            .redirectErrorStream(true)
            .apply {
                environment()["F95GM_E2E_URL"] = "http://127.0.0.1:$port"
                environment()["F95GM_E2E_COMMAND"] = launcher.absolutePath
                environment()["JAVA_TOOL_OPTIONS"] =
                    "-Df95gm.host=127.0.0.1 -Df95gm.port=$port -Df95gm.openBrowser=false -Df95gm.database=${database.absolutePath} -Df95gm.upstreamBaseUrl=http://127.0.0.1:18081"
            }
            .inheritIO()
            .start()
        check(browserTest.waitFor() == 0) { "Browser E2E smoke test failed" }
    }
}

val generateApiContract = tasks.register<JavaExec>("generateApiContract") {
    group = "verification"
    description = "Generates the OpenAPI contract from the shared endpoint descriptors."
    dependsOn("jvmJar")
    mainClass.set("f95gm.server.app.GenerateOpenApiContractKt")
    val output = layout.buildDirectory.file("generated/api/f95gm-openapi.json")
    args(output.get().asFile.absolutePath)
    classpath = files(
        layout.buildDirectory.file("libs/${project.name}-jvm-${project.version}.jar"),
        kotlin.jvm().compilations["main"].runtimeDependencyFiles
    )
    outputs.file(output)
}

val verifyApiContract = tasks.register("verifyApiContract") {
    group = "verification"
    description = "Checks the generated API contract against the checked-in snapshot."
    dependsOn(generateApiContract)
    val snapshot = layout.projectDirectory.file("api/f95gm-openapi.json")
    val generated = layout.buildDirectory.file("generated/api/f95gm-openapi.json")
    inputs.file(snapshot)
    inputs.file(generated)
    doLast {
        check(snapshot.asFile.isFile) { "Missing checked-in API contract snapshot: ${snapshot.asFile}" }
        val expected = snapshot.asFile.readText()
        val actual = generated.get().asFile.readText()
        check(expected == actual) {
            "Generated API contract differs from ${snapshot.asFile}; run generateApiContract and review the API change."
        }
    }
}

val verifyPackagingAssets = tasks.register("verifyPackagingAssets") {
    group = "verification"
    description = "Checks that packaging icons and generated browser assets are present."
    dependsOn(copyJsToJvm)
    val assets = listOf(
        layout.projectDirectory.file("src/jsMain/resources/F95GM.ico").asFile,
        layout.projectDirectory.file("src/jsMain/resources/F95GM.png").asFile,
        layout.projectDirectory.file("src/jsMain/resources/F95GM.icns").asFile,
        layout.buildDirectory.file("generated/jvmResources/main/static/index.html").get().asFile,
        layout.buildDirectory.file("generated/jvmResources/main/static/asset-manifest.json").get().asFile
    )
    inputs.files(assets)
    doLast {
        assets.forEach { asset -> check(asset.isFile) { "Missing packaging asset: $asset" } }
    }
}

val verifyFrontendBudgets = tasks.register("verifyFrontendBudgets") {
    group = "verification"
    description = "Rejects unexpectedly large production browser bundles."
    dependsOn(copyJsToJvm)
    val distribution = layout.buildDirectory.dir("dist/js/productionExecutable")
    inputs.dir(distribution)
    doLast {
        val javascriptBytes = distribution.get().asFile.walkTopDown()
            .filter { it.isFile && it.extension == "js" }
            .sumOf { it.length() }
        check(javascriptBytes <= 5L * 1024L * 1024L) {
            "Production JavaScript bundle exceeds 5 MiB: $javascriptBytes bytes"
        }
    }
}

val verifyKotlinStyle = tasks.register("verifyKotlinStyle") {
    group = "verification"
    description = "Runs lightweight source static-analysis and formatting checks."
    val sourceRoots = listOf(layout.projectDirectory.dir("src").asFile, layout.projectDirectory.dir("packaging").asFile)
    val sourceFiles =
        sourceRoots.flatMap { root -> root.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList() }
    inputs.files(sourceFiles)
    doLast {
        sourceFiles.forEach { file ->
            val text = file.readText()
            check(text.lineSequence().none { it.endsWith(" ") || it.endsWith("\t") }) {
                "Trailing whitespace found in $file"
            }
            check(!text.contains("println(")) { "Use structured logging instead of println in $file" }
        }
    }
}

val dependencyPolicy = tasks.register("dependencyPolicy") {
    group = "verification"
    description = "Rejects dynamic dependency versions that make builds non-reproducible."
    val catalog = layout.projectDirectory.file("gradle/libs.versions.toml")
    inputs.file(catalog)
    doLast {
        val catalogText = catalog.asFile.readText()
        val dynamicVersionLine = catalogText.lineSequence().firstOrNull { line ->
            line.contains("version =") && Regex(
                """version\s*=\s*[\"'][^\"']*(latest\.release|latest\.integration|SNAPSHOT|\+|[\[\(,<>])""",
                RegexOption.IGNORE_CASE
            ).containsMatchIn(line)
        }
        check(dynamicVersionLine == null) {
            "Dynamic or snapshot dependency version detected in the version catalog: $dynamicVersionLine"
        }
    }
}

val verifyLocalizationKeys = tasks.register("verifyLocalizationKeys") {
    group = "verification"
    description = "Runs executable localization-key tests."
    dependsOn("jvmTest")
}

val testQualityGate = tasks.register("testQualityGate") {
    group = "verification"
    description = "Runs the JVM and JS test suites as an executable quality gate."
    dependsOn("allTests")
}

val verifySourceConventions = tasks.register("verifySourceConventions") {
    group = "verification"
    description = "Runs executable source-convention tests."
    dependsOn("jvmTest")
}

val verifyLayerBoundaries = tasks.register("verifyLayerBoundaries") {
    group = "verification"
    description = "Runs import-based architectural boundary tests."
    dependsOn("jvmTest")
}

tasks.named("check") {
    dependsOn(
        verifyPackagingAssets,
        verifyFrontendBudgets,
        verifyKotlinStyle,
        dependencyPolicy,
        verifyLocalizationKeys,
        testQualityGate,
        verifyApiContract
    )
}

tasks.named("check") {
    dependsOn(verifySourceConventions, verifyLayerBoundaries, "detekt")
}

val jvmCoverageReport = tasks.register<org.gradle.testing.jacoco.tasks.JacocoReport>("jvmCoverageReport") {
    group = "verification"
    description = "Generates the JVM test coverage report."
    dependsOn("jvmTest")
    executionData(layout.buildDirectory.file("jacoco/jvmTest.exec"))
    sourceDirectories.from(files("src/jvmMain/kotlin"))
    classDirectories.from(layout.buildDirectory.dir("classes/kotlin/jvm/main"))
    reports {
        html.required.set(true)
        xml.required.set(true)
    }
}

tasks.register<org.gradle.testing.jacoco.tasks.JacocoCoverageVerification>("jvmCoverageCheck") {
    group = "verification"
    description = "Checks the minimum JVM line coverage threshold."
    dependsOn(jvmCoverageReport)
    executionData(layout.buildDirectory.file("jacoco/jvmTest.exec"))
    sourceDirectories.from(files("src/jvmMain/kotlin"))
    classDirectories.from(layout.buildDirectory.dir("classes/kotlin/jvm/main"))
    violationRules {
        rule {
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = 0.60.toBigDecimal()
            }
        }
        rule {
            limit {
                counter = "BRANCH"
                value = "COVEREDRATIO"
                minimum = 0.45.toBigDecimal()
            }
        }
    }
}

tasks.named("check") {
    dependsOn("jvmCoverageCheck")
}
