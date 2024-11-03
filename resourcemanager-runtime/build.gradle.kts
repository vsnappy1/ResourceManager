import javax.xml.parsers.DocumentBuilderFactory

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "dev.randos.resourcemanager.runtime"
    compileSdk = 34

    defaultConfig {
        minSdk = 23

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

kotlin {
    sourceSets {
        main {
            /*
             Adds the specified generated directory as a source for Kotlin files.
             This allows Kotlin compiler to recognize and include code files generated during the
             build process as part of the "main" source set.
             */
            kotlin.srcDir(layout.buildDirectory.dir("generated/project/main"))
        }
    }
}

tasks.register("generateProjectDetailsClass") {
    group = "generation"
    description =
        "Generates a ProjectDetails.kt file containing a list of package names for all modules in the project."

    // Gather all module names
    val packages = rootProject.subprojects.map {
        it.extensions.findByType<com.android.build.gradle.BaseExtension>()?.let { module ->
            module.namespace
                ?: getPackageNameFromManifestFile(it.file("src/main/AndroidManifest.xml"))
        }
    }

    // Define output directory and file path within the build directory
    val outputFile =
        layout.buildDirectory.file("generated/project/main/ProjectDetails.kt").get().asFile

    // Optimization: Define the input properties and output file here to allow Gradle to cache the
    // task result and skip execution if the input properties and output file are already up-to-date.
    inputs.properties(Pair("packages", packages))
    outputs.file(outputFile)

    doLast {
        // Build the content for the ProjectDetails class
        val packagesString = packages.filterNotNull().joinToString(",") { "\"$it\"" }
        val projectDetailsClassContent = StringBuilder().apply {
            appendLine("package dev.randos.resourcemanager.runtime")
            appendLine()
            appendLine("internal object ProjectDetails {")
            appendLine("\tfun packages() = listOf($packagesString)")
            appendLine("}")
        }.toString()

        // Ensure the output directory exists
        outputFile.parentFile.mkdirs()

        // Write content to the file
        try {
            outputFile.writeText(projectDetailsClassContent)
            println("ProjectDetails.kt generated successfully at ${outputFile.path}:")
        } catch (e: Exception) {
            println("Failed to generate ProjectDetails.kt: ${e.message}")
        }
    }
}

// Ensure `generateProjectDetailsClass` runs when resourcemanager-runtime module is compiled.
project(":resourcemanager-runtime").tasks.matching { it.name.startsWith("compile") }
    .configureEach {
        dependsOn("generateProjectDetailsClass")
    }

// Helper function to retrieve the package name from an AndroidManifest.xml file.
fun getPackageNameFromManifestFile(file: File): String? {
    if (!file.exists()) return null
    val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
    document.documentElement.normalize()
    return document.documentElement.getAttribute("package")
}