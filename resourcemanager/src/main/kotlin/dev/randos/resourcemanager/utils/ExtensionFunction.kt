package dev.randos.resourcemanager.utils

import java.io.File

// Function to convert snake_case to camelCase
internal fun String.toCamelCase(): String {
    return split('_') // Split by underscores
        .mapIndexed { index, word ->
            if (index == 0) {
                word.lowercase() // Modify first character to be lowercase.
            } else {
                word.replaceFirstChar { it.uppercase() } // Capitalize the first letter of subsequent words.
            }
        }
        .joinToString("") // Join the words back together without spaces
}

internal fun Array<File>?.getXmlFiles(): List<File> {
    return this?.filter { it.extension == "xml" } ?: emptyList()
}