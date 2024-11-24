package dev.randos.resourcemanager.model

/**
 * Represents details of a source file that has undergone migration or changes.
 *
 * @property name The name of the file.
 * @property path The full path to the file in the project structure.
 * @property changes A list of changes made to this file, where each change includes details such as
 * the line number, original content, and updated content.
 */
internal class SourceFileDetails(
    val name: String,
    val path: String,
    val changes: List<Change>
)