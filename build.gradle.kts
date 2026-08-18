group = "com.github.lure0xaos"
version = "1.0.0"

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.i18n4k)
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
