# ResourceManager
ResourceManager is an Android plugin that simplifies accessing Android resources (strings, colors, drawables, etc.) in both Android and non-Android components (e.g., ViewModel) using generated code. It supports both Kotlin and Java projects.

[![Maven Central](https://img.shields.io/maven-central/v/dev.randos/resourcemanager-runtime.svg)](https://central.sonatype.com/artifact/dev.randos/resourcemanager-runtime)
![Platform Support](https://img.shields.io/badge/platform-Android-brightgreen.svg)

## Setup

### Step 1: Set Up the KSP Plugin
Add the KSP plugin to your project's (root) __build.gradle__.
```kotlin
plugins {
    id("com.android.application") version "8.0.1" apply false
    id("org.jetbrains.kotlin.android") version "1.8.21" apply false
    ...
    id("com.google.devtools.ksp") version "1.9.0-1.0.12"    // Add this line for KSP support
}

ext {
    set("kspVersion", "1.9.0-1.0.12")    // Define kspVersion to be used by ResourceManager
}
```
__Note:__ Ensure the KSP version matches your Kotlin version to avoid compatibility issues.

### Step 2: Add Dependencies
Add the ResourceManager dependencies in your module-level __build.gradle__.
```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    ...
    id("com.google.devtools.ksp")  // Apply KSP
}
...
dependencies {
    ...
    implementation("dev.randos:resourcemanager-runtime:0.0.2")    // Runtime dependency
    ksp("dev.randos:resourcemanager-compiler:0.0.2")    // Compiler dependency for KSP
}
```

### Step 3: Initialize ResourceManager
To enable ResourceManager, follow these steps:

1. Annotate your `Application` class with `@InstallResourceManager`.
2. Build the project to trigger KSP code generation.
3. Initialize ResourceManager in the `onCreate` method of your `Application` class.

```kotlin
@InstallResourceManager    // Annotation to set up ResourceManager
class MyApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        ...
        ResourceManager.initialize(this)    // Initializes ResourceManager
    }
}
```
__Note:__ 
If your `Application` class is located in the root package (e.g., `com.example.yourapp`), you don’t need to specify a namespace. However, if your Application class is in a sub-package (e.g., `com.example.yourapp.app`), you must specify the namespace in the annotation to correctly reference the resources.
```kotlin
@InstallResourceManager(namespace = "com.example.yourapp")  // Specify the namespace if Application is in a sub-package
    class MyApplication: Application() {
        ...
}
```

## Usage
Here’s an example of how to use ResourceManager in a ViewModel

```kotlin
class MyViewModel : ViewModel() {
    ...
    fun getData() {
        _title.postValue(ResourceManager.Strings.title())
        _icon.postValue(ResourceManager.Drawables.icDoneButton())
        _color.postValue(ResourceManager.Colors.primaryGreen())
    }
}
```

__Note:__
To ensure that ResourceManager works correctly with __*ProGuard*__ or __*R8*__, add the following rule to your `proguard-rules.pro` file:
```python
# Keep all classes named ResourceManager, regardless of their package, and retain the initialize method.
-keepclassmembers class **.ResourceManager {
    public void initialize(android.app.Application);
}
```

