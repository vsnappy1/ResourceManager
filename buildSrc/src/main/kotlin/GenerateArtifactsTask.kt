import org.gradle.api.DefaultTask
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.bundling.Jar
import java.io.File
import org.gradle.api.publish.maven.MavenPom
import org.gradle.api.publish.maven.tasks.GenerateMavenPom

/**
 * A Gradle task for generating multiple build artifacts, including:
 * - A base JAR containing compiled classes.
 * - A Javadoc JAR.
 * - A Sources JAR.
 * - A Maven POM file.
 *
 * This task combines multiple artifact generation tasks into one, ensuring
 * that all necessary files for a Maven publication are created in the specified
 * output directory.
 */
abstract class GenerateArtifactsTask : DefaultTask() {

    /**
     * The name of the artifact, including its version, used to label
     * generated files consistently.
     */
    @get:Input
    lateinit var artifactNameWithVersion: String

    /**
     * The directory where all generated artifacts will be saved.
     */
    @get:InputDirectory
    lateinit var outputDirectory: File

    /**
     * The Maven POM configuration for generating the POM file.
     */
    @get:Input
    lateinit var pom: MavenPom

    /**
     * Main task action that orchestrates the creation of all artifacts.
     */
    @TaskAction
    fun run() {
        // Run each sub-task by directly configuring and calling the respective actions.
        generateBaseJar()
        generateJavadocJar()
        generateSourcesJar()
        generatePom()

        // Log the generated artifacts
        outputDirectory.walkTopDown().forEach {
            println("Generated file: ${it.name}")
        }
    }

    /**
     * Generates the base JAR file, containing the compiled classes.
     */
    private fun generateBaseJar() {
        project.tasks.create("generateBaseJar", Jar::class.java) {
            group = "build"
            description = "Generates a base JAR file containing compiled classes."
            archiveBaseName.set(artifactNameWithVersion)
            destinationDirectory.set(outputDirectory)
            from(project.tasks.getByName("classes").outputs.files)
            dependsOn(":resourcemanager-compiler:build")
        }.also { it.actions.forEach { action -> action.execute(it) } }
    }

    /**
     * Generates a Javadoc JAR file containing the Javadocs.
     */
    private fun generateJavadocJar() {
        project.tasks.create("generateJavadocJar", Jar::class.java) {
            group = "build"
            description = "Generates a JAR file containing Javadoc."
            archiveBaseName.set(artifactNameWithVersion)
            archiveClassifier.set("javadoc")
            destinationDirectory.set(outputDirectory)
            from(project.tasks.getByName("javadoc").outputs.files)
            dependsOn("javadoc")
        }.also { it.actions.forEach { action -> action.execute(it) } }
    }

    /**
     * Generates a Sources JAR file containing the source files of the main.
     */
    private fun generateSourcesJar() {
        val extensions = project.extensions.getByType(JavaPluginExtension::class.java)
        project.tasks.create("generateSourcesJar", Jar::class.java) {
            group = "build"
            description = "Generates a JAR file containing source files."
            archiveBaseName.set(artifactNameWithVersion)
            archiveClassifier.set("sources")
            destinationDirectory.set(outputDirectory)
            from(extensions.sourceSets.getByName("main").allSource)
        }.also { it.actions.forEach { action -> action.execute(it) } }
    }

    /**
     * Generates a Maven POM file in the specified output directory.
     * This file provides metadata about the project and is necessary for
     * publishing to Maven repositories.
     */
    private fun generatePom() {
        project.tasks.create("generatePom", GenerateMavenPom::class.java) {
            group = "build"
            description = "Generates the POM XML file for the Maven publication."
            destination = File(outputDirectory, "$artifactNameWithVersion.pom")
            pom = this@GenerateArtifactsTask.pom
        }.also { it.actions.forEach { action -> action.execute(it) } }
    }
}
