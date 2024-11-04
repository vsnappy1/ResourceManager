package dev.randos.resourcemanager.compiler.manager

import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Manages caching operations for the generated resource files to reduce redundant file generation
 * and support cache reuse when there are no relevant changes in observed files.
 *
 * @property buildDirectory The build directory where cache and generated files are located.
 * @property filesUnderObservation A list of files whose last modified timestamps are observed for changes.
 */
class CacheManager(
    private val buildDirectory: File?,
    private val filesUnderObservation: List<File>
) {

    companion object {
        private const val CACHE_DIRECTORY_PATH = "cache/dev/randos/resourcemanager"
        private const val CONTENT_FILE_NAME = "content.txt"
        private const val LAST_MODIFIED_TIMESTAMP_FILE_NAME = "last_modified_timestamp.txt"
    }

    /**
     * Caches the content, path, and last modified timestamp of the generated ResourceManager file
     * if it exists in the KSP-generated directory.
     */
    fun cache() {
        val moduleName = buildDirectory?.parentFile?.name ?: return

        val kspDirectory = File(buildDirectory, "generated/ksp")
        if (!kspDirectory.exists()) return

        val resourceManagerFile =
            kspDirectory.walkTopDown().find { it.nameWithoutExtension == "ResourceManager" }
                ?: return

        val cacheDirectory = File(buildDirectory, "$CACHE_DIRECTORY_PATH/$moduleName")
        cacheDirectory.mkdirs()

        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        // Cache the content, file path, and last modified timestamp
        File(cacheDirectory, CONTENT_FILE_NAME)
            .writeText("${resourceManagerFile.readText()}// Cached on $timestamp")
        File(cacheDirectory, LAST_MODIFIED_TIMESTAMP_FILE_NAME)
            .writeText(getMostRecentLastModifiedTimestamp())

        println("ResourceManager: Cached successfully.")
    }

    /**
     * Retrieves the cached content of the ResourceManager file.
     *
     * @return The cached content as a String, or null if the cache does not exist.
     */
    fun getCachedContent() : String? {
        val moduleName = buildDirectory?.parentFile?.name ?: return null
        val cacheDirectory = File(buildDirectory, "$CACHE_DIRECTORY_PATH/$moduleName")

        val contentFile = File(cacheDirectory, CONTENT_FILE_NAME)
        if (contentFile.exists()) {
            return contentFile.readText()
        }
        return null
    }

    /**
     * Invalidates the cache by deleting the cached files for the module.
     */
    fun invalidateCache() {
        val moduleName = buildDirectory?.parentFile?.name ?: return
        val cacheDirectory = File(buildDirectory, "$CACHE_DIRECTORY_PATH/$moduleName")

        if (cacheDirectory.exists()) {
            if (cacheDirectory.deleteRecursively()) {
                println("ResourceManager: Cache invalidated successfully.")
            } else {
                println("ResourceManager: Cache invalidation failed.")
            }
        }
    }

    /**
     * Checks if the cache is up to date by comparing the stored last modified timestamps of observed files
     * with the current timestamps.
     *
     * @return `true` if cache is up to date, `false` otherwise.
     */
    fun isCacheUpToDate(): Boolean {
        val moduleName = buildDirectory?.parentFile?.name ?: return false
        val cacheDirectory = File(buildDirectory, "$CACHE_DIRECTORY_PATH/$moduleName")

        val lastModifiedTimestampFile = File(cacheDirectory, LAST_MODIFIED_TIMESTAMP_FILE_NAME)
        if (lastModifiedTimestampFile.exists()) {
            val storedTimestamp = getMostRecentLastModifiedTimestamp()
            return storedTimestamp == lastModifiedTimestampFile.readText()
        }
        return false
    }

    /**
     * Retrieves the most recent last modified timestamp from the list of  files under observation.
     *
     * @return The most recent last modified timestamp as a string.
     */
    private fun getMostRecentLastModifiedTimestamp(): String {
        val mostRecentTimestamp = filesUnderObservation.maxOfOrNull { it.lastModified() } ?: 0L
        return mostRecentTimestamp.toString()
    }
}