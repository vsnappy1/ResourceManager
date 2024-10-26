package com.randos.resourcemanager

import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.randos.resourcemanager.file.generation.ClassFileGenerator
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
                    dependencies = Dependencies(false),
                    packageName = packageName,
                    fileName = "ResourceManager"
                )

                file.bufferedWriter().use { out ->
                    val resourceFiles = getResourceFiles(File(containingFile.filePath))
                    val classFile = ClassFileGenerator.generateClassFile(packageName, resourceFiles)
                    out.write(classFile)
                }
                logger.info("Generated file for $className")
            }
        }
        return emptyList()
    }

    /**
     * Scans the directory structure starting from the given file path to locate default resource file
     * within an Android project's res/values directories.
     *
     * @param pathToAnnotatedFile A File object representing the path to the annotated file.
     * @return A list of files with xml extension.
     */
    private fun getResourceFiles(pathToAnnotatedFile: File): List<File> {
        var pathToMainDirectory = pathToAnnotatedFile

        return try {
            // Traverse the directory structure upwards until the "main" directory is found.
            while (pathToMainDirectory.name != "main") {
                pathToMainDirectory = pathToMainDirectory.parentFile
            }

            // Locate the "res" directory within the "main" directory.
            val pathToResDirectory = File(pathToMainDirectory, "res")

            // Locate the default "values" directory within the "res" directory.
            val pathToValuesDirectory = File(pathToResDirectory, "values")

            // Get all XML files in the default "values" directory.
            pathToValuesDirectory.listFiles().getXmlFiles()
        } catch (e: Exception) {
            // Print the stack trace and return an empty map in case of any exception.
            e.printStackTrace()
            emptyList()
        }
    }

    private fun Array<File>?.getXmlFiles(): List<File> {
        return this?.filter { it.extension == "xml" } ?: emptyList()
    }
}
