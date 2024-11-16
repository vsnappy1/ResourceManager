package dev.randos.resourcemanager.manager

import dev.randos.resourcemanager.model.ModuleDetails
import dev.randos.resourcemanager.model.Resource
import dev.randos.resourcemanager.model.ResourceType
import java.io.File

/**
 * Manages resource files within an Android project, including identifying files under observation
 * and gathering resources from both the current module and its dependencies.
 *
 * @param moduleFile The root directory of the module being managed.
 */
internal class ResourceManager(private val moduleFile: File) {

    /**
     * Returns resource files under observation such as strings.xml, dimens.xml, ic_gift.png etc,
     * which are taken as input to generate ResourceManager (a generated class).
     */
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
    fun getResources(
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