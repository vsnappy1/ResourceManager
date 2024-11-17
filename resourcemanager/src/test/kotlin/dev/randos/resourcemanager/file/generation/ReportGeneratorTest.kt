package dev.randos.resourcemanager.file.generation

import dev.randos.resourcemanager.model.Change
import dev.randos.resourcemanager.model.SourceFileDetails
import dev.randos.resourcemanager.utils.MockFileReader
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

class ReportGeneratorTest {
    @Before
    fun setup() {
        mockkStatic(LocalDateTime::class)
        every { LocalDateTime.now() } returns LocalDateTime.of(2024, 12, 1, 0, 0)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun generateMigrationReport() {
        // Given
        val changes = listOf(Change(1, "getString(R.strings.app_name)", "ResourceManager.Strings.appName()"))
        val sourceFilesDetail =
            listOf(
                SourceFileDetails("MainActivity.kt", "/Users/xyz/MainActivity.kt", changes)
            )

        // When
        val report = ReportGenerator.generateMigrationReport(sourceFilesDetail)

        // Then
        val expectedResult = MockFileReader.read("migration_report.txt")
        assertEquals(expectedResult, report)
    }
}