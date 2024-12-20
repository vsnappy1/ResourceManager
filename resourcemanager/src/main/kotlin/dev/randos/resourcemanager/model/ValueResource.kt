package dev.randos.resourcemanager.model

/**
 * Represents a value resource in the application.
 *
 * @property name The name of the resource (i.e. is_premium in case of <bool name="is_premium">false</bool>)
 * @property type The type of the resource
 * @see [ValueResourceType]
 */
internal class ValueResource(
    val name: String,
    val type: ValueResourceType
)