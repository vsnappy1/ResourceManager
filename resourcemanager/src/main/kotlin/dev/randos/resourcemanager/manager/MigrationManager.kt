package dev.randos.resourcemanager.manager

import dev.randos.resourcemanager.file.generation.ClassFileGenerator
import dev.randos.resourcemanager.file.generation.ReportGenerator
import dev.randos.resourcemanager.file.parser.XmlParser
import dev.randos.resourcemanager.model.Change
import dev.randos.resourcemanager.model.Resource
import dev.randos.resourcemanager.model.ResourceType
import dev.randos.resourcemanager.model.SourceFileDetails
import dev.randos.resourcemanager.utils.getXmlFiles
import java.io.File

/**
 * Responsible for handling the migration to ResourceManager way of accessing
 * android resources (drawables, strings, etc).
 *
 */
internal class MigrationManager(
    private val projectDir: File,
    private val moduleDir: File
) {
    /**
     * A common regex pattern used to match optional method or property chains preceding a resource call.
     *
     * Examples of matches:
     * - `someObject.`
     * - `methodCall().`
     */
    private val common = "(?:\\w+\\??\\.|\\w+(?:<.*>)?\\(\\)\\??\\.)*"

    /**
     * A regex pattern to identify import statements for Android resource classes.
     */
    private val importStatementRegex = Regex("import (.+)\\.R;?$")

    /**
     * A lazily-initialized map of regex patterns to their corresponding replacement strings.
     *
     * Each regex pattern is designed to match resource retrieval calls (e.g., `getString`, `getColor`)
     * and map them to a standardized `ResourceManager` API format.
     *
     * Example transformations:
     * - `getString(R.string.app_name)` -> `ResourceManager.Strings.app_name`
     * - `getColor(R.color.primary)` -> `ResourceManager.Colors.primary`
     */
    private val replacements: Map<Regex, String> by lazy {
        mapOf(
            getZeroParamRegex("getBoolean", "bool") to "Booleans",
            getZeroParamRegex("getColor", "color") to "Colors",
            getZeroParamRegex("getDimension", "dimen") to "Dimensions",
            getZeroParamRegex("getDrawable", "drawable") to "Drawables",
            getZeroParamRegex("getIntArray", "array") to "IntArrays",
            getZeroParamRegex("getInteger", "integer") to "Integers",
            getZeroParamRegex("getString", "string") to "Strings",
            getZeroParamRegex("getStringArray", "array") to "StringArrays",
            getOneParamRegex("getColor", "color") to "Colors",
            getOneParamRegex("getDrawable", "drawable") to "Drawables",
            getOneParamRegex("getQuantityString", "plurals") to "Plurals",
            getOneParamRegex("getString", "string") to "Strings",
            getTwoParamRegex("getFraction", "fraction") to "Fractions"
        )
    }

    /**
     * A map defining the relationship between `ResourceManager` categories and Android resource types.
     */
    private val androidResMap =
        mapOf(
            "Booleans" to "bool",
            "Colors" to "color",
            "Dimensions" to "dimen",
            "Drawables" to "drawable",
            "IntArrays" to "array",
            "Integers" to "integer",
            "StringArrays" to "array",
            "Plurals" to "plurals",
            "Strings" to "string",
            "Fractions" to "fraction"
        )

    /**
     * Constructs a regex pattern to match resource retrieval methods with no additional parameter.
     *
     * Example match:
     * - `getString(R.string.app_name)`
     */
    private fun getZeroParamRegex(
        methodName: String,
        resourceType: String
    ): Regex {
        return Regex("${common}${methodName}\\(\\s*(.+)?R\\.${resourceType}\\.(\\w+)\\s*\\)")
    }

    /**
     * Constructs a regex pattern to match resource retrieval methods with one additional parameter.
     *
     * Example match:
     * - `getString(R.string.app_name, someValue)`
     */
    private fun getOneParamRegex(
        methodName: String,
        resourceType: String
    ): Regex {
        return Regex("${common}${methodName}\\(\\s*(.+)?R\\.${resourceType}\\.(\\w+),\\s*(.+)\\s*\\)")
    }

    /**
     * Constructs a regex pattern to match resource retrieval methods with two additional parameters.
     *
     * Example match:
     * - `getFraction(R.fraction.some_fraction, param1, param2)`
     */
    private fun getTwoParamRegex(
        methodName: String,
        resourceType: String
    ): Regex {
        return Regex("${common}${methodName}\\(\\s*(.+)?R\\.${resourceType}\\.(\\w+),\\s*(.+)\\s*,\\s*(.+)\\s*\\)")
    }

    fun migrate() {
        val moduleManager = ModuleManager(moduleDir)
        val resourceManager = ResourceManager(projectDir, moduleDir)
        val sourceFiles = getSourceFiles()

        val namespace = moduleManager.getNamespace()
        val filesUnderObservation = resourceManager.getFilesUnderObservation()
        val resourceIds = getResourceIds(filesUnderObservation)
        val resourceIdFunctionNameMap = getResourceIdFunctionNameMap(resourceManager)

        println("\n***************** MIGRATION START *****************\n")

        val updatedSourceFiles = mutableListOf<SourceFileDetails>()

        sourceFiles.forEach { sourceFile ->
            var currentResourceImportStatement: String? = null

            val changes = mutableListOf<Change>()

            val fileContent = StringBuilder()
            var index = 0
            for (line in sourceFile.readLines()) {
                index++

                // Check if it is a import statement.
                val matchResultImport = importStatementRegex.find(line)
                if (matchResultImport?.groups?.size == 2) {
                    currentResourceImportStatement = matchResultImport.groups[0]?.value.orEmpty()
                }

                var matchResultResource: MatchResult? = null

                // Check if it is resource access, iterate through replacement regex
                for ((regex, resourcePath) in replacements) {
                    matchResultResource = regex.find(line)
                    if (matchResultResource == null) continue

                    val moduleNamespace =
                        matchResultResource.groups[1]?.value.orEmpty() // 'com.example.mylibrary1.' in case of ->  getString(com.example.mylibrary1.R.string.fun_title)
                    val resourceId =
                        matchResultResource.groups[2]?.value.orEmpty() // 'fun_title' in case of ->  getString(com.example.mylibrary1.R.string.fun_title)/getString(R.string.fun_title)

                    // If we don't have this resourceId in our set, simply skip this.
                    if (!resourceIds.contains(resourceId)) {
                        matchResultResource = null
                        log(sourceFile, index, false, "ResourceId not found in generated ResourceManager class.")
                        break
                    }

                    val completeResourceId = "R.${androidResMap[resourcePath]}.$resourceId"
                    val functionNames = resourceIdFunctionNameMap[completeResourceId]
                    if (functionNames == null) {
                        matchResultResource = null
                        log(sourceFile, index, false, "ResourceId not found in generated ResourceManager class.")
                        break
                    }

                    // TODO improve this so it handles situation when there are more than one function names for a given resourceId.
                    if (functionNames.size > 1) {
                        matchResultResource = null
                        log(sourceFile, index, false, "More than one function names found for this ResourceId.")
                        break
                    }

                    val resourceCallReplacementString = "ResourceManager.$resourcePath.${resourceIdFunctionNameMap[completeResourceId]?.first()}"

                    // Apply changes to fileContent based on group size.
                    when (matchResultResource.groups.size) {
                        // Group size will be 3 when there is no params passed to resource access code.
                        3 -> {
                            val currentResourceAccessCode = matchResultResource.value
                            val newResourceAccessCode = "$resourceCallReplacementString()"

                            val newLine = line.replace(currentResourceAccessCode, newResourceAccessCode)
                            fileContent.appendLine(newLine)
                            changes.add(
                                Change(index, currentResourceAccessCode, newResourceAccessCode)
                            )
                            log(sourceFile, index)
                            break
                        }

                        // Group size will be 4 when there is one params passed to resource access code.
                        4 -> {
                            val param = matchResultResource.groups[3]?.value.orEmpty()
                            val currentResourceAccessCode = matchResultResource.value
                            val newResourceAccessCode = "$resourceCallReplacementString($param)"

                            val newLine = line.replace(currentResourceAccessCode, newResourceAccessCode)
                            fileContent.appendLine(newLine)
                            changes.add(
                                Change(index, currentResourceAccessCode, newResourceAccessCode)
                            )
                            log(sourceFile, index)
                            break
                        }

                        // Group size will be 5 when there is two params passed to resource access code.
                        5 -> {
                            val param1 = matchResultResource.groups[3]?.value.orEmpty()
                            val param2 = matchResultResource.groups[4]?.value.orEmpty()
                            val currentResourceAccessCode = matchResultResource.value
                            val newResourceAccessCode = "$resourceCallReplacementString($param1, $param2)"

                            val newLine =
                                line.replace(currentResourceAccessCode, newResourceAccessCode)
                            fileContent.appendLine(newLine)
                            changes.add(
                                Change(index, currentResourceAccessCode, newResourceAccessCode)
                            )
                            log(sourceFile, index)
                            break
                        }

                        else -> {
                            matchResultResource = null
                        }
                    }
                }

                // Skip this line if already processed by matchResultResource.
                if (matchResultResource != null) {
                    continue
                }

                fileContent.appendLine(line)
            }

            // If some changes are done to the file content rewrite the file content.
            if (changes.isNotEmpty()) {
                val contentToWrite = getContentToWrite(fileContent, sourceFile, namespace, currentResourceImportStatement, changes, index)
                sourceFile.writeText(contentToWrite)
                updatedSourceFiles.add(
                    SourceFileDetails(sourceFile.name, sourceFile.absolutePath, changes)
                )
            }
        }

        println(
            "\nModified ${updatedSourceFiles.count()} file(s) with a total of ${updatedSourceFiles.flatMap { it.changes }.size} changes applied."
        )
        generateMigrationReport(updatedSourceFiles)
        println("\n***************** MIGRATION END *****************\n")
    }

    private fun getContentToWrite(
        fileContent: StringBuilder,
        sourceFile: File,
        namespace: String?,
        currentResourceImportStatement: String?,
        changes: MutableList<Change>,
        index: Int
    ): String {
        if (currentResourceImportStatement == null) return fileContent.toString()

        var contentToWrite = fileContent.toString()

        val semiColan = if (sourceFile.extension == "java") ";" else ""
        val resourcemanagerImportStatement = "import $namespace.ResourceManager$semiColan"
        // Add the resourcemanager import statement.
        val newImportStatement = "$currentResourceImportStatement\n$resourcemanagerImportStatement"
        contentToWrite = contentToWrite.replace(currentResourceImportStatement, newImportStatement)
        changes.add(Change(index, currentResourceImportStatement, newImportStatement))
        log(sourceFile, index)
        return contentToWrite
    }

    /**
     * Extracts a set of resource IDs from the specified files under observation.
     *
     * This function processes a list of files to identify resource IDs from XML files
     * and drawable resource file names. It collects the names of the resources and
     * returns them as a set of unique resource identifiers.
     *
     * - **XML Resource IDs**: Parsed using an `XmlParser` to extract resource names from XML files.
     * - **Drawable Resource Names**: Extracted from files in the `main/res/drawable` directory.
     *
     * @param filesUnderObservation A list of files to process for resource IDs.
     * @return A set containing unique resource IDs.
     */
    private fun getResourceIds(filesUnderObservation: List<File>): Set<String> {
        val resourceIds = mutableSetOf<String>()
        val valueResourceFiles = filesUnderObservation.filter { it.extension == "xml" }

        // Add value resource ids.
        valueResourceFiles.forEach { file ->
            resourceIds.addAll(XmlParser.parseXML(file).map { it.name })
        }

        // Add drawable resources names.
        val drawableResourceFiles =
            filesUnderObservation.filter { it.absolutePath.contains("main/res/drawable") }

        drawableResourceFiles.forEach { file ->
            resourceIds.add(file.nameWithoutExtension)
        }
        return resourceIds
    }

    /**
     * Retrieves a list of source files from the project's main source directory.
     *
     * @return A list of [File] objects representing the source files found in the `src/main` directory.
     */
    private fun getSourceFiles(): List<File> {
        val sourceDirectory = moduleDir.resolve("src/main")
        val sourceFiles = sourceDirectory.walkTopDown().filter { it.isSourceFile() }.toList()
        return sourceFiles
    }

    /**
     * Generates a map associating resource IDs with the corresponding function names that operate on those resources.
     *
     * @param resourceManager The [ResourceManager] instance providing access to project resources.
     * @return A mutable map where keys are resource IDs (e.g., `R.string.resource_name`, `R.drawable.icon_name`)
     *         and values are lists of function names associated with those resources.
     */
    private fun getResourceIdFunctionNameMap(resourceManager: ResourceManager): MutableMap<String, MutableList<String>> {
        val resourceIdFunctionNameMap = mutableMapOf<String, MutableList<String>>()
        val resourceMap = resourceManager.getResources().groupBy { it.type }
        resourceMap.forEach { (resourceType, value) ->
            when (resourceType) {
                ResourceType.VALUES -> {
                    processValueResources(value, resourceIdFunctionNameMap)
                }

                ResourceType.DRAWABLES -> {
                    processDrawablesResources(value, resourceIdFunctionNameMap)
                }
            }
        }
        return resourceIdFunctionNameMap
    }

    private fun processValueResources(
        resources: List<Resource>,
        resourceIdFunctionNameMap: MutableMap<String, MutableList<String>>
    ) {
        resources.forEach { resource ->
            resource.moduleDetails.resDirectory.listFiles().getXmlFiles().forEach { file ->
                val xmlResources = XmlParser.parseXML(file)
                xmlResources.forEach {
                    val resourceId = "R.${it.type.value}.${it.name}"
                    val functionName = ClassFileGenerator.getValueResourceMethodName(it, resource.moduleDetails)
                    resourceIdFunctionNameMap[resourceId] = resourceIdFunctionNameMap.getOrDefault(resourceId, mutableListOf())
                    resourceIdFunctionNameMap[resourceId]?.add(functionName)
                }
            }
        }
    }

    private fun processDrawablesResources(
        resources: List<Resource>,
        resourceIdFunctionNameMap: MutableMap<String, MutableList<String>>
    ) {
        resources.forEach { resource ->
            resource.moduleDetails.resDirectory.listFiles()?.forEach { file ->
                val resourceId = "R.drawable.${file.nameWithoutExtension}"
                val functionName = ClassFileGenerator.getDrawableMethodName(file.nameWithoutExtension, resource.moduleDetails)
                resourceIdFunctionNameMap[resourceId] = resourceIdFunctionNameMap.getOrDefault(resourceId, mutableListOf())
                resourceIdFunctionNameMap[resourceId]?.add(functionName)
            }
        }
    }

    /**
     * Determines if the file is a source file based on its extension.
     *
     * @return `true` if the file is a source file; otherwise, `false`.
     */
    private fun File.isSourceFile(): Boolean {
        return extension == "kt" || extension == "java"
    }

    /**
     * Generates a migration report in HTML format and saves it to `build/reports/migration/` directory.
     *
     * @param files A list of source file details used to generate the migration report.
     */
    private fun generateMigrationReport(files: List<SourceFileDetails>) {
        val report =
            File(
                moduleDir,
                "build/reports/migration/resourcemanager-migration-report.html"
            )
        report.parentFile.mkdirs()

        val htmlContent = ReportGenerator.generateMigrationReport(files)
        report.writeText(htmlContent)
        println("Migration Report: file://${report.absolutePath}")
    }

    /**
     * Logs the updated line in a source file.
     */
    private fun log(
        sourceFile: File,
        lineNumber: Int,
        updated: Boolean = true,
        details: String? = null
    ) {
        println("${if (updated) "Updated" else "Skipped"}: ${sourceFile.absolutePath}:$lineNumber: ${details.orEmpty()}")
    }
}