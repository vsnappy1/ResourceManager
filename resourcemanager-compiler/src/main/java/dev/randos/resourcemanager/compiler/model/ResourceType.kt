package dev.randos.resourcemanager.compiler.model

/**
 * Sealed class representing different types of resources in an android application.
 */
internal sealed class ResourceType{
    data object VALUES: ResourceType()
    data object DRAWABLES: ResourceType()
}
