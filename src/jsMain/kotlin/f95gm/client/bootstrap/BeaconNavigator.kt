package f95gm.client.bootstrap

internal external interface BeaconNavigator {
    fun sendBeacon(url: String): Boolean
}
