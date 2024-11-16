package dev.randos.resourcemanager.utils

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