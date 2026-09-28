import f95gm.build.GenerateApiFixtureManifestTask
import f95gm.build.VerifyApiBoundaryTask
import f95gm.build.VerifyClientGlobalUsageTask
import f95gm.build.VerifyCommonJvmSeparationTask
import f95gm.build.VerifyExceptionBoundariesTask
import f95gm.build.VerifyEnvironmentTask
import f95gm.build.VerifyJavaRuntimeTask
import f95gm.build.VerifyRouteApplicationDependenciesTask
import f95gm.build.VerifyRouteDependencyOwnershipTask
import f95gm.build.VerifyServerDependencyGraphTask
import f95gm.build.VerifyServiceFactoryUsageTask
import f95gm.build.VerifySystemPropertyAccessTask
import f95gm.build.VerifyApplicationPortPurityTask
import f95gm.build.VerifyToolchainConsistencyTask
import f95gm.build.VerifyTypedClientArchitectureTask
import f95gm.build.VerifyTypedObservabilityTask
import f95gm.build.VerifyTypedBoundaryTask
import org.gradle.api.artifacts.VersionCatalogsExtension

val architectureSource = layout.projectDirectory
val architectureLibs = extensions.getByType<VersionCatalogsExtension>().named("libs")

tasks.register<VerifyApiBoundaryTask>("verifyApiBoundaryConventions") {
    group = "verification"
    description = "Rejects raw API paths and transport calls outside their boundaries."
    sourceFiles.from(fileTree(architectureSource.dir("src")) {
        include("**/*Main/**/*.kt")
        exclude("commonMain/kotlin/f95gm/domain/api/**")
    })
}

tasks.register<VerifyTypedClientArchitectureTask>("verifyTypedClientArchitecture") {
    group = "verification"
    description = "Rejects reintroduction of legacy untyped client boundary APIs."
    sourceFiles.from(fileTree(architectureSource.dir("src/jsMain/kotlin")) { include("**/*.kt") })
}

tasks.register<VerifyClientGlobalUsageTask>("verifyClientGlobalUsage") {
    group = "verification"
    description = "Prevents deprecated client-global state access from increasing during migration."
    sourceFiles.from(fileTree(architectureSource.dir("src/jsMain/kotlin")) { include("**/*.kt") })
    maximumUsages.set(70)
}

tasks.register<VerifyServerDependencyGraphTask>("verifyServerDependencyGraph") {
    group = "verification"
    description = "Prevents route code from bypassing declared dependency ports."
    sourceFiles.from(fileTree(architectureSource.dir("src/jvmMain/kotlin/f95gm/server")) { include("**/*.kt") })
}

tasks.register<VerifyJavaRuntimeTask>("verifyJavaRuntime") {
    group = "verification"
    description = "Checks that Gradle is running on the supported Java runtime."
}

tasks.register<VerifyEnvironmentTask>("verifyEnvironment") {
    group = "verification"
    description = "Reports and validates the local Java, Node, npm, and database environment."
    expectedGradleVersion.set(gradle.gradleVersion)
}

tasks.register<VerifyToolchainConsistencyTask>("verifyToolchainConsistency") {
    group = "verification"
    description = "Checks that local and CI builds use the same pinned Java/Kotlin toolchain."
    expectedJavaMajor.set(architectureLibs.findVersion("java").get().requiredVersion.toInt())
    expectedKotlinVersion.set(architectureLibs.findVersion("kotlin").get().requiredVersion)
    buildScript.set(architectureSource.file("build.gradle.kts"))
    versionCatalog.set(architectureSource.file("gradle/libs.versions.toml"))
}

tasks.register<VerifyCommonJvmSeparationTask>("verifyCommonJvmSeparation") {
    group = "verification"
    description = "Prevents common code from importing JVM-only infrastructure."
    sourceFiles.from(fileTree(architectureSource.dir("src/commonMain/kotlin")) { include("**/*.kt") })
}

tasks.register<VerifyRouteDependencyOwnershipTask>("verifyRouteDependencyOwnership") {
    group = "verification"
    description = "Ensures route code uses only declared dependency ports."
    sourceFiles.from(fileTree(architectureSource.dir("src/jvmMain/kotlin/f95gm/server/routes")) { include("**/*.kt") })
}

tasks.register<VerifyRouteApplicationDependenciesTask>("verifyRouteApplicationDependencies") {
    group = "verification"
    description = "Prevents route code from depending directly on application services or runtime context."
    sourceFiles.from(fileTree(architectureSource.dir("src/jvmMain/kotlin/f95gm/server/routes")) { include("**/*.kt") })
}

tasks.register<VerifyApplicationPortPurityTask>("verifyApplicationPortPurity") {
    group = "verification"
    description = "Prevents application ports from importing concrete implementation layers."
    sourceFiles.from(fileTree(architectureSource.dir("src/jvmMain/kotlin/f95gm/server/application/port")) { include("**/*.kt") })
}

tasks.register<VerifySystemPropertyAccessTask>("verifySystemPropertyAccess") {
    group = "verification"
    description = "Keeps process configuration access behind the typed configuration boundary."
    sourceFiles.from(fileTree(architectureSource.dir("src/jvmMain/kotlin")) { include("**/*.kt") })
}

tasks.register<VerifyServiceFactoryUsageTask>("verifyServiceFactoryUsage") {
    group = "verification"
    description = "Prevents direct infrastructure construction outside the composition factories."
    sourceFiles.from(fileTree(architectureSource.dir("src/jvmMain/kotlin/f95gm/server")) { include("**/*.kt") })
}

tasks.register<GenerateApiFixtureManifestTask>("generateApiFixtureManifest") {
    group = "verification"
    description = "Generates a reviewable hash manifest for API response fixtures."
    fixtureDirectory.set(architectureSource.dir("src/jvmTest/resources/fixtures"))
    manifest.set(layout.buildDirectory.file("reports/api-fixtures/manifest.txt"))
}

tasks.register<VerifyExceptionBoundariesTask>("verifyExceptionBoundaries") {
    group = "verification"
    description = "Restricts broad exception catches to documented infrastructure boundaries."
    sourceFiles.from(fileTree(architectureSource.dir("src/jvmMain/kotlin")) { include("**/*.kt") })
}

tasks.register<VerifyTypedObservabilityTask>("verifyTypedObservability") {
    group = "verification"
    description = "Prevents untyped structured-log field names from bypassing the event schema."
    sourceFiles.from(fileTree(architectureSource.dir("src/jvmMain/kotlin")) { include("**/*.kt") })
}

tasks.register<VerifyTypedBoundaryTask>("verifyTypedBoundaries") {
    group = "verification"
    description = "Prevents irregular JSON and raw transport values from crossing typed boundaries."
    sourceFiles.from(fileTree(architectureSource.dir("src")) {
        include("**/*Main/**/*.kt")
    })
}
