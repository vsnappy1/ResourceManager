package dev.randos.resourcemanager.model

/**
 * Represents a specific change made to a line of code in a file.
 *
 * @property lineNumber The line number in the file where the change occurred.
 * @property current The original content of the line before the change.
 * @property updated The new content of the line after the change.
 */
internal class Change(
    val lineNumber: Int,
    val current: String,
    val updated: String
)