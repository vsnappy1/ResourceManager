package dev.randos.resourcemanager.model

/**
 * Represents a resource in the application, including its type and file path.
 *
 * @property type refers to type of Resource.
 * @property moduleDetails refers to module details.
 * @see [ResourceType]
 * @see [ModuleDetails]
 */
internal data class Resource(
    val type: ResourceType,
    val moduleDetails: ModuleDetails
)