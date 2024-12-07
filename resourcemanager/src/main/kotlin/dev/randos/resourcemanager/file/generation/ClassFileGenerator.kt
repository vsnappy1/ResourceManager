package dev.randos.resourcemanager.file.generation

import dev.randos.resourcemanager.file.parser.XmlParser
import dev.randos.resourcemanager.model.ModuleDetails
import dev.randos.resourcemanager.model.Resource
import dev.randos.resourcemanager.model.ResourceType
import dev.randos.resourcemanager.model.ValueResource
import dev.randos.resourcemanager.model.ValueResourceType
import dev.randos.resourcemanager.utils.toCamelCase
import org.jetbrains.annotations.VisibleForTesting

internal object ClassFileGenerator {
    /**
     * A set to store the names of functions generated in the class, ensuring uniqueness.
     *
     * This mutable set keeps track of function names to avoid generating duplicate
     * function declarations in the output class.
     * Add is cleared before processing every new object class creation.
     */
    private val functionNames = mutableSetOf<String>()

    fun generateClassFile(
        namespace: String,
        files: List<Resource>
    ): String {
        return StringBuilder().apply {
            appendLine("package $namespace\n")
            appendLine("import android.app.Application")
            appendLine("import android.content.Context")
            appendLine("import android.graphics.drawable.Drawable")
            appendLine("import android.content.res.Resources.Theme")
            appendLine("import android.os.Build")
            appendLine("import $namespace.R\n")
            appendLine("object ResourceManager {\n")
            appendLine("\tprivate var _application: Application? = null")
            appendLine("\tprivate val application: Application")
            appendLine("\t\tget() = _application ?: throw IllegalStateException(\"ResourceManager is not initialized. Please invoke ResourceManager.initialize(this) in onCreate method of your Application class.\")\n")
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
            functionNames.clear()
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
        functionNames.clear()
        appendLine(getDocumentation("drawable", "Drawables"))
        appendLine("\t@Suppress(\"DEPRECATION\")")
        appendLine("\tobject Drawables {")
        resources.forEach { resource ->
            resource.moduleDetails.resourceFiles.sorted().forEach { file ->
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
        val map = mutableMapOf<ValueResourceType, MutableList<Pair<ModuleDetails, ValueResource>>>()
        resources.forEach { resource ->
            resource.moduleDetails.resourceFiles.forEach { file ->
                val xmlResources = XmlParser.parseXML(file)
                xmlResources.forEach {
                    val key = it.type
                    if (!map.containsKey(key)) {
                        map[key] = mutableListOf()
                    }
                    map[key]?.add(Pair(resource.moduleDetails, it))
                }
            }
        }

        map.toSortedMap { o1, o2 -> o1::class.simpleName!!.compareTo(o2::class.simpleName!!) }.forEach {
            val resourceObject = generateObject(it.key, it.value.sortedBy { pair -> pair.second.name })
            appendLine(resourceObject)
        }
    }

    private fun generateObject(
        valueResourceType: ValueResourceType,
        pairs: List<Pair<ModuleDetails, ValueResource>>
    ): String {
        val defaultIndentation = "\t\t"
        return StringBuilder().apply {
            functionNames.clear()

            val objectName = "${valueResourceType::class.simpleName}s"
            appendLine(getDocumentation(valueResourceType.value, objectName))
            if (valueResourceType is ValueResourceType.Color) {
                appendLine("\t@Suppress(\"DEPRECATION\")")
            }
            appendLine("\tobject $objectName {")

            when (valueResourceType) {
                ValueResourceType.Array, ValueResourceType.StringArray -> {
                    pairs.sortedBy { it.second.name }.forEach { (moduleDetails, resource) ->
                        appendStringArrayResource(
                            resource = resource,
                            defaultIndentation = defaultIndentation,
                            moduleDetails = moduleDetails
                        )
                    }
                }

                ValueResourceType.Boolean -> {
                    pairs.sortedBy { it.second.name }.forEach { (moduleDetails, resource) ->
                        appendBooleanResource(
                            resource = resource,
                            defaultIndentation = defaultIndentation,
                            moduleDetails = moduleDetails
                        )
                    }
                }

                ValueResourceType.Color -> {
                    pairs.sortedBy { it.second.name }.forEach { (moduleDetails, resource) ->
                        appendColorResource(
                            resource = resource,
                            defaultIndentation = defaultIndentation,
                            moduleDetails = moduleDetails
                        )
                    }
                }

                ValueResourceType.Dimension -> {
                    pairs.sortedBy { it.second.name }.forEach { (moduleDetails, resource) ->
                        appendDimensionResource(
                            resource = resource,
                            defaultIndentation = defaultIndentation,
                            moduleDetails = moduleDetails
                        )
                    }
                }

                ValueResourceType.Fraction -> {
                    pairs.sortedBy { it.second.name }.forEach { (moduleDetails, resource) ->
                        appendFractionResource(
                            resource = resource,
                            defaultIndentation = defaultIndentation,
                            moduleDetails = moduleDetails
                        )
                    }
                }

                ValueResourceType.IntArray -> {
                    pairs.sortedBy { it.second.name }.forEach { (moduleDetails, resource) ->
                        appendIntArrayResource(
                            resource = resource,
                            defaultIndentation = defaultIndentation,
                            moduleDetails = moduleDetails
                        )
                    }
                }

                ValueResourceType.Integer -> {
                    pairs.sortedBy { it.second.name }.forEach { (moduleDetails, resource) ->
                        appendIntegerResource(
                            resource = resource,
                            defaultIndentation = defaultIndentation,
                            moduleDetails = moduleDetails
                        )
                    }
                }

                ValueResourceType.Plural -> {
                    pairs.sortedBy { it.second.name }.forEach { (moduleDetails, resource) ->
                        appendPluralResource(
                            resource = resource,
                            defaultIndentation = defaultIndentation,
                            moduleDetails = moduleDetails
                        )
                    }
                }

                ValueResourceType.String -> {
                    pairs.sortedBy { it.second.name }.forEach { (moduleDetails, resource) ->
                        appendStringResource(
                            resource = resource,
                            defaultIndentation = defaultIndentation,
                            moduleDetails = moduleDetails
                        )
                    }
                }
            }
            appendLine("\t}")
        }.toString()
    }

    private fun getDocumentation(
        resourceType: String,
        objectName: String
    ): String {
        return """
            /**
             * Provides access to $resourceType resources.
             *
             * By default, each function in the `$objectName` object uses the `Application` context to
             * fetch $resourceType resource.
             *
             * To support dynamic locale, theme or orientation changes at runtime, pass
             * the appropriate context (e.g., an `Activity` context) to the function.
             */
            """.trimIndent().prependIndent("\t")
    }

    private fun StringBuilder.appendDrawableResource(
        name: String,
        defaultIndentation: String,
        moduleDetails: ModuleDetails
    ) {
        val namespaceString = getNamespace(moduleDetails)
        val moduleName = getDrawableMethodName(name, moduleDetails)
        if (isMethodNameUsedBefore(moduleName)) return
        appendLine(
            "$defaultIndentation@JvmOverloads @JvmStatic fun $moduleName(context: Context = application) : Drawable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) { context.resources.getDrawable(${namespaceString}R.drawable.$name, context.theme) } else { context.resources.getDrawable(${namespaceString}R.drawable.$name) }"
        )
    }

    private fun StringBuilder.appendDimensionResource(
        resource: ValueResource,
        defaultIndentation: String,
        moduleDetails: ModuleDetails
    ) {
        val namespaceString = getNamespace(moduleDetails)
        val methodName = getValueResourceMethodName(resource, moduleDetails)
        if (isMethodNameUsedBefore(methodName)) return
        appendLine(
            "$defaultIndentation@JvmOverloads @JvmStatic fun $methodName(context: Context = application) : Float = context.resources.getDimension(${namespaceString}R.dimen.${resource.name})"
        )
    }

    private fun StringBuilder.appendColorResource(
        resource: ValueResource,
        defaultIndentation: String,
        moduleDetails: ModuleDetails
    ) {
        val namespaceString = getNamespace(moduleDetails)
        val methodName = getValueResourceMethodName(resource, moduleDetails)
        if (isMethodNameUsedBefore(methodName)) return
        appendLine(
            "$defaultIndentation@JvmOverloads @JvmStatic fun $methodName(context: Context = application) : Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { context.resources.getColor(${namespaceString}R.color.${resource.name}, context.theme) } else { context.resources.getColor(${namespaceString}R.color.${resource.name}) }"
        )
    }

    private fun StringBuilder.appendIntegerResource(
        resource: ValueResource,
        defaultIndentation: String,
        moduleDetails: ModuleDetails
    ) {
        val namespaceString = getNamespace(moduleDetails)
        val methodName = getValueResourceMethodName(resource, moduleDetails)
        if (isMethodNameUsedBefore(methodName)) return
        appendLine(
            "$defaultIndentation@JvmOverloads @JvmStatic fun $methodName(context: Context = application) : Int = context.resources.getInteger(${namespaceString}R.integer.${resource.name})"
        )
    }

    private fun StringBuilder.appendBooleanResource(
        resource: ValueResource,
        defaultIndentation: String,
        moduleDetails: ModuleDetails
    ) {
        val namespaceString = getNamespace(moduleDetails)
        val methodName = getValueResourceMethodName(resource, moduleDetails)
        if (isMethodNameUsedBefore(methodName)) return
        appendLine(
            "$defaultIndentation@JvmOverloads @JvmStatic fun $methodName(context: Context = application) : Boolean = context.resources.getBoolean(${namespaceString}R.bool.${resource.name})"
        )
    }

    private fun StringBuilder.appendFractionResource(
        resource: ValueResource,
        defaultIndentation: String,
        moduleDetails: ModuleDetails
    ) {
        val namespaceString = getNamespace(moduleDetails)
        val methodName = getValueResourceMethodName(resource, moduleDetails)
        if (isMethodNameUsedBefore(methodName)) return
        appendLine(
            "$defaultIndentation@JvmOverloads @JvmStatic fun $methodName(base: Int = 0, pbase: Int = 0, context: Context = application) : Float = context.resources.getFraction(${namespaceString}R.fraction.${resource.name}, base, pbase)"
        )
    }

    private fun StringBuilder.appendStringResource(
        resource: ValueResource,
        defaultIndentation: String,
        moduleDetails: ModuleDetails
    ) {
        val namespaceString = getNamespace(moduleDetails)
        val methodName = getValueResourceMethodName(resource, moduleDetails)
        if (isMethodNameUsedBefore(methodName)) return
        appendLine(
            "$defaultIndentation@JvmOverloads @JvmStatic fun $methodName(vararg args: Any? = emptyArray(), context: Context = application) : String = if (args.isEmpty()) context.resources.getString(${namespaceString}R.string.${resource.name}) else context.resources.getString(${namespaceString}R.string.${resource.name}, *args)"
        )
    }

    private fun StringBuilder.appendPluralResource(
        resource: ValueResource,
        defaultIndentation: String,
        moduleDetails: ModuleDetails
    ) {
        val namespaceString = getNamespace(moduleDetails)
        val methodName = getValueResourceMethodName(resource, moduleDetails)
        if (isMethodNameUsedBefore(methodName)) return
        appendLine(
            "$defaultIndentation@JvmOverloads @JvmStatic fun $methodName(quantity: Int, vararg args: Any = emptyArray(), context: Context = application) : String = context.resources.getQuantityString(${namespaceString}R.plurals.${resource.name}, quantity, args)"
        )
    }

    private fun StringBuilder.appendStringArrayResource(
        resource: ValueResource,
        defaultIndentation: String,
        moduleDetails: ModuleDetails
    ) {
        val namespaceString = getNamespace(moduleDetails)
        val methodName = getValueResourceMethodName(resource, moduleDetails)
        if (isMethodNameUsedBefore(methodName)) return
        appendLine(
            "$defaultIndentation@JvmOverloads @JvmStatic fun $methodName(context: Context = application) : kotlin.Array<String> = context.resources.getStringArray(${namespaceString}R.array.${resource.name})"
        )
    }

    private fun StringBuilder.appendIntArrayResource(
        resource: ValueResource,
        defaultIndentation: String,
        moduleDetails: ModuleDetails
    ) {
        val namespaceString = getNamespace(moduleDetails)
        val methodName = getValueResourceMethodName(resource, moduleDetails)
        if (isMethodNameUsedBefore(methodName)) return
        appendLine(
            "$defaultIndentation@JvmOverloads @JvmStatic fun $methodName(context: Context = application) : IntArray = context.resources.getIntArray(${namespaceString}R.array.${resource.name})"
        )
    }

    /**
     * Checks whether the given method name has already been used in the generated ResourceManager class
     * (specifically in object currently being processed).
     *
     * @param methodName The name of the method to check for uniqueness.
     * @return `true` if the method name has been used before, otherwise `false`.
     */
    private fun isMethodNameUsedBefore(methodName: String): Boolean {
        /*
          This ensures there are only unique function names in the generated ResourceManager class to avoid
          conflicting overloads(compile time error).
         */
        return !functionNames.add(methodName)
    }

    @VisibleForTesting
    fun getFunctionNamesSize(): Int {
        return functionNames.size
    }

    /**
     * Generates a method name based on the provided resource name and module details.
     * Combines the resource name in camel case with the module name if it exists.
     *
     * @return A string representing the generated method name in camel case format.
     */
    fun getValueResourceMethodName(
        resource: ValueResource,
        moduleDetails: ModuleDetails
    ): String {
        val moduleNameString = getModuleNameString(moduleDetails)
        return "${resource.name.toCamelCase()}$moduleNameString"
    }

    /**
     * Generates a method name based on the provided drawable resource nameWithoutExtension and module details.
     * Combines the resource nameWithoutExtension in camel case with the module name if it exists.
     *
     * @return A string representing the generated method name in camel case format.
     */
    fun getDrawableMethodName(
        name: String,
        moduleDetails: ModuleDetails
    ): String {
        val moduleNameString = getModuleNameString(moduleDetails)
        return "${name.toCamelCase()}$moduleNameString"
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