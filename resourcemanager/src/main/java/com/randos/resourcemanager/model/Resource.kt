package com.randos.resourcemanager.model

import java.io.File

/**
 * Represents a resource in the application, including its type and file path.
 *
 * @property type refers to type of Resource.
 * @property directoryPath path to the directory of resource.
 * @see [ResourceType]
 */
data class Resource(
    val type: ResourceType,
    val directoryPath: File
)