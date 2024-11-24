import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    `maven-publish`
    alias(libs.plugins.gradle.plugin.publish)
    alias(libs.plugins.jetbrains.kotlin.jvm)
    alias(libs.plugins.ktlint)
    jacoco
}
apply(from = "../gradle/jacoco.gradle.kts")

val agp: String by project
val kotlinVersion: String by project
val pluginGroup = "dev.randos"
val pluginVersion = "0.0.9"
val pluginName = "resourcemanager"

dependencies {
    compileOnly(gradleApi())
    compileOnly(localGroovy())
    compileOnly(libs.android.tools)
    compileOnly(libs.kotlin.gradle.plugin)
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(gradleTestKit())
}

group = pluginGroup
version = pluginVersion

gradlePlugin {
    website.set("https://github.com/vsnappy1/resourcemanager")
    vcsUrl.set("https://github.com/vsnappy1/resourcemanager")
    plugins {
        create("resourcemanager") {
            id = "dev.randos.resourcemanager"
            implementationClass = "ResourceManagerPlugin"
            displayName = "Resource Manager"
            description = "ResourceManager is an Android plugin that simplifies accessing Android resources (strings, colors, drawables, etc.) in both Android and non-Android components (e.g., ViewModel) using generated code."
            tags.set(listOf("Android", "Android Resources", "Code Generation"))
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