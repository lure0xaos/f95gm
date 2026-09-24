package f95gm.server.persistence.sessions

import f95gm.domain.values.F95AccountKey
import f95gm.domain.values.F95SessionId
import f95gm.server.config.constants.ServerConstants
import f95gm.server.config.logging.*
import f95gm.server.config.session.sessionMaxAgeSeconds
import f95gm.server.model.auth.UpstreamSession
import f95gm.server.persistence.database.DatabaseExecutor
import f95gm.server.persistence.schema.SessionCookiesTable
import f95gm.server.persistence.schema.SessionsTable
import f95gm.server.state.runtime.ServerClock
import f95gm.server.state.runtime.session.SessionRegistry
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.selectAll

internal suspend fun restoreSessions(database: DatabaseExecutor, registry: SessionRegistry, clock: ServerClock) {
    val now = clock.nowMillis()
    database.transactionSuspending {
        SessionsTable.selectAll().forEach { row ->
            val rawId = row[SessionsTable.id]
            val accountKey = row[SessionsTable.accountKey]
            val updatedAt = row[SessionsTable.updatedAt]
            val validIdentity = rawId.isValidStoredValue(ServerConstants.SESSION_ID_LENGTH) &&
                    accountKey.isValidStoredValue(ServerConstants.ACCOUNT_KEY_LENGTH)
            if (!validIdentity ||
                updatedAt <= 0L ||
                updatedAt > now ||
                now - updatedAt > sessionMaxAgeSeconds * ServerConstants.MILLIS_PER_SECOND
            ) {
                logger.warn {
                    auditLog(
                        AuditEvent(
                            AuditEventName.SESSION_ROW_QUARANTINED,
                            listOf(logField("reason", if (!validIdentity) "invalid_identity" else "expired_or_future"))
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
                        if (name.isValidStoredValue(ServerConstants.COOKIE_NAME_LENGTH) &&
                            value.isValidStoredValue(ServerConstants.COOKIE_VALUE_LENGTH) &&
                            value.none { it == ';' || it == '\r' || it == '\n' || it.code < 0x20 }
                        ) name to value else null
                    }
                    .take(ServerConstants.MAX_UPSTREAM_COOKIE_COUNT)
                    .associate { it }
                val id = F95SessionId(rawId)
                registry.put(
                    id, UpstreamSession(
                        browserId = id,
                        accountKey = F95AccountKey(accountKey),
                        cookies = cookies
                    )
                )
            }
        }
    }
}

private fun String.isValidStoredValue(maxLength: Int): Boolean =
    isNotBlank() && length <= maxLength && none { it == '\u0000' }
