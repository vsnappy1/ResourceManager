package dev.randos.resourcemanager

import java.io.File

import dev.randos.resourcemanager.manager.ModuleManager
import dev.randos.resourcemanager.file.generation.ClassFileGenerator
import dev.randos.resourcemanager.manager.ResourceManager

internal class ResourceManagerGenerator(
    private val moduleFile: File,
    private val generatedFile: File
) {

    fun generate() {

        val moduleManager = ModuleManager(moduleFile)
        val resourceManager = ResourceManager(moduleFile)

        try {
            // Ensure directory exists before writing to the file.
            generatedFile.parentFile.mkdirs()

            // Write the generated class content to the file
            generatedFile.bufferedWriter().use { out ->

                val classFile = ClassFileGenerator.generateClassFile(
                    namespace = moduleManager.getNamespace()
                        ?: throw IllegalStateException("Namespace could not be found in either build.gradle, build.gradle.kts or AndroidManifest.xml. Please ensure the module is properly configured."),
                    files = resourceManager.getResources(moduleFile, moduleManager.getModuleDependencies())
                )

                out.write(classFile)
                out.close()
            }
        } catch (e: Exception) {
            println("Error: ${e.localizedMessage}")
            e.printStackTrace()
        }
    }
}
