package dev.randos.resourcemanager.runtime

import android.app.Application
import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.util.Log

/**
 * ContentProvider implementation to initialize generated ResourceManager classes dynamically.
 *
 * This provider initializes `ResourceManager` for each relevant package within the application.
 * It executes early in the application's lifecycle due to its presence in the AndroidManifest,
 * ensuring that the `ResourceManager` classes have access to the application context.
 * This approach avoids the need for explicit initialization in the Application class.
 */
internal class ResourceManagerContentProvider : ContentProvider() {

    companion object {
        private const val TAG = "ResourceManagerContentProvider"
    }

    /**
     * Called when the provider is created, initializing ResourceManager classes.
     *
     * This method retrieves all relevant packages, attempts to locate each generated
     * `ResourceManager` class, and invokes its `initialize` method using reflection.
     * The `initialize` method provides the application context, required for
     * accessing system resources.
     *
     * @return `true` if the provider was successfully created, `false` otherwise.
     */
    override fun onCreate(): Boolean {
        Log.d(TAG, "onCreate: content provider $context")

        // Retrieve package names and attempt to initialize each package's ResourceManager
        val packages = getPackages()
        Log.i(TAG, "${packages.size} packages found for ResourceManager initialization.")
        packages.forEach {
            try {
                // Use reflection to find and initialize ResourceManager in each package
                Class.forName("$it.ResourceManager").run {
                    val initialize = getMethod("initialize", Application::class.java)
                    initialize.invoke(
                        getDeclaredConstructor().newInstance(),
                        context as Application
                    )
                    Log.i(TAG, "$it.ResourceManager initialized.")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.w(TAG, "$it.ResourceManager not found.", e)
            }
        }
        return true
    }

    override fun query(
        p0: Uri,
        p1: Array<out String>?,
        p2: String?,
        p3: Array<out String>?,
        p4: String?
    ): Cursor? = null

    override fun getType(p0: Uri): String? = null

    override fun insert(p0: Uri, p1: ContentValues?): Uri? = null

    override fun delete(p0: Uri, p1: String?, p2: Array<out String>?): Int = -1

    override fun update(p0: Uri, p1: ContentValues?, p2: String?, p3: Array<out String>?): Int = -1

    /**
     * Retrieves a list of package names where generated `ResourceManager` classes may be present.
     *
     * This method uses a kotlin class `ProjectDetails`  which is generated during build,
     * which provides a list of package names to search.
     *
     * @return A list of package names to check for `ResourceManager` initialization.
     */
    private fun getPackages(): List<String> {
        return ProjectDetails.packages()
    }
}