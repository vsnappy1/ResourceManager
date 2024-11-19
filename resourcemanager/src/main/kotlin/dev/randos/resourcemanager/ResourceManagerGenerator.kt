package dev.randos.resourcemanager

import dev.randos.resourcemanager.file.generation.ClassFileGenerator
import dev.randos.resourcemanager.manager.ModuleManager
import dev.randos.resourcemanager.manager.ResourceManager
import java.io.File

internal class ResourceManagerGenerator(
    private val projectDir: File,
    private val moduleFile: File,
    private val generatedFile: File,
    private val moduleManager: ModuleManager = ModuleManager(moduleFile),
    private val resourceManager: ResourceManager = ResourceManager(projectDir, moduleFile)
) {
    fun generate() {
        try {
            // Ensure directory exists before writing to the file.
            generatedFile.parentFile.mkdirs()

            // Write the generated class content to the file
            val resourceManagerClassContent =
                ClassFileGenerator.generateClassFile(
                    namespace =
                        moduleManager.getNamespace()
                            ?: throw IllegalStateException("Namespace could not be found in either build.gradle(.kts) or AndroidManifest.xml. Please ensure the module is properly configured."),
                    files = resourceManager.getResources()
                )
            generatedFile.writeText(resourceManagerClassContent)
        } catch (e: Exception) {
            println("Error: ${e.localizedMessage}")
            e.printStackTrace()
        }
    }
}