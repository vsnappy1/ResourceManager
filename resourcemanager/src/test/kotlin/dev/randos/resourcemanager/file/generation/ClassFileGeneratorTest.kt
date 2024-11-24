package dev.randos.resourcemanager.file.generation

import dev.randos.resourcemanager.model.ModuleDetails
import dev.randos.resourcemanager.model.Resource
import dev.randos.resourcemanager.model.ResourceType
import dev.randos.resourcemanager.utils.MockFileReader
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class ClassFileGeneratorTest {
    private val moduleName = "application"
    private val namespace: String = "com.example.$moduleName"
    private val moduleDir: File = Files.createTempDirectory(moduleName).toFile()
    private val valueResDirectory = moduleDir.resolve("src/main/res/values")
    private val drawableResDirectory = moduleDir.resolve("src/main/res/drawable")

    private val moduleNameLib1 = "lib1"
    private val namespaceLib1: String = "com.example.$moduleNameLib1"
    private val moduleDirLib1: File = Files.createTempDirectory(moduleNameLib1).toFile()
    private val valueResDirectoryLib1 = moduleDirLib1.resolve("src/main/res/values")
    private val drawableResDirectoryLib1 = moduleDirLib1.resolve("src/main/res/drawable")

    private val moduleNameLib2 = "lib2"
    private val namespaceLib2: String = "com.example.$moduleNameLib2"
    private val moduleDirLib2: File = Files.createTempDirectory(moduleNameLib2).toFile()
    private val valueResDirectoryLib2 = moduleDirLib2.resolve("src/main/res/values")
    private val drawableResDirectoryLib2 = moduleDirLib2.resolve("src/main/res/drawable")

    @Before
    fun setup() {
        valueResDirectory.mkdirs()
        valueResDirectoryLib1.mkdirs()
        valueResDirectoryLib2.mkdirs()
        drawableResDirectory.mkdirs()
        drawableResDirectoryLib1.mkdirs()
        drawableResDirectoryLib2.mkdirs()
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun generate_whenResourceDirectoriesHaveResources_shouldReturnWellFormatedClass() {
        // Given
        val gradleFileContent =
            """
            android {
                namespace = "$namespace"
                compileSdk = 34
                ..
            }
            
            dependencies {
                ...
                implementation(project(":$moduleNameLib1"))
                implementation(project(":$moduleNameLib2"))
            }
            """.trimIndent()
        val gradleFile = File(moduleDir, "build.gradle.kts")
        gradleFile.writeText(gradleFileContent)

        val colorResContent = getColorResourceFileContent()

        File(valueResDirectory, "resources.xml").also {
            it.writeText(MockFileReader.read("all_resources.txt"))
        }

        File(valueResDirectory, "colors.xml").also {
            it.writeText(colorResContent)
        }

        File(valueResDirectoryLib1, "resources.xml").also {
            it.writeText(MockFileReader.read("all_resources_module.txt"))
        }

        File(valueResDirectoryLib2, "colors.xml").also {
            it.writeText(colorResContent)
        }

        File(drawableResDirectory, "ic_launcher_foreground.xml").also {
            it.writeText("")
        }
        File(drawableResDirectory, "ic_gift.png").also {
            it.writeText("")
        }

        File(drawableResDirectory, "ic_arrow.svg").also {
            it.writeText("")
        }

        File(drawableResDirectoryLib1, "ic_cart.xml").also {
            it.writeText("")
        }

        File(drawableResDirectoryLib2, "ic_done.xml").also {
            it.writeText("")
        }

        // When
        val classContent =
            ClassFileGenerator.generateClassFile(
                namespace,
                getResources()
            )

        // Then
        val expectedResult = MockFileReader.read("resource_manager.txt")
        assertEquals(expectedResult, classContent)
    }

    @Test
    fun generate_whenClassFileIsGenerated_shouldClearTheFunctionNamesSet() {
        // Given
        val gradleFileContent =
            """
            android {
                namespace = "$namespace"
                compileSdk = 34
                ..
            }
            """.trimIndent()
        val gradleFile = File(moduleDir, "build.gradle.kts")
        gradleFile.writeText(gradleFileContent)

        File(valueResDirectory, "resources.xml").also {
            it.writeText(MockFileReader.read("all_resources.txt"))
        }

        File(drawableResDirectory, "ic_gift.png").also {
            it.writeText("")
        }

        // When
        ClassFileGenerator.generateClassFile(
            namespace,
            getResources()
        )

        // Then
        assertEquals(0, ClassFileGenerator.getFunctionNamesSize())
    }

    private fun getColorResourceFileContent(): String {
        return """
            <?xml version="1.0" encoding="utf-8"?>
            <resources>
                <color name="black">#FF000000</color>
                <color name="white">#FFFFFFFF</color>
            </resources>
            """.trimIndent()
    }

    private fun getResources(): List<Resource> {
        val list = mutableListOf<Resource>()
        return list.apply {
            add(
                Resource(ResourceType.VALUES, ModuleDetails("", "", valueResDirectory.listFiles() ?: emptyArray()))
            )
            add(
                Resource(ResourceType.VALUES, ModuleDetails(moduleNameLib1, namespaceLib1, valueResDirectoryLib1.listFiles() ?: emptyArray()))
            )
            add(
                Resource(ResourceType.VALUES, ModuleDetails(moduleNameLib2, namespaceLib2, valueResDirectoryLib2.listFiles() ?: emptyArray()))
            )
            add(
                Resource(ResourceType.DRAWABLES, ModuleDetails("", "", drawableResDirectory.listFiles() ?: emptyArray()))
            )
            add(
                Resource(ResourceType.DRAWABLES, ModuleDetails(moduleNameLib1, namespaceLib1, drawableResDirectoryLib1.listFiles() ?: emptyArray()))
            )
            add(
                Resource(ResourceType.DRAWABLES, ModuleDetails(moduleNameLib2, namespaceLib2, drawableResDirectoryLib2.listFiles() ?: emptyArray()))
            )
        }
    }
}