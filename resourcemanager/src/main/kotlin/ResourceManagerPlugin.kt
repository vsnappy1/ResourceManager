import dev.randos.resourcemanager.ResourceManagerGenerator
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File

class ResourceManagerPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val generatedFile = File(project.projectDir, "build/generated/resourcemanager/main/ResourceManager.kt")
        val resourceManager = ResourceManagerGenerator(project.projectDir, generatedFile)

        val generateResourceManagerTask = project.tasks.register("generateResourceManager") {
            inputs.files(resourceManager.getFilesUnderObservation())
            outputs.files(generatedFile)
            doLast {
                resourceManager.generate()
            }
        }

        project.tasks.matching { it.name.startsWith("compile") }
            .configureEach {
                dependsOn(generateResourceManagerTask)
            }
    }
}