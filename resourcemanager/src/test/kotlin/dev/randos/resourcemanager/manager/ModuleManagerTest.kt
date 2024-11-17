package dev.randos.resourcemanager.manager

import groovy.test.GroovyTestCase.assertEquals
import io.mockk.unmockkAll
import org.gradle.internal.impldep.org.testng.Assert.assertNull
import org.gradle.internal.impldep.org.testng.Assert.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class ModuleManagerTest {
    private lateinit var moduleManager: ModuleManager

    private lateinit var moduleFile: File

    @Before
    fun setUp() {
        moduleFile = Files.createTempDirectory("app").toFile()
        moduleManager = ModuleManager(moduleFile)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun getNamespace_whenGradleFileExistAndHasNamespace_shouldReturnThatNamespace() {
        // Given
        val namespace = "com.example.myapplication"
        val gradleFileContent =
            """
            android {
                namespace = "$namespace"
                compileSdk = 34
                ..
            }
            """.trimIndent()
        val gradleFile = File(moduleFile, "build.gradle.kts")
        gradleFile.writeText(gradleFileContent)

        // When
        val applicationNamespace = moduleManager.getNamespace()

        // Then
        assertEquals(namespace, applicationNamespace)
    }

    @Test
    fun getNamespace_whenGradleFileExistAndHasNamespaceCommented_shouldReturnOtherNamespace() {
        // Given
        val namespace = "com.example.myapplication"
        val gradleFileContent =
            """
            android {
                namespace = "$namespace"
                // namespace = "dev.randos.resourcemanager"
                compileSdk = 34
                ..
            }
            """.trimIndent()
        val gradleFile = File(moduleFile, "build.gradle.kts")
        gradleFile.writeText(gradleFileContent)

        // When
        val applicationNamespace = moduleManager.getNamespace()

        // Then
        assertEquals(namespace, applicationNamespace)
    }

    @Test
    fun getNamespace_whenGradleFileExistAndHasNamespaceBlockCommented_shouldReturnOtherNamespace() {
        // Given
        val namespace = "com.example.myapplication"
        val gradleFileContent =
            """
            android {
                /*
                namespace = "dev.randos.resourcemanager"
                */
                namespace = "$namespace"
                compileSdk = 34
                ..
            }
            """.trimIndent()
        val gradleFile = File(moduleFile, "build.gradle.kts")
        gradleFile.writeText(gradleFileContent)

        // When
        val applicationNamespace = moduleManager.getNamespace()

        // Then
        assertEquals(namespace, applicationNamespace)
    }

    @Test
    fun getNamespace_whenGradleFileExistButDoesHasNamespace_shouldReturnThatNamespaceFromManifest() {
        // Given
        val namespace = "com.example.myapplication"
        val gradleFileContent =
            """
            android {
                compileSdk = 34
                ..
            }
            """.trimIndent()
        val manifestFileContent =
            """
            <?xml version="1.0" encoding="utf-8"?>
            <manifest 
                xmlns:android="http://schemas.android.com/apk/res/android"
                xmlns:tools="http://schemas.android.com/tools"
                package="$namespace">
            
                <application
                    android:name=".MyApplication"
                    android:allowBackup="true">
                    <activity
                        android:name=".MainActivity"
                        android:exported="true">
                        <intent-filter>
                            <action android:name="android.intent.action.MAIN" />
                            <category android:name="android.intent.category.LAUNCHER" />
                        </intent-filter>
                    </activity>
                </application>
            </manifest>
            """.trimIndent()
        val gradleFile = File(moduleFile, "build.gradle.kts")
        gradleFile.writeText(gradleFileContent)

        val manifestFile = File(moduleFile, "src/main/AndroidManifest.xml")
        manifestFile.parentFile.mkdirs()
        manifestFile.writeText(manifestFileContent)

        // When
        val applicationNamespace = moduleManager.getNamespace()

        // Then
        assertEquals(namespace, applicationNamespace)
    }

    @Test
    fun getNamespace_whenGradleFileDoesNotExist_shouldReturnNamespaceFromManifest() {
        // Given
        val namespace = "com.example.myapplication"
        val manifestFileContent =
            """
            <?xml version="1.0" encoding="utf-8"?>
            <manifest 
                xmlns:android="http://schemas.android.com/apk/res/android"
                xmlns:tools="http://schemas.android.com/tools"
                package="$namespace">
            
                <application
                    android:name=".MyApplication"
                    android:allowBackup="true">
                    <activity
                        android:name=".MainActivity"
                        android:exported="true">
                        <intent-filter>
                            <action android:name="android.intent.action.MAIN" />
                            <category android:name="android.intent.category.LAUNCHER" />
                        </intent-filter>
                    </activity>
                </application>
            </manifest>
            """.trimIndent()

        val manifestFile = File(moduleFile, "src/main/AndroidManifest.xml")
        manifestFile.parentFile.mkdirs()
        manifestFile.writeText(manifestFileContent)

        // When
        val applicationNamespace = moduleManager.getNamespace()

        // Then
        assertEquals(namespace, applicationNamespace)
    }

    @Test
    fun getNamespace_whenGradleFileDoesNotExistAndManifestDoesNotHavePackageAttribute_shouldReturnNull() {
        // Given
        val manifestFileContent =
            """
            <?xml version="1.0" encoding="utf-8"?>
            <manifest 
                xmlns:android="http://schemas.android.com/apk/res/android"
                xmlns:tools="http://schemas.android.com/tools">
            
                <application
                    android:name=".MyApplication"
                    android:allowBackup="true">
                    <activity
                        android:name=".MainActivity"
                        android:exported="true">
                        <intent-filter>
                            <action android:name="android.intent.action.MAIN" />
                            <category android:name="android.intent.category.LAUNCHER" />
                        </intent-filter>
                    </activity>
                </application>
            </manifest>
            """.trimIndent()

        val manifestFile = File(moduleFile, "src/main/AndroidManifest.xml")
        manifestFile.parentFile.mkdirs()
        manifestFile.writeText(manifestFileContent)

        // When
        val applicationNamespace = moduleManager.getNamespace()

        // Then
        assertNull(applicationNamespace)
    }

    @Test
    fun getNamespace_whenGradleAndAndroidManifestFilesDoNotExist_shouldReturnNull() {
        // When
        val applicationNamespace = moduleManager.getNamespace()

        // Then
        assertNull(applicationNamespace)
    }

    @Test
    fun getModuleDependencies_whenProjectHasNModuleDependency_shouldReturnNModuleNames() {
        // Given
        val gradleFileContent =
            """
            plugin {
            
            }
            
            android {
            
            }
            
            dependencies {
                // ...
                implementation(project(":mylibrary1"))
                // ...
                implementation(project(":mylibrary2"))
                implementation(project(":mylibrary3"))
            }
            """.trimIndent()
        val gradleFile = File(moduleFile, "build.gradle.kts")
        gradleFile.writeText(gradleFileContent)

        // When
        val dependencies = moduleManager.getModuleDependencies()

        // Then
        assertEquals("mylibrary1", dependencies[0])
        assertEquals("mylibrary2", dependencies[1])
        assertEquals("mylibrary3", dependencies[2])
    }

    @Test
    fun getModuleDependencies_whenModuleDependencyCommented_shouldNotConsiderIt() {
        // Given
        val gradleFileContent =
            """
            ..
            dependencies {
                // ...
                implementation(project(":mylibrary1"))
                // ...
                implementation(project(":mylibrary2"))
                // implementation(project(":mylibrary3"))
            }
            """.trimIndent()
        val gradleFile = File(moduleFile, "build.gradle.kts")
        gradleFile.writeText(gradleFileContent)

        // When
        val dependencies = moduleManager.getModuleDependencies()

        // Then
        assertEquals("mylibrary1", dependencies[0])
        assertEquals("mylibrary2", dependencies[1])
        assertEquals(2, dependencies.size)
    }

    @Test
    fun getModuleDependencies_whenModuleDependencyBlockCommented_shouldNotConsiderIt() {
        // Given
        val gradleFileContent =
            """
            ..
            dependencies {
                // ...
                implementation(project(":mylibrary1"))
                // ...
                /*
                implementation(project(":mylibrary2"))
                implementation(project(":mylibrary3"))
                */
                implementation(project(":mylibrary4"))
            }
            """.trimIndent()
        val gradleFile = File(moduleFile, "build.gradle.kts")
        gradleFile.writeText(gradleFileContent)

        // When
        val dependencies = moduleManager.getModuleDependencies()

        // Then
        assertEquals("mylibrary1", dependencies[0])
        assertEquals("mylibrary4", dependencies[1])
        assertEquals(2, dependencies.size)
    }

    @Test
    fun getModuleDependencies_whenBuildGradleFileDoesNotExist_shouldReturnEmptyList() {
        // When
        val dependencies = moduleManager.getModuleDependencies()

        // Then
        assertTrue(dependencies.isEmpty())
    }

    @Test
    fun getBuildGradleFile_whenBuildGradleFilePresent_shouldReturnIt() {
        // Given
        val gradleFile = File(moduleFile, "build.gradle")
        gradleFile.writeText("")

        // When
        val file = moduleManager.getBuildGradleFile()

        // Then
        assertEquals(gradleFile, file)
    }

    @Test
    fun getBuildGradleFile_whenBuildGradleKtsFilePresent_shouldReturnIt() {
        // Given
        val gradleFile = File(moduleFile, "build.gradle.kts")
        gradleFile.writeText("")

        // When
        val file = moduleManager.getBuildGradleFile()

        // Then
        assertEquals(gradleFile, file)
    }
}