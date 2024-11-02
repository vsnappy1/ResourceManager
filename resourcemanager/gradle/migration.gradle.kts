val common = "(?:\\w+(?:\\.\\w+|\\(.*\\))*)*\\s*"
val replacements: Map<Regex, String> by lazy {
    mapOf(
        Regex("${common}.getString\\(\\s*R\\.\\s*string\\.\\s*(\\w+)\\s*\\)") to "ResourceManager.Strings.",
        Regex("${common}.getString\\(\\s*R\\.\\s*string\\.\\s*(\\w+)\\s*,\\s*(.*)\\s*\\)") to "ResourceManager.Strings.",
        Regex("${common}.getString\\(\\s*\\w+\\s*,\\s*R\\.\\s*string\\.\\s*(\\w+)\\s*\\)") to "ResourceManager.Strings.",
        Regex("${common}.getColor\\(\\s*R\\.\\s*color\\.\\s*(\\w+)\\s*\\)") to "ResourceManager.Colors.",
        Regex("${common}.getColor\\(\\s*R\\.\\s*color\\.\\s*(\\w+)\\s*,\\s*(.*)\\s*\\)") to "ResourceManager.Colors.",
        Regex("${common}.getColor\\(\\s*\\w+\\s*,\\s*R\\.\\s*color\\.\\s*(\\w+)\\s*\\)") to "ResourceManager.Colors.",
        Regex("${common}.getDrawable\\(\\s*R\\.\\s*drawable\\.\\s*(\\w+)\\s*\\)") to "ResourceManager.Drawables.",
        Regex("${common}.getDrawable\\(\\s*R\\.\\s*drawable\\.\\s*(\\w+)\\s*,\\s*(.*)\\s*\\)") to "ResourceManager.Drawables.",
        Regex("${common}.getDrawable\\(\\s*\\w+\\s*,\\s*R\\.\\s*drawable\\.\\s*(\\w+)\\s*\\)") to "ResourceManager.Drawables.",
        Regex("${common}.getStringArray\\(\\s*R\\.\\s*array\\.\\s*(\\w+)\\s*\\)") to "ResourceManager.StringArrays.",
        Regex("${common}.getIntArray\\(\\s*R\\.\\s*array\\.\\s*(\\w+)\\s*\\)") to "ResourceManager.IntArrays.",
        Regex("${common}.getBoolean\\(\\s*R\\.\\s*bool\\.\\s*(\\w+)\\s*\\)") to "ResourceManager.Booleans.",
        Regex("${common}.getFraction\\(\\s*R\\.\\s*fraction\\.\\s*(\\w+)\\s*\\)") to "ResourceManager.Fractions.",
        Regex("${common}.getDimension\\(\\s*R\\.\\s*dimen\\.\\s*(\\w+)\\s*\\)") to "ResourceManager.Dimensions.",
        Regex("${common}.getInteger\\(\\s*R\\.\\s*integer\\.\\s*(\\w+)\\s*\\)") to "ResourceManager.Integers.",
        Regex("${common}.getFraction\\(\\s*R\\.\\s*fraction\\.\\s*(\\w+)\\s*,\\s*(.*)\\s*\\)") to "ResourceManager.Fractions.",
        Regex("${common}.getQuantityString\\(\\s*R\\.\\s*plurals\\.\\s*(\\w+)\\s*,\\s*(.*)\\s*\\)") to "ResourceManager.Plurals."
    )
}

tasks.register("migrateToResourceManager") {
    group = "migration"
    description =
        "Replaces android resource access calls with the ResourceManager equivalents across the specified module."

    // Check for module flag
    if (!project.hasProperty("module")) {
        val message = "Error: Missing module name. Specify the module to migrate using -Pmodule=<module_name>."
        throw GradleException("Migration cancelled. $message")
    }

    // Check for confirmation flag
    if (!project.hasProperty("confirmMigration")) {
        println("Warning: This task will modify project files.")
        val message = "Error: Confirmation flag missing. To proceed, rerun the task with -PconfirmMigration=true."
        throw GradleException("Migration cancelled. $message")
    }

    val module = project.properties["module"]
    val modulePath = file("${file(projectDir.absolutePath).parent}/$module/src/main")

    // Check if given module exist
    if (!modulePath.exists()) {
        throw IllegalArgumentException("The specified module path does not exist: $modulePath. Please check the module name: '$module'.")
    }

    val codeFiles =
        fileTree(modulePath).files.filter { it.extension == "kt" || it.extension == "java" }

    println("\n***************** MIGRATION START *****************\n")
    var totalChangesCount = 0
    var fileUpdateCount = 0

    codeFiles.forEach { file ->
        var changesCount = 0
        var content = file.readText()

        replacements.forEach { (regex, s) ->
            val matches = regex.findAll(content)
            matches.forEach { matchResult ->
                val resourceId = matchResult.groups[1]?.value.orEmpty()
                val resourceIdCamelCase =
                    "${s}${resourceId.toCamelCase().replaceFirstChar { id -> id.lowercase() }}"

                content = when (matchResult.groups.size) {
                    2 -> {
                        content.replace(matchResult.value, "${resourceIdCamelCase}()")
                    }

                    3 -> {
                        val param = matchResult.groups[2]?.value.orEmpty()
                        content.replace(matchResult.value, "${resourceIdCamelCase}($param)")
                    }

                    else -> {
                        content
                    }
                }
                changesCount++
            }
        }
        if (changesCount > 0) {
            file.writeText(content)
            println("Updated file '${file.name}' (${file.path}:) with $changesCount change(s).")
            fileUpdateCount++
        }
        totalChangesCount += changesCount
    }
    println("\nModified $fileUpdateCount file(s) with a total of $totalChangesCount changes applied.")
    println("\n***************** MIGRATION END *****************\n")
}

fun String.toCamelCase(): String {
    return split('_') // Split by underscores
        .mapIndexed { index, word ->
            if (index == 0) word.lowercase() // First word stays lowercase
            else word.replaceFirstChar { it.uppercase() } // Capitalize the first letter of subsequent words
        }
        .joinToString("") // Join the words back together without spaces
}