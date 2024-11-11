package dev.randos.resourcemanager.model

import java.io.File

/**
 * A data class representing the details of a module in an Android project.
 *
 * @property moduleName The name of the module, Default value is an empty string if not provided.
 * @property namespace The namespace associated with the module, usually matching the module's package name.
 *                     Default value is an empty string if not provided.
 * @property resDirectory The directory where the module's resources are stored, usually in the module's
 *                        `src/main/res` path.
 */
internal data class ModuleDetails(
    val moduleName: String = "",
    val namespace: String = "",
    val resDirectory: File
)
