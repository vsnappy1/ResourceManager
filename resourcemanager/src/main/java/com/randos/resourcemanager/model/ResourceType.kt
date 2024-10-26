package com.randos.resourcemanager.model

/**
 * Sealed class representing different types of resources in an android application.
 */
sealed class ResourceType{
    data object VALUES: ResourceType()
    data object DRAWABLES: ResourceType()
}
