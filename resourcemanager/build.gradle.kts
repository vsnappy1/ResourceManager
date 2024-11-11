plugins {
    `kotlin-dsl`
    java
    id("com.gradle.plugin-publish") version "1.3.0"
}

dependencies {
    implementation(gradleApi())
    implementation(localGroovy())
}

group = "dev.randos"
version = "0.0.1"

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