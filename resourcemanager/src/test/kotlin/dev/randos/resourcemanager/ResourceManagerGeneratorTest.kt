package dev.randos.resourcemanager

import dev.randos.resourcemanager.file.generation.ClassFileGenerator
import dev.randos.resourcemanager.manager.ModuleManager
import dev.randos.resourcemanager.manager.ResourceManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class ResourceManagerGeneratorTest {
    private lateinit var moduleFile: File

    private lateinit var generatedFile: File

    private lateinit var moduleManager: ModuleManager

    private lateinit var resourceManager: ResourceManager

    private lateinit var resourceManagerGenerator: ResourceManagerGenerator

    @Before
    fun setUp() {
        mockkObject(ClassFileGenerator)
        val projectDir = Files.createTempDirectory("project").toFile()
        moduleFile = File(projectDir, "app")
        generatedFile =
            File(moduleFile, "build/generated/source/resourcemanager/main/ResourceManager.kt")
        moduleManager = mockk()
        resourceManager = mockk()
        resourceManagerGenerator =
            ResourceManagerGenerator(projectDir, moduleFile, generatedFile, moduleManager, resourceManager)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun generate_whenClassContentIsGenerated_shouldWriteToTheGivenFile() {
        // Given
        val classContent =
            """
            class ResourceManager {}
            """.trimIndent()

        every { moduleManager.getNamespace() } returns "com.example.app"
        every { moduleManager.getModuleDependencies() } returns emptyList()
        every { resourceManager.getResources() } returns emptyList()
        every { ClassFileGenerator.generateClassFile(any(), any()) } returns classContent

        // When
        resourceManagerGenerator.generate()

        // Then
        assertTrue(generatedFile.exists())
        assertEquals(classContent, generatedFile.readText())
    }

    @Test
    fun generate_whenNamespaceNotFound_shouldNotWriteToFileAndPrintException() {
        // Given
        every { moduleManager.getNamespace() } returns null
        every { moduleManager.getModuleDependencies() } returns emptyList()
        every { resourceManager.getResources() } returns emptyList()

        // When
        resourceManagerGenerator.generate()

        // Then
        assertEquals("", generatedFile.readText())
    }
}