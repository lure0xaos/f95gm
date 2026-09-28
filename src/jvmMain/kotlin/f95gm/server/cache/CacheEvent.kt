package f95gm.server.cache

internal sealed interface CacheEvent {
    data class Evicted(val namespace: CacheNamespace, val expired: Boolean) : CacheEvent
}
