package dev.randos.resourcemanager

import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import dev.randos.resourcemanager.file.generation.ClassFileGenerator
import dev.randos.resourcemanager.model.Resource
import dev.randos.resourcemanager.model.ResourceType
import java.io.File

internal class ResourceManagerAnnotationProcessor(
    environment: SymbolProcessorEnvironment
) : SymbolProcessor {
    private val logger = environment.logger
    private val codeGenerator = environment.codeGenerator

    override fun process(resolver: Resolver): List<KSAnnotated> {
        logger.info("Processing annotations")

        val symbols =
            resolver.getSymbolsWithAnnotation(InstallResourceManager::class.qualifiedName!!)

        symbols.filterIsInstance<KSClassDeclaration>().forEach {
            val containingFile = it.containingFile
            if (containingFile != null) {
                val className = it.simpleName.asString()
                val packageName = it.packageName.asString()
                val file = codeGenerator.createNewFile(
                    // TODO pass in the list of files in Dependency so ksp can observe and when these files changes only then regenerate code otherwise not.
                    dependencies = Dependencies(true, containingFile),
                    packageName = packageName,
                    fileName = "ResourceManager"
                )

                file.bufferedWriter().use { out ->
                    val resources = getResources(File(containingFile.filePath))
                    val classFile = ClassFileGenerator.generateClassFile(
                        packageName = packageName,
                        namespace = getNameSpace(it) ?: packageName,
                        files = resources
                    )
                    out.write(classFile)
                }
                logger.info("Generated file for $className")
            }
        }
        return emptyList()
    }

    private fun getNameSpace(it: KSClassDeclaration): String? {
        val namespaceAnnotation =
            it.annotations.firstOrNull { annotation -> annotation.annotationType.resolve().declaration.qualifiedName?.asString() == InstallResourceManager::class.qualifiedName }
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
}
