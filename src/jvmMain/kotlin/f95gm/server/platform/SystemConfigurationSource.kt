package f95gm.server.platform

import f95gm.server.config.ConfigurationSource

/** Process-backed configuration source used only at the application boundary. */
internal object SystemConfigurationSource : ConfigurationSource {
    override val name: String = "system-properties"
    fun property(name: String): String? = System.getProperty(name)
    override fun properties(): Map<String, String> = System.getProperties().stringPropertyNames()
        .associateWith { name -> property(name).orEmpty() }
    override fun environment(): Map<String, String> = System.getenv()
}
