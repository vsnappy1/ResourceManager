package dev.randos.resourcemanager.compiler.manager

import org.w3c.dom.Document
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class ModuleManager(private val moduleFile: File) {

    fun getNamespace(): String? {
        return getNamespaceFromBuildGradle() ?: getNamespaceFromManifest()
    }

    fun getModuleDependencies(): List<String> {
        // Find the appropriate build.gradle file (either .gradle or .gradle.kts)
        var gradleFile = File(moduleFile, "build.gradle")
        if (!gradleFile.exists()) {
            gradleFile = File(moduleFile, "build.gradle.kts")
        }
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