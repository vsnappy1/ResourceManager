# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Keep lifecycle components for Jetpack Compose
-keep class androidx.lifecycle.** { *; }
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.platform.** { *; }
-keep class androidx.activity.ComponentActivity { *; }

# Preserve CompositionLocals
-keep @androidx.compose.runtime.Composable public class * { *; }

# Retain LocalLifecycleOwner
-keep class * extends androidx.compose.runtime.CompositionLocal { *; }

# Keep all classes named ResourceManager, regardless of their package, and retain the initialize method.
-keepclassmembers class **.ResourceManager {
    public void initialize(android.app.Application);
}
