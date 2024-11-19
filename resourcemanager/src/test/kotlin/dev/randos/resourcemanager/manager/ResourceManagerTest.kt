package dev.randos.resourcemanager.manager

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.unmockkAll
import org.gradle.internal.impldep.org.testng.Assert.assertEquals
import org.gradle.internal.impldep.org.testng.Assert.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class ResourceManagerTest {
    private lateinit var resourceManager: ResourceManager

    private lateinit var moduleFile: File

    @MockK
    private lateinit var moduleManager: ModuleManager

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        val projectDir = Files.createTempDirectory("project").toFile()
        moduleFile = File(projectDir, "app")
        resourceManager = ResourceManager(projectDir, moduleFile, moduleManager)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun getFilesUnderObservation_whenThereAreNFilesInResourceDirectories_shouldReturnNFiles() {
        // Given
        val resFile = File(moduleFile, "src/main/res")
        val gradleFile = File(moduleFile, "build.gradle.kts")

        every { moduleManager.getModuleDependencies() } returns emptyList()
        every { moduleManager.getBuildGradleFile() } returns gradleFile

        val valuesDirectory =
            File(resFile, "values").also {
                it.mkdirs()
            }
        val drawableDirectory =
            File(resFile, "drawable").also {
                it.mkdirs()
            }
        val stringsXml =
            File(valuesDirectory, "strings.xml").also {
                it.writeText("")
            }
        val dimenXml =
            File(valuesDirectory, "dimen.xml").also {
                it.writeText("")
            }
        val drawableRes1 =
            File(drawableDirectory, "ic_launcher_foreground.xml").also {
                it.writeText("")
            }
        val drawableRes2 =
            File(drawableDirectory, "ic_launcher_background.xml").also {
                it.writeText("")
            }

        // When
        val files = resourceManager.getFilesUnderObservation()

        // Then
        assertTrue(files.contains(stringsXml))
        assertTrue(files.contains(dimenXml))
        assertTrue(files.contains(drawableRes1))
        assertTrue(files.contains(drawableRes2))
        assertTrue(files.contains(gradleFile))
        assertEquals(5, files.size)
    }

    @Test
    fun getResources_whenDependencyModulesHaveResources_shouldReturnCurrentModulesResourcesPlusDependencyModuleResources() {
        // Given
        val gradleFile = File(moduleFile, "build.gradle.kts")
        val moduleName = "myModule"
        val resFile = File(moduleFile, "src/main/res")

        every { moduleManager.getModuleDependencies() } returns listOf(moduleName)
        every { moduleManager.getBuildGradleFile() } returns gradleFile

        val stringsXml =
            File(resFile, "values/strings.xml").also {
                it.parentFile.mkdirs()
                it.writeText("")
            }
        val drawableRes1 =
            File(resFile, "drawable/ic_launcher_foreground.xml").also {
                it.parentFile.mkdirs()
                it.writeText("")
            }
        val stringsXmlFromDependencyModule =
            File(moduleFile.parentFile, "$moduleName/src/main/res/values/strings.xml").also {
                it.parentFile.mkdirs()
                it.writeText("")
            }
        val drawableFromDependencyModule =
            File(moduleFile.parentFile, "$moduleName/src/main/res/drawable/ic_gift.png").also {
                it.parentFile.mkdirs()
                it.writeText("")
            }

        val namespace = "com.example.myapplication"
        val gradleFileContent =
            """
            android {
                namespace = "$namespace"
                compileSdk = 34
                ..
            }
            """.trimIndent()
        val moduleGradleFile = File(moduleFile.parentFile, "$moduleName/build.gradle.kts")
        moduleGradleFile.writeText(gradleFileContent)

        // When
        val files = resourceManager.getFilesUnderObservation()

        // Then
        assertTrue(files.contains(stringsXml))
        assertTrue(files.contains(drawableRes1))
        assertTrue(files.contains(gradleFile))
        assertTrue(files.contains(stringsXmlFromDependencyModule))
        assertTrue(files.contains(drawableFromDependencyModule))
        assertEquals(5, files.size)
    }
}