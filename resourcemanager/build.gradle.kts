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
val kspVersion: String = if(project.findProperty("kspVersion") != null) project.findProperty("kspVersion").toString() else "1.9.0-1.0.12"

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

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("java"){
                from(components["java"])
                groupId = "dev.randos"
                artifactId = "resourcemanager"
                version = "1.0.0"
            }
        }
    }
}