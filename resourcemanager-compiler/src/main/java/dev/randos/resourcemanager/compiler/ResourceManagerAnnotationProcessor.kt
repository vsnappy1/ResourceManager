package dev.randos.resourcemanager.compiler

import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import dev.randos.resourcemanager.compiler.file.generation.ClassFileGenerator
import dev.randos.resourcemanager.compiler.manager.CacheManager
import dev.randos.resourcemanager.compiler.model.Resource
import dev.randos.resourcemanager.compiler.model.ResourceType
import java.io.File

internal class ResourceManagerAnnotationProcessor(
    environment: SymbolProcessorEnvironment
) : SymbolProcessor {
    private val logger = environment.logger
    private val codeGenerator = environment.codeGenerator
    private val annotationQualifiedName =
        "dev.randos.resourcemanager.runtime.InstallResourceManager"
    private lateinit var cacheManager: CacheManager

    override fun process(resolver: Resolver): List<KSAnnotated> {
        logger.info("Processing annotations")

        // Get all symbols annotated with [InstallResourceManager] annotation.
        val symbols = resolver.getSymbolsWithAnnotation(annotationQualifiedName)
            .filterIsInstance<KSClassDeclaration>()

        val unprocessedSymbols = mutableListOf<KSAnnotated>()

        for (symbol in symbols) {
            val containingFile = symbol.containingFile ?: continue
            val annotatedFile = File(containingFile.filePath)
            val resources = getResources(annotatedFile)
            val packageName = symbol.packageName.asString()

            cacheManager = CacheManager(
                buildDirectory = annotatedFile.getPathToBuildDirectory(),
                filesUnderObservation = resources.getFilesUnderObservation()
            )

            try {
                // Create the new file with a dependency on `containingFile`
                val file = codeGenerator.createNewFile(
                    dependencies = Dependencies(true, containingFile),
                    packageName = packageName,
                    fileName = "ResourceManager"
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
                            packageName = packageName,
                            namespace = getNameSpace(symbol) ?: packageName,
                            files = resources
                        )
                    }

                    out.write(classFile)
                    out.close()
                }
                // Store the latest generated ResourceManager file in the cache for future use.
                cacheManager.cache()
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

    private fun getNameSpace(it: KSClassDeclaration): String? {
        val namespaceAnnotation =
            it.annotations.firstOrNull { annotation -> annotation.annotationType.resolve().declaration.qualifiedName?.asString() == annotationQualifiedName }
        val namespace = namespaceAnnotation?.arguments?.firstOrNull()?.value as? String
        if (namespace?.isEmpty() == true) return null
        return namespace
    }

    /**
     * Scans the directory structure starting from the given file path to locate default resource file
     * within an Android project's res/values directories.
     *
     * @param pathToAnnotatedFile A File object representing the path to the annotated file.
     * @return A list of [Resource].
     */
    private fun getResources(pathToAnnotatedFile: File): List<Resource> {
        var pathToMainDirectory = pathToAnnotatedFile
        val list = mutableListOf<Resource>()

        return try {
            // Traverse the directory structure upwards until the "main" directory is found.
            while (pathToMainDirectory.name != "main") {
                pathToMainDirectory = pathToMainDirectory.parentFile
            }

            // Locate the "res" directory within the "main" directory.
            val pathToResDirectory = File(pathToMainDirectory, "res")

            // Locate the "values" directory within the "res" directory.
            list.add(Resource(ResourceType.VALUES, File(pathToResDirectory, "values")))

            // Locate the "drawable" directory within the "res" directory.
            list.add(Resource(ResourceType.DRAWABLES, File(pathToResDirectory, "drawable")))

            list
        } catch (e: Exception) {
            // Print the stack trace and return an empty map in case of any exception.
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Generates a list of all files within the directories specified by each `Resource` object.
     *
     * @receiver List of `Resource` objects, each containing a directory path.
     * @return A list of all files under each directory in the `Resource` list.
     */
    private fun List<Resource>.getFilesUnderObservation(): List<File> {
        val files = mutableListOf<File>()
        this.map { it.directoryPath }.forEach { directory ->
            directory.walkTopDown().forEach { file ->
                if (file.isFile) {
                    files.add(file)
                }
            }
        }
        return files
    }

    /**
     * Determines the `build` directory path for a given source file.
     *
     * @receiver The `File` object representing a file in the project.
     * @return The path to the `build` directory if found; `null` otherwise.
     */
    private fun File.getPathToBuildDirectory(): File? {
        var file = this
        while (file.name != "src") {
            file = file.parentFile ?: return null
        }
        val buildDirectory = File(file.parentFile, "build")
        return if (buildDirectory.exists()) buildDirectory else null
    }
}
