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

    private fun getResourceFiles(pathToAnnotatedFile: File): List<File> {
        var pathToMainDirectory = pathToAnnotatedFile
        return try {
            while (pathToMainDirectory.name != "main") {
                pathToMainDirectory = pathToMainDirectory.parentFile
            }
            val pathToResDirectory = File(pathToMainDirectory, "res")
            val pathToValuesDirectory = File(pathToResDirectory, "values")
            pathToValuesDirectory.listFiles()?.filter { it.extension == "xml" } ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
