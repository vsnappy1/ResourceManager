package dev.randos.resourcemanager.file.parser

import dev.randos.resourcemanager.model.ValueResourceType
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
        val resourceFileContent =
            """
            <resources>
                <string name="string_res">My Application</string>
                <string name="string_parameterized_res">Hello %s</string>
                <bool name="bool_res">false</bool>
                <color name="color_res">#FF000000</color>
                <dimen name="dimen_res">20dp</dimen>
                <fraction name="fraction_res">0.3</fraction>
                <integer name="integer_res">10</integer>
                <string-array name="string_array_res">
                    <item>USA</item>
                    <item>UK</item>
                    <item>Canada</item>
                </string-array>
                <array name="array_res">
                    <item>#FF3700B3</item>
                    <item>#FF03DAC5</item>
                    <item>#FF018786</item>
                </array>
                <integer-array name="int_array_res">
                    <item>23</item>
                    <item>30</item>
                    <item>40</item>
                </integer-array>
                <plurals name="plurals_res" translatable="false">
                    <item quantity="zero">Empty</item>
                    <item quantity="one">There is one item</item>
                    <item quantity="two">There is two item</item>
                    <item quantity="few">There is few item</item>
                    <item quantity="many">There is many item</item>
                    <item quantity="other">There are %d items</item>
                </plurals>
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
        val map = valueResources.groupBy { it.name }
        assertTrue(map["string_res"]?.first()?.type is ValueResourceType.String)
        assertTrue(map["string_parameterized_res"]?.first()?.type is ValueResourceType.String)
        assertTrue(map["bool_res"]?.first()?.type is ValueResourceType.Boolean)
        assertTrue(map["color_res"]?.first()?.type is ValueResourceType.Color)
        assertTrue(map["dimen_res"]?.first()?.type is ValueResourceType.Dimension)
        assertTrue(map["fraction_res"]?.first()?.type is ValueResourceType.Fraction)
        assertTrue(map["integer_res"]?.first()?.type is ValueResourceType.Integer)
        assertTrue(map["string_array_res"]?.first()?.type is ValueResourceType.StringArray)
        assertTrue(map["array_res"]?.first()?.type is ValueResourceType.StringArray)
        assertTrue(map["int_array_res"]?.first()?.type is ValueResourceType.IntArray)
        assertTrue(map["plurals_res"]?.first()?.type is ValueResourceType.Plural)
        assertEquals(11, valueResources.size)
    }
}