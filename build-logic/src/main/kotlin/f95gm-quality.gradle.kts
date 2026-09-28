import org.gradle.api.file.FileCollection
import f95gm.build.VerifyKotlinStyleTask
import f95gm.build.VerifyOpenApiSnapshotTask
import f95gm.build.VerifyTypedContractSurfaceTask
import f95gm.build.VerifyUpstreamBoundariesTask
import f95gm.build.VerificationTask
import f95gm.build.VerifyPackageCyclesTask
import f95gm.build.VerifyPrimitiveBoundaryTask
import f95gm.build.VerifySensitiveLoggingTask
import f95gm.build.VerifyClockAccessTask

val sourceRoots = listOf(
    layout.projectDirectory.dir("src").asFile,
    layout.projectDirectory.dir("packaging").asFile,
    layout.projectDirectory.dir("build-logic/src").asFile
)

val styleSourceFiles: FileCollection = files(sourceRoots.map { root ->
    fileTree(root) { include("**/*.kt", "**/*.kts") }
}, fileTree(layout.projectDirectory) {
    include("*.gradle.kts", "settings.gradle.kts", "build-logic/**/*.kt", "build-logic/**/*.kts")
    exclude("build-logic/.gradle/**", "build-logic/build/**")
})

tasks.register<VerifyKotlinStyleTask>("verifyKotlinStyle") {
    group = "verification"
    description = "Checks Kotlin sources, build logic, and Gradle Kotlin scripts for basic style issues."
    sourceFiles.from(styleSourceFiles)
}

tasks.register<VerifyOpenApiSnapshotTask>("verifyOpenApiSnapshot") {
    group = "verification"
    description = "Rejects breaking changes to the previous OpenAPI operation and schema contract."
    dependsOn("generateApiContract")
    baseline.set(layout.projectDirectory.file("api/f95gm-openapi-previous.json"))
    generated.set(layout.buildDirectory.file("generated/api/f95gm-openapi.json"))
}

tasks.register<VerifyUpstreamBoundariesTask>("verifyUpstreamBoundaries") {
    group = "verification"
    description = "Prevents route and application layers from constructing upstream transport requests directly."
    val protectedRoots = listOf(
        layout.projectDirectory.dir("src/jvmMain/kotlin/f95gm/server/routes"),
        layout.projectDirectory.dir("src/jvmMain/kotlin/f95gm/server/state/runtime")
    )
    protectedSources.from(protectedRoots.map { fileTree(it) { include("**/*.kt") } })
}

tasks.register<VerifyTypedContractSurfaceTask>("verifyTypedContractSurface") {
    group = "verification"
    description = "Checks generated API operations for request/response schema metadata."
    dependsOn("generateFrontendApiClient")
    generated.set(layout.buildDirectory.file(
        "generated/frontend/f95gm/client/data/api/GeneratedF95ApiOperations.kt"
    ))
}

tasks.register<VerifyPackageCyclesTask>("verifyPackageCycles") {
    group = "verification"
    description = "Rejects dependency cycles between server packages."
    sourceFiles.from(fileTree(layout.projectDirectory.dir("src/jvmMain/kotlin/f95gm/server")) { include("**/*.kt") })
    baseline.set(layout.projectDirectory.file("config/package-cycles.baseline"))
    report.set(layout.buildDirectory.file("reports/architecture/package-cycles.txt"))
}

tasks.register<VerifyPrimitiveBoundaryTask>("verifyPrimitiveBoundaries") {
    group = "verification"
    description = "Rejects primitive timestamps at persistence and server-model boundaries."
    sourceFiles.from(
        fileTree(layout.projectDirectory.dir("src/jvmMain/kotlin/f95gm/server/model")) { include("**/*.kt") },
        fileTree(layout.projectDirectory.dir("src/jvmMain/kotlin/f95gm/server/persistence")) { include("**/*.kt") }
    )
}

tasks.register<VerifySensitiveLoggingTask>("verifySensitiveLogging") {
    sourceFiles.from(fileTree(layout.projectDirectory.dir("src")) {
        include("**/*.kt")
    })
}

tasks.register<VerifyClockAccessTask>("verifyClockAccess") {
    group = "verification"
    description = "Rejects direct system clock access outside the ServerClock boundary."
    sourceFiles.from(fileTree(layout.projectDirectory.dir("src/jvmMain/kotlin")) { include("**/*.kt") })
}

/** Single quality-gate dependency bundle shared by local check and CI. */
tasks.register<VerificationTask>("verifyArchitecture") {
    group = "verification"
    description = "Runs the repository architecture, contract, style, test, coverage, and packaging checks."
    dependsOn(
        gradle.includedBuild("build-logic").task(":test"),
        "verifyPackagingAssets",
        "verifyFrontendBudgets",
        "verifyKotlinStyle",
        "dependencyPolicy",
        "verifyLocalizationKeys",
        "testQualityGate",
        "verifyApiContract",
        "verifyReproducibleGeneratedArtifacts",
        "verifyPackagingReproducibility",
        "verifyPackagingArchiveReproducibility",
        "verifyOpenApiSnapshot",
        "verifyTypedContractSurface",
        "verifySourceConventions",
        "verifyLayerBoundaries",
        "verifyServerDependencyGraph",
        "verifyJavaRuntime",
        "verifyEnvironment",
        "verifyToolchainConsistency",
        "verifyCommonJvmSeparation",
        "verifyRouteDependencyOwnership",
        "verifyRouteApplicationDependencies",
        "verifyApplicationPortPurity",
        "verifySystemPropertyAccess",
        "verifyServiceFactoryUsage",
        "generateApiFixtureManifest",
        "verifyClientGlobalUsage",
        "verifyExceptionBoundaries",
        "verifyTypedObservability",
        "verifyTypedBoundaries",
        "detekt",
        "verifyApiBoundaryConventions",
        "verifyPrimitiveBoundaries",
        "verifyClockAccess",
        "verifyTypedClientArchitecture",
        "verifyUpstreamBoundaries",
        "verifyPackageCycles",
        "jvmCoverageCheck"
    )
}
