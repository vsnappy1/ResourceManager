package dev.randos.resourcemanager.model

/**
 * Sealed class representing different types of resources in an android application.
 */
internal sealed class ResourceType {
    object VALUES : ResourceType()

    object DRAWABLES : ResourceType()
}