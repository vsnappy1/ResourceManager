# ResourceManager
ResourceManager is an Android plugin that simplifies accessing Android resources (strings, colors, drawables, etc.) in both Android and non-Android components (e.g., ViewModel) using generated code.

![Platform Support](https://img.shields.io/badge/platform-Android-brightgreen.svg)

## Setup

### Step 1: Add ResourceManager Plugin
Add the resourcemanager plugin to your project's (root) __build.gradle__.
```kotlin
plugins {
    id("com.android.application") version "8.0.1" apply false
    id("org.jetbrains.kotlin.android") version "1.8.21" apply false
    ...
    id("dev.randos.resourcemanager") version "0.0.4" apply false    // Add this line for ResourceManager support
}
```
Or
```kotlin
buildScripts {
    ...
    dependencies {
        ...
        classpath 'dev.randos:resourcemanager:0.0.4'    // Add this line for ResourceManager support
    }
}
```

### Step 2: Apply ResourceManager Plugin
Apply ResourceManager plugin in your module-level __build.gradle__.
```kotlin
plugins {
    id("com.android.application")
    ...
    id("dev.randos.resourcemanager")    // Apply ResourceManager
}
```
Or
```kotlin
apply plugin: 'com.android.application'
...
apply plugin: 'dev.randos.resourcemanager'    // Apply ResourceManager
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

## Migration
To streamline the transition to ResourceManager, plugin comes with a Gradle task to automate key aspects of the migration process. Please follow these steps carefully.

#### Important Warning ⚠️

Before starting the migration process, it is highly recommend to:
1.	__Use Version Control:__ Ensure your project is tracked with a version control system like Git.
2.	__Create a Backup:__ Either create a new branch or make a copy of your project to prevent unintended changes or data loss during migration.

#### Running the Migration Task

To perform the migration, execute the following Gradle command:

```bash
./gradlew migrateToResourceManager -PconfirmMigration=true
```
__Note:*__ The `-PconfirmMigration=true` parameter confirms that you understand the potential impacts of the migration and agree to proceed.

#### Post-Migration Checklist
1. Review the generated migration report, located at *.../build/reports/migration/resourcemanager-migration-report.html*.
2. Verify your project builds successfully without warnings or errors.
3. Ensure that all resources are correctly migrated and that the application behaves as expected.