import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

plugins {
    `kotlin-dsl`
    kotlin("jvm")
    id("java-gradle-plugin")
    id("com.gradle.plugin-publish") version "1.3.0"
    id("maven-publish")
    id("org.jlleitschuh.gradle.ktlint")
    jacoco
}
apply(from = "../gradle/jacoco.gradle.kts")

val agp: String by project
val pluginGroup = "dev.randos"
val pluginVersion = "0.0.2"
val pluginName = "resourcemanager"

dependencies {
    implementation(gradleApi())
    implementation(localGroovy())
    compileOnly("com.android.tools.build:gradle:$agp")
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(gradleTestKit())
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

ktlint {
    reporters {
        reporter(ReporterType.HTML)
    }
}

publishing {
    repositories {
        mavenLocal()
    }
}