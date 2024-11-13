import java.util.Properties

plugins {
    `kotlin-dsl`
    kotlin("jvm")
    id("java-gradle-plugin")
    id("com.gradle.plugin-publish") version "1.3.0"
    id("maven-publish")
}
val agp: String by project
val pluginGroup = "dev.randos"
val pluginVersion = "0.0.1"
val pluginName = "resourcemanager"

dependencies {
    implementation(gradleApi())
    implementation(localGroovy())
    compileOnly("com.android.tools.build:gradle:$agp")
}

group = pluginGroup
version = pluginVersion

gradlePlugin {
    website.set("https://github.com/vsnappy1/ResourceManager")
    vcsUrl.set("https://github.com/vsnappy1/ResourceManager")
    plugins {
        create("resourcemanager") {
            id = "dev.randos.resourcemanager"
            implementationClass = "ResourceManagerPlugin"
            displayName = "Resource Manager"
            description = "ResourceManager is an Android plugin that simplifies accessing Android resources (strings, colors, drawables, etc.) in both Android and non-Android components (e.g., ViewModel) using generated code."
            tags.set(listOf("android", "androidResources", "codeGeneration"))
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

kotlin {
    jvmToolchain(8)
}

publishing {
    repositories {
        mavenLocal()
    }
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

val publishBundleRepo: File by lazy { file(layout.buildDirectory.dir("publish/bundle"))}

val artifacts: List<File> by lazy {
    val fullQualifiedPath = "${artifactRepo}/${pluginName}-${pluginVersion}"
    listOf(
        file("${fullQualifiedPath}.jar"),
        file("${fullQualifiedPath}-javadoc.jar"),
        file("${fullQualifiedPath}-sources.jar"),
        file("${fullQualifiedPath}.pom")
    )
}

/* ----------------- Generate Artifacts ----------------- */

tasks.register<GenerateArtifactsTask>("generateArtifacts") {
    group = "build"
    description = "Generates base AAR, javadoc JAR, sources JAR, and POM file."
    artifactName = pluginName
    artifactVersion = pluginVersion
    outputDirectory = artifactRepo
    pom = mavenPublication.pom
    artifactRepo.mkdirs()
}

/* ----------------- Generate CheckSum ----------------- */

tasks.register<GenerateChecksumTask>("generateChecksum") {
    group = "verification"
    description = "Generates MD5 and SHA-1 checksums for specified files."
    filesToChecksum = artifacts
}

/* ----------------- Sign Artifacts ----------------- */

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

/* ----------------- Maven Publish (Meta data) ----------------- */
publishing {
    publications {
        register<MavenPublication>("java") {
            groupId = pluginGroup
            artifactId = pluginName
            version = pluginVersion

            pom {
                name = "${groupId}:${artifactId}"
                description =
                    "ResourceManager is an Android plugin that simplifies accessing Android resources (strings, colors, drawables, etc.) in both Android and non-Android components (e.g., ViewModel) using generated code."
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
                from(components["java"])
            }
        }
    }
}