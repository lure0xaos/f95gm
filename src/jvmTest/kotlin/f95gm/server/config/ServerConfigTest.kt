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
        assertEquals(9090, config.port.value)
        assertEquals("data/app", config.database)
        assertEquals("public", config.staticDirectory)
    }

    @Test
    fun readsOperationalPolicyLimitsFromConfiguration() {
        val config = ServerConfig.from(
            mapOf(
                "f95gm.maxMediaResponseBytes" to "2048",
                "f95gm.mediaDownloadBufferBytes" to "512",
                "f95gm.maxCatalogPage" to "25",
                "f95gm.maxCatalogTags" to "4",
                "f95gm.maxCatalogTextLength" to "80",
                "f95gm.maxImportedQuickLinks" to "12",
                "f95gm.maxFilterIdLength" to "48"
            )
        )

        assertEquals(2048L, config.limits.maxMediaResponseBytes.value)
        assertEquals(512, config.limits.mediaDownloadBufferBytes.value)
        assertEquals(25, config.limits.maxCatalogPage.value)
        assertEquals(4, config.limits.maxCatalogTags.value)
        assertEquals(80, config.limits.maxCatalogTextLength.value)
        assertEquals(12, config.limits.maxImportedQuickLinks.value)
        assertEquals(48, config.limits.maxFilterIdLength.value)
    }

    @Test
    fun systemPropertiesWinOverEnvironmentAndCamelCaseMapsToUnderscores() {
        val config = ServerConfig.fromSources(
            mapOf("f95gm.port" to "9090"),
            mapOf("F95GM_PORT" to "9091", "F95GM_STATIC_DIR" to "public")
        )

        assertEquals(9090, config.port.value)
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
        assertEquals("<configured>", config.redactedSummary().database)
    }

    @Test
    fun trimsAndNormalizesScalarConfigurationValues() {
        val config = ServerConfig.from(
            mapOf(
                "f95gm.port" to " 9090 ",
                "f95gm.openBrowser" to " TRUE ",
                "f95gm.deploymentMode" to " single_instance "
            )
        )

        assertEquals(9090, config.port.value)
        assertEquals(true, config.openBrowser)
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

    @Test
    fun validationRejectsAccountLimitAboveIpLimit() {
        assertFailsWith<IllegalArgumentException> {
            ServerConfig.from(
                mapOf(
                    "f95gm.apiRequestsPerMinute" to "10",
                    "f95gm.accountRequestsPerMinute" to "11"
                )
            ).validate()
        }
    }

    @Test
    fun validationReportsIndependentErrorsTogether() {
        val config = ServerConfig.from(emptyMap()).copy(
            timeouts = TimeoutConfig(connectMillis = Millis(40_000L)),
            traffic = TrafficConfig(
                accountRequestsPerMinute = RequestsPerMinute(10),
                ipRequestsPerMinute = RequestsPerMinute(5)
            ),
            upstreamRetry = UpstreamRetryConfig(baseDelayMillis = Millis(2_000L), maxDelayMillis = Millis(1_000L))
        )

        val error = assertFailsWith<ConfigurationValidationException> { config.validate() }
        assertEquals(3, error.errors.size)
    }

    @Test
    fun malformedAndOverflowingConfigurationValuesAreRejectedConsistently() {
        val malformed = listOf("", " ", "NaN", "2147483648", "-9223372036854775809", "yes")
        malformed.forEach { value ->
            assertFailsWith<IllegalStateException> {
                ServerConfig.from(mapOf("f95gm.port" to value, "f95gm.openBrowser" to value))
            }
        }
    }
}
