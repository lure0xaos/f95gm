package f95gm.server.config

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
        assertEquals(Keys.PORT.default.toInt(), defaultWins.port.value)
        assertEquals(
            ConfigurationValueSource.DEFAULT,
            defaultWins.configurationDiagnostics[Keys.PORT]
        )
    }
}
