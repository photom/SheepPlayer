# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Security: Obfuscate classes and methods to prevent reverse engineering
-dontskipnonpubliclibraryclassmembers
-keepattributes *Annotation*

# Keep Google API classes
-keep class com.google.api.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# Keep data classes used in serialization
-keep class com.hitsuji.sheepplayer2.Artist { *; }
-keep class com.hitsuji.sheepplayer2.Album { *; }
-keep class com.hitsuji.sheepplayer2.Track { *; }
-keep class com.hitsuji.sheepplayer2.CachedMetadata { *; }

# Keep interfaces and their implementations
-keep interface com.hitsuji.sheepplayer2.interfaces.** { *; }
-keep class com.hitsuji.sheepplayer2.interfaces.**$* { *; }

# Security: Remove debug information in release builds
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Keep line numbers for crash reports but hide source file names
-keepattributes LineNumberTable
-renamesourcefileattribute SourceFile

# Security: Optimize and remove unused code
-allowaccessmodification
-mergeinterfacesaggressively

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep enum classes
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}