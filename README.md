# ResourceManager
ResourceManager is an Android plugin that simplifies accessing Android resources (strings, colors, drawables, etc.) in both Android and non-Android components (e.g., ViewModel) using generated code.

## Setup

### Step 1: Add JitPack Repository
Add the JitPack repository to your root __build.gradle__ (or __settings.gradle__ for newer projects).
```kotlin
repositories {
    google()
    mavenCentral()
    ...
    maven("https://jitpack.io")  <--
}
```

### Step 2: Apply the KSP Plugin
Include the KSP plugin in your root __build.gradle__.
```kotlin
plugins {
    id("com.android.application") version '8.0.1' apply false
    id("org.jetbrains.kotlin.android") version '1.8.21' apply false
    ...
    id("com.google.devtools.ksp") version "1.9.0-1.0.12"  <--
}
```

### Step 3: Add Dependencies
Add the ResourceManager dependencies in your module-level __build.gradle__.
```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    ...
    id("com.google.devtools.ksp")  <--
}
...
dependencies {
    ...
    implementation("com.github.vsnappy1:ResourceManager:1.0.0")  <--
    ksp("com.github.vsnappy1:ResourceManager:1.0.0")  <--
}
```

### Step 4: Initialize ResourceManager
Annotate your application class, initialize ResourceManager and build the project to enable KSP to generate the necessary code.

```kotlin
@InstallResourceManager  <--
class MyApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        ...
        ResourceManager.initialize(this)  <--
    }
}
```

## Usage
Here’s an example of how to use ResourceManager in a ViewModel

```kotlin
class MyViewModel : ViewModel() {
    ...
    fun getData() {
        _title.postValue(ResourceManager.Strings.title())
    }
}
```

## Note
If your namespace differs from the application file location, specify the namespace in the annotation.

```kotlin
@InstallResourceManager(namespace = "<name_space>")
class MyApplication: Application() {
...
}
```

By default, this library uses KSP version __1.9.0-1.0.12__. Make sure to use the same version for project and the library. You can set the KSP version in your project-level __build.gradle__ file, and the library will use this version.

```kotlin
...
ext {
    set("kspVersion", <ksp_version>)
}
```
