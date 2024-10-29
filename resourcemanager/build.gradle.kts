import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.archivesName
import java.security.MessageDigest

plugins {
    id("java-library")
    id("org.jetbrains.kotlin.jvm")
    id("com.google.devtools.ksp")
    id("maven-publish")
}

/**
 * Retrieves the KSP (Kotlin Symbol Processing) version to be used in the project.
 *
 * This value is determined by checking for a project property named "kspVersion".
 * If the property is defined in the project-level build.gradle or build.gradle.kts file,
 * its value is used. Otherwise, the default version "1.9.0-1.0.12" is used.
 *
 * To specify a custom version, add the following to your project-level build.gradle or build.gradle.kts file:
 *
 * ```
 * ext {
 *     set("kspVersion", <ksp_version>)
 * }
 * ```
 */
val kspVersion: String =
    if (project.findProperty("kspVersion") != null) project.findProperty("kspVersion")
        .toString() else "1.9.0-1.0.12"

dependencies {
    implementation("com.google.devtools.ksp:symbol-processing-api:$kspVersion")
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

kotlin {
    jvmToolchain(11)
}

val mavenPublication: MavenPublication by lazy {
    (publishing.publications.getByName("java") as MavenPublication)
}

val artifactRepo: File by lazy {
    val path = mavenPublication.let {
        StringBuilder().apply {
            append("upload/")
            append("${it.groupId.replace(".", "/")}/")
            append("${it.artifactId}/")
            append(it.version)
        }.toString()
    }
    file(layout.buildDirectory.dir(path))
}

val fullArtifactName: String by lazy {
    "${mavenPublication.artifactId}-${mavenPublication.version}"
}

/* ----------------- Generate Artifacts ----------------- */

// Task to generate Javadoc JAR
tasks.register<Jar>("generateJavadocJar") {
    group = "build"
    description = "Generates a JAR file containing Javadoc."

    archiveBaseName.set(mavenPublication.artifactId)
    archiveVersion.set(mavenPublication.version)
    archiveClassifier.set("javadoc")
    from(provider { tasks.named<Javadoc>("javadoc").get().destinationDir })
    dependsOn(tasks.named("javadoc"))
}

// Task to generate sources JAR
tasks.register<Jar>("generateSourcesJar") {
    group = "build"
    description = "Generates a JAR file containing source files."
    archiveBaseName.set(mavenPublication.artifactId)
    archiveVersion.set(mavenPublication.version)
    archiveClassifier.set("sources")
    from(sourceSets["main"].allSource)
}

// Task to generate the POM file
tasks.register<GenerateMavenPom>("generatePom") {
    group = "build"
    description = "Generates the POM XML file for the Maven publication."
    destination = file(layout.buildDirectory.dir("generated/maven/${fullArtifactName}.pom"))
    pom = mavenPublication.pom
}

// Combined task to execute all tasks
tasks.register("generateArtifacts") {
    group = "build"
    description = "Generates base JAR, javadoc JAR, sources JAR, and POM file."
    dependsOn("build", "generateJavadocJar", "generateSourcesJar", "generatePom")
    doLast {
        println("Generated file: ${fullArtifactName}.jar")
        println("Generated file: ${fullArtifactName}-javadoc.jar")
        println("Generated file: ${fullArtifactName}-sources.jar")
        println("Generated file: ${fullArtifactName}.pom")
    }
    finalizedBy("moveGeneratedArtifacts")
}

// Renames the original jar file
tasks.named("build") {
    archivesName = fullArtifactName
}

// Task to move generated artifacts to the upload directory
tasks.register<Copy>("moveGeneratedArtifacts") {
    group = "move" // Grouping under upload category
    description =
        "Moves generated base JAR, javadoc JAR, sources JAR, and POM file to the upload directory." // Description of the task

    // Define the source files
    val baseJarFile = file(layout.buildDirectory.dir("libs/${fullArtifactName}.jar"))
    val javadocJarFile = file(layout.buildDirectory.dir("libs/${fullArtifactName}-javadoc.jar"))
    val sourcesJarFile = file(layout.buildDirectory.dir("libs/${fullArtifactName}-sources.jar"))
    val pomFile = file(layout.buildDirectory.dir("generated/maven/${fullArtifactName}.pom"))

    // Specify the files to move
    from(baseJarFile, javadocJarFile, sourcesJarFile, pomFile)
    into(artifactRepo)

    // Ensure the task depends on the artifacts being generated
    dependsOn("generateArtifacts")
}

/* ----------------- Generate CheckSum ----------------- */

// Task to generate checksums
tasks.register("generateCheckSum") {
    group = "verification" // Grouping under verification category
    description = "Generates MD5 and SHA-1 checksums for specified files."

    doLast {
        // List of files to generate checksums for
        val baseJarFile = file("${artifactRepo}/${fullArtifactName}.jar")
        val javadocJarFile = file("${artifactRepo}/${fullArtifactName}-javadoc.jar")
        val sourcesJarFile = file("${artifactRepo}/${fullArtifactName}-sources.jar")
        val pomFile = file("${artifactRepo}/${fullArtifactName}.pom")

        val filesToChecksum = listOf(baseJarFile, javadocJarFile, sourcesJarFile, pomFile)

        filesToChecksum.forEach { file ->
            if (file.exists()) {
                val md5File = file("${file.absolutePath}.md5")
                val sha1File = file("${file.absolutePath}.sha1")

                // Generate and write MD5 checksum
                md5File.writeText(file.md5())
                // Generate and write SHA-1 checksum
                sha1File.writeText(file.sha1())

                println("Generated checksums for: ${file.name}")
            } else {
                println("File does not exist: ${file.name}")
            }
        }
    }
    mustRunAfter("moveGeneratedArtifacts")
    // Ensure the task depends on the artifacts being generated and moved to upload directory
    dependsOn("moveGeneratedArtifacts")
}

// Extension function to generate MD5 checksum
fun File.md5(): String {
    return this.inputStream().use { inputStream ->
        val digest = MessageDigest.getInstance("MD5")
        val buffer = ByteArray(8192)
        var read = inputStream.read(buffer)
        while (read > 0) {
            digest.update(buffer, 0, read)
            read = inputStream.read(buffer)
        }
        digest.digest().joinToString("") { "%02x".format(it) }
    }
}

// Extension function to generate SHA-1 checksum
fun File.sha1(): String {
    return this.inputStream().use { inputStream ->
        val digest = MessageDigest.getInstance("SHA-1")
        val buffer = ByteArray(8192)
        var read = inputStream.read(buffer)
        while (read > 0) {
            digest.update(buffer, 0, read)
            read = inputStream.read(buffer)
        }
        digest.digest().joinToString("") { "%02x".format(it) }
    }
}

/* ----------------- Sign Artifacts ----------------- */

// Register the signing tasks
tasks.register("signArtifacts") {
    group = "signing" // Grouping under signing category
    description = "Signs all artifacts with GPG/PGP."

    // Define the files to sign
    val baseJarFile = file("${artifactRepo}/${fullArtifactName}.jar")
    val javadocJarFile = file("${artifactRepo}/${fullArtifactName}-javadoc.jar")
    val sourcesJarFile = file("${artifactRepo}/${fullArtifactName}-sources.jar")
    val pomFile = file("${artifactRepo}/${fullArtifactName}.pom")

    doLast {
        val filesToSign = listOf(baseJarFile, javadocJarFile, sourcesJarFile, pomFile)

        // Signing each file
        filesToSign.forEach { file ->
            if (file.exists()) {
                exec {
                    commandLine("gpg", "-ab", file.absolutePath)
                }
                println("Generated signed file for: ${file.name}")
            } else {
                println("File does not exist, skipping: ${file.name}")
            }
        }
    }
    mustRunAfter("moveGeneratedArtifacts")
    // Ensure the task depends on the artifacts being generated and moved to upload directory
    dependsOn("moveGeneratedArtifacts")
}

tasks.register<Zip>("createBundle") {
    group = "build" // or any other logical grouping you prefer
    description = "Creates a ZIP bundle of the generated artifacts for distribution."
    archiveFileName = "bundle.zip"
    destinationDirectory = file(layout.buildDirectory.dir("zip"))
    from(file(layout.buildDirectory.dir("upload")))
    doLast {
        println("Bundle generated.")
    }
    dependsOn("moveGeneratedArtifacts", "generateCheckSum", "signArtifacts")
}

/* ----------------- Maven Publish (Meta data) ----------------- */

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("java") {
                from(components["java"])
                groupId = "dev.randos"
                artifactId = "resourcemanager"
                version = "1.0.0"

                val bundle = file(layout.buildDirectory.dir("zip/bundle.zip"))
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
            }
        }
    }
}
