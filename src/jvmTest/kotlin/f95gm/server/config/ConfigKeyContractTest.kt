package f95gm.server.config

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class ConfigKeyContractTest {
    @Test
    fun everyKeyHasUniqueNameAndStableDefault() {
        assertEquals(Keys.all.size, Keys.all.map { it.name }.toSet().size)
        assertTrue(Keys.all.all { it.name.startsWith("f95gm.") || it.name == "database.url" })
        assertTrue(Keys.HOST.default.isNotBlank())
        assertTrue(Keys.DEPLOYMENT_MODE.default.isNotBlank())
    }

    @Test
    fun propertyReaderUsesDeclaredDefaults() {
        val properties = ConfigProperties { _, default -> default }
        assertEquals(Keys.HOST.default, properties.text(Keys.HOST))
        assertEquals(Keys.PORT.default, properties.port(Keys.PORT))
        assertEquals(Keys.REQUEST_TIMEOUT.default, properties.positiveDuration(Keys.REQUEST_TIMEOUT))
        assertEquals(Keys.MAX_SESSIONS.default, properties.positiveInt(Keys.MAX_SESSIONS))
        assertEquals(Keys.OPEN_BROWSER.default, properties.boolean(Keys.OPEN_BROWSER))
        assertEquals(DeploymentMode.SINGLE_INSTANCE, properties.deploymentMode(Keys.DEPLOYMENT_MODE))
        properties.throwIfInvalid()
    }

    @Test
    fun upstreamBaseUrlIsValidatedAndStoredInTypedConfig() {
        val config = ServerConfig.from(mapOf("f95gm.upstreamBaseUrl" to "http://127.0.0.1:18080/"))
        assertEquals("http://127.0.0.1:18080", config.upstreamBaseUrl)
        assertEquals(config.upstreamBaseUrl, config.upstreamEndpoints.baseUrl.value)
    }

    @Test
    fun configurationPrecedenceAndProvenanceAreExplicit() {
        val propertyWins = ServerConfigParser.parse(
            values = mapOf("f95gm.port" to "9100"),
            environment = mapOf("F95GM_PORT" to "9200")
        )
        assertEquals(9100, propertyWins.port.value)
        assertEquals(
            ConfigurationValueSource.SYSTEM_PROPERTY,
            propertyWins.configurationDiagnostics[Keys.PORT]
        )

        val environmentWins = ServerConfigParser.parse(
            values = emptyMap(),
            environment = mapOf("F95GM_PORT" to "9200")
        )
        assertEquals(9200, environmentWins.port.value)
        assertEquals(
            ConfigurationValueSource.ENVIRONMENT,
            environmentWins.configurationDiagnostics[Keys.PORT]
        )

        val defaultWins = ServerConfigParser.parse(emptyMap(), emptyMap())
        assertEquals(Keys.PORT.default, defaultWins.port.value)
        assertEquals(
            ConfigurationValueSource.DEFAULT,
            defaultWins.configurationDiagnostics[Keys.PORT]
        )
    }

    @Test
    fun invalidInputsFallBackAreReportedAndCannotBeReadAfterSnapshot() {
        val supplied = mapOf(
            Keys.PORT.name to "65536",
            Keys.REQUEST_TIMEOUT.name to "-1",
            Keys.MAX_SESSIONS.name to "not-a-number",
            Keys.OPEN_BROWSER.name to "yes",
            Keys.DEPLOYMENT_MODE.name to "not-a-mode"
        )
        val properties = ConfigProperties { key, default -> supplied[key.name] ?: default }

        assertEquals(Keys.PORT.default, properties.port(Keys.PORT))
        assertEquals(Keys.REQUEST_TIMEOUT.default, properties.positiveDuration(Keys.REQUEST_TIMEOUT))
        assertEquals(Keys.MAX_SESSIONS.default, properties.positiveInt(Keys.MAX_SESSIONS))
        assertEquals(Keys.OPEN_BROWSER.default, properties.boolean(Keys.OPEN_BROWSER))
        assertEquals(DeploymentMode.SINGLE_INSTANCE, properties.deploymentMode(Keys.DEPLOYMENT_MODE))
        assertEquals(5, properties.snapshot().issues.size)
        assertFailsWith<ServerConfigurationException> { properties.throwIfInvalid() }
        assertFailsWith<IllegalStateException> { properties.text(Keys.HOST) }
    }
}
