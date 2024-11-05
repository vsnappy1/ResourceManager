import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * A Gradle task that signs a list of files using GPG (GNU Privacy Guard).
 * This task takes in files to be signed and a passphrase for authentication, then generates
 * signed `.asc` files for each input file.
 */
abstract class SignArtifactsTask : DefaultTask() {

    /**
     * A list of files that need to be signed.
     */
    @get:InputFiles
    lateinit var filesToSign: List<File>

    /**
     * The passphrase for GPG signing. Used to authenticate the signing process.
     */
    @get:Input
    lateinit var passphrase: String

    /**
     * Executes the GPG signing process on each file in [filesToSign].
     */
    @TaskAction
    fun run() {
        filesToSign.forEach { file ->
            if (file.exists()) {
                project.exec {
                    commandLine(
                        "gpg", "--batch", "--yes", "--passphrase", passphrase,
                        "--pinentry-mode", "loopback", "-ab", file.absolutePath
                    )
                }
                println("Generated signed file for: ${file.name}")
            } else {
                println("File does not exist: ${file.name}")
            }
        }
    }
}
