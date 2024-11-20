package dev.randos.resourcemanager.file.generation

import dev.randos.resourcemanager.file.parser.XmlParser
import dev.randos.resourcemanager.model.ModuleDetails
import dev.randos.resourcemanager.model.Resource
import dev.randos.resourcemanager.model.ResourceType
import dev.randos.resourcemanager.model.ValueResource
import dev.randos.resourcemanager.model.ValueResourceType
import dev.randos.resourcemanager.utils.toCamelCase
import java.io.File

internal object ClassFileGenerator {
    fun generateClassFile(
        namespace: String,
        files: List<Resource>
    ): String {
        return StringBuilder().apply {
            appendLine("package $namespace\n")
            appendLine("import android.app.Application")
            appendLine("import android.graphics.drawable.Drawable")
            appendLine("import android.content.res.Resources.Theme")
            appendLine("import $namespace.R\n")
            appendLine("object ResourceManager {\n")
            appendLine("\tprivate var _application: Application? = null")
            appendLine("\tprivate val application: Application")
            appendLine("\t\tget() = _application ?: throw IllegalStateException(\"ResourceManager is not initialized. Please call ResourceManager.initialize(this) in your Application class.\")\n")
            appendLine("\t@JvmStatic")
            appendLine("\tfun initialize(application: Application) {")
            appendLine("\t\t_application = application")
            appendLine("\t}\n")

            val resourceMap = files.groupBy { it.type }.toSortedMap(resourceTypeComparator())

            resourceMap.forEach { (resourceType, value) ->
                appendLine("\t// ----- ${resourceType::class.simpleName} -----")
                when (resourceType) {
                    ResourceType.VALUES -> {
                        generateObjectForValueResources(value)
                    }

                    ResourceType.DRAWABLES -> {
                        generateObjectForDrawableResources(value)
                    }
                }
            }
            appendLine("}")
        }.toString()
    }

    private fun resourceTypeComparator() =
        object : Comparator<ResourceType> {
            override fun compare(
                rt1: ResourceType,
                rt2: ResourceType
            ): Int {
                val name1 = rt1::class.simpleName
                val name2 = rt2::class.simpleName
                if (name1 == null || name2 == null) return 0
                return name2 compareTo name1
            }
        }

    private fun StringBuilder.generateObjectForDrawableResources(resources: List<Resource>) {
        appendLine("\tobject Drawables {")
        resources.forEach { resource ->
            resource.moduleDetails.resDirectory.listFiles()?.sorted()?.forEach { file ->
                appendDrawableResource(
                    name = file.nameWithoutExtension,
                    defaultIndentation = "\t\t",
                    moduleDetails = resource.moduleDetails
                )
            }
        }
        appendLine("\t}")
    }

    private fun StringBuilder.generateObjectForValueResources(resources: List<Resource>) {
        val map = mutableMapOf<String, MutableList<Pair<ModuleDetails, ValueResource>>>()
        resources.forEach { resource ->
            resource.moduleDetails.resDirectory.listFiles().getXmlFiles().forEach { file ->
                val xmlResources = XmlParser.parseXML(file)
                xmlResources.forEach {
                    val key = it.type::class.simpleName.toString()
                    if (!map.containsKey(key)) {
                        map[key] = mutableListOf()
                    }
                    map[key]?.add(Pair(resource.moduleDetails, it))
                }
            }
        }

        map.toSortedMap().forEach {
            val resourceObject = generateObject("${it.key}s", it.value.sortedBy { pair -> pair.second.name })
            appendLine(resourceObject)
        }
    }

    private fun generateObject(
        name: String,
        pairs: List<Pair<ModuleDetails, ValueResource>>
    ): String {
        val defaultIndentation = "\t\t"
        return StringBuilder().apply {
            appendLine("\tobject $name {")
            pairs.sortedBy { it.second.name }.forEach { (moduleDetails, resource) ->
                when (resource.type) {
                    ValueResourceType.Array, ValueResourceType.StringArray ->
                        appendStringArrayResource(
                            resource = resource,
                            defaultIndentation = defaultIndentation,
                            moduleDetails = moduleDetails
                        )

                    ValueResourceType.Boolean ->
                        appendBooleanResource(
                            resource = resource,
                            defaultIndentation = defaultIndentation,
                            moduleDetails = moduleDetails
                        )

                    ValueResourceType.Color ->
                        appendColorResource(
                            resource = resource,
                            defaultIndentation = defaultIndentation,
                            moduleDetails = moduleDetails
                        )

                    ValueResourceType.Dimension ->
                        appendDimensionResource(
                            resource = resource,
                            defaultIndentation = defaultIndentation,
                            moduleDetails = moduleDetails
                        )

                    ValueResourceType.Fraction ->
                        appendFractionResource(
                            resource = resource,
                            defaultIndentation = defaultIndentation,
                            moduleDetails = moduleDetails
                        )

                    ValueResourceType.IntArray ->
                        appendIntArrayResource(
                            resource = resource,
                            defaultIndentation = defaultIndentation,
                            moduleDetails = moduleDetails
                        )

                    ValueResourceType.Integer ->
                        appendIntegerResource(
                            resource = resource,
                            defaultIndentation = defaultIndentation,
                            moduleDetails = moduleDetails
                        )

                    ValueResourceType.Plural ->
                        appendPluralResource(
                            resource = resource,
                            defaultIndentation = defaultIndentation,
                            moduleDetails = moduleDetails
                        )

                    is ValueResourceType.String ->
                        appendStringResource(
                            resource = resource,
                            defaultIndentation = defaultIndentation,
                            moduleDetails = moduleDetails
                        )
                }
            }
            appendLine("\t}")
        }.toString()
    }

    private fun StringBuilder.appendDrawableResource(
        name: String,
        defaultIndentation: String,
        moduleDetails: ModuleDetails
    ) {
        val namespaceString = getNamespace(moduleDetails)
        val moduleNameString = getModuleNameString(moduleDetails)
        appendLine(
            "$defaultIndentation@JvmOverloads @JvmStatic fun ${name.toCamelCase()}$moduleNameString(theme: Theme = application.theme) : Drawable = application.resources.getDrawable(${namespaceString}R.drawable.$name, theme)"
        )
    }

    private fun StringBuilder.appendDimensionResource(
        resource: ValueResource,
        defaultIndentation: String,
        moduleDetails: ModuleDetails
    ) {
        val namespaceString = getNamespace(moduleDetails)
        val methodName = getMethodName(resource, moduleDetails)
        appendLine(
            "$defaultIndentation@JvmStatic fun $methodName() : Float = application.resources.getDimension(${namespaceString}R.dimen.${resource.name})"
        )
    }

    private fun StringBuilder.appendColorResource(
        resource: ValueResource,
        defaultIndentation: String,
        moduleDetails: ModuleDetails
    ) {
        val namespaceString = getNamespace(moduleDetails)
        val methodName = getMethodName(resource, moduleDetails)
        appendLine(
            "$defaultIndentation@JvmOverloads @JvmStatic fun $methodName(theme: Theme = application.theme) : Int = application.resources.getColor(${namespaceString}R.color.${resource.name}, theme)"
        )
    }

    private fun StringBuilder.appendIntegerResource(
        resource: ValueResource,
        defaultIndentation: String,
        moduleDetails: ModuleDetails
    ) {
        val namespaceString = getNamespace(moduleDetails)
        val methodName = getMethodName(resource, moduleDetails)
        appendLine(
            "$defaultIndentation@JvmStatic fun $methodName() : Int = application.resources.getInteger(${namespaceString}R.integer.${resource.name})"
        )
    }

    private fun StringBuilder.appendBooleanResource(
        resource: ValueResource,
        defaultIndentation: String,
        moduleDetails: ModuleDetails
    ) {
        val namespaceString = getNamespace(moduleDetails)
        val methodName = getMethodName(resource, moduleDetails)
        appendLine(
            "$defaultIndentation@JvmStatic fun $methodName() : Boolean = application.resources.getBoolean(${namespaceString}R.bool.${resource.name})"
        )
    }

    private fun StringBuilder.appendFractionResource(
        resource: ValueResource,
        defaultIndentation: String,
        moduleDetails: ModuleDetails
    ) {
        val namespaceString = getNamespace(moduleDetails)
        val methodName = getMethodName(resource, moduleDetails)
        appendLine(
            "$defaultIndentation@JvmStatic fun $methodName(base: Int = 0, pbase: Int = 0) : Float = application.resources.getFraction(${namespaceString}R.fraction.${resource.name}, base, pbase)"
        )
    }

    private fun StringBuilder.appendStringResource(
        resource: ValueResource,
        defaultIndentation: String,
        moduleDetails: ModuleDetails
    ) {
        val namespaceString = getNamespace(moduleDetails)
        val methodName = getMethodName(resource, moduleDetails)
        appendLine(
            "$defaultIndentation@JvmStatic fun $methodName(vararg args: Any? = emptyArray()) : String = if (args.isEmpty()) application.resources.getString(${namespaceString}R.string.${resource.name}) else application.resources.getString(${namespaceString}R.string.${resource.name}, *args)"
        )
    }

    private fun StringBuilder.appendPluralResource(
        resource: ValueResource,
        defaultIndentation: String,
        moduleDetails: ModuleDetails
    ) {
        val namespaceString = getNamespace(moduleDetails)
        val methodName = getMethodName(resource, moduleDetails)
        appendLine(
            "$defaultIndentation@JvmStatic fun $methodName(quantity: Int, vararg args: Any = emptyArray()) : String = application.resources.getQuantityString(${namespaceString}R.plurals.${resource.name}, quantity, args)"
        )
    }

    private fun StringBuilder.appendStringArrayResource(
        resource: ValueResource,
        defaultIndentation: String,
        moduleDetails: ModuleDetails
    ) {
        val namespaceString = getNamespace(moduleDetails)
        val methodName = getMethodName(resource, moduleDetails)
        appendLine(
            "$defaultIndentation@JvmStatic fun $methodName() : kotlin.Array<String> = application.resources.getStringArray(${namespaceString}R.array.${resource.name})"
        )
    }

    private fun StringBuilder.appendIntArrayResource(
        resource: ValueResource,
        defaultIndentation: String,
        moduleDetails: ModuleDetails
    ) {
        val namespaceString = getNamespace(moduleDetails)
        val methodName = getMethodName(resource, moduleDetails)
        appendLine(
            "$defaultIndentation@JvmStatic fun $methodName() : IntArray = application.resources.getIntArray(${namespaceString}R.array.${resource.name})"
        )
    }

    /**
     * Generates a method name based on the provided resource name and module details.
     * Combines the resource name in camel case with the module name if it exists.
     *
     * @return A string representing the generated method name in camel case format.
     */
    private fun getMethodName(
        resource: ValueResource,
        moduleDetails: ModuleDetails
    ): String {
        val moduleNameString = getModuleNameString(moduleDetails)
        return "${resource.name.toCamelCase()}$moduleNameString"
    }

    /**
     * Retrieves a formatted module name string based on the provided module details.
     * If the module name is not empty, it is appended in camel case format prefixed with an underscore.
     *
     * @return The formatted module name string, or an empty string if no module name is provided.
     */
    private fun getModuleNameString(moduleDetails: ModuleDetails): String {
        // Replace all non-word characters in module name with _
        val moduleName = moduleDetails.moduleName.replace(Regex("\\W"), "_")
        return if (moduleDetails.moduleName.isNotEmpty()) "_${moduleName.toCamelCase()}" else ""
    }

    /**
     * Retrieves the namespace from the module details, if specified, followed by a period.
     *
     * @return The namespace with a trailing period, or an empty string if no namespace is specified.
     */
    private fun getNamespace(moduleDetails: ModuleDetails) =
        if (moduleDetails.namespace.isNotEmpty()) "${moduleDetails.namespace}." else ""
}

private fun Array<File>?.getXmlFiles(): List<File> {
    return this?.filter { it.extension == "xml" } ?: emptyList()
}