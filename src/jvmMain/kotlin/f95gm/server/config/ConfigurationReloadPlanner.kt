package f95gm.server.config

/** Pure reload plan calculated before the state lock mutates the active snapshot. */
internal data class ConfigurationReloadPlan(
    val previous: ServerConfigSnapshot,
    val next: ServerConfigSnapshot,
    val decision: ConfigurationReloadDecision,
    val changes: ConfigurationChangeSet
) {
    val changed: Boolean get() = !changes.isEmpty
    val unchanged: Boolean get() = !changed
}

internal object ConfigurationReloadPlanner {
    fun plan(previous: ServerConfigSnapshot, parsedNext: ServerConfigSnapshot): ConfigurationReloadPlan {
        val next = parsedNext.withRevision(previous.revision + 1)
        val changes = ConfigurationChangeSet(ConfigurationChangeDetector.fields(previous.value, next.value))
        return ConfigurationReloadPlan(
            previous = previous,
            next = next,
            decision = ConfigurationReloadPolicy.decide(changes),
            changes = changes
        )
    }
}
