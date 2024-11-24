package dev.randos.resourcemanager.manager

import dev.randos.resourcemanager.model.ModuleDetails
import dev.randos.resourcemanager.model.Resource
import dev.randos.resourcemanager.model.ResourceType
import java.io.File
import java.io.FileFilter

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
        val xmlFileFilter = FileFilter { it.isXmlFile() }
        val drawableFileFilter = FileFilter { it.isValidResourceName() }
        var resFile = File(moduleFile, "src/main/res")

        // Locate the "values" directory within the "res" directory.
        list.add(
            Resource(
                type = ResourceType.VALUES,
                moduleDetails = ModuleDetails(resourceFiles = File(resFile, "values").listFiles(xmlFileFilter) ?: emptyArray())
            )
        )

        // Locate all "drawable" directory within the "res" directory.
        resFile.listFiles()?.filter { it.isDirectory && it.name.startsWith("drawable") }?.forEach { drawableDirectory ->
            list.add(
                Resource(
                    type = ResourceType.DRAWABLES,
                    moduleDetails = ModuleDetails(resourceFiles = File(resFile, drawableDirectory.name).listFiles(drawableFileFilter) ?: emptyArray())
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
                        moduleDetails = ModuleDetails(module, namespace, File(resFile, "values").listFiles(xmlFileFilter) ?: emptyArray())
                    )
                )

                // Locate all "drawable" directory within the "res" directory.
                resFile.listFiles()?.filter { it.isDirectory && it.name.startsWith("drawable") }?.forEach { drawableDirectory ->
                    list.add(
                        Resource(
                            type = ResourceType.DRAWABLES,
                            moduleDetails = ModuleDetails(module, namespace, File(resFile, drawableDirectory.name).listFiles(drawableFileFilter) ?: emptyArray())
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
        forEach { resource ->
            files.addAll(resource.moduleDetails.resourceFiles)
        }
        return files
    }

    private fun File.isXmlFile(): Boolean {
        return extension.lowercase() == "xml"
    }

    private fun File.isValidResourceName(): Boolean {
        val validResourceNameRegex = "^[a-z][a-z0-9_]*$".toRegex()
        return nameWithoutExtension.isNotEmpty() && validResourceNameRegex.matches(nameWithoutExtension)
    }
}