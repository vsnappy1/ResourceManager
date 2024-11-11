package dev.randos.resourcemanager

import java.io.File

import dev.randos.resourcemanager.model.Resource
import dev.randos.resourcemanager.model.ResourceType
import dev.randos.resourcemanager.model.ModuleDetails
import dev.randos.resourcemanager.manager.ModuleManager
import dev.randos.resourcemanager.file.generation.ClassFileGenerator

internal class ResourceManagerGenerator(
    private val moduleFile: File,
    private val generatedFile: File
) {

    fun generate() {

        val moduleManager = ModuleManager(moduleFile)
        val resources = getResources(moduleFile, moduleManager.getModuleDependencies())

        try {
            // Create the new file with a dependency on all source files
            generatedFile.parentFile.mkdirs()

            // Write the generated class content to the file
            generatedFile.bufferedWriter().use { out ->

                val classFile = ClassFileGenerator.generateClassFile(
                    namespace = moduleManager.getNamespace()
                        ?: throw IllegalStateException("Namespace could not be found in either build.gradle, build.gradle.kts or AndroidManifest.xml. Please ensure the module is properly configured."),
                    files = resources
                )

                out.write(classFile)
                out.close()
            }
        } catch (e: Exception) {
            println("Error: ${e.localizedMessage}")
            e.printStackTrace()
        }
    }

    fun getFilesUnderObservation(): List<File> {
        val moduleManager = ModuleManager(moduleFile)
        val resources = getResources(moduleFile, moduleManager.getModuleDependencies())
        return resources.getFilesUnderObservation() + listOf(moduleManager.getBuildGradleFile()) // Also observe the module build.gradle file.
    }

    /**
     * Scans the directory structure starting from the given file path to locate default resource file
     * within an Android project's res/values directories.
     *
     * @return A list of [Resource].
     */
    private fun getResources(
        moduleFile: File,
        moduleDependencies: List<String>
    ): List<Resource> {
        val list = mutableListOf<Resource>()

        var resFile = File(moduleFile, "src/main/res")

        // Locate the "values" directory within the "res" directory.
        list.add(
            Resource(
                type = ResourceType.VALUES,
                moduleDetails = ModuleDetails(resDirectory = File(resFile, "values"))
            )
        )

        // Locate the "drawable" directory within the "res" directory.
        list.add(
            Resource(
                type = ResourceType.DRAWABLES,
                moduleDetails = ModuleDetails(resDirectory = File(resFile, "drawable"))
            )
        )

        /*
         Also add resources for project dependencies. (i.e. implementation(project(":my_library")))
         */
        val projectFile = moduleFile.parentFile
        moduleDependencies.forEach { module ->
            val dependencyModuleFile = File(projectFile, module)
            resFile = File(dependencyModuleFile, "src/main/res")

            ModuleManager(dependencyModuleFile).getNamespace()?.let { namespace ->
                // Locate the "values" directory within the "res" directory.
                list.add(
                    Resource(
                        type = ResourceType.VALUES,
                        moduleDetails = ModuleDetails(module, namespace, File(resFile, "values"))
                    )
                )

                // Locate the "drawable" directory within the "res" directory.
                list.add(
                    Resource(
                        type = ResourceType.DRAWABLES,
                        moduleDetails = ModuleDetails(module, namespace, File(resFile, "drawable"))
                    )
                )
            }
        }
        return list
    }

    /**
     * Generates a list of all files within the directories specified by each `Resource` object.
     *
     * @receiver List of `Resource` objects, each containing a directory path.
     * @return A list of all files under each directory in the `Resource` list.
     */
    private fun List<Resource>.getFilesUnderObservation(): List<File> {
        val files = mutableListOf<File>()
        this.map { it.moduleDetails.resDirectory }.forEach { directory ->
            directory.walkTopDown().forEach { file ->
                if (file.isFile) {
                    files.add(file)
                }
            }
        }
        return files
    }
}
