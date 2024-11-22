package dev.randos.resourcemanager.file.parser

import dev.randos.resourcemanager.model.ValueResourceType
import dev.randos.resourcemanager.utils.MockFileReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class XmlParserTest {
    @Test
    fun parseXML_whenFileContainsNValueResources_shouldReturnNValueResources() {
        // Given
        val resourceFileContent =
            """
            <resources>
                <string name="app_name">My Application</string>
                <string name="app_name2">My Application</string>
                <string name="greetings">Hello %s</string>
            </resources>
            """.trimIndent()

        val moduleDirectory = Files.createTempDirectory("app").toFile()
        val resourceFile =
            File(moduleDirectory, "src/main/res/values/strings.xml").also {
                it.parentFile.mkdirs()
                it.writeText(resourceFileContent)
            }

        // When
        val valueResources = XmlParser.parseXML(resourceFile)

        // Then
        val resourceIds = valueResources.map { it.name }
        assertTrue(resourceIds.contains("app_name"))
        assertTrue(resourceIds.contains("app_name2"))
        assertTrue(resourceIds.contains("greetings"))
        assertEquals(3, valueResources.size)
    }

    @Test
    fun parseXML_whenFileContainsNDifferentTypeValueResources_shouldReturnNValueResources() {
        // Given
        val resourceFileContent = MockFileReader.read("all_resources.txt")
        val moduleDirectory = Files.createTempDirectory("app").toFile()
        val resourceFile =
            File(moduleDirectory, "src/main/res/values/strings.xml").also {
                it.parentFile.mkdirs()
                it.writeText(resourceFileContent)
            }

        // When
        val valueResources = XmlParser.parseXML(resourceFile)

        // Then
        val map = valueResources.groupBy { it.name }
        assertTrue(map["string_res"]?.first()?.type is ValueResourceType.String)
        assertTrue(map["string_parameterized_res"]?.first()?.type is ValueResourceType.String)
        assertTrue(map["bool_res"]?.first()?.type is ValueResourceType.Boolean)
        assertTrue(map["color_res"]?.first()?.type is ValueResourceType.Color)
        assertTrue(map["dimen_res"]?.first()?.type is ValueResourceType.Dimension)
        assertTrue(map["fraction_res"]?.first()?.type is ValueResourceType.Fraction)
        assertTrue(map["integer_res"]?.first()?.type is ValueResourceType.Integer)
        assertTrue(map["string_array_res"]?.first()?.type is ValueResourceType.StringArray)
        assertTrue(map["array_res"]?.first()?.type is ValueResourceType.IntArray)
        assertTrue(map["array_color"]?.first()?.type is ValueResourceType.IntArray)
        assertTrue(map["array_int"]?.first()?.type is ValueResourceType.IntArray)
        assertTrue(map["array_mixed"]?.first()?.type is ValueResourceType.StringArray)
        assertTrue(map["int_array_res"]?.first()?.type is ValueResourceType.IntArray)
        assertTrue(map["plurals_res"]?.first()?.type is ValueResourceType.Plural)
        assertEquals(14, valueResources.size)
    }
}