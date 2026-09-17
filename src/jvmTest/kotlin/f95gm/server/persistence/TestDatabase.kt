package f95gm.server.persistence

import java.util.concurrent.atomic.AtomicLong

private val databaseSequence = AtomicLong()

/** Produces isolated, deterministic-in-shape H2 names for persistence tests. */
internal fun testDatabaseUrl(name: String): String =
    "jdbc:h2:mem:${name}_${databaseSequence.incrementAndGet()};DB_CLOSE_DELAY=-1"
