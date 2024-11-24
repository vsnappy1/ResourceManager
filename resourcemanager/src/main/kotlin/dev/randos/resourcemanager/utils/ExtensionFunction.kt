package dev.randos.resourcemanager.utils

/**
 * Converts a snake_case or dot.separated string to camelCase.
 * If the string does not contain valid delimiters (e.g., "_" or "."),
 * the original string is returned unchanged.
 *
 * Valid function names must start with a letter or underscore and contain only
 * letters, digits, or underscores (following typical programming conventions).
 *
 * Example:
 * "example_string" -> "exampleString"
 * "another.example.test" -> "anotherExampleTest"
 * "alreadyCamelCase" -> "alreadyCamelCase"
 *
 * @receiver String in snake_case, dot.separated, or any other format.
 * @return The string converted to camelCase if in the expected format, or the original string.
 */
internal fun String.toCamelCase(): String {
    // Regular expression for valid function names: starts with a letter/underscore, contains letters, digits, or underscores
    val validFunctionNameRegex = "^[a-zA-Z_][a-zA-Z0-9_]*$".toRegex()

    // Check if the string contains valid delimiters
    return if (contains("_") || contains(".") || contains("-")) {
        split('_', '.', '-')
            .mapIndexed { index, word ->
                when (index) {
                    0 -> word.lowercase() // Keep the first word in lowercase.
                    else -> word.replaceFirstChar { it.uppercase() } // Capitalize subsequent words.
                }
            }
            .joinToString("")
            .takeIf { it.matches(validFunctionNameRegex) } ?: this
    } else {
        this
    }
}