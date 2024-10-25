plugins {
    id("java-library")
    kotlin("jvm") version "1.9.0"
    id("com.google.devtools.ksp") version "1.9.0-1.0.12"
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation("com.google.devtools.ksp:symbol-processing-api:1.9.0-1.0.12")
}