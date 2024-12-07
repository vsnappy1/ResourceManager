# ResourceManager
ResourceManager is an Android plugin that simplifies accessing Android resources (strings, colors, drawables, etc.) in both Android (e.g., Activity, Fragment, Composable) and non-Android components (e.g., ViewModel) using generated code.

<p>
  <a href="https://plugins.gradle.org/plugin/dev.randos.resourcemanager"><img alt="License" src="https://img.shields.io/maven-metadata/v?label=Gradle%20Plugin%20Portal&metadataUrl=https%3A%2F%2Fplugins.gradle.org%2Fm2%2Fdev%2Frandos%2Fresourcemanager%2Fdev.randos.resourcemanager.gradle.plugin%2Fmaven-metadata.xml"/></a>
  <a href="https://android-arsenal.com/api?level=4"><img alt="API" src="https://img.shields.io/badge/API-4%2B-brightgreen.svg?style=flat"/></a>
  <a href="https://github.com/vsnappy1/resourcemanager/actions"><img alt="Build Status" src="https://github.com/vsnappy1/resourcemanager/workflows/Android%20CI/badge.svg"/></a>
  <a href="https://opensource.org/licenses/Apache-2.0"><img alt="License" src="https://img.shields.io/badge/License-Apache%202.0-blue.svg"/></a>
</p>

## Setup

### Step 1: Add ResourceManager Plugin
Add resourcemanager plugin to your project's root __build.gradle(.kts)__ file.
```kotlin
// If your project uses the plugins block, add the following:
plugins {
    id("com.android.application") version "8.0.1" apply false
    id("dev.randos.resourcemanager") version "0.1.1" apply false
    ....
}

// Alternatively, if your project uses the buildscript block, include this:
buildScripts {
    dependencies {
        classpath "com.android.tools.build:gradle:8.0.1"
        classpath 'dev.randos:resourcemanager:0.1.1'
        ....
    }
```

### Step 2: Apply ResourceManager Plugin
Apply the ResourceManager plugin in your module-level __build.gradle(.kts)__ file.
```kotlin
// If you are using the plugins block, add the following:
plugins {
    id("com.android.application")
    id("dev.randos.resourcemanager")
    ....
}

// Alternatively, if your project uses the apply statement, include this:
apply plugin: 'com.android.application'
apply plugin: 'dev.randos.resourcemanager'
....
```

### Step 3: Initialize ResourceManager
To enable ResourceManager, follow these steps:

1. Build the project to trigger code generation.
2. Initialize ResourceManager in the `onCreate` method of your `Application` class.

```kotlin
class MyApplication: Application() {
    
    override fun onCreate() {
        super.onCreate()
        ResourceManager.initialize(this)
    }
}
```

## Usage
Here’s an example of how `ResourceManager` is typically used in an `Activity` and a `ViewModel`.

```kotlin
class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.titleTextView.setText(ResourceManager.Strings.greetings("Kumar"))
        binding.avatarImageView.setImageDrawable(ResourceManager.Drawables.icAvatar())
    }
}

class MyViewModel : ViewModel() {
    
    fun getData() {
        _title.postValue(ResourceManager.Strings.title())
        _icon.postValue(ResourceManager.Drawables.icDoneButton())
        _color.postValue(ResourceManager.Colors.primaryGreen())
    }
}
```
__Note*__ If your app supports dynamic __locale__, __theme__ or __orientation__ changes at runtime (via code), pass the appropriate context (e.g., an `Activity` context) to the function (i.e `ResourceManager.Colors.primaryGreen(context)`).

## Migration (Beta)
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

## Contributions
Contributions are highly encouraged, whether it’s fixing a bug, suggesting improvements, or adding new features.

Create a branch for your changes, and submit a pull request. Let’s build something amazing together! 🚀
