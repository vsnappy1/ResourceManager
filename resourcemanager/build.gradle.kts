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

// Task to generate Javadoc JAR
tasks.register<Jar>("generateJavadocJar") {
    group = "build"
    description = "Generates a JAR file containing Javadoc."
    dependsOn(tasks.named("javadoc"))
    val publication = (publishing.publications.getByName("java") as MavenPublication)
    archiveBaseName.set(publication.artifactId)
    archiveVersion.set(publication.version)
    archiveClassifier.set("javadoc")
    from(provider { tasks.named<Javadoc>("javadoc").get().destinationDir })
}

// Task to generate sources JAR
tasks.register<Jar>("generateSourcesJar") {
    group = "build"
    description = "Generates a JAR file containing source files."
    val publication = (publishing.publications.getByName("java") as MavenPublication)
    archiveBaseName.set(publication.artifactId)
    archiveVersion.set(publication.version)
    archiveClassifier.set("sources")
    from(sourceSets["main"].allSource)
}

// Task to generate the POM file
tasks.register<GenerateMavenPom>("generatePomXml") {
    group = "build"
    description = "Generates the POM XML file for the Maven publication."
    val publication = (publishing.publications.getByName("java") as MavenPublication)
    destination =
        file(layout.buildDirectory.dir("generated/maven/${publication.artifactId}-${publication.version}.pom"))
    pom = publication.pom
}

// Combined task to execute all tasks
tasks.register("generateArtifacts") {
    group = "build"
    description = "Generates base JAR, javadoc JAR, sources JAR, and POM file."
    dependsOn("build", "generateJavadocJar", "generateSourcesJar", "generatePomXml")
    doLast {
        println("All artifacts (Javadoc JAR, sources JAR, and POM) have been generated successfully.")
    }
}

tasks.named("build") {
    val publication = (publishing.publications.getByName("java") as MavenPublication)
    archivesName = "${publication.artifactId}-${publication.version}"
}

// Task to move generated artifacts to the upload directory
tasks.register<Copy>("moveGeneratedArtifacts") {
    group = "move" // Grouping under upload category
    description = "Moves generated base JAR, javadoc JAR, sources JAR, and POM file to the upload directory." // Description of the task

    // Define the source files
    val publication = (publishing.publications.getByName("java") as MavenPublication)
    val baseJarFile = file(layout.buildDirectory.dir("libs/${publication.artifactId}-${publication.version}.jar"))
    val javadocJarFile = file(layout.buildDirectory.dir("libs/${publication.artifactId}-${publication.version}-javadoc.jar"))
    val sourcesJarFile = file(layout.buildDirectory.dir("libs/${publication.artifactId}-${publication.version}-sources.jar"))
    val pomFile = file(layout.buildDirectory.dir("generated/maven/${publication.artifactId}-${publication.version}.pom"))

    // Define the destination directory
    val uploadDir = file(layout.buildDirectory.dir("upload"))

    // Specify the files to move
    from(baseJarFile, javadocJarFile, sourcesJarFile, pomFile)
    into(uploadDir)

    // Ensure the task depends on the artifacts being generated
    dependsOn("generateArtifacts")
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

// Task to generate checksums
tasks.register("generateCheckSum") {
    group = "verification" // Grouping under verification category
    description = "Generates MD5 and SHA-1 checksums for specified files."

    val publication = (publishing.publications.getByName("java") as MavenPublication)

    // Define the files for which checksums will be generated
    val baseJarFile = file(layout.buildDirectory.dir("upload/${publication.artifactId}-${publication.version}.jar"))
    val javadocJarFile = file(layout.buildDirectory.dir("upload/${publication.artifactId}-${publication.version}-javadoc.jar"))
    val sourcesJarFile = file(layout.buildDirectory.dir("upload/${publication.artifactId}-${publication.version}-sources.jar"))
    val pomFile = file(layout.buildDirectory.dir("upload/${publication.artifactId}-${publication.version}.pom"))

    doLast {
        // List of files to generate checksums for
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
                println(" - MD5: ${md5File.readText()}")
                println(" - SHA-1: ${sha1File.readText()}")
            } else {
                println("File does not exist: ${file.name}")
            }
        }
    }
    // Ensure the task depends on the artifacts being generated and moved to upload directory
    dependsOn("moveGeneratedArtifacts")
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("java") {
                from(components["java"])
                groupId = "dev.randos"
                artifactId = "resourcemanager"
                version = "1.0.0"

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
                            organizationUrl = "http://www.randos.dev"
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