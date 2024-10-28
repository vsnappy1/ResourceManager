package dev.randos.resourcemanager.model

/**
 * Sealed class representing different types of resources in an android application.
 */
internal sealed class ResourceType{
    data object VALUES: ResourceType()
    data object DRAWABLES: ResourceType()
}
