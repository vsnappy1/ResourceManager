import java.util.Properties

plugins {
    id("java-library")
    id("org.jetbrains.kotlin.jvm")
    id("com.google.devtools.ksp")
    id("maven-publish")
}

apply("gradle/migration.gradle.kts")

/**
 * Retrieves the KSP (Kotlin Symbol Processing) version to be used in the project.
 *
 * Must add the following to your project-level build.gradle or build.gradle.kts file:
 *
 * ```
 * ext {
 *     set("kspVersion", <ksp_version>)
 * }
 * ```
 */
val kspVersion: String = project.findProperty("kspVersion").toString()

dependencies {
    implementation("com.google.devtools.ksp:symbol-processing-api:$kspVersion")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

kotlin {
    jvmToolchain(8)
}

val mavenPublication: MavenPublication by lazy {
    (publishing.publications.getByName("java") as MavenPublication)
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

val fullArtifactName: String by lazy {
    "${mavenPublication.artifactId}-${mavenPublication.version}"
}

val artifacts: List<File> by lazy {
    listOf(
        file("${artifactRepo}/${fullArtifactName}.jar"),
        file("${artifactRepo}/${fullArtifactName}-javadoc.jar"),
        file("${artifactRepo}/${fullArtifactName}-sources.jar"),
        file("${artifactRepo}/${fullArtifactName}.pom")
    )
}

/* ----------------- Generate Artifacts ----------------- */

tasks.register<GenerateArtifactsTask>("generateArtifacts") {
    group = "build"
    description = "Generates base JAR, javadoc JAR, sources JAR, and POM file."
    artifactNameWithVersion = fullArtifactName
    outputDirectory = artifactRepo
    pom = mavenPublication.pom
    artifactRepo.mkdirs()
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
    destinationDirectory = file(layout.buildDirectory.dir("publish/bundle"))
    from(artifactRepo.parentFile.parentFile.parentFile.parentFile)
    doLast { println("Bundle generated.") }
}

/* ----------------- Maven Publish (Meta data) ----------------- */
afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("java") {
                from(components["java"])
                groupId = "dev.randos"
                artifactId = "resourcemanager-compiler"
                version = "0.0.1"

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
