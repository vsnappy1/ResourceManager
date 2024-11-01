package dev.randos.resourcemanager.file.parser

import dev.randos.resourcemanager.model.ValueResource
import dev.randos.resourcemanager.model.ValueResourceType
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

internal class XmlParser {

    companion object {

        private const val STRING_TAG = "string"
        private const val COLOR_TAG = "color"
        private const val BOOLEAN_TAG = "bool"
        private const val INTEGER_TAG = "integer"
        private const val DIMENSION_TAG = "dimen"
        private const val FRACTION_TAG = "fraction"
        private const val ARRAY_TAG = "array"
        private const val STRING_ARRAY_TAG = "string-array"
        private const val INTEGER_ARRAY_TAG = "integer-array"
        private const val PLURAL_TAG = "plurals"

        /**
         * Parses an XML file and its variants to create a list of resources.
         *
         * @param file The XML file to parse.
         * @return A list of com.randos.resourcemanager.model.Resource objects containing the parsed data.
         */
        fun parseXML(file: File): List<ValueResource> {
            val resources = mutableListOf<ValueResource>()

            // Create a DocumentBuilder
            val factory = DocumentBuilderFactory.newInstance()
            val builder = factory.newDocumentBuilder()

            // Parse the XML file
            val document = builder.parse(file)
            document.documentElement.normalize()

            // Get the root element
            val root = document.documentElement

            // Get all the elements under <resources>
            val resourceItems = root.childNodes

            for (i in 0 until resourceItems.length) {
                val node = resourceItems.item(i)
                if (node is Element) {
                    // Handle different types of resources
                    handleElement(node, resources)
                }
            }
            return resources
        }

        private fun handleElement(
            node: Element,
            resources: MutableList<ValueResource>
        ) {
            val tagName = node.tagName
            val attributeName = node.getAttribute("name")

            when (tagName) {
                STRING_TAG -> {
                    val isParameterized = node.textContent.contains(Regex("%s|%d|%f|%x|%o|%c|%e"))
                    resources.add(ValueResource(attributeName, ValueResourceType.String(isParameterized)))
                }

                COLOR_TAG -> {
                    resources.add(ValueResource(attributeName, ValueResourceType.Color))
                }

                BOOLEAN_TAG -> {
                    resources.add(ValueResource(attributeName, ValueResourceType.Boolean))
                }

                INTEGER_TAG -> {
                    resources.add(ValueResource(attributeName, ValueResourceType.Integer))
                }

                DIMENSION_TAG -> {
                    resources.add(ValueResource(attributeName, ValueResourceType.Dimension))
                }

                FRACTION_TAG -> {
                    resources.add(ValueResource(attributeName, ValueResourceType.Fraction))
                }

                ARRAY_TAG -> {
                    resources.add(ValueResource(attributeName, ValueResourceType.Array))
                }

                STRING_ARRAY_TAG -> {
                    resources.add(ValueResource(attributeName, ValueResourceType.StringArray))
                }

                INTEGER_ARRAY_TAG -> {
                    resources.add(ValueResource(attributeName, ValueResourceType.IntArray))
                }

                PLURAL_TAG -> {
                    resources.add(ValueResource(attributeName, ValueResourceType.Plural))
                }
            }
        }
    }
}
