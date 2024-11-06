import com.android.build.gradle.internal.tasks.factory.dependsOn
import java.util.Properties
import javax.xml.parsers.DocumentBuilderFactory

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
}

android {
    namespace = "dev.randos.resourcemanager"
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

val mavenPublication: MavenPublication by lazy {
    (publishing.publications.getByName("release") as MavenPublication)
}

val artifactRepo: File by lazy {
    val path = mavenPublication.let {
        StringBuilder().apply {
            append("artifacts/")
            append("${it.groupId.replace(".", "/")}/")
            append("${it.artifactId}/")
            append(it.version)
        }.toString()
    }
    file(layout.buildDirectory.dir(path))
}

val publishBundleRepo: File by lazy { file(layout.buildDirectory.dir("publish/bundle"))}

val fullArtifactName: String by lazy {
    "${mavenPublication.artifactId}-${mavenPublication.version}"
}

val artifacts: List<File> by lazy {
    listOf(
        file("${artifactRepo}/${fullArtifactName}.aar"),
        file("${artifactRepo}/${fullArtifactName}-javadoc.jar"),
        file("${artifactRepo}/${fullArtifactName}-sources.jar"),
        file("${artifactRepo}/${fullArtifactName}.pom")
    )
}

/* ----------------- Generate Artifacts ----------------- */

tasks.register("generateAar") {
    dependsOn("assembleRelease")
    doLast {
        val aarFile = file(layout.buildDirectory.dir("outputs/aar/${project.name}-release.aar"))

        // Create the destination directory if it doesn't exist
        artifactRepo.mkdirs()

        // Copy the AAR file to the destination directory
        copy {
            from(aarFile)
            rename { it.replace("${project.name}-release", fullArtifactName) }
            into(artifactRepo)
        }
    }
}

tasks.register<Jar>("generateSourcesJar"){
    archiveBaseName.set(fullArtifactName)
    archiveClassifier.set("sources")
    destinationDirectory.set(artifactRepo)
    from(android.sourceSets.getByName("main").java.srcDirs)
    dependsOn("generateProjectDetailsClass")
}

tasks.register<Javadoc>("javadoc") {
    isFailOnError = false
    source = android.sourceSets.getByName("main").java.getSourceFiles()
    classpath += project.files(android.bootClasspath.joinToString(File.separator))
    setDestinationDir(file(layout.buildDirectory.dir("docs/javadoc")))
}

tasks.register<Jar>("generateJavadocJar") {
    archiveBaseName.set(fullArtifactName)
    archiveClassifier.set("javadoc") // Correct the classifier to "javadoc"
    destinationDirectory.set(artifactRepo)

    // Use the output directory of the Javadoc task
    from(tasks.named<Javadoc>("javadoc").get().destinationDir)

    // Make sure this task depends on the Javadoc task
    dependsOn("javadoc")
}

tasks.register<GenerateMavenPom>("generatePom"){
    destination = File(artifactRepo, "$fullArtifactName.pom")
    pom = mavenPublication.pom
}

tasks.register("generateArtifacts") {
    group = "build"
    description = "Generates base JAR, javadoc JAR, sources JAR, and POM file."
    artifactRepo.mkdirs()
    dependsOn("generateAar", "generateJavadocJar", "generateSourcesJar", "generatePom")
}

/* ----------------- Generate CheckSum ----------------- */

// Task to generate checksums
tasks.register<GenerateChecksumTask>("generateChecksum") {
    group = "verification"
    description = "Generates MD5 and SHA-1 checksums for specified files."
    filesToChecksum = artifacts
}

/* ----------------- Sign Artifacts ----------------- */

// Register the signing tasks
tasks.register<SignArtifactsTask>("signArtifacts") {
    group = "signing" // Grouping under signing category
    description = "Signs all artifacts with GPG/PGP."
    filesToSign = artifacts
    passphrase = getSecret("GPG_PASSPHRASE")
}

fun getSecret(name: String): String {
    // Load passphrase from local.properties or environment variable
    val properties = Properties()
    val localPropertiesFile = project.rootProject.file("local.properties")

    // Check if local.properties exists
    if (localPropertiesFile.exists()) {
        properties.load(localPropertiesFile.inputStream())
    }

    return properties.getProperty(name) ?: System.getenv(name)
    ?: throw GradleException("$name not found in local.properties or environment variables.")
}

/* ----------------- Create Bundle For Publication ----------------- */

tasks.register<Zip>("generateBundle") {
    group = "build" // or any other logical grouping you prefer
    description = "Creates a ZIP bundle of the generated artifacts for distribution."
    archiveFileName = "$fullArtifactName.zip"
    destinationDirectory = publishBundleRepo
    from(artifactRepo.parentFile.parentFile.parentFile.parentFile)
    doLast { println("Bundle generated.") }
}

/* ----------------- Maven Publish (Meta data) ----------------- */
publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "dev.randos"
            artifactId = "resourcemanager-runtime"
            version = "0.0.1"

            val bundle = file("${publishBundleRepo.absolutePath}/$artifactId-$version.zip")
            artifact(bundle)

            pom {
                name = "${groupId}:${artifactId}"
                description =
                    "An Android library that simplifies accessing Android resources (strings, colors, drawables, etc.) in both Android and non-Android components (e.g. ViewModel) using generated code."
                url = "https://github.com/vsnappy1/ResourceManager"

                licenses {
                    license {
                        name = "The Apache License, Version 2.0"
                        url = "http://www.apache.org/licenses/LICENSE-2.0.txt"
                    }
                }

                developers {
                    developer {
                        name = "Vishal Kumar"
                        email = "vsnappy1@gmail.com"
                        organization = "Randos"
                        organizationUrl = "https://www.randos.dev"
                    }
                }

                scm {
                    connection = "scm:git:git://github.com/vsnappy1/ResourceManager.git"
                    developerConnection =
                        "scm:git:ssh://github.com/vsnappy1/ResourceManager.git"
                    url = "https://github.com/vsnappy1/ResourceManager"
                }
            }
            afterEvaluate {
                from(components["release"])
            }
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