# Macsense-2 release ProGuard/R8 rules.
#
# Guiding principle: keep only what reflection/serialization/JNI/Compose actually need at
# runtime, and let R8 shrink/obfuscate everything else. The previous blanket
# `-keep class com.macsense.ai.** { * }` defeated shrinking and obfuscation for the entire
# app; it has been replaced with targeted rules below.

# ---------------------------------------------------------------------------
# General Android / Kotlin
# ---------------------------------------------------------------------------
-dontwarn kotlin.**
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes SourceFile,LineNumberTable

# Kotlin coroutines internals that R8 sometimes needs help with.
-keepclassmembernames class kotlinx.coroutines.internal.MainDispatcherFactory { *; }
-keepclassmembernames class kotlinx.coroutines.android.AndroidExceptionPreHandler { *; }
-dontwarn kotlinx.coroutines.**

# ---------------------------------------------------------------------------
# kotlinx.serialization
# Required so @Serializable data classes used for Gemini/Ari request+response DTOs
# (GenerateContentRequest, Content, Part, AriCommand, etc.) keep the generated
# serializer companions and can still be reflected on by the JSON converter.
# ---------------------------------------------------------------------------
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <1>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers @kotlinx.serialization.Serializable class com.macsense.ai.** {
    *** Companion;
}
-keep,includedescriptorclasses class com.macsense.ai.**$$serializer { *; }
-keepclassmembers class com.macsense.ai.** {
    *** Companion;
}
-keepclasseswithmembers class com.macsense.ai.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ---------------------------------------------------------------------------
# Retrofit / OkHttp
# ---------------------------------------------------------------------------
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keepattributes Exceptions
-keep class retrofit2.Response
-keepclasseswithmembers interface * {
    @retrofit2.http.* <methods>;
}
# Retrofit's service interfaces are created via dynamic proxy; keep the interface + its
# annotations so the HTTP method/path/header metadata survives.
-keep,allowobfuscation interface com.macsense.ai.api.GeminiApiService

# ---------------------------------------------------------------------------
# Room
# ---------------------------------------------------------------------------
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class *
-keepclassmembers class * extends androidx.room.RoomDatabase { *; }
-dontwarn androidx.room.paging.**

# ---------------------------------------------------------------------------
# Jetpack Compose
# AGP's default Compose rules cover most cases; these are extra keeps for
# reflection-sensitive Compose internals seen in minified release builds.
# ---------------------------------------------------------------------------
-keep class androidx.compose.runtime.** { *; }
-dontwarn androidx.compose.**

# ---------------------------------------------------------------------------
# Native / JNI (native playback + DSP engine)
# Keep classes with native methods so JNI bindings resolve correctly, and keep
# any classes referenced from C++ via JNI by fully-qualified name.
# ---------------------------------------------------------------------------
-keepclasseswithmembers,includedescriptorclasses class * {
    native <methods>;
}
-keep class com.macsense.ai.audio.NativePlaybackEngine { *; }
-keep class com.macsense.ai.audio.LiveMeterEngine { *; }

# ---------------------------------------------------------------------------
# App-specific data models that cross process/serialization boundaries
# (Ari commands, DAW section/state models). Field names matter because they
# are used as JSON keys.
# ---------------------------------------------------------------------------
-keepclassmembers class com.macsense.ai.api.AriCommand { *; }
-keepclassmembers class com.macsense.ai.api.GenerateContentRequest { *; }
-keepclassmembers class com.macsense.ai.api.GenerateContentResponse { *; }
-keepclassmembers class com.macsense.ai.api.Content { *; }
-keepclassmembers class com.macsense.ai.api.Part { *; }
-keepclassmembers class com.macsense.ai.api.GenerationConfig { *; }
-keepclassmembers class com.macsense.ai.api.Candidate { *; }
