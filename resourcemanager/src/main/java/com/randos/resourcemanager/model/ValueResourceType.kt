package com.randos.resourcemanager.model

/**
 * Sealed class representing various types of value resources in an android application.
 */
internal sealed class ValueResourceType{
    data object String: ValueResourceType()
    data object Color: ValueResourceType()
    data object Boolean: ValueResourceType()
    data object Integer: ValueResourceType()
    data object Dimension: ValueResourceType()
    data object Fraction: ValueResourceType()
    data object Array: ValueResourceType()
    data object StringArray: ValueResourceType()
    data object IntArray: ValueResourceType()
    data object Plural: ValueResourceType()
}
