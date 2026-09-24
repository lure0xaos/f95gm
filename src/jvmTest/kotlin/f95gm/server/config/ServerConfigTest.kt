package f95gm.server.config

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ServerConfigTest {
    @Test
    fun rejectsPortsOutsideTheValidRange() {
        assertFailsWith<IllegalStateException> {
            ServerConfig.from(mapOf("f95gm.port" to "70000"))
        }
    }

    @Test
    fun readsExplicitConfigurationValues() {
        val config = ServerConfig.from(
            mapOf(
                "f95gm.host" to "0.0.0.0",
                "f95gm.port" to "9090",
                "f95gm.database" to "data/app",
                "f95gm.staticDir" to "public"
            )
        )

        assertEquals("0.0.0.0", config.host)
        assertEquals(9090, config.port)
        assertEquals("data/app", config.database)
        assertEquals("public", config.staticDirectory)
    }

    @Test
    fun systemPropertiesWinOverEnvironmentAndCamelCaseMapsToUnderscores() {
        val config = ServerConfig.fromSources(
            mapOf("f95gm.port" to "9090"),
            mapOf("F95GM_PORT" to "9091", "F95GM_STATIC_DIR" to "public")
        )

        assertEquals(9090, config.port)
        assertEquals("public", config.staticDirectory)
    }

    @Test
    fun rejectsUnknownDeploymentModes() {
        assertFailsWith<IllegalStateException> {
            ServerConfig.from(mapOf("f95gm.deploymentMode" to "MULTI_INSTANCE"))
        }
    }

    @Test
    fun parsesBooleanConfigurationAndRedactsOperationalSummary() {
        val config = ServerConfig.from(
            mapOf("f95gm.openBrowser" to "false", "f95gm.database" to "C:/private/session-db")
        )

        assertEquals(false, config.openBrowser)
        assertEquals("<configured>", config.redactedSummary()["database"])
    }

    @Test
    fun rejectsNonPositiveNumericConfigurationValues() {
        assertFailsWith<IllegalStateException> {
            ServerConfig.from(mapOf("f95gm.maxSessions" to "0"))
        }
        assertFailsWith<IllegalStateException> {
            ServerConfig.from(mapOf("f95gm.requestTimeoutMillis" to "-1"))
        }
    }

    @Test
    fun validationRejectsInconsistentTimeouts() {
        val config = ServerConfig.from(
            mapOf(
                "f95gm.requestTimeoutMillis" to "1000",
                "f95gm.connectTimeoutMillis" to "2000"
            )
        )

        assertFailsWith<IllegalArgumentException> { config.validate() }
    }

    @Test
    fun treatsWhitespaceOnlyStaticDirectoryAsAbsent() {
        val config = ServerConfig.from(mapOf("f95gm.staticDir" to "  "))

        assertEquals(null, config.staticDirectory)
    }

    @Test
    fun validationRejectsInvalidTrafficAndResourceLimits() {
        assertFailsWith<IllegalStateException> {
            ServerConfig.from(mapOf("f95gm.apiRequestsPerMinute" to "0"))
        }
        assertFailsWith<IllegalStateException> {
            ServerConfig.from(mapOf("f95gm.maxRateLimitBuckets" to "0"))
        }
        assertFailsWith<IllegalStateException> {
            ServerConfig.from(mapOf("f95gm.maxRequestBodyBytes" to "0"))
        }
    }

    @Test
    fun validationRejectsRetryRangeViolations() {
        assertFailsWith<IllegalStateException> {
            ServerConfig.from(mapOf("f95gm.retryMaxAttempts" to "0"))
        }
        assertFailsWith<IllegalArgumentException> {
            ServerConfig.from(
                mapOf(
                    "f95gm.retryBaseDelayMillis" to "2000",
                    "f95gm.retryMaxDelayMillis" to "1000"
                )
            ).validate()
        }
    }
}
