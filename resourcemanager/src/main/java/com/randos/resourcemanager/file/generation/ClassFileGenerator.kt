package com.randos.resourcemanager.file.generation

import com.randos.resourcemanager.file.parser.XmlParser
import com.randos.resourcemanager.model.Resource
import com.randos.resourcemanager.model.ResourceType
import com.randos.resourcemanager.utils.toCamelCase
import java.io.File

internal class ClassFileGenerator {

    companion object {
        fun generateClassFile(packageName: String, files: List<File>): String {
            return StringBuilder().apply {
                appendLine("package $packageName\n")
                appendLine("import android.app.Application")
                appendLine("import ${packageName}.R\n")
                appendLine("object ResourceManager {\n")
                appendLine("\tprivate var _application: Application? = null")
                appendLine("\tprivate val application: Application")
                appendLine("\t\tget() = _application ?: throw IllegalStateException(\"Application is not initialized, seems like you forgot to invoke ResourceManager.initialize(this) in application class.\")\n")
                appendLine("\tfun initialize(application: Application) {")
                appendLine("\t\t_application = application")
                appendLine("\t}\n")

                files.forEach { file ->
                    val resources = XmlParser.parseXML(file)
                    // Only create object for file when given file has some resources (i.e. not empty).
                    if (resources.isNotEmpty()) {
                        val resourceObject = generateObject(
                            file.nameWithoutExtension,
                            resources
                        )
                        appendLine(resourceObject)
                    }
                }
                appendLine("}")
            }.toString()
        }

        private fun generateObject(name: String, pairs: List<Resource>): String {
            val defaultIndentation = "\t\t"
            return StringBuilder().apply {
                appendLine("\tobject ${name.toCamelCase().replaceFirstChar { it.uppercase() }} {")
                pairs.forEach { resource ->
                    when (resource.type) {
                        ResourceType.Array -> appendStringArrayResource(resource, defaultIndentation)
                        ResourceType.Boolean -> appendBooleanResource(resource, defaultIndentation)
                        ResourceType.Color -> appendColorResource(resource, defaultIndentation)
                        ResourceType.Dimension -> appendDimensionResource(resource, defaultIndentation)
                        ResourceType.Fraction -> appendFractionResource(resource, defaultIndentation)
                        ResourceType.IntArray -> appendIntArrayResource(resource, defaultIndentation)
                        ResourceType.Integer -> appendIntegerResource(resource, defaultIndentation)
                        ResourceType.Plural -> appendPluralResource(resource, defaultIndentation)
                        ResourceType.String -> appendStringResource(resource, defaultIndentation)
                        ResourceType.StringArray -> appendStringArrayResource(resource, defaultIndentation)
                    }
                }
                appendLine("\t}")
            }.toString()
        }

        private fun StringBuilder.appendDimensionResource(resource: Resource, defaultIndentation: String) {
            appendLine("${defaultIndentation}fun ${resource.name.toCamelCase()}() : Float = application.resources.getDimension(R.dimen.${resource.name})")
        }

        private fun StringBuilder.appendColorResource(resource: Resource, defaultIndentation: String) {
            appendLine("${defaultIndentation}fun ${resource.name.toCamelCase()}() : Int = application.resources.getColor(R.color.${resource.name}, application.theme)")
        }

        private fun StringBuilder.appendIntegerResource(resource: Resource, defaultIndentation: String) {
            appendLine("${defaultIndentation}fun ${resource.name.toCamelCase()}() : Int = application.resources.getInteger(R.integer.${resource.name})")
        }

        private fun StringBuilder.appendBooleanResource(resource: Resource, defaultIndentation: String) {
            appendLine("${defaultIndentation}fun ${resource.name.toCamelCase()}() : Boolean = application.resources.getBoolean(R.bool.${resource.name})")
        }

        private fun StringBuilder.appendFractionResource(resource: Resource, defaultIndentation: String) {
            appendLine("${defaultIndentation}fun ${resource.name.toCamelCase()}() : Float = application.resources.getFraction(R.fraction.${resource.name}, 0, 0)")
        }

        private fun StringBuilder.appendStringResource(resource: Resource, defaultIndentation: String) {
            appendLine("${defaultIndentation}fun ${resource.name.toCamelCase()}() : String = application.resources.getString(R.string.${resource.name})")
        }

        private fun StringBuilder.appendPluralResource(resource: Resource, defaultIndentation: String) {
            appendLine("${defaultIndentation}fun ${resource.name.toCamelCase()}(quantity: Int) : String = application.resources.getQuantityString(R.plurals.${resource.name}, quantity)")
        }

        private fun StringBuilder.appendStringArrayResource(resource: Resource, defaultIndentation: String) {
            appendLine("${defaultIndentation}fun ${resource.name.toCamelCase()}() : kotlin.Array<String> = application.resources.getStringArray(R.array.${resource.name})")
        }

        private fun StringBuilder.appendIntArrayResource(resource: Resource, defaultIndentation: String) {
            appendLine("${defaultIndentation}fun ${resource.name.toCamelCase()}() : IntArray = application.resources.getIntArray(R.array.${resource.name})")
        }
    }
}


