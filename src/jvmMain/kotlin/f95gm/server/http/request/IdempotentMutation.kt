package f95gm.server.http.request

import f95gm.messages.UiMessages
import f95gm.server.config.constants.ServerConstants
import f95gm.server.config.logging.*
import f95gm.server.http.response.ServerMessageKeys
import f95gm.server.http.response.respondApiFailure
import f95gm.server.http.response.respondCached
import f95gm.server.state.runtime.IdempotencyRegistry
import f95gm.server.state.runtime.routeDependencies
import f95gm.server.state.runtime.upstreamJson
import io.ktor.server.application.*
import kotlinx.serialization.json.*
import java.security.MessageDigest

internal class IdempotencyLease private constructor(
    private val service: MutationService?,
    private val key: String?,
    private val fingerprint: String?
) : AutoCloseable {
    private var completed = false

    fun complete(response: IdempotencyRegistry.CachedResponse) {
        if (!completed) {
            if (service != null && key != null && fingerprint != null) service.complete(key, fingerprint, response)
            completed = true
        }
    }

    fun release() {
        if (!completed) {
            if (service != null && key != null) service.release(key)
            completed = true
        }
    }

    override fun close() = release()

    companion object {
        fun withoutKey() = IdempotencyLease(null, null, null)
        fun claimed(service: MutationService, key: String, fingerprint: String) =
            IdempotencyLease(service, key, fingerprint)
    }
}

internal suspend fun ApplicationCall.beginIdempotentMutation(
    key: String?,
    fingerprint: String = ""
): IdempotencyLease? {
    if (key == null) return IdempotencyLease.withoutKey()
    val digest = fingerprint.canonicalFingerprint().sha256()
    val mutations = routeDependencies.applicationServices.mutations
    mutations.lookup(key, digest)?.let {
        respondCached(this, it)
        return null
    }
    if (!mutations.claim(key, digest)) {
        val messageKey = if (mutations.hasFingerprintMismatch(key, digest)) {
            ServerMessageKeys.IDEMPOTENCY_KEY_REUSED
        } else {
            ServerMessageKeys.MUTATION_IN_PROGRESS
        }
        val message = if (routeDependencies.applicationServices.mutations.hasFingerprintMismatch(key, digest)) {
            UiMessages.server_idempotencyKeyReused()
        } else {
            UiMessages.server_mutationInProgress()
        }
        logger.info {
            auditLog(
                AuditEvent(
                    AuditEventName.IDEMPOTENCY_REJECTED, listOf(
                        logField("requestId", currentRequestContext()?.requestId?.value),
                        logField("reason", messageKey.value),
                        logField("outcome", "rejected")
                    )
                )
            )
        }
        respondApiFailure(this, ServerConstants.STATUS_CONFLICT, message, messageKey)
        return null
    }
    return IdempotencyLease.claimed(mutations, key, digest)
}

private fun String.sha256(): String = MessageDigest.getInstance("SHA-256")
    .digest(encodeToByteArray())
    .joinToString("") { "%02x".format(it) }

private fun String.canonicalFingerprint(): String = runCatching {
    canonicalize(upstreamJson.parseToJsonElement(this)).toString()
}.getOrDefault(this.trim())

private fun canonicalize(element: JsonElement): JsonElement = when (element) {
    is JsonObject -> buildJsonObject {
        element.keys.sorted().forEach { key -> put(key, canonicalize(element.getValue(key))) }
    }

    is JsonArray -> buildJsonArray { element.forEach { add(canonicalize(it)) } }
    is JsonPrimitive -> element
}
