import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.archivesName

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
    description = "Generates Javadoc JAR, sources JAR, and POM XML file."
    dependsOn("build", "generateJavadocJar", "generateSourcesJar", "generatePomXml")
    doLast {
        println("All artifacts (Javadoc JAR, sources JAR, and POM) have been generated successfully.")
    }
}

tasks.named("build") {
    val publication = (publishing.publications.getByName("java") as MavenPublication)
    archivesName = "${publication.artifactId}-${publication.version}"
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