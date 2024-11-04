package dev.randos.resourcemanager.compiler.model

/**
 * Sealed class representing various types of value resources in an android application.
 */
internal sealed class ValueResourceType{
    data class String(val isParameterized: kotlin.Boolean = false) : ValueResourceType()
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
