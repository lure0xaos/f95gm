package f95gm.server.kernel

/** Typed ownership boundary for failures crossing infrastructure layers. */
internal enum class FailureBoundary(val wireName: String) {
    UPSTREAM("upstream"),
    PERSISTENCE("persistence"),
    CACHE("cache"),
    LIFECYCLE("lifecycle")
}
