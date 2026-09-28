package f95gm.build

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SourceBoundaryChecksTest {
    private val limiter = "src/jvmMain/kotlin/f95gm/server/http/request/ratelimit/RequestRateLimiter.kt"
    private val feature = "src/jsMain/kotlin/f95gm/client/actions/catalog/Load.kt"

    @Test
    fun rawNowIsRejectedEvenWhenCompatibilityWindowIsPresent() {
        assertEquals(1, SourceBoundaryChecks.typedViolations(limiter,
            "fun acquire(nowMillis: Long, windowMillis: Long) = work()").size)
        assertTrue(SourceBoundaryChecks.typedViolations(limiter,
            "fun acquire(now: RateLimitNow, windowMillis: Long) = work()").isEmpty())
        assertTrue(SourceBoundaryChecks.typedViolations(limiter,
            "// nowMillis\nval example = \"nowMillis\"\nval windowMillis = 5L").isEmpty())
        assertTrue(SourceBoundaryChecks.typedViolations(limiter.replace("ratelimit", "other"),
            "val nowMillis = 1L").isEmpty())
    }

    @Test
    fun jsonExemptionsUseExactSourceSetAndAdapterPath() {
        val adapter = "src/commonMain/kotlin/f95gm/domain/api/ApiContractDtos.kt"
        val source = "import kotlinx.serialization.json.JsonElement"
        assertTrue(SourceBoundaryChecks.typedViolations(adapter, source).isEmpty())
        assertEquals(1, SourceBoundaryChecks.typedViolations(adapter.replace("commonMain", "jvmMain"), source).size)
        assertTrue(SourceBoundaryChecks.typedViolations(feature, "// $source").isEmpty())
    }

    @Test
    fun transportCallsCannotEscapeThroughWhitespaceOrMultilineFormatting() {
        listOf("transport.request()", "transport . request ()", "transport\n . request\n ()",
            "requestJsonEnvelope ()").forEach { call ->
            assertEquals(1, SourceBoundaryChecks.apiViolations(feature, call).size)
        }
        assertTrue(SourceBoundaryChecks.apiViolations(feature,
            "// transport.request()\nval example = \"requestJsonEnvelope()\"").isEmpty())
        assertTrue(SourceBoundaryChecks.apiViolations(feature.replace("actions/catalog", "data/api"),
            "transport . request ()").isEmpty())
    }

    @Test
    fun transportCallPolicyStillProtectsEveryMainSourceSet() {
        listOf("commonMain", "jvmMain", "jsMain").forEach { sourceSet ->
            val path = "src/$sourceSet/kotlin/f95gm/other/Feature.kt"
            assertEquals(1, SourceBoundaryChecks.apiViolations(path, "transport . request ()").size)
        }
        val wrongSourceSet = "src/jvmMain/kotlin/f95gm/client/data/api/Feature.kt"
        assertEquals(1, SourceBoundaryChecks.apiViolations(wrongSourceSet, "requestJsonEnvelope()").size)
    }

    @Test
    fun transportImportsHaveNarrowFactoryAndAdapterExemptions() {
        val source = "import io.ktor.client.engine.mock.MockEngine"
        assertEquals(1, SourceBoundaryChecks.apiViolations(feature, source).size)
        assertTrue(SourceBoundaryChecks.apiViolations("C:/checkout/src/jsMain/kotlin/f95gm/client/state/runtime/Http.kt",
            source).isEmpty())
        assertEquals(1, SourceBoundaryChecks.apiViolations(feature,
            "import io.ktor.client.request.*").size)
        assertTrue(SourceBoundaryChecks.apiViolations(feature, "import io.ktor.client.HttpClient").isEmpty())
        assertTrue(SourceBoundaryChecks.apiViolations(feature, "/* $source */").isEmpty())
    }

    @Test
    fun featureActionsAndCapabilitiesCannotRegainControllerAccess() {
        assertEquals(1, SourceBoundaryChecks.controllerViolations(feature, "runtime.controller.close()").size)
        assertEquals(1, SourceBoundaryChecks.controllerViolations(feature, "runtime . controller . close()").size)
        assertTrue(SourceBoundaryChecks.controllerViolations(feature, "runtime.requests.cancelJob(key)").isEmpty())
        assertTrue(SourceBoundaryChecks.controllerViolations(feature, "// runtime.controller.close()").isEmpty())
        assertEquals(1, SourceBoundaryChecks.controllerViolations(
            "src/jsMain/kotlin/f95gm/client/state/runtime/ClientCapabilities.kt",
            "interface ClientOperationPort { val owner: ClientController }").size)
    }
}
