@file:Suppress("MatchingDeclarationName") // The lease implementation belongs with the mutation entry-point contract.

package f95gm.server.routes.ports

import f95gm.messages.UiMessages
import f95gm.server.observability.*
import f95gm.server.http.request.IdempotencyFingerprintService
import f95gm.server.http.request.currentRequestIdValue
import f95gm.server.http.response.ServerMessageKey
import f95gm.server.http.response.respondApiFailure
import f95gm.server.http.response.respondCached
import f95gm.server.application.port.IdempotencyApplicationPort
import f95gm.server.application.port.IdempotencyDecision
import f95gm.server.application.port.IdempotencyRejectionReason
import f95gm.server.application.port.IdempotencyKey
import f95gm.server.application.port.IdempotencyFingerprint
import f95gm.server.application.port.CachedResponse
import f95gm.server.application.IdempotencyRegistry
import io.ktor.server.application.*
import f95gm.server.kernel.LifecycleResource

internal class IdempotencyLease private constructor(
    private val service: IdempotencyApplicationPort?,
    private val key: IdempotencyKey?,
    private val fingerprint: IdempotencyFingerprint?
) : LifecycleResource {
    private var completed = false

    fun complete(response: CachedResponse) {
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
    override val isClosed: Boolean get() = completed

    companion object {
        fun withoutKey() = IdempotencyLease(null, null, null)
        fun claimed(service: IdempotencyApplicationPort, key: IdempotencyKey, fingerprint: IdempotencyFingerprint) =
            IdempotencyLease(service, key, fingerprint)
    }
}

internal suspend fun ApplicationCall.beginIdempotentMutation(
    key: String?,
    fingerprint: String = ""
): IdempotencyLease? {
    val digest = IdempotencyFingerprintService.digest(fingerprint)
    val typedKey = key?.let(::IdempotencyKey)
    val mutations = applicationDependencies.applicationServices.mutations
    return when (val decision = mutations.begin(typedKey, digest)) {
        IdempotencyDecision.NotRequired -> IdempotencyLease.withoutKey()
        is IdempotencyDecision.Replay -> {
            respondCached(this, decision.response)
            null
        }
        is IdempotencyDecision.Rejected -> {
            val fingerprintMismatch = decision.reason == IdempotencyRejectionReason.FINGERPRINT_MISMATCH
            val messageKey = if (fingerprintMismatch) ServerMessageKey.IDEMPOTENCY_KEY_REUSED else ServerMessageKey.MUTATION_IN_PROGRESS
            val message = if (fingerprintMismatch) UiMessages.server_idempotencyKeyReused() else UiMessages.server_mutationInProgress()
            logger.info {
            auditLog(
                AuditEvent(
                    AuditEventName.IDEMPOTENCY_REJECTED, listOf(
                        logField(StructuredLogKeys.REQUEST_ID, currentRequestIdValue()),
                        logField(StructuredLogKeys.REASON, messageKey.value),
                        logField(StructuredLogKeys.OUTCOME, "rejected")
                    )
                )
            )
            }
            respondApiFailure(this, f95gm.server.config.constants.HttpStatusCatalog.CONFLICT, message, messageKey)
            null
        }
        is IdempotencyDecision.Claimed ->
            IdempotencyLease.claimed(mutations, decision.key, decision.fingerprint)
    }
}

