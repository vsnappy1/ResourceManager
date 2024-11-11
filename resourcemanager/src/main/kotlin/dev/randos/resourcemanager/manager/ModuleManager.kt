package dev.randos.resourcemanager.manager

import org.w3c.dom.Document
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Manages module-related operations, including retrieving namespaces and module dependencies
 * for an Android project module.
 *
 * @property moduleFile The root directory of the module.
 */
class ModuleManager(private val moduleFile: File) {


    /**
     * Retrieves the namespace of the module by checking both the build.gradle file and the AndroidManifest.xml.
     * It prioritizes the namespace specified in the build.gradle file.
     *
     * @return The module's namespace if found, or `null` if not specified.
     */
    fun getNamespace(): String? {
        return getNamespaceFromBuildGradle() ?: getNamespaceFromManifest()
    }

    /**
     * Retrieves a list of dependencies for this module from the build.gradle file,
     * specifically those declared like `implementation project(':module-name')` or `implementation(project(":module-name"))`.
     *
     * @return A list of module dependencies as strings, or an empty list if no dependencies are found.
     */
    fun getModuleDependencies(): List<String> {
        val gradleFile = getBuildGradleFile()
        if (!gradleFile.exists()) {
            return emptyList()
        }

        val dependencies = mutableListOf<String>()

        // Define regex patterns for the two styles of module dependencies
        val dependencyPatterns = listOf(
            Regex("\\s*implementation\\s*\\(\\s*project\\s*\\(\\s*['\"]\\s*:\\s*(.+)\\s*['\"]\\s*\\)\\s*\\)"),  // Matches: implementation(project(":module-name"))
            Regex("\\s*implementation\\s* \\s*project\\s*\\(\\s*['\"]\\s*:\\s*(.+)\\s*['\"]\\s*\\)"),           // Matches: implementation project(':module-name')
        )

        var isBlockComment = false
        val lines = gradleFile.readLines().filter { it.isNotBlank() }

        for (line in lines) {
            // Handle block comments
            if (line.trim().startsWith("/*")) {
                isBlockComment = true
            }
            if (isBlockComment) {
                if (line.contains("*/")) {
                    isBlockComment = false
                }
                continue
            }

            // Ignore single-line comments
            if (line.trim().startsWith("//")) continue

            // Check each regex pattern to find module dependencies.
            dependencyPatterns.forEach { pattern ->
                val mathResults = pattern.find(line)
                mathResults?.run {
                    dependencies.add(mathResults.groupValues[1])
                }
            }
        }
        return dependencies
    }

    /**
     * Finds and returns the build.gradle file in the module directory, supporting both `.gradle` and `.gradle.kts` extensions.
     *
     * @return The build.gradle file if it exists, or a reference to a non-existent file if neither variant is present.
     */
    fun getBuildGradleFile(): File {
        // Find the appropriate build.gradle file (either .gradle or .gradle.kts)
        var gradleFile = File(moduleFile, "build.gradle")
        if (!gradleFile.exists()) {
            gradleFile = File(moduleFile, "build.gradle.kts")
        }
        return gradleFile
    }

    /**
     * Extracts the namespace from the build.gradle file.
     *
     * @return The namespace as specified in the build.gradle file, or `null` if not found.
     */
    private fun getNamespaceFromBuildGradle(): String? {
        // Find the appropriate build.gradle file (either .gradle or .gradle.kts)
        var gradleFile = File(moduleFile, "build.gradle")
        if (!gradleFile.exists()) {
            gradleFile = File(moduleFile, "build.gradle.kts")
        }
        if (!gradleFile.exists()) {
            println("Error: Failed to find build.gradle/build.gradle.kts at path: ${gradleFile.absolutePath}")
            return null
        }

        // Define regex pattern for the style of namespace
        val namespacePattern =
            Regex("\\s*namespace\\s*=\\s*[\"'](.+)[\"']")   // Matches: namespace = "com.example.app"

        var isBlockComment = false
        val lines = gradleFile.readLines().filter { it.isNotBlank() }

        for (line in lines) {
            // Handle block comments
            if (line.trim().startsWith("/*")) {
                isBlockComment = true
            }
            if (isBlockComment) {
                if (line.contains("*/")) {
                    isBlockComment = false
                }
                continue
            }

            // Ignore single-line comments
            if (line.trim().startsWith("//")) continue

            // Check each regex pattern to find module dependencies.
            val mathResults = namespacePattern.find(line)
            mathResults?.run {
                return mathResults.groupValues[1]
            }
        }

        println("Error: Failed to find namespace in gradle file at path: ${gradleFile.absolutePath}")
        return null
    }

    /**
     * Retrieves the namespace from the AndroidManifest.xml.
     *
     * @return The package name as specified in AndroidManifest.xml, or `null` if the attribute is not found.
     */
    private fun getNamespaceFromManifest(): String? {
        val manifestFile = File(moduleFile, "src/main/AndroidManifest.xml")
        if (!manifestFile.exists()) {
            println("Error: Failed to find AndroidManifest.xml at path: ${manifestFile.absolutePath}")
            return null
        }

        try {
            val document: Document =
                DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(manifestFile)
            document.documentElement.normalize()

            // Find the 'package' attribute in the <manifest> tag
            val packageName = document.documentElement.getAttribute("package")
            if (packageName.isNotEmpty()) {
                return packageName
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        println("Error: Failed to find attribute `package` in AndroidManifest.xml at path: ${manifestFile.absolutePath}")
        return null
    }
}