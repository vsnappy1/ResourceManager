import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.security.MessageDigest

/**
 * A Gradle task for generating checksum files (MD5 and SHA-1) for a list of files.
 * This task takes in a list of files and generates a `.md5` and `.sha1` file
 * for each, storing the respective checksum values.
 */
abstract class GenerateChecksumTask : DefaultTask() {

    /**
     * List of files for which the checksum files will be generated.
     */
    @get:InputFiles
    lateinit var filesToChecksum: List<File>

    /**
     * Main task action that generates `.md5` and `.sha1` files for each input file.
     */
    @TaskAction
    fun run() {
        filesToChecksum.forEach { file ->
            if (file.exists()) {
                // Create files to store the MD5 and SHA-1 checksums.
                val md5File = File("${file.absolutePath}.md5")
                val sha1File = File("${file.absolutePath}.sha1")

                // Write the checksum results to each file.
                md5File.writeText(file.md5())
                sha1File.writeText(file.sha1())

                println("Generated checksum file for: ${file.name}")
            } else {
                println("File does not exist: ${file.name}")
            }
        }
    }

    /**
     * Extension function for calculating the MD5 checksum of a file.
     *
     * @return A string representing the MD5 hash of the file's contents.
     */
    private fun File.md5(): String = checksum("MD5")

    /**
     * Extension function for calculating the SHA-1 checksum of a file.
     *
     * @return A string representing the SHA-1 hash of the file's contents.
     */
    private fun File.sha1(): String = checksum("SHA-1")

    /**
     * Helper function that computes a checksum based on the specified algorithm.
     *
     * @param algorithm The hashing algorithm to use, such as "MD5" or "SHA-1".
     * @return A string representing the computed hash of the file's contents.
     */
    private fun File.checksum(algorithm: String): String {
        val digest = MessageDigest.getInstance(algorithm)
        return inputStream().use { fis ->
            digest.digest(fis.readBytes()).joinToString("") { "%02x".format(it) }
        }
    }
}

