package f95gm.build

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BoundarySourcePolicyTest {
    @Test
    fun uiCannotRegainMutableCapabilitiesThroughInheritance() {
        val path = "src/jsMain/kotlin/f95gm/client/state/runtime/ClientCapabilities.kt"
        assertEquals(1, BoundarySourcePolicy.uiCapabilityViolations(path,
            "internal interface ClientLoginUiPort : ClientAuthActionsPort { }").size)
        assertTrue(BoundarySourcePolicy.uiCapabilityViolations(path,
            "internal interface ClientLoginUiPort { val commands: ClientLoginCommands }").isEmpty())
        val ui = "src/jsMain/kotlin/f95gm/client/ui/Login.kt"
        assertEquals(1, BoundarySourcePolicy.uiCapabilityViolations(ui, "runtime.controller.close()").size)
        assertTrue(BoundarySourcePolicy.uiCapabilityViolations(ui, "// runtime.controller.close()").isEmpty())
    }
    @Test
    fun uiCannotInheritOrConsumeMutableLifecyclePorts() {
        val capabilities = "src/jsMain/kotlin/f95gm/client/state/runtime/ClientCapabilities.kt"
        listOf("ClientRequestTrackingPort", "ClientFeatureLoadingPort", "ClientCatalogLoadingPort",
            "ClientFiltersLoadingPort", "ClientDetailLoadingPort", "ClientSessionLifecyclePort", "ClientHealthProbePort"
        ).forEach { port ->
            assertEquals(1, BoundarySourcePolicy.uiCapabilityViolations(capabilities,
                "internal interface ClientCatalogUiPort : $port { }").size)
            assertEquals(1, BoundarySourcePolicy.uiCapabilityViolations(
                "src/jsMain/kotlin/f95gm/client/ui/Feature.kt", "fun render(port: $port) {}").size)
        }
    }

    @Test
    fun wireExemptionsRequireTheExactDirectoryAndSourceSet() {
        assertTrue(BoundarySourcePolicy.isJsonBoundary("C:/checkout/src/commonMain/kotlin/f95gm/domain/api/ApiContractDtos.kt"))
        assertFalse(BoundarySourcePolicy.isJsonBoundary("C:/checkout/src/jvmMain/kotlin/f95gm/domain/api/ApiContractDtos.kt"))
        assertFalse(BoundarySourcePolicy.isJsonBoundary("src/commonMain/kotlin/f95gm/other/ApiContractDtos.kt"))
        assertEquals(
            "src/jvmMain/kotlin/f95gm/server/kernel/CloseGuard.kt",
            sourcePolicyPath("C:\\checkout\\src\\jvmMain\\kotlin\\f95gm\\server\\kernel\\CloseGuard.kt")
        )
    }

    @Test
    fun commentsAndLiteralsDoNotTriggerCodeChecks() {
        val text = """
            // System.nanoTime(); catch (failure: Throwable) {}
            /* import f95gm.server.persistence.Secret */
            val example = "System.getenv(); catch (failure: Exception) {}"
            import f95gm.domain.values.F95ThreadId
        """.trimIndent()
        assertTrue(BoundarySourcePolicy.broadCatchViolations("src/jvmMain/kotlin/f95gm/Feature.kt", text).isEmpty())
        assertFalse(kotlinCode(text).contains("System."))
        assertEquals(setOf("f95gm.domain.values.F95ThreadId"), kotlinImports(text))
    }

    @Test
    fun allThreeBroadCatchTypesRequireAnApprovedBoundary() {
        listOf("Exception", "RuntimeException", "Throwable", "kotlin.Throwable").forEach { type ->
            val text = "try { work() } catch (failure: $type) {}"
            assertEquals(1, BoundarySourcePolicy.broadCatchViolations("src/jvmMain/kotlin/f95gm/Feature.kt", text).size)
        }
    }

    @Test
    fun suppressionRequiresExecutableAnnotationAndExactBoundaryPath() {
        val path = "src/jvmMain/kotlin/f95gm/server/kernel/CloseGuard.kt"
        val catch = "try { work() } catch (failure: Throwable) {}"
        val suppression = "@Suppress(\"TooGenericExceptionCaught\")"
        assertTrue(BoundarySourcePolicy.broadCatchViolations(path, "$suppression\n$catch").isEmpty())
        assertTrue(BoundarySourcePolicy.broadCatchViolations(path, "@file:Suppress(\"TooGenericExceptionCaught\")\n$catch").isEmpty())
        assertEquals(1, BoundarySourcePolicy.broadCatchViolations(path, "// $suppression\n$catch").size)
        assertEquals(1, BoundarySourcePolicy.broadCatchViolations(path,
            "@Suppress(/* \"TooGenericExceptionCaught\" */ \"OtherRule\")\n$catch").size)
        assertEquals(1, BoundarySourcePolicy.broadCatchViolations(path.replace("kernel", "other"), "$suppression\n$catch").size)
    }

    @Test
    fun liveMarkBatchFailureBoundaryRequiresSuppressionAtItsExactPath() {
        val path = "src/jvmMain/kotlin/f95gm/server/routes/marks/live/LoadLiveMarks.kt"
        val catch = "try { load() } catch (failure: Exception) {}"
        val suppression = "@Suppress(\"TooGenericExceptionCaught\")"

        assertTrue(BoundarySourcePolicy.broadCatchViolations(path, "$suppression\n$catch").isEmpty())
        assertEquals(1, BoundarySourcePolicy.broadCatchViolations(path.replace("LoadLiveMarks", "Other"), "$suppression\n$catch").size)
    }
}
