package com.randos.resourcemanager.file.generation

import com.randos.resourcemanager.file.parser.XmlParser
import com.randos.resourcemanager.model.Resource
import com.randos.resourcemanager.utils.toCamelCase
import java.io.File

internal class ClassFileGenerator {

    companion object {
        fun generateClassFile(packageName: String, files: List<File>): String {
            return StringBuilder().apply {
                appendLine("package $packageName\n")
                appendLine("object ResourceManager {\n")
                files.forEach {
                    val resources = XmlParser.parseXML(it)
                    // Only create object for file when given file has some resources (i.e. not empty).
                    if (resources.isNotEmpty()) {
                        val resourceObject = generateObject(
                            it.nameWithoutExtension,
                            resources
                        )
                        appendLine(resourceObject)
                    }
                }
                appendLine("}")
            }.toString()
        }

        private fun generateObject(name: String, pairs: List<Resource<Any>>): String {
            return StringBuilder().apply {
                appendLine("\tobject ${name.toCamelCase().replaceFirstChar { it.uppercase() }} {")
                pairs.forEach { resource ->
                    when (resource.value::class.simpleName) {
                        String::class.simpleName -> {
                            appendLine("\t\tfun ${resource.name}() : ${resource.returnType} = \"${resource.value}\"")
                        }

                        Float::class.simpleName -> {
                            appendLine("\t\tfun ${resource.name}() : ${resource.returnType} = ${resource.value}f")
                        }

                        ArrayList::class.simpleName -> {
                            appendLine(
                                "\t\tfun ${resource.name}() : ${resource.returnType} = listOf(${
                                    resource.value.toString().replace(Regex("[\\[\\]]"), "")
                                })"
                            )
                        }

                        HashMap::class.simpleName -> {
                            if (resource.value is Map<*, *> && resource.value.all { it.key is String && it.value is String }) {
                                @Suppress("UNCHECKED_CAST") // Suppress the warning because we've checked the types
                                append(generateFunctionForPlural(resource.name, resource.value as Map<String, String>))
                            }
                        }

                        else -> {
                            appendLine("\t\tfun ${resource.name}() : ${resource.returnType} = ${resource.value}")
                        }
                    }
                }
                appendLine("\t}")
            }.toString()
        }

        private fun generateFunctionForPlural(name: String, map: Map<String, String>): String {
            return StringBuilder().apply {
                appendLine("\t\tfun ${name}String(count: Int): String {")
                if (map.containsKey("zero")) {
                    appendLine("\t\t\tif (count == 0) {")
                    appendLine("\t\t\t\treturn \"${map["zero"]}\"")
                    appendLine("\t\t\t}")
                }

                if (map.containsKey("one")) {
                    appendLine("\t\t\tif (count == 1) {")
                    appendLine("\t\t\t\treturn \"${map["one"]}\"")
                    appendLine("\t\t\t}")
                }

                if (map.containsKey("two")) {
                    appendLine("\t\t\tif (count == 2) {")
                    appendLine("\t\t\t\treturn \"${map["two"]}\"")
                    appendLine("\t\t\t}")
                }

                if (map.containsKey("few")) {
                    appendLine("\t\t\tif (count in 3..5) {")
                    appendLine("\t\t\t\treturn \"${map["few"]}\"")
                    appendLine("\t\t\t}")
                }

                if (map.containsKey("many")) {
                    appendLine("\t\t\tif (count in 6..9) {")
                    appendLine("\t\t\t\treturn \"${map["many"]}\"")
                    appendLine("\t\t\t}")
                }

                if (map.containsKey("other")) {
                    appendLine("\t\t\treturn \"${map["other"]}\"")
                }else{
                    appendLine("\t\t\treturn \"\"")
                }
                appendLine("\t\t}")
            }.toString()
        }
    }
}
