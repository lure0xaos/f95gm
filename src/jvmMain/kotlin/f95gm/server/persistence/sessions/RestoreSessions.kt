package f95gm.server.persistence.sessions

import f95gm.domain.values.F95AccountKey
import f95gm.domain.values.F95SessionId
import f95gm.server.config.constants.ServerStorageLimits
import f95gm.server.config.ServerDefaults
import f95gm.server.observability.*
import f95gm.server.model.auth.UpstreamSession
import f95gm.server.persistence.database.TransactionBoundary
import f95gm.server.observability.DatabaseOperation
import f95gm.server.persistence.database.DatabaseEpochMillis
import f95gm.server.persistence.schema.SessionCookiesTable
import f95gm.server.persistence.schema.SessionsTable
import f95gm.server.kernel.ServerClock
import f95gm.server.session.SessionRegistryPort
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.selectAll

internal suspend fun restoreSessions(database: TransactionBoundary, registry: SessionRegistryPort, clock: ServerClock) {
    val now = clock.epochMillis().value
    database.transactionSuspending(DatabaseOperation.SESSIONS_RESTORE) {
        SessionsTable.selectAll().forEach { row ->
            val rawId = row[SessionsTable.id]
            val accountKey = row[SessionsTable.accountKey]
            val updatedAt = DatabaseEpochMillis(row[SessionsTable.updatedAt]).toDomain()
            val validIdentity = rawId.isValidStoredValue(ServerStorageLimits.SESSION_ID_LENGTH) &&
                    accountKey.isValidStoredValue(ServerStorageLimits.ACCOUNT_KEY_LENGTH)
            if (invalidSessionTimestamp(updatedAt.value, now) || !validIdentity) {
                logger.warn {
                    auditLog(
                        AuditEvent(
                            AuditEventName.SESSION_ROW_QUARANTINED,
                            listOf(logField(StructuredLogKeys.REASON, if (!validIdentity) "invalid_identity" else "expired_or_future"))
                        )
                    )
                }
                SessionCookiesTable.deleteWhere { SessionCookiesTable.sessionId eq rawId }
                SessionsTable.deleteWhere { SessionsTable.id eq rawId }
            } else {
                val cookies = SessionCookiesTable.selectAll()
                    .where { SessionCookiesTable.sessionId eq rawId }
                    .mapNotNull { cookie ->
                        val name = cookie[SessionCookiesTable.name]
                        val value = cookie[SessionCookiesTable.value]
                        if (isSafeCookie(name, value)) name to value else null
                    }
                    .take(ServerStorageLimits.MAX_UPSTREAM_COOKIE_COUNT)
                    .associate { it }
                val id = F95SessionId(rawId)
                registry.put(
                    id, UpstreamSession.fromPersistedCookies(
                        browserId = id,
                        accountKey = F95AccountKey(accountKey),
                        cookies = cookies
                    ),
                    clock.monotonicMillisTimestamp()
                )
            }
        }
    }
}

private fun String.isValidStoredValue(maxLength: Int): Boolean =
    isNotBlank() && length <= maxLength && none { it == '\u0000' }

private fun invalidSessionTimestamp(storedTimestamp: Long, now: Long): Boolean =
    storedTimestamp <= 0L || storedTimestamp > now || now - storedTimestamp > ServerDefaults.SESSION_MAX_AGE_SECONDS * 1000L

private fun isSafeCookie(name: String, value: String): Boolean =
    name.isValidStoredValue(ServerStorageLimits.COOKIE_NAME_LENGTH) &&
        value.isValidStoredValue(ServerStorageLimits.COOKIE_VALUE_LENGTH) &&
        value.none { it == ';' || it == '\r' || it == '\n' || it.code < 0x20 }
