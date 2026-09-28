package f95gm.server.app

import f95gm.server.kernel.FailureCategory
import f95gm.server.kernel.failureCategory
import kotlinx.coroutines.CancellationException
import java.io.IOException
import java.sql.SQLException
import kotlin.test.Test
import kotlin.test.assertEquals

class FailureCategoryTest {
    @Test
    fun infrastructureFailuresHaveStableCategories() {
        assertEquals(FailureCategory.CANCELLATION, CancellationException().failureCategory())
        assertEquals(FailureCategory.UPSTREAM_TRANSPORT, IOException().failureCategory())
        assertEquals(FailureCategory.DATABASE, SQLException().failureCategory())
        assertEquals(FailureCategory.APPLICATION, IllegalStateException().failureCategory())
    }
}
