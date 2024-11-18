package task

import dev.randos.resourcemanager.manager.MigrationManager
import io.mockk.mockk
import io.mockk.verify
import org.gradle.api.GradleException
import org.gradle.testfixtures.ProjectBuilder
import org.junit.After
import org.junit.Before
import org.junit.Test

class ResourceManagerMigrationTaskTest {
    private lateinit var migrationManager: MigrationManager

    private val project = ProjectBuilder.builder().build()

    @Before
    fun setUp() {
        migrationManager = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
    }

    @Test(expected = GradleException::class)
    fun run_whenConfirmMigrationFlagMissing_shouldThrowException() {
        // Given
        val task = project.tasks.create("migrateToResourceManager", ResourceManagerMigrationTask::class.java)

        // When
        task.run()
    }

    @Test
    fun run_whenConfirmMigrationFlagPresent_shouldInvokeMigrateMethodOnMigrationManager() {
        // Given
        val task = project.tasks.create("migrateToResourceManager", ResourceManagerMigrationTask::class.java)
        project.extensions.extraProperties.set("confirmMigration", "true")
        task.setMigrationManager(migrationManager)

        // When
        task.run()

        // Then
        verify { migrationManager.migrate() }
    }
}