package dev.randos.resourcemanager.manager

import dev.randos.resourcemanager.utils.MockFileReader
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class MigrationManagerTest {
    private lateinit var projectDir: File

    private lateinit var app: File

    private lateinit var moduleDir: File

    private lateinit var migrationManager: MigrationManager

    @Before
    fun setUp() {
        projectDir = Files.createTempDirectory("dummy").toFile()
        app = File(projectDir, "app").also { it.mkdirs() }
        moduleDir = File(projectDir, "module").also { it.mkdirs() }
        migrationManager = MigrationManager(projectDir, app)
        app.mkdirs()
        val buildGradleFileContent =
            """
            ...
            android {
                namespace = "com.example.app"
                ...
            }
            
            dependencies {
                ...
                implementation(project(":module"))
            }
            tasks.register(\"migrateToResourceManager\", ResourceManagerMigrationTask::class.java)
            """.trimIndent()

        val moduleBuildGradleFileContent =
            """
            ...
            android {
                namespace = "com.example.module"
                ...
            }
            """.trimIndent()
        File(app, "build.gradle.kts").writeText(buildGradleFileContent)
        File(moduleDir, "build.gradle.kts").writeText(moduleBuildGradleFileContent)

        val valueResDir = File(app, "src/main/res/values")
        valueResDir.mkdirs()
        val drawableResDir = File(app, "src/main/res/drawable")
        drawableResDir.mkdirs()
        val moduleValueResDir = File(moduleDir, "src/main/res/values")
        moduleValueResDir.mkdirs()
        val moduleDrawableResDir = File(moduleDir, "src/main/res/drawable")
        moduleDrawableResDir.mkdirs()

        File(valueResDir, "resources.xml").writeText(MockFileReader.read("all_resources.txt"))
        File(drawableResDir, "ic_repeat.xml").writeText("<p>Drawable</p>")
        File(moduleValueResDir, "resources.xml").writeText(MockFileReader.read("all_resources_module.txt"))
        File(moduleDrawableResDir, "ic_app_icon.xml").writeText("<p>Drawable</p>")
    }

    @After
    fun tearDown() {
    }

    @Test
    fun run_whenSourceFilesExists_shouldUpdateThemAccordingly() {
        // Given
        val sourceDirs = File(app, "src/main/kotlin/com/example/app")
        sourceDirs.mkdirs()
        val sourceFile = File(sourceDirs, "MainActivity.kt")
        sourceFile.writeText(MockFileReader.read("main_activity_kt_before_migration.txt"))

        // When
        migrationManager.migrate()

        // Then
        val expectedResult = MockFileReader.read("main_activity_kt_after_migration.txt")
        assertEquals(expectedResult, sourceFile.readText())
    }

    @Test
    fun run_whenMigrationTaskRunMultipleTimes_shouldUpdateGivenFileAccordinglyAndResultShouldNotChange() {
        // Given
        val sourceDirs = File(app, "src/main/kotlin/com/example/app")
        sourceDirs.mkdirs()
        val sourceFile = File(sourceDirs, "MainActivity.kt")
        sourceFile.writeText(MockFileReader.read("main_activity_kt_before_migration.txt"))

        // When
        migrationManager.migrate()
        migrationManager.migrate()
        migrationManager.migrate()

        // Then
        val expectedResult = MockFileReader.read("main_activity_kt_after_migration.txt")
        assertEquals(expectedResult, sourceFile.readText())
    }

    @Test
    fun run_whenSourceFileHaveResourceImportStatementFromModule_shouldUpdateFileAccordingly() {
        // Given
        val sourceDirs = File(app, "src/main/kotlin/com/example/app")
        sourceDirs.mkdirs()
        val sourceFile = File(sourceDirs, "MainActivity.kt")
        sourceFile.writeText(MockFileReader.read("my_application_kt_before_migration.txt"))

        // When
        migrationManager.migrate()

        // Then
        val expectedResult = MockFileReader.read("my_application_kt_after_migration.txt")
        assertEquals(expectedResult, sourceFile.readText())
    }

    @Test
    fun run_whenMigrationRuns_shouldGenerateMigrationReport() {
        // Given
        val report =
            File(
                app,
                "build/reports/migration/resourcemanager-migration-report.html"
            )

        // When
        migrationManager.migrate()

        // Then
        assertTrue(report.exists())
    }
}