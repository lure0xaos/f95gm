package f95gm.server.observability

import f95gm.server.config.ConfigurationReloadEventSink
import f95gm.server.config.ConfigurationReloadResult
import f95gm.server.config.ReloadImpact
import f95gm.server.config.RestartRequirement

internal data class ConfigurationReloadedEvent(
    val impact: ReloadImpact,
    val restartReasons: List<RestartRequirement>
) : StructuredEvent {
    override val name: StructuredEventName = StructuredLogEventCatalog.eventName(StructuredLogEventCatalog.Event.CONFIGURATION_RELOADED)
    override val fields: List<StructuredLogField> = listOf(
        logField(StructuredLogKeys.IMPACT, impact.name.lowercase()),
        logField(StructuredLogKeys.RESTART_REASONS, restartReasons.joinToString(",") { it.wireName })
    )
}

internal data class ConfigurationReloadUnchangedEvent(val source: String) : StructuredEvent {
    override val name: StructuredEventName = StructuredLogEventCatalog.eventName(StructuredLogEventCatalog.Event.CONFIGURATION_RELOAD_UNCHANGED)
    override val fields: List<StructuredLogField> = listOf(logField(StructuredLogKeys.REASON, source))
}

internal object StructuredConfigurationReloadEventSink : ConfigurationReloadEventSink {
    override fun onReload(result: ConfigurationReloadResult) {
        logger.info {
            structuredLog(
                ConfigurationReloadedEvent(
                    result.impact,
                    result.restartReasons
                )
            )
        }
    }

    override fun onUnchanged(source: String) {
        logger.info { structuredLog(ConfigurationReloadUnchangedEvent(source)) }
    }
}
