package com.randos.resourcemanager.file.parser

import com.randos.resourcemanager.model.Resource
import com.randos.resourcemanager.model.ValueIdentifier
import com.randos.resourcemanager.model.ValueVariant
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
        private const val PLURAL_TAG = "plurals"

        private val variantList = mutableListOf<Pair<String, Map<String, Map<String, Any>>>>()

        /**
         * Parses an XML file and its variants to create a list of resources.
         *
         * @param file The XML file to parse.
         * @param variants A list of ValueIdentifier objects representing different variants of the file.
         * @return A list of Resource objects containing the parsed data.
         */
        fun parseXML(file: File, variants: List<ValueIdentifier>): List<Resource<Any>> {
            val list = mutableListOf<Resource<Any>>()

            // Initialize the variant list with the given variants.
            initializeVariantList(variants)

            // Generate the resource map from the main XML file and process each entry.
            generateResourceMap(file).forEach { tagEntry ->
                tagEntry.value.forEach { pair ->
                    list.add(
                        Resource(
                            name = pair.key,
                            value = pair.value,
                            variants = getVariants(tagEntry.key, pair.key)
                        )
                    )
                }
            }
            return list
        }

        private fun initializeVariantList(variants: List<ValueIdentifier>) {
            variants.forEach {
                variantList.add(Pair(it.identifier!!, generateResourceMap(it.file)))
            }
        }

        /**
         * Retrieves the variants for a given tag and attribute name.
         *
         * @param tag The XML tag (e.g., "string", "color").
         * @param attributeName The name attribute of the XML element.
         * @return A list of ValueVariant objects containing the variants.
         */
        private fun getVariants(tag: String, attributeName: String): List<ValueVariant<Any>> {
            val list = mutableListOf<ValueVariant<Any>>()

            // Iterate over the variant list to find matching tags and attributes.
            variantList.forEach { variant ->
                val (id, map) = variant
                if (map.containsKey(tag)) {
                    map[tag]?.get(attributeName)?.let { value ->
                        list.add(ValueVariant(id, value))
                    }
                }
            }
            return list
        }

        private fun generateResourceMap(file: File): Map<String, Map<String, Any>> { // TAG, NAME, VALUE
            val map = mutableMapOf<String, MutableMap<String, Any>>()

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
                    handleElement(node, map)
                }
            }
            return map
        }

        private fun handleElement(
            node: Element,
            resources: MutableMap<String, MutableMap<String, Any>>
        ) {
            val tagName = node.tagName
            val attributeName = node.getAttribute("name").toCamelCase()

            when (tagName) {
                STRING_TAG -> {
                    val value = node.textContent.trim()
                    resources[STRING_TAG] = resources.getOrDefault(STRING_TAG, mutableMapOf())
                    resources[STRING_TAG]?.put(attributeName, value)
                }

                COLOR_TAG -> {
                    val value = node.textContent.trim()
                    resources[COLOR_TAG] = resources.getOrDefault(COLOR_TAG, mutableMapOf())
                    resources[COLOR_TAG]?.put(attributeName, value)
                }

                BOOLEAN_TAG -> {
                    val value = node.textContent.trim().toBoolean()
                    resources[BOOLEAN_TAG] = resources.getOrDefault(BOOLEAN_TAG, mutableMapOf())
                    resources[BOOLEAN_TAG]?.put(attributeName, value)
                }

                INTEGER_TAG -> {
                    val value = node.textContent.trim().toInt()
                    resources[INTEGER_TAG] = resources.getOrDefault(INTEGER_TAG, mutableMapOf())
                    resources[INTEGER_TAG]?.put(attributeName, value)
                }

                DIMEN_TAG -> {
                    val value = node.textContent.trim().toDimen()
                    resources[DIMEN_TAG] = resources.getOrDefault(DIMEN_TAG, mutableMapOf())
                    resources[DIMEN_TAG]?.put(attributeName, value)
                }

                FRACTION_TAG -> {
                    val value = node.textContent.trim().toFloat()
                    resources[FRACTION_TAG] = resources.getOrDefault(FRACTION_TAG, mutableMapOf())
                    resources[FRACTION_TAG]?.put(attributeName, value)
                }

                ARRAY_TAG -> {
                    val items = node.getElementsByTagName("item")
                    val arrayItems = ArrayList<String>()
                    for (j in 0 until items.length) {
                        arrayItems.add("\"${items.item(j).textContent}\"")
                    }
                    resources[ARRAY_TAG] = resources.getOrDefault(ARRAY_TAG, mutableMapOf())
                    resources[ARRAY_TAG]?.put(attributeName, arrayItems)
                }

                STRING_ARRAY_TAG -> {
                    val items = node.getElementsByTagName("item")
                    val arrayItems = ArrayList<String>()
                    for (j in 0 until items.length) {
                        arrayItems.add("\"${items.item(j).textContent}\"")
                    }
                    resources[STRING_ARRAY_TAG] = resources.getOrDefault(STRING_ARRAY_TAG, mutableMapOf())
                    resources[STRING_ARRAY_TAG]?.put(attributeName, arrayItems)
                }

                INTEGER_ARRAY_TAG -> {
                    val items = node.getElementsByTagName("item")
                    val arrayItems = ArrayList<Int>()
                    for (j in 0 until items.length) {
                        arrayItems.add(items.item(j).textContent.toInt())
                    }
                    resources[INTEGER_ARRAY_TAG] = resources.getOrDefault(INTEGER_ARRAY_TAG, mutableMapOf())
                    resources[INTEGER_ARRAY_TAG]?.put(attributeName, arrayItems)
                }

                PLURAL_TAG -> {
                    val items = node.getElementsByTagName("item")
                    val map = HashMap<String, String>()
                    for (j in 0 until items.length) {
                        val quantity = (items.item(j) as Element).getAttribute("quantity")
                        val text = items.item(j).textContent.trim()
                        map[quantity] = text
                    }
                    resources[PLURAL_TAG] = resources.getOrDefault(PLURAL_TAG, mutableMapOf())
                    resources[PLURAL_TAG]?.put(attributeName, map)
                    println("generated")
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