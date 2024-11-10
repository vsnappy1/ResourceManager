package dev.randos.resourcemanager.compiler

import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import dev.randos.resourcemanager.InstallResourceManager
import dev.randos.resourcemanager.compiler.file.generation.ClassFileGenerator
import dev.randos.resourcemanager.compiler.manager.CacheManager
import dev.randos.resourcemanager.compiler.manager.ModuleManager
import dev.randos.resourcemanager.compiler.model.ModuleDetails
import dev.randos.resourcemanager.compiler.model.Resource
import dev.randos.resourcemanager.compiler.model.ResourceType
import java.io.File

internal class ResourceManagerAnnotationProcessor(
    environment: SymbolProcessorEnvironment
) : SymbolProcessor {
    private val logger = environment.logger
    private val codeGenerator = environment.codeGenerator

    private val namespaceNotFoundExpectation =
        IllegalStateException("Namespace could not be found in either build.gradle, build.gradle.kts or AndroidManifest.xml. Please ensure the module is properly configured.")

    override fun process(resolver: Resolver): List<KSAnnotated> {
        logger.info("Processing annotations")

        // Get all symbols annotated with [InstallResourceManager] annotation.
        val symbols = resolver.getSymbolsWithAnnotation(InstallResourceManager::class.java.name)
            .filterIsInstance<KSClassDeclaration>()

        val unprocessedSymbols = mutableListOf<KSAnnotated>()

        for (symbol in symbols) {
            val containingFile = symbol.containingFile ?: continue
            val annotatedFile = File(containingFile.filePath)
            val moduleFile = getModuleRootFile(annotatedFile) ?: continue
            val moduleManager = ModuleManager(moduleFile)
            val resources = getResources(annotatedFile, moduleManager.getModuleDependencies())
            val packageName = symbol.packageName.asString()

            try {
                // Create the new file with a dependency on all source files
                val file = codeGenerator.createNewFile(
                    dependencies = Dependencies.ALL_FILES,
                    packageName = packageName,
                    fileName = "ResourceManager"
                )

                val cacheManager = CacheManager(
                    moduleDirectory = moduleFile,
                    filesUnderObservation = resources.getFilesUnderObservation() + listOf(moduleManager.getBuildGradleFile()) // Also observe the module build.gradle file.
                )

                // Write the generated class content to the file
                file.bufferedWriter().use { out ->

                    var classFile: String? = null
                    // Check if the cache is up-to-date to avoid unnecessary regeneration
                    if (cacheManager.isCacheUpToDate()) {
                        // Get cached data directly if cache is valid
                        classFile = cacheManager.getCachedContent()
                    } else {
                        // Invalidate outdated cache to prepare for fresh generation
                        cacheManager.invalidateCache()
                    }

                    if (classFile == null) {
                        classFile = ClassFileGenerator.generateClassFile(
                            namespace = moduleManager.getNamespace()
                                ?: throw namespaceNotFoundExpectation,
                            files = resources
                        )
                    }

                    out.write(classFile)
                    out.close()
                }
                // Store the latest generated ResourceManager file in the cache for future use.
                if(!cacheManager.isCacheUpToDate()){
                    cacheManager.cache()
                }

                logger.info("Generated ResourceManager file for package $packageName.", symbol)
            } catch (e: Exception) {
                logger.error(
                    "Error generating ResourceManager file for package $packageName: ${e.message}",
                    symbol
                )
                unprocessedSymbols.add(symbol)
            }
        }
        return unprocessedSymbols
    }

    /**
     * Scans the directory structure starting from the given file path to locate default resource file
     * within an Android project's res/values directories.
     *
     * @param pathToAnnotatedFile A File object representing the path to the annotated file.
     * @return A list of [Resource].
     */
    private fun getResources(
        pathToAnnotatedFile: File,
        moduleDependencies: List<String>
    ): List<Resource> {
        val pathToModuleDirectory = getModuleRootFile(pathToAnnotatedFile) ?: return emptyList()
        val list = mutableListOf<Resource>()

        var resFile = File(pathToModuleDirectory, "src/main/res")

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
        val projectFile = getProjectRootFile(pathToAnnotatedFile)
        moduleDependencies.forEach { module ->
            val moduleFile = File(projectFile, module)
            resFile = File(moduleFile, "src/main/res")

            ModuleManager(moduleFile).getNamespace()?.let { namespace ->
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

    private fun getModuleRootFile(pathToAnnotatedFile: File): File? {
        var pathToSrcDirectory: File? = pathToAnnotatedFile

        return try {
            // Traverse the directory structure upwards until the "src" directory is found.
            while (pathToSrcDirectory != null && pathToSrcDirectory.name != "src") {
                pathToSrcDirectory = pathToSrcDirectory.parentFile
            }
            pathToSrcDirectory?.parentFile
        } catch (e: Exception) {
            // Print the stack trace and return null in case of any exception.
            e.printStackTrace()
            null
        }
    }

    private fun getProjectRootFile(pathToAnnotatedFile: File): File? {
        return getModuleRootFile(pathToAnnotatedFile)?.parentFile
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
