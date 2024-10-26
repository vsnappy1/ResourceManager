package com.randos.resourcemanager

import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.randos.resourcemanager.file.generation.ClassFileGenerator
import com.randos.resourcemanager.model.ValueIdentifier
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
     * Scans the directory structure starting from the given file path to locate all resource files
     * within an Android project's res/values directories.
     *
     * @param pathToAnnotatedFile A File object representing the path to the annotated file.
     * @return A map where the key is the name of the resource file (String) and the value is a list of
     * ValueIdentifier objects representing the files found in different values directories.
     */
    private fun getResourceFiles(pathToAnnotatedFile: File): Map<String, MutableList<ValueIdentifier>> {
        var pathToMainDirectory = pathToAnnotatedFile
        val map = mutableMapOf<String, MutableList<ValueIdentifier>>()

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
            val filesInDefaultDirectory = pathToValuesDirectory.listFiles().getXmlFiles()

            // Map each file in the default "values" directory to a list containing a single ValueIdentifier object.
            filesInDefaultDirectory.forEach {
                map[it.name] = mutableListOf(ValueIdentifier(file = it))
            }

            // Identify all "values-.*" directories (e.g., values-en, values-fr) within the "res" directory.
            val allValuesDirectories =
                pathToResDirectory.listFiles()?.filter { it.absolutePath.contains(Regex("values-.+")) }

            // Process each identified "values-.*" directory.
            allValuesDirectories?.forEach { directory ->
                val identifier = directory.name.toIdentifier()
                // Add corresponding ValueIdentifier objects to the map entries if the file names match those in the default "values" directory.
                directory.listFiles().getXmlFiles().forEach {
                    if (map.containsKey(it.name)) {
                        map[it.name]?.add(ValueIdentifier(identifier = identifier, it))
                    }
                }
            }
            // Return the populated map.
            map
        } catch (e: Exception) {
            // Print the stack trace and return an empty map in case of any exception.
            e.printStackTrace()
            emptyMap()
        }
    }

    private fun Array<File>?.getXmlFiles(): List<File> {
        return this?.filter { it.extension == "xml" } ?: emptyList()
    }

    private fun String.toIdentifier(): String {
        return split("-")[1]
    }
}
