package f95gm.client.state.runtime

internal interface BrowserStorageRuntime {
    val localStorage: BrowserStoragePort
    val sessionStorage: BrowserStoragePort
}
