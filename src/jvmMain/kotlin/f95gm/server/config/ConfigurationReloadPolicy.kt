package f95gm.server.config

/** Pure decision logic for deciding whether a snapshot can be applied live. */
internal object ConfigurationReloadPolicy {
    fun decide(previous: ServerConfig, next: ServerConfig): ConfigurationReloadDecision {
        return decide(ConfigurationChangeSet(ConfigurationChangeDetector.fields(previous, next)))
    }

    fun decide(changes: ConfigurationChangeSet): ConfigurationReloadDecision {
        val reasons = changes.fields.mapNotNull(ConfigurationChangeField::restartRequirement)
        return if (reasons.isEmpty()) ConfigurationReloadDecision.Apply
        else ConfigurationReloadDecision.RequiresRestart(reasons)
    }
}
