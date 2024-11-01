package dev.randos.resourcemanager.file.generation

import dev.randos.resourcemanager.file.parser.XmlParser
import dev.randos.resourcemanager.model.Resource
import dev.randos.resourcemanager.model.ResourceType
import dev.randos.resourcemanager.model.ValueResource
import dev.randos.resourcemanager.model.ValueResourceType
import dev.randos.resourcemanager.utils.toCamelCase
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
                appendLine("import android.content.res.Resources.Theme")
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
            val map = mutableMapOf<String, MutableList<ValueResource>>()
            resource.directoryPath.listFiles().getXmlFiles().forEach { file ->
                val resources = XmlParser.parseXML(file)
                // Only create object for file when given file has some resources (i.e. not empty).
                resources.forEach {
                    val key = it.type::class.simpleName.toString()
                    if(!map.containsKey(key)){
                        map[key] = mutableListOf()
                    }
                    map[key]?.add(it)
                }
            }

            map.forEach {
                val resourceObject = generateObject(
                    "${it.key}s",
                    it.value.sortedBy { resource -> resource.name }
                )
                appendLine(resourceObject)
            }
        }

        private fun generateObject(name: String, pairs: List<ValueResource>): String {
            val defaultIndentation = "\t\t"
            return StringBuilder().apply {
                appendLine("\tobject $name {")
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

                        is ValueResourceType.String -> appendStringResource(
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
            appendLine("${defaultIndentation}@JvmStatic fun ${resource.name.toCamelCase()}(theme: Theme = application.theme) : Int = application.resources.getColor(R.color.${resource.name}, theme)")
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
            appendLine("${defaultIndentation}@JvmStatic fun ${resource.name.toCamelCase()}(base: Int = 0, pbase: Int = 0) : Float = application.resources.getFraction(R.fraction.${resource.name}, base, pbase)")
        }

        private fun StringBuilder.appendStringResource(
            resource: ValueResource,
            defaultIndentation: String
        ) {
            if (resource.type is ValueResourceType.String && resource.type.isParameterized) {
                appendLine("${defaultIndentation}@JvmStatic fun ${resource.name.toCamelCase()}(vararg args: Any = emptyArray()) : String = if (args.isEmpty()) application.resources.getString(R.string.${resource.name}) else application.resources.getString(R.string.${resource.name}, *args)")
            } else {
                appendLine("${defaultIndentation}@JvmStatic fun ${resource.name.toCamelCase()}() : String = application.resources.getString(R.string.${resource.name})")
            }
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
