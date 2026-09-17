package f95gm.server.state.runtime

/** Maps the last completed startup stage to an actionable failure category. */
internal fun StartupStage.failureCategory(): StartupFailureCategory = when (this) {
    StartupStage.CREATED -> StartupFailureCategory.CONFIGURATION
    StartupStage.CONFIGURED -> StartupFailureCategory.DATABASE
    StartupStage.DATABASE_READY -> StartupFailureCategory.COMPOSITION
    StartupStage.COMPOSED -> StartupFailureCategory.BINDING
    else -> StartupFailureCategory.RUNTIME
}
