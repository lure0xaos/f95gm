
import f95gm.build.GenerateAssetManifestTask
import f95gm.build.GenerateFrontendApiClientTask
import f95gm.build.GenerateServerApiRegistryTask
import f95gm.build.VerifyGeneratedOperationInventoriesTask
import f95gm.build.GenerateApiContractChangelogTask
import f95gm.build.VerifyGeneratedFileTask
import f95gm.build.VerifyDirectoryReproducibilityTask
import f95gm.build.VerifyFilesPresentTask
import f95gm.build.VerifyApiContractCompatibilityTask
import f95gm.build.VerifyApiDeprecationsTask
import f95gm.build.VerifyReproducibleGeneratedArtifactsTask
import f95gm.build.VerifyFrontendBudgetsTask
import f95gm.build.VerifyDependencyPolicyTask
import f95gm.build.VerificationTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

group = "com.github.lure0xaos"
version = "1.0.0"

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.i18n4k)
    alias(libs.plugins.detekt)
    id("f95gm-quality")
    id("f95gm-e2e")
    id("f95gm-architecture")
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

tasks.withType<dev.detekt.gradle.Detekt>().configureEach {
    jvmTarget = "22"
    baseline.set(layout.projectDirectory.file("config/detekt/baseline.xml"))
    reports {
        html.required.set(true)
        sarif.required.set(true)
    }
}

tasks.withType<dev.detekt.gradle.DetektCreateBaselineTask>().configureEach {
    jvmTarget = "22"
}

// Keep non-public data-class constructors non-public in generated copy methods.
tasks.withType<KotlinCompilationTask<*>>().configureEach {
    compilerOptions.freeCompilerArgs.add("-Xconsistent-data-class-copy-visibility")
}

tasks.named("detekt") {
    mustRunAfter("detektBaseline")
}

kotlin {
    jvmToolchain(libs.versions.java.get().toInt())
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
            kotlin.srcDir(layout.buildDirectory.dir("generated/server"))
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

// Keep dependency resolution reproducible; refresh lockfiles deliberately with
// `./gradlew dependencies --write-locks` during dependency upgrades.
dependencyLocking {
    lockAllConfigurations()
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
}

val generateAssetManifest = tasks.register<GenerateAssetManifestTask>("generateAssetManifest") {
    group = "application"
    description = "Generates the content manifest for the production browser bundle."
    dependsOn(copyJsToJvm)
    assets.set(layout.buildDirectory.dir("generated/jvmResources/main/static"))
    manifest.set(layout.buildDirectory.file("generated/jvmResources/main/static/asset-manifest.json"))
}

tasks.named("installDist") {
    dependsOn(generateAssetManifest)
}

tasks.named("jvmProcessResources") {
    dependsOn(generateAssetManifest)
}

configurations.configureEach {
    exclude(group = "org.fusesource.jansi", module = "jansi")
}

tasks.named<JavaExec>("runJvm") {
    systemProperty("f95gm.port", providers.gradleProperty("f95gm.port").orElse("8080"))
    systemProperty("f95gm.database", providers.gradleProperty("f95gm.database"))
    dependsOn(copyJsToJvm)
}

tasks.register<VerificationTask>("jvmRunApp") {
    group = "application"
    description = "Builds the Fritz2 browser bundle and runs the complete app on the JVM."
    dependsOn("runJvm")
}

tasks.register<VerificationTask>("jpackage") {
    group = "distribution"
    description = "Builds the F95GM Windows installer."
    dependsOn(":packaging:jpackage")
}

val generateApiContract = tasks.register<JavaExec>("generateApiContract") {
    group = "verification"
    description = "Generates the OpenAPI contract from the shared endpoint descriptors."
    dependsOn("compileKotlinJvm")
    mainClass.set("f95gm.server.app.GenerateOpenApiContractKt")
    val output = layout.buildDirectory.file("generated/api/f95gm-openapi.json")
    args(output.get().asFile.absolutePath)
    classpath = files(
        layout.buildDirectory.dir("classes/kotlin/jvm/main"),
        kotlin.jvm().compilations["main"].runtimeDependencyFiles
    )
    outputs.file(output)
}

val generateApiContractRepeat = tasks.register<JavaExec>("generateApiContractRepeat") {
    group = "verification"
    description = "Generates the OpenAPI contract in an isolated second output for reproducibility checks."
    dependsOn("compileKotlinJvm")
    mainClass.set("f95gm.server.app.GenerateOpenApiContractKt")
    val output = layout.buildDirectory.file("generated/api/f95gm-openapi-repeat.json")
    args(output.get().asFile.absolutePath)
    classpath = files(
        layout.buildDirectory.dir("classes/kotlin/jvm/main"),
        kotlin.jvm().compilations["main"].runtimeDependencyFiles
    )
    outputs.file(output)
}

val migrationDryRun = tasks.register<JavaExec>("migrationDryRun") {
    group = "verification"
    description = "Reports the deterministic database migration plan without changing a database."
    dependsOn("compileKotlinJvm")
    mainClass.set("f95gm.server.app.GenerateMigrationPlanKt")
    val output = layout.buildDirectory.file("generated/database/migration-plan.txt")
    args(output.get().asFile.absolutePath)
    classpath = files(
        layout.buildDirectory.dir("classes/kotlin/jvm/main"),
        kotlin.jvm().compilations["main"].runtimeDependencyFiles
    )
    outputs.file(output)
}

val verifyApiContract = tasks.register<VerifyGeneratedFileTask>("verifyApiContract") {
    group = "verification"
    description = "Checks the generated API contract against the checked-in snapshot."
    dependsOn(generateApiContract)
    expected.set(layout.projectDirectory.file("api/f95gm-openapi.json"))
    actual.set(layout.buildDirectory.file("generated/api/f95gm-openapi.json"))
}

val generateFrontendApiClient = tasks.register<GenerateFrontendApiClientTask>("generateFrontendApiClient") {
    group = "verification"
    description = "Generates the frontend client operation manifest from the checked-in canonical OpenAPI contract."
    contract.set(layout.projectDirectory.file("api/f95gm-openapi.json"))
    output.set(layout.buildDirectory.file("generated/frontend/f95gm/client/data/api/GeneratedF95ApiOperations.kt"))
}

val generateFrontendApiClientRepeat = tasks.register<GenerateFrontendApiClientTask>("generateFrontendApiClientRepeat") {
    group = "verification"
    description = "Generates a second frontend operation manifest for reproducibility checks."
    dependsOn(generateApiContractRepeat)
    contract.set(layout.buildDirectory.file("generated/api/f95gm-openapi-repeat.json"))
    output.set(layout.buildDirectory.file("generated/frontend-repeat/f95gm/client/data/api/GeneratedF95ApiOperations.kt"))
}

val generateServerApiRegistry = tasks.register<GenerateServerApiRegistryTask>("generateServerApiRegistry") {
    group = "verification"
    description = "Generates the server operation inventory from the shared API contract."
    contract.set(layout.projectDirectory.file("api/f95gm-openapi.json"))
    output.set(layout.buildDirectory.file("generated/server/f95gm/server/app/GeneratedServerApiRegistry.kt"))
}

tasks.named("compileKotlinJvm").configure { dependsOn(generateServerApiRegistry) }

val verifyGeneratedOperationInventories = tasks.register<VerifyGeneratedOperationInventoriesTask>("verifyGeneratedOperationInventories") {
    group = "verification"
    description = "Checks that generated server and frontend operation inventories are identical."
    dependsOn(generateServerApiRegistry, generateFrontendApiClient)
    serverRegistry.set(layout.buildDirectory.file("generated/server/f95gm/server/app/GeneratedServerApiRegistry.kt"))
    frontendRegistry.set(layout.buildDirectory.file("generated/frontend/f95gm/client/data/api/GeneratedF95ApiOperations.kt"))
}

val verifyApiContractCompatibility = tasks.register<VerifyApiContractCompatibilityTask>("verifyApiContractCompatibility") {
    group = "verification"
    description = "Checks the complete previous-release OpenAPI snapshot for breaking changes."
    dependsOn(generateApiContract)
    baseline.set(layout.projectDirectory.file("api/f95gm-openapi-previous.json"))
    generated.set(layout.buildDirectory.file("generated/api/f95gm-openapi.json"))
}

val verifyApiDeprecations = tasks.register<VerifyApiDeprecationsTask>("verifyApiDeprecations") {
    group = "verification"
    description = "Requires replacement and sunset metadata for every deprecated operation."
    dependsOn(generateApiContract)
    generated.set(layout.buildDirectory.file("generated/api/f95gm-openapi.json"))
}

val generateApiContractChangelog = tasks.register<GenerateApiContractChangelogTask>("generateApiContractChangelog") {
    group = "documentation"
    description = "Generates a deterministic changelog from the previous and current OpenAPI contracts."
    dependsOn(generateApiContract)
    baseline.set(layout.projectDirectory.file("api/f95gm-openapi-previous.json"))
    generated.set(layout.buildDirectory.file("generated/api/f95gm-openapi.json"))
    output.set(layout.buildDirectory.file("generated/api/CHANGELOG.md"))
}

verifyApiContract.configure { dependsOn(generateFrontendApiClient) }
verifyApiContract.configure { dependsOn(generateServerApiRegistry) }
verifyApiContract.configure { dependsOn(verifyGeneratedOperationInventories) }

val verifyReproducibleGeneratedArtifacts = tasks.register<VerifyReproducibleGeneratedArtifactsTask>("verifyReproducibleGeneratedArtifacts") {
    group = "verification"
    description = "Rejects generated API and frontend artifacts containing unstable build data."
    dependsOn(
        generateApiContract,
        generateApiContractRepeat,
        generateFrontendApiClient,
        generateFrontendApiClientRepeat,
        verifyApiContractCompatibility,
        verifyApiDeprecations,
        generateApiContractChangelog,
        copyJsToJvm
    )
    generatedContract.set(layout.buildDirectory.file("generated/api/f95gm-openapi.json"))
    repeatContract.set(layout.buildDirectory.file("generated/api/f95gm-openapi-repeat.json"))
    generatedClient.set(layout.buildDirectory.file("generated/frontend/f95gm/client/data/api/GeneratedF95ApiOperations.kt"))
    repeatClient.set(layout.buildDirectory.file("generated/frontend-repeat/f95gm/client/data/api/GeneratedF95ApiOperations.kt"))
}

val installPackagingRepeat = tasks.register<Sync>("installPackagingRepeat") {
    group = "verification"
    description = "Stages the packaged distribution a second time for reproducibility checks."
    dependsOn(":packaging:installDist")
    from(layout.projectDirectory.dir("packaging/build/install/packaging"))
    into(layout.buildDirectory.dir("packaging-repeat"))
}

val verifyPackagingReproducibility = tasks.register<VerifyDirectoryReproducibilityTask>("verifyPackagingReproducibility") {
    group = "verification"
    description = "Compares the packaged distribution with a second isolated staging output."
    dependsOn(":packaging:installDist", installPackagingRepeat)
    first.from(layout.projectDirectory.dir("packaging/build/install/packaging"))
    second.from(layout.buildDirectory.dir("packaging-repeat"))
}

val packageReproducibleArchive = tasks.register<org.gradle.api.tasks.bundling.Zip>("packageReproducibleArchive") {
    group = "verification"
    description = "Creates a packaging archive with normalized timestamps and deterministic file ordering."
    dependsOn(":packaging:installDist")
    from(layout.projectDirectory.dir("packaging/build/install/packaging"))
    archiveFileName.set("f95gm-packaging-reproducible.zip")
    destinationDirectory.set(layout.buildDirectory.dir("reproducible-packaging"))
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

val packageReproducibleArchiveRepeat = tasks.register<org.gradle.api.tasks.bundling.Zip>("packageReproducibleArchiveRepeat") {
    group = "verification"
    description = "Creates the second deterministic packaging archive for byte comparison."
    dependsOn(":packaging:installDist")
    from(layout.projectDirectory.dir("packaging/build/install/packaging"))
    archiveFileName.set("f95gm-packaging-reproducible-repeat.zip")
    destinationDirectory.set(layout.buildDirectory.dir("reproducible-packaging-repeat"))
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

val verifyPackagingArchiveReproducibility = tasks.register<VerifyGeneratedFileTask>("verifyPackagingArchiveReproducibility") {
    group = "verification"
    description = "Verifies normalized packaging archives are byte-for-byte reproducible."
    dependsOn(packageReproducibleArchive, packageReproducibleArchiveRepeat)
    expected.set(packageReproducibleArchive.flatMap { it.archiveFile })
    actual.set(packageReproducibleArchiveRepeat.flatMap { it.archiveFile })
}
kotlin.sourceSets.named("jsMain") {
    kotlin.srcDir(layout.buildDirectory.dir("generated/frontend"))
}
tasks.named("compileKotlinJs").configure { dependsOn(generateFrontendApiClient) }

val verifyPackagingAssets = tasks.register<VerifyFilesPresentTask>("verifyPackagingAssets") {
    group = "verification"
    description = "Checks that packaging icons and generated browser assets are present."
    dependsOn(generateAssetManifest)
    val assets = listOf(
        layout.projectDirectory.file("src/jsMain/resources/F95GM.ico").asFile,
        layout.projectDirectory.file("src/jsMain/resources/F95GM.png").asFile,
        layout.projectDirectory.file("src/jsMain/resources/F95GM.icns").asFile,
        layout.buildDirectory.file("generated/jvmResources/main/static/index.html").get().asFile,
        layout.buildDirectory.file("generated/jvmResources/main/static/asset-manifest.json").get().asFile
    )
    filesToCheck.from(assets)
}

val verifyFrontendBudgets = tasks.register<VerifyFrontendBudgetsTask>("verifyFrontendBudgets") {
    group = "verification"
    description = "Rejects unexpectedly large production browser bundles."
    dependsOn(copyJsToJvm)
    distribution.set(layout.buildDirectory.dir("dist/js/productionExecutable"))
    maxJavaScriptBytes.set(5L * 1024L * 1024L)
}

val dependencyPolicy = tasks.register<VerifyDependencyPolicyTask>("dependencyPolicy") {
    group = "verification"
    description = "Rejects dynamic dependency versions that make builds non-reproducible."
    versionCatalog.set(layout.projectDirectory.file("gradle/libs.versions.toml"))
}

val verifyLocalizationKeys = tasks.register<VerificationTask>("verifyLocalizationKeys") {
    group = "verification"
    description = "Runs executable localization-key tests."
    dependsOn("jvmTest")
}

val testQualityGate = tasks.register<VerificationTask>("testQualityGate") {
    group = "verification"
    description = "Runs the JVM and JS test suites as an executable quality gate."
    dependsOn("allTests")
}

val verifySourceConventions = tasks.register<VerificationTask>("verifySourceConventions") {
    group = "verification"
    description = "Runs executable source-convention tests."
    dependsOn("jvmTest")
}

val verifyLayerBoundaries = tasks.register<VerificationTask>("verifyLayerBoundaries") {
    group = "verification"
    description = "Runs import-based architectural boundary tests."
    dependsOn("jvmTest")
}

val hardeningChecks = tasks.register<VerificationTask>("hardeningChecks") {
    group = "verification"
    description = "Runs lifecycle, audit, boundary, style, and dependency hardening checks."
    dependsOn(
        "jvmTest",
        "dependencyPolicy",
        "verifySensitiveLogging",
        "verifyPackageCycles",
        "jvmCoverageCheck"
    )
}

/** Fast, non-packaging feedback for local edits. */
val fastCheck = tasks.register<VerificationTask>("fastCheck") {
    group = "verification"
    description = "Runs fast source, architecture, and JVM verification checks."
    dependsOn(
        "verifyKotlinStyle",
        "verifySourceConventions",
        "verifyLayerBoundaries",
        "verifyPrimitiveBoundaries",
        "verifyClockAccess",
        "jvmTest"
    )
}

/** Contract-focused verification for changes to shared API types or generated clients. */
val contractCheck = tasks.register<VerificationTask>("contractCheck") {
    group = "verification"
    description = "Runs API contract, generated-client, and compatibility verification."
    dependsOn(
        "verifyApiContract",
        "verifyOpenApiSnapshot",
        "verifyTypedContractSurface",
        "verifyGeneratedOperationInventories",
        "verifyApiContractCompatibility",
        "verifyApiDeprecations"
    )
}

/** Integration verification, including both platform test suites and persistence checks. */
val integrationCheck = tasks.register<VerificationTask>("integrationCheck") {
    group = "verification"
    description = "Runs JVM and JS integration-oriented verification."
    dependsOn("allTests", "verifySensitiveLogging", "verifyPackageCycles", "jvmCoverageCheck")
}

/** Full release-oriented verification including reproducible generated artifacts and packaging. */
val releaseCheck = tasks.register<VerificationTask>("releaseCheck") {
    group = "verification"
    description = "Runs the complete release and reproducibility verification suite."
    dependsOn("verifyArchitecture", "hardeningChecks")
}

tasks.named("check") {
    dependsOn("verifyArchitecture", hardeningChecks)
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
