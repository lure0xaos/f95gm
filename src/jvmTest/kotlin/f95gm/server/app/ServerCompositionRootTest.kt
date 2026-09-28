package f95gm.server.app

import f95gm.server.config.ServerConfig
import f95gm.server.persistence.database.DatabaseExecutor
import f95gm.server.persistence.testDatabaseUrl
import org.jetbrains.exposed.v1.jdbc.Database
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class ServerCompositionRootTest {
    @Test
    fun compositionWiresRouteServicesThroughTheRuntimeFactories() {
        val database = Database.connect(testDatabaseUrl("composition_wiring"), driver = "org.h2.Driver")
        val context = ServerCompositionRoot.handle(ServerConfig.from(emptyMap()), database)
        try {
            val routes = context.composition.routes
            assertNotNull(routes.services.staticCache)
            assertNotNull(routes.services.publicHostResolver)
            assertNotNull(routes.services.mediaFetcher)
            assertNotNull(routes.services.mediaService)
            assertNotNull(routes.services.catalogService)
            assertNotNull(routes.services.itemService)
            assertNotNull(routes.services.liveMarkCache)
            assertNotNull(routes.dependencies.item.upstream)
        } finally {
            context.close()
        }
    }

    @Test
    fun compositionRootRemainsClosableAfterPartialStartupFailure() {
        val database = Database.connect(testDatabaseUrl("composition_failure"), driver = "org.h2.Driver")
        val context = ServerCompositionRoot.handle(ServerConfig.from(emptyMap()), database)
        try {
            context.composition.routes.dependencies.item.upstream
        } finally {
            context.close()
            context.close()
        }
        assertFailsWith<IllegalStateException> { context.composition.routes.dependencies.item.upstream }
    }
}
