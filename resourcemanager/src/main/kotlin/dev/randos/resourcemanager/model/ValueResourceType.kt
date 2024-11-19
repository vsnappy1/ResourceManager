package dev.randos.resourcemanager.model

/**
 * Sealed class representing various types of value resources in an android application.
 */
internal sealed class ValueResourceType {
    object String : ValueResourceType()

    object Color : ValueResourceType()

    object Boolean : ValueResourceType()

    object Integer : ValueResourceType()

    object Dimension : ValueResourceType()

    object Fraction : ValueResourceType()

    object Array : ValueResourceType()

    object StringArray : ValueResourceType()

    object IntArray : ValueResourceType()

    object Plural : ValueResourceType()
}