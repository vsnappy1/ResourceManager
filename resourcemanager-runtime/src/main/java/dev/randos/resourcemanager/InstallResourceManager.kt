package dev.randos.resourcemanager

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
/**
 * Annotation to indicate a class should be managed as a resource manager.
 *
 * @param namespace Specifies the namespace for the resource manager.
 *                  If the namespace differs from the package name of
 *                  the application file, it should be provided here.
 *                  This allows for flexibility in resource management
 *                  and helps ensure that the correct resources are
 *                  referenced in the application.
 */
annotation class InstallResourceManager(val namespace: String = "")