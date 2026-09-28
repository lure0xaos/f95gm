package f95gm.server.kernel

internal enum class LifecycleState(val wireName: String) {
    OPEN("open"), CLOSING("closing"), COMPLETED("completed"), FAILED("failed")
}
