package f95gm.server.http.request

import kotlin.uuid.Uuid

@JvmInline
internal value class ShutdownToken(val value: String) {
    init {
        require(value.length in 8..128 && value.all { it.isLetterOrDigit() || it in "-_" })
    }
}

internal fun interface RequestIdGenerator {
    fun next(): RequestId
}

internal fun interface ShutdownTokenGenerator {
    fun next(): ShutdownToken
}

internal open class RequestIdentifierFactory(
    private val uuid: () -> Uuid = Uuid::random
) {
    val requestIds = RequestIdGenerator { RequestId(uuid().toString()) }
    val shutdownTokens = ShutdownTokenGenerator { ShutdownToken(uuid().toString()) }
}

internal object SecureRequestIdentifiers : RequestIdentifierFactory()
