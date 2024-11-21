package dev.randos.resourcemanager.model

/**
 * Sealed class representing various types of value resources in an android application.
 */
internal sealed class ValueResourceType(val value: kotlin.String) {
    object String : ValueResourceType("string")

    object Color : ValueResourceType("color")

    object Boolean : ValueResourceType("bool")

    object Integer : ValueResourceType("integer")

    object Dimension : ValueResourceType("dimen")

    object Fraction : ValueResourceType("fraction")

    object Array : ValueResourceType("array")

    object StringArray : ValueResourceType("array")

    object IntArray : ValueResourceType("array")

    object Plural : ValueResourceType("plurals")
}