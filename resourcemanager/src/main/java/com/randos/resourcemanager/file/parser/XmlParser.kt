package com.randos.resourcemanager.file.parser

import com.randos.resourcemanager.model.Resource
import com.randos.resourcemanager.utils.toCamelCase
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

internal class XmlParser {

    companion object {

        private const val STRING_TAG = "string"
        private const val COLOR_TAG = "color"
        private const val BOOLEAN_TAG = "bool"
        private const val INTEGER_TAG = "integer"
        private const val DIMEN_TAG = "dimen"
        private const val FRACTION_TAG = "fraction"
        private const val ARRAY_TAG = "array"
        private const val STRING_ARRAY_TAG = "string-array"
        private const val INTEGER_ARRAY_TAG = "integer-array"
        private const val PLURAL_TAG = "plural"

        fun parseXML(file: File): List<Resource<Any>> {
            val list = mutableListOf<Resource<Any>>()

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
                    handleElement(node, list)
                }
            }
            return list
        }

        private fun handleElement(
            node: Element,
            resources: MutableList<Resource<Any>>
        ) {
            val tagName = node.tagName
            val attributeName = node.getAttribute("name").toCamelCase()

            when (tagName) {
                STRING_TAG, COLOR_TAG -> {
                    val value = node.textContent.trim()
                    resources.add(Resource(attributeName, value))
                }

                BOOLEAN_TAG -> {
                    val value = node.textContent.trim().toBoolean()
                    resources.add(Resource(attributeName, value))
                }

                INTEGER_TAG -> {
                    val value = node.textContent.trim().toInt()
                    resources.add(Resource(attributeName, value))
                }

                DIMEN_TAG -> {
                    val value = node.textContent.trim().toDimen()
                    resources.add(Resource(attributeName, value))
                }

                FRACTION_TAG -> {
                    val value = node.textContent.trim().toFloat()
                    resources.add(Resource(attributeName, value))
                }

                ARRAY_TAG, STRING_ARRAY_TAG -> {
                    val items = node.getElementsByTagName("item")
                    val arrayItems = ArrayList<String>()
                    for (j in 0 until items.length) {
                        arrayItems.add("\"${items.item(j).textContent}\"")
                    }
                    resources.add(
                        Resource(
                            attributeName,
                            arrayItems,
                            "List<${String::class.simpleName.toString()}>"
                        )
                    )
                }

                INTEGER_ARRAY_TAG -> {
                    val items = node.getElementsByTagName("item")
                    val arrayItems = ArrayList<Int>()
                    for (j in 0 until items.length) {
                        arrayItems.add(items.item(j).textContent.toInt())
                    }
                    resources.add(
                        Resource(
                            attributeName,
                            arrayItems,
                            "List<${Int::class.simpleName.toString()}>"
                        )
                    )
                }

                PLURAL_TAG -> {
                    val items = node.getElementsByTagName("item")
                    val map = HashMap<String, String>()
                    for (j in 0 until items.length) {
                        val quantity = (items.item(j) as Element).getAttribute("quantity")
                        val text = items.item(j).textContent.trim()
                        map[quantity] = text
                    }
                    resources.add(Resource(attributeName, map, String::class.simpleName.toString()))
                }
            }
        }

        private fun String.toDimen(): Int {
            // Match one or more digits at the start of the string
            val regex = Regex("^\\d+")

            // Find the first match for the number part
            return regex.find(trim())?.value?.toIntOrNull() ?: 0
        }
    }
}