package f95gm.server.app

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GenerateMigrationPlanTest {
    @Test
    fun writesDeterministicPlanToTheRequestedNestedPath() {
        val root = Files.createTempDirectory("migration-plan")
        val output = root.resolve("nested/plan.txt")
        try {
            main(arrayOf(output.toString()))
            val first = Files.readString(output)
            main(arrayOf(output.toString()))

            assertEquals(first, Files.readString(output))
            assertContains(first, "from=0")
            assertContains(first, "to=")
            assertContains(first, "noOp=false")
            assertContains(first, "apply|")
        } finally {
            Files.deleteIfExists(output)
            Files.deleteIfExists(output.parent)
            Files.deleteIfExists(root)
        }
    }

    @Test
    fun rejectsInvalidArgumentShapes() {
        assertFailsWith<IllegalStateException> { main(emptyArray()) }
        assertFailsWith<IllegalStateException> { main(arrayOf("one", "two")) }
    }
}
