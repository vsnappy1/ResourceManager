# ResourceManager
ResourceManager is an Android plugin that simplifies accessing Android resources (strings, colors, drawables, etc.) in both Android and non-Android components (e.g., ViewModel) using generated code.

![Platform Support](https://img.shields.io/badge/platform-Android-brightgreen.svg)

## Setup

### Step 1: Set Up the ResourceManager Plugin (project)
Add the resourcemanager plugin to your project's (root) __build.gradle__.
```kotlin
plugins {
    id("com.android.application") version "8.0.1" apply false
    id("org.jetbrains.kotlin.android") version "1.8.21" apply false
    ...
    id(dev.randos.resourcemanager") version "0.0.1" apply false    // Add this line for ResourceManager support
}
```

### Step 2: Set Up the ResourceManager Plugin (module)
Add the ResourceManager plugin in your module-level __build.gradle__.
```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    ...
    id("dev.randos.resourcemanager")  // Apply ResourceManager
}
```

### Step 3: Initialize ResourceManager
To enable ResourceManager, follow these steps:

1. Build the project to trigger code generation.
2. Initialize ResourceManager in the `onCreate` method of your `Application` class.

```kotlin
class MyApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        ...
        ResourceManager.initialize(this)    // Initializes ResourceManager
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
        _icon.postValue(ResourceManager.Drawables.icDoneButton())
        _color.postValue(ResourceManager.Colors.primaryGreen())
    }
}
```
