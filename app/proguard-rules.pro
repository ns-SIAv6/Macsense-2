-keep class com.macsense.ai.** { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase { *; }
-keep class * extends androidx.room.RoomDatabase
-keep class * implements androidx.room.RoomDatabase
-keepclassmembers class * implements androidx.room.RoomDatabase { *; }

# --- Phase 1 hardening: AriCommand + Retrofit/kotlinx.serialization models ---
# kotlinx.serialization generates synthetic $serializer companions and uses reflection-adjacent
# lookups for serializer resolution; without explicit keep rules, R8 can strip or rename these in
# release builds, silently breaking the AriCommand JSON parsing path (highest-risk, user-facing
# AI-command logic per PRODUCTION_GAP_ANALYSIS.md item E).
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# Keep the generated *$Companion serializer() accessor for every @Serializable class.
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}
-if @kotlinx.serialization.Serializable class ** {
    static **$Companion Companion;
}
-keepclassmembers class <1>$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}

# Explicitly keep AriCommand and its serializer machinery even if the wildcard package rule above
# is later narrowed — this is the single highest-risk data class in the app (Ari's parsed AI
# command payload: update_bpm, update_lyrics, reorder_sections, apply_preset, update_effects,
# breed_sounds, resurrect_sound).
-keep,includedescriptorclasses class com.macsense.ai.api.AriCommand { *; }
-keep,includedescriptorclasses class com.macsense.ai.api.AriCommand$* { *; }
-keep,includedescriptorclasses class com.macsense.ai.api.**$$serializer { *; }

# Retrofit + OkHttp: keep service interfaces and their method signatures/generics so Retrofit's
# runtime proxy generation and Retrofit-kotlinx-serialization converter continue to resolve
# response types correctly in a minified build.
-keepattributes Signature, Exceptions, RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keep,allowobfuscation interface com.macsense.ai.api.**
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowoptimization,allowshrinking,allowobfuscation interface <1>

# Suppress known-safe warnings from Retrofit/OkHttp optional platform reflection so release builds
# don't fail R8 on notes that aren't actionable for a minSdk-26 Android target.
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn kotlinx.serialization.**
