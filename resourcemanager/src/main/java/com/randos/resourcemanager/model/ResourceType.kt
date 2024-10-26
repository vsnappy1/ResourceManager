package com.randos.resourcemanager.model

sealed class ResourceType{
    data object String: ResourceType()
    data object Color: ResourceType()
    data object Boolean: ResourceType()
    data object Integer: ResourceType()
    data object Dimension: ResourceType()
    data object Fraction: ResourceType()
    data object Array: ResourceType()
    data object StringArray: ResourceType()
    data object IntArray: ResourceType()
    data object Plural: ResourceType()
}
