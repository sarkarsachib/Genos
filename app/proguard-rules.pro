# Retrofit
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions
-keepattributes *Annotation*
-keep class sun.misc.Unsafe { *; }
-keep class com.google.gson.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

# OkHttp
-dontwarn okhttp3.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Google ML Kit
-keep class com.google.mlkit.** { *; }
-keepattributes *Annotation*

# Tesseract
-keep class com.rmtheis.tesstwo.** { *; }

# Timber
-keep class timber.log.Timber { *; }
-keepclassmembers class timber.log.Timber { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
-keep @androidx.room.Entity class *
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <methods>;
}

# Shizuku
-keep class dev.rikka.shizuku.** { *; }

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Kotlin Coroutines
-keepnames kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames kotlinx.coroutines.CoroutineExceptionHandler {}

# GENOS specific
-keep class ai.genos.core.** { *; }
-keepclassmembers class ai.genos.core.** { *; }
-keepattributes *Annotation*
-keep class ai.genos.core.ai.models.** { *; }
-keepclassmembers class ai.genos.core.ai.models.** { *; }

# Accessibility Service
-keep class ai.genos.core.accessibility.GenosAccessibilityService { *; }

# View Binding
-keep public class * extends androidx.viewbinding.ViewBinding {
    public static *** inflate(android.view.LayoutInflater);
    public static *** inflate(android.view.LayoutInflater, android.view.ViewGroup, boolean);
}
