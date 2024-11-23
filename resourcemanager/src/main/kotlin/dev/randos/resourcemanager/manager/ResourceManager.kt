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
internal class ResourceManager(
    private val projectDir: File,
    private val moduleFile: File,
    private val moduleManager: ModuleManager = ModuleManager(moduleFile)
) {
    /**
     * Returns resource files under observation such as strings.xml, dimens.xml, ic_gift.png etc,
     * which are taken as input to generate ResourceManager (a generated class).
     */
    fun getFilesUnderObservation(): List<File> {
        val resources = getResources()
        // Return resource files and build.gradle(.kts) file to be observed for change.
        return resources.getFilesUnderObservation() + listOf(moduleManager.getBuildGradleFile())
    }

    /**
     * Scans the directory structure starting from the given file path to locate default resource file
     * within an Android project's res/values directories.
     *
     * @return A list of [Resource].
     */
    fun getResources(): List<Resource> {
        val list = mutableListOf<Resource>()
        val moduleDependencies = moduleManager.getModuleDependencies()
        var resFile = File(moduleFile, "src/main/res")

        // Locate the "values" directory within the "res" directory.
        list.add(
            Resource(
                type = ResourceType.VALUES,
                moduleDetails = ModuleDetails(resDirectory = File(resFile, "values"))
            )
        )

        // Locate all "drawable" directory within the "res" directory.
        resFile.listFiles()?.filter { it.isDirectory && it.name.startsWith("drawable") }?.forEach { drawableDirectory ->
            list.add(
                Resource(
                    type = ResourceType.DRAWABLES,
                    moduleDetails = ModuleDetails(resDirectory = File(resFile, drawableDirectory.name))
                )
            )
        }

        /*
         Also add resources for project dependencies. (i.e. implementation(project(":my_library")))
         */
        moduleDependencies.forEach { module ->
            val dependencyModuleFile = File(projectDir, module)
            resFile = File(dependencyModuleFile, "src/main/res")

            ModuleManager(dependencyModuleFile).getNamespace()?.let { namespace ->
                // Locate the "values" directory within the "res" directory.
                list.add(
                    Resource(
                        type = ResourceType.VALUES,
                        moduleDetails = ModuleDetails(module, namespace, File(resFile, "values"))
                    )
                )

                // Locate all "drawable" directory within the "res" directory.
                resFile.listFiles()?.filter { it.isDirectory && it.name.startsWith("drawable") }?.forEach { drawableDirectory ->
                    list.add(
                        Resource(
                            type = ResourceType.DRAWABLES,
                            moduleDetails = ModuleDetails(module, namespace, File(resFile, drawableDirectory.name))
                        )
                    )
                }
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