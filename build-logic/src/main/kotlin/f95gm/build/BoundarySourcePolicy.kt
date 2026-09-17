package f95gm.build

/** Exact source-set-relative identities keep exemptions independent of checkout location. */
internal fun sourcePolicyPath(path: String): String = path.replace('\\', '/').let { normalized ->
    Regex("(?:^|/)(src/(?:commonMain|jsMain|jvmMain)/kotlin/.*)$")
        .find(normalized)?.groupValues?.get(1) ?: normalized
}

internal object BoundarySourcePolicy {
    private val jsonBoundaries = setOf(
        "src/commonMain/kotlin/f95gm/client/data/api/ClientPayloads.kt",
        "src/jsMain/kotlin/f95gm/client/data/api/ClientPayloadAdapters.kt",
        "src/commonMain/kotlin/f95gm/domain/api/ApiContractDtos.kt",
        "src/jvmMain/kotlin/f95gm/server/http/request/DecodeJson.kt",
        "src/jsMain/kotlin/f95gm/client/data/json/JsonAccess.kt",
        "src/commonMain/kotlin/f95gm/domain/json/JsonScalarAdapters.kt",
        "src/jvmMain/kotlin/f95gm/server/http/request/IdempotencyFingerprintService.kt",
        "src/jvmMain/kotlin/f95gm/server/routes/marks/live/UpstreamCatalogPayload.kt",
        "src/jvmMain/kotlin/f95gm/server/routes/marks/live/UpstreamCatalogItemValues.kt",
        "src/jsMain/kotlin/f95gm/client/data/catalog/CatalogOptionsPayload.kt",
        "src/jvmMain/kotlin/f95gm/server/model/catalog/CatalogOptionsResponse.kt"
    )
    private val exceptionBoundaries = setOf(
        "src/jvmMain/kotlin/f95gm/server/app/ConfigureRequestMonitoring.kt",
        "src/jvmMain/kotlin/f95gm/server/desktop/ServerShutdownController.kt",
        "src/jvmMain/kotlin/f95gm/server/config/ServerConfigurationState.kt",
        "src/jvmMain/kotlin/f95gm/server/observability/FailureReporter.kt",
        "src/jvmMain/kotlin/f95gm/server/persistence/database/DatabaseMigrations.kt",
        "src/jvmMain/kotlin/f95gm/server/persistence/database/DbQuery.kt",
        "src/jvmMain/kotlin/f95gm/server/persistence/database/InitDatabase.kt",
        "src/jvmMain/kotlin/f95gm/server/kernel/RetryExecutor.kt",
        "src/jvmMain/kotlin/f95gm/server/app/RequestMonitoringInterceptor.kt",
        "src/jvmMain/kotlin/f95gm/server/kernel/SingleFlight.kt",
        "src/jvmMain/kotlin/f95gm/server/kernel/ObserverFailureBoundary.kt",
        "src/jvmMain/kotlin/f95gm/server/state/runtime/CloseSafely.kt",
        "src/jvmMain/kotlin/f95gm/server/state/runtime/FailureCleanup.kt",
        "src/jvmMain/kotlin/f95gm/server/app/ServerRuntimeState.kt",
        "src/jvmMain/kotlin/f95gm/server/state/runtime/ServerLifecycle.kt",
        "src/jvmMain/kotlin/f95gm/server/state/runtime/ServerResources.kt",
        "src/jvmMain/kotlin/f95gm/server/state/runtime/StartupState.kt",
        "src/jvmMain/kotlin/f95gm/server/app/StartServer.kt",
        "src/jvmMain/kotlin/f95gm/server/config/ConfigurationReloadPublisher.kt",
        "src/jvmMain/kotlin/f95gm/server/config/ConfigurationPublicationDispatcher.kt",
        "src/jvmMain/kotlin/f95gm/server/cache/CacheLoadFailureBoundary.kt",
        "src/jvmMain/kotlin/f95gm/server/persistence/database/DatabaseOperationRunner.kt",
        "src/jvmMain/kotlin/f95gm/server/desktop/ServerShutdownDrain.kt",
        "src/jvmMain/kotlin/f95gm/server/kernel/CloseGuard.kt",
        "src/jvmMain/kotlin/f95gm/server/state/runtime/ServerRuntimeServices.kt",
        "src/jvmMain/kotlin/f95gm/server/routes/marks/live/LoadLiveMarks.kt"
    )
    private val broadCatch = Regex("catch\\s*\\([^)]*:\\s*(?:kotlin\\.)?(Exception|RuntimeException|Throwable)\\s*\\)")

    fun isJsonBoundary(path: String): Boolean = sourcePolicyPath(path) in jsonBoundaries

    fun uiCapabilityViolations(path: String, text: String): List<String> {
        val relative = sourcePolicyPath(path)
        val code = kotlinCode(text)
        val mutableCapability = Regex("\\bClient(?:Operation|Action|\\w*Actions|\\w*State|\\w*Loading|RequestTracking|SessionLifecycle|HealthProbe)Port\\b|\\bRootStore\\b|\\bruntime\\.controller\\b")
        if (relative.startsWith("src/jsMain/kotlin/f95gm/client/ui/")) {
            return mutableCapability.findAll(code).map { "$relative: UI must use commands and read-only stores" }.toList()
        }
        if (relative != "src/jsMain/kotlin/f95gm/client/state/runtime/ClientCapabilities.kt") return emptyList()
        val inheritance = Regex("\\binterface\\s+(Client\\w+Port)\\s*:\\s*([^{}]*?)(?=\\{|\\binternal\\s+interface|$)")
        return inheritance.findAll(code).filter { match ->
            val name = match.groupValues[1]
            name.endsWith("UiPort") || name.endsWith("PagePort") || name == "ClientNavbarReloadPort"
        }.filter { mutableCapability.containsMatchIn(it.groupValues[2]) }
            .map { "$relative: ${it.groupValues[1]} inherits mutable action/state capabilities" }.toList()
    }

    fun broadCatchViolations(path: String, text: String): List<String> {
        val relative = sourcePolicyPath(path)
        val code = kotlinCode(text)
        val catches = broadCatch.findAll(code).toList()
        if (catches.isEmpty()) return emptyList()
        val documented = kotlinCodeRegions(text, Regex("@(?:file\\s*:\\s*)?Suppress\\s*"))
            .any { region ->
                val annotation = kotlinCode(text.substring(region.start, region.endExclusive), preserveLiterals = true)
                Regex("\"TooGenericExceptionCaught\"").containsMatchIn(annotation)
            }
        if (relative in exceptionBoundaries && documented) return emptyList()
        return catches.map { match ->
            val line = code.take(match.range.first).count { it == '\n' } + 1
            "$relative:$line: broad catch requires an approved path and an explicit suppression"
        }
    }
}
