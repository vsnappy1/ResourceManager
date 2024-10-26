package com.randos.resourcemanager.file.generation

import com.randos.resourcemanager.file.parser.XmlParser
import com.randos.resourcemanager.model.Resource
import com.randos.resourcemanager.model.ValueIdentifier
import com.randos.resourcemanager.utils.toCamelCase

internal class ClassFileGenerator {

    companion object {
        fun generateClassFile(packageName: String, files: Map<String, MutableList<ValueIdentifier>>): String {
            return StringBuilder().apply {
                appendLine("package $packageName\n")
                appendLine("import java.util.*\n")
                appendLine("object ResourceManager {\n")
                appendLine("\tprivate val locale = Locale.getDefault()\n")
                files.forEach {
                    val defaultFile = it.value.removeFirst().file
                    val resources = XmlParser.parseXML(defaultFile, it.value)
                    // Only create object for file when given file has some resources (i.e. not empty).
                    if (resources.isNotEmpty()) {
                        val resourceObject = generateObject(
                            defaultFile.nameWithoutExtension,
                            resources
                        )
                        appendLine(resourceObject)
                    }
                }
                appendLine("}")
            }.toString()
        }

        private fun generateObject(name: String, pairs: List<Resource<Any>>): String {
            val defaultIndentation = "\t\t"
            return StringBuilder().apply {
                appendLine("\tobject ${name.toCamelCase().replaceFirstChar { it.uppercase() }} {")
                pairs.forEach { resource ->
                    when (resource.value::class.simpleName) {
                        String::class.simpleName -> {
                            appendStringResource(resource, defaultIndentation)
                        }

                        Float::class.simpleName -> {
                            appendFloatResource(resource, defaultIndentation)
                        }

                        ArrayList::class.simpleName -> {
                            appendArrayListResource(resource, defaultIndentation)
                        }

                        HashMap::class.simpleName -> {
                            appendMapResource(resource, defaultIndentation)
                        }

                        else -> {
                            appendLine("${defaultIndentation}fun ${resource.name}() : ${resource.returnType} = ${resource.value}")
                        }
                    }
                }
                appendLine("\t}")
            }.toString()
        }

        @Suppress("UNCHECKED_CAST")
        private fun StringBuilder.appendMapResource(resource: Resource<Any>, defaultIndentation: String) {
            if (resource.value is Map<*, *> && resource.value.all { it.key is String && it.value is String }) {
                appendLine("${defaultIndentation}fun ${resource.name}String(count: Int): String {")
                resource.variants.forEach {
                    appendLine("${defaultIndentation}\tif(locale.language == \"${it.identifier}\"){")
                    appendLine(generateFunctionForPlural(it.value as Map<String, String>, defaultIndentation+"\t"))
                    appendLine("${defaultIndentation}\t}")
                }
                append(generateFunctionForPlural(resource.value as Map<String, String>, defaultIndentation))
                appendLine("${defaultIndentation}}")
            }
        }

        private fun StringBuilder.appendArrayListResource(
            resource: Resource<Any>,
            defaultIndentation: String
        ) {
            val dataType = if (resource.value is ArrayList<*> && resource.value.all { it is Int }) {
                Int::class.simpleName
            } else {
                String::class.simpleName
            }
            appendLine("${defaultIndentation}fun ${resource.name}() : ${resource.returnType}<$dataType> {")
            resource.variants.forEach {
                appendLine("${defaultIndentation}\tif(locale.language == \"${it.identifier}\"){")
                appendLine(
                    "${defaultIndentation}\t\treturn arrayListOf(${
                        it.value.toString().replace(Regex("[\\[\\]]"), "")
                    })"
                )
                appendLine("${defaultIndentation}\t}")
            }
            appendLine(
                "${defaultIndentation}\treturn arrayListOf(${
                    resource.value.toString().replace(Regex("[\\[\\]]"), "")
                })"
            )
            appendLine("${defaultIndentation}}")
        }

        private fun StringBuilder.appendFloatResource(resource: Resource<Any>, defaultIndentation: String) {
            appendLine("${defaultIndentation}fun ${resource.name}() : ${resource.returnType} = ${resource.value}f")
        }

        private fun StringBuilder.appendStringResource(resource: Resource<Any>, defaultIndentation: String) {
            appendLine("${defaultIndentation}fun ${resource.name}() : ${resource.returnType} {")
            resource.variants.forEach {
                appendLine("${defaultIndentation}\tif(locale.language == \"${it.identifier}\"){")
                appendLine("${defaultIndentation}\t\treturn \"${it.value}\"")
                appendLine("${defaultIndentation}\t}")
            }
            appendLine("${defaultIndentation}\treturn \"${resource.value}\"")
            appendLine("${defaultIndentation}}")
        }

        private fun generateFunctionForPlural(
            map: Map<String, String>,
            defaultIndentation: String
        ): String {
            return StringBuilder().apply {
                if (map.containsKey("zero")) {
                    appendLine("${defaultIndentation}\tif (count == 0) {")
                    appendLine("${defaultIndentation}\t\treturn \"${map["zero"]}\"")
                    appendLine("${defaultIndentation}\t}")
                }

                if (map.containsKey("one")) {
                    appendLine("${defaultIndentation}\tif (count == 1) {")
                    appendLine("${defaultIndentation}\t\treturn \"${map["one"]}\"")
                    appendLine("${defaultIndentation}\t}")
                }

                if (map.containsKey("two")) {
                    appendLine("${defaultIndentation}\tif (count == 2) {")
                    appendLine("${defaultIndentation}\t\treturn \"${map["two"]}\"")
                    appendLine("${defaultIndentation}\t}")
                }

                if (map.containsKey("few")) {
                    appendLine("${defaultIndentation}\tif (count in 3..5) {")
                    appendLine("${defaultIndentation}\t\treturn \"${map["few"]}\"")
                    appendLine("${defaultIndentation}\t}")
                }

                if (map.containsKey("many")) {
                    appendLine("${defaultIndentation}\tif (count in 6..9) {")
                    appendLine("${defaultIndentation}\t\treturn \"${map["many"]}\"")
                    appendLine("${defaultIndentation}\t}")
                }

                if (map.containsKey("other")) {
                    appendLine("${defaultIndentation}\treturn \"${map["other"]}\"")
                } else {
                    appendLine("${defaultIndentation}\treturn \"\"")
                }
            }.toString()
        }
    }
}

