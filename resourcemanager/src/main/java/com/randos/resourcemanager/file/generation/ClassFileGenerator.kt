package com.randos.resourcemanager.file.generation

import com.randos.resourcemanager.file.parser.XmlParser
import com.randos.resourcemanager.model.Resource
import com.randos.resourcemanager.model.ResourceType
import com.randos.resourcemanager.model.ValueResource
import com.randos.resourcemanager.model.ValueResourceType
import com.randos.resourcemanager.utils.toCamelCase
import java.io.File

internal class ClassFileGenerator {

    companion object {
        fun generateClassFile(
            packageName: String,
            namespace: String,
            files: List<Resource>
        ): String {
            return StringBuilder().apply {
                appendLine("package $packageName\n")
                appendLine("import android.app.Application")
                appendLine("import android.graphics.drawable.Drawable")
                appendLine("import ${namespace}.R\n")
                appendLine("object ResourceManager {\n")
                appendLine("\tprivate var _application: Application? = null")
                appendLine("\tprivate val application: Application")
                appendLine("\t\tget() = _application ?: throw IllegalStateException(\"ResourceManager is not initialized. Please call ResourceManager.initialize(this) in your Application class.\")\n")
                appendLine("\tfun initialize(application: Application) {")
                appendLine("\t\t_application = application")
                appendLine("\t}\n")

                files.forEach { resource ->
                    appendLine("\t// ----- ${resource.type::class.simpleName} -----")
                    when (resource.type) {
                        ResourceType.VALUES -> {
                            generateObjectForValueResources(resource)
                        }

                        ResourceType.DRAWABLES -> {
                            generateObjectForDrawableResources(resource)
                        }
                    }
                }
                appendLine("}")
            }.toString()
        }

        private fun StringBuilder.generateObjectForDrawableResources(resource: Resource) {
            appendLine("\tobject Drawables {")
            resource.directoryPath.listFiles()?.forEach { file ->
                appendDrawableResource(file.nameWithoutExtension, "\t\t")
            }
            appendLine("\t}")
        }

        private fun StringBuilder.generateObjectForValueResources(resource: Resource) {
            resource.directoryPath.listFiles().getXmlFiles().forEach { file ->
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
        }

        private fun generateObject(name: String, pairs: List<ValueResource>): String {
            val defaultIndentation = "\t\t"
            return StringBuilder().apply {
                appendLine("\tobject ${name.toCamelCase().replaceFirstChar { it.uppercase() }} {")
                pairs.forEach { resource ->
                    when (resource.type) {
                        ValueResourceType.Array, ValueResourceType.StringArray -> appendStringArrayResource(
                            resource,
                            defaultIndentation
                        )

                        ValueResourceType.Boolean -> appendBooleanResource(
                            resource,
                            defaultIndentation
                        )

                        ValueResourceType.Color -> appendColorResource(resource, defaultIndentation)
                        ValueResourceType.Dimension -> appendDimensionResource(
                            resource,
                            defaultIndentation
                        )

                        ValueResourceType.Fraction -> appendFractionResource(
                            resource,
                            defaultIndentation
                        )

                        ValueResourceType.IntArray -> appendIntArrayResource(
                            resource,
                            defaultIndentation
                        )

                        ValueResourceType.Integer -> appendIntegerResource(
                            resource,
                            defaultIndentation
                        )

                        ValueResourceType.Plural -> appendPluralResource(
                            resource,
                            defaultIndentation
                        )

                        ValueResourceType.String -> appendStringResource(
                            resource,
                            defaultIndentation
                        )
                    }
                }
                appendLine("\t}")
            }.toString()
        }

        private fun StringBuilder.appendDrawableResource(name: String, defaultIndentation: String) {
            appendLine("${defaultIndentation}@JvmStatic fun ${name.toCamelCase()}() : Drawable = application.resources.getDrawable(R.drawable.${name}, application.theme)")
        }

        private fun StringBuilder.appendDimensionResource(
            resource: ValueResource,
            defaultIndentation: String
        ) {
            appendLine("${defaultIndentation}@JvmStatic fun ${resource.name.toCamelCase()}() : Float = application.resources.getDimension(R.dimen.${resource.name})")
        }

        private fun StringBuilder.appendColorResource(
            resource: ValueResource,
            defaultIndentation: String
        ) {
            appendLine("${defaultIndentation}@JvmStatic fun ${resource.name.toCamelCase()}() : Int = application.resources.getColor(R.color.${resource.name}, application.theme)")
        }

        private fun StringBuilder.appendIntegerResource(
            resource: ValueResource,
            defaultIndentation: String
        ) {
            appendLine("${defaultIndentation}@JvmStatic fun ${resource.name.toCamelCase()}() : Int = application.resources.getInteger(R.integer.${resource.name})")
        }

        private fun StringBuilder.appendBooleanResource(
            resource: ValueResource,
            defaultIndentation: String
        ) {
            appendLine("${defaultIndentation}@JvmStatic fun ${resource.name.toCamelCase()}() : Boolean = application.resources.getBoolean(R.bool.${resource.name})")
        }

        private fun StringBuilder.appendFractionResource(
            resource: ValueResource,
            defaultIndentation: String
        ) {
            appendLine("${defaultIndentation}@JvmStatic fun ${resource.name.toCamelCase()}() : Float = application.resources.getFraction(R.fraction.${resource.name}, 0, 0)")
        }

        private fun StringBuilder.appendStringResource(
            resource: ValueResource,
            defaultIndentation: String
        ) {
            appendLine("${defaultIndentation}@JvmStatic fun ${resource.name.toCamelCase()}() : String = application.resources.getString(R.string.${resource.name})")
        }

        private fun StringBuilder.appendPluralResource(
            resource: ValueResource,
            defaultIndentation: String
        ) {
            appendLine("${defaultIndentation}@JvmStatic fun ${resource.name.toCamelCase()}(quantity: Int) : String = application.resources.getQuantityString(R.plurals.${resource.name}, quantity)")
        }

        private fun StringBuilder.appendStringArrayResource(
            resource: ValueResource,
            defaultIndentation: String
        ) {
            appendLine("${defaultIndentation}@JvmStatic fun ${resource.name.toCamelCase()}() : kotlin.Array<String> = application.resources.getStringArray(R.array.${resource.name})")
        }

        private fun StringBuilder.appendIntArrayResource(
            resource: ValueResource,
            defaultIndentation: String
        ) {
            appendLine("${defaultIndentation}@JvmStatic fun ${resource.name.toCamelCase()}() : IntArray = application.resources.getIntArray(R.array.${resource.name})")
        }
    }
}

private fun Array<File>?.getXmlFiles(): List<File> {
    return this?.filter { it.extension == "xml" } ?: emptyList()
}
