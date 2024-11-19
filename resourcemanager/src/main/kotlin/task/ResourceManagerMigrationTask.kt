package task

import dev.randos.resourcemanager.manager.MigrationManager
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction
import org.jetbrains.annotations.TestOnly

internal abstract class ResourceManagerMigrationTask : DefaultTask() {
    private var migrationManager: MigrationManager = MigrationManager(project.rootDir, project.projectDir)

    @TaskAction
    fun run() {
        // Check for confirmation flag
        if (!(project.hasProperty("confirmMigration") && project.findProperty("confirmMigration") == "true")) {
            println("Warning: This task will modify project files.")
            val message =
                "Error: Confirmation flag missing. To proceed, rerun the task with -PconfirmMigration=true."
            throw GradleException("Migration cancelled. $message")
        }
        migrationManager.migrate()
    }

    @TestOnly
    fun setMigrationManager(migrationManager: MigrationManager) {
        this.migrationManager = migrationManager
    }
}