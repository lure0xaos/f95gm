package f95gm.server.state.runtime

import kotlin.test.Test
import kotlin.test.assertNotNull

class RouteDependencyContractTest {
    @Test
    fun everyRoutePortIsComposedIntoTheApplicationBoundary() {
        val ports = testServerContext().applicationComposition.routes.dependencies
        assertNotNull(ports.login)
        assertNotNull(ports.catalog)
        assertNotNull(ports.item)
        assertNotNull(ports.filter)
        assertNotNull(ports.mark)
        assertNotNull(ports.media)
    }
}
