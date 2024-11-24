# ResourceManager
ResourceManager is an Android plugin that simplifies accessing Android resources (strings, colors, drawables, etc.) in both Android and non-Android components (e.g., ViewModel) using generated code.

[![Gradle Plugin Portal](https://img.shields.io/maven-metadata/v?label=Gradle%20Plugin%20Portal&metadataUrl=https%3A%2F%2Fplugins.gradle.org%2Fm2%2Fdev%2Frandos%2Fresourcemanager%2Fdev.randos.resourcemanager.gradle.plugin%2Fmaven-metadata.xml)](https://plugins.gradle.org/plugin/dev.randos.resourcemanager)
![Platform Support](https://img.shields.io/badge/platform-Android-brightgreen.svg)

## Setup

### Step 1: Add ResourceManager Plugin
Add resourcemanager plugin to your project's root __build.gradle(.kts)__ file.
- If your project uses the plugins block.
```kotlin
plugins {
    id("com.android.application") version "8.0.1" apply false
    id("org.jetbrains.kotlin.android") version "1.8.21" apply false
    ...
    id("dev.randos.resourcemanager") version "0.0.9" apply false    // Add ResourceManager plugin
}
```
- If your project uses the buildscript block.
```kotlin
buildScripts {
    ...
    dependencies {
        classpath "com.android.tools.build:gradle:8.0.1"
        classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:1.8.21"
        ...
        classpath 'dev.randos:resourcemanager:0.0.9'    // Add ResourceManager plugin
    }
}
```

### Step 2: Apply ResourceManager Plugin
Apply the ResourceManager plugin in your module-level __build.gradle(.kts)__ file.
- If you are using the plugins block, add the following:
```kotlin
plugins {
    id("com.android.application")
    ...
    id("dev.randos.resourcemanager")    // Apply ResourceManager plugin
}
```
- If your project uses the apply statement, include this:
```kotlin
apply plugin: 'com.android.application'
...
apply plugin: 'dev.randos.resourcemanager'    // Apply ResourceManager plugin
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