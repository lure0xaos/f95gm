package f95gm.server.http.request

import f95gm.server.http.serialization.upstreamJson
import f95gm.server.application.port.IdempotencyFingerprint
import kotlinx.serialization.json.*
import java.security.MessageDigest

/** Owns canonical request fingerprints so idempotency semantics are independent of HTTP handlers. */
internal object IdempotencyFingerprintService {
    fun digest(raw: String): IdempotencyFingerprint = IdempotencyFingerprint(sha256(canonical(raw)))

    private fun canonical(raw: String): String = parseJsonElementOrNull(raw)
        ?.let(::canonicalize)
        ?.toString()
        ?: raw.trim()

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.encodeToByteArray())
        .joinToString("") { "%02x".format(it) }

    private fun canonicalize(element: JsonElement): JsonElement = when (element) {
        is JsonObject -> buildJsonObject {
            element.keys.sorted().forEach { key -> put(key, canonicalize(element.getValue(key))) }
        }
        is JsonArray -> buildJsonArray { element.forEach { add(canonicalize(it)) } }
        is JsonPrimitive -> element
    }
}
