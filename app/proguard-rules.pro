# Please add these rules to your existing keep rules in order to suppress warnings.
# This is generated automatically by the Android Gradle plugin.
-dontwarn org.bouncycastle.jsse.BCSSLParameters
-dontwarn org.bouncycastle.jsse.BCSSLSocket
-dontwarn org.bouncycastle.jsse.provider.BouncyCastleJsseProvider
-dontwarn org.conscrypt.Conscrypt$Version
-dontwarn org.conscrypt.Conscrypt
-dontwarn org.conscrypt.ConscryptHostnameVerifier
-dontwarn org.openjsse.javax.net.ssl.SSLParameters
-dontwarn org.openjsse.javax.net.ssl.SSLSocket
-dontwarn org.openjsse.net.ssl.OpenJSSE
-dontwarn com.daimajia.easing.Glider
-dontwarn com.daimajia.easing.Skill

# Retrofit rules
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

-keepattributes *Annotation*
-keep class androidx.room.** { *; }
-keep @androidx.room.Entity class * { *; }

-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response

# Kotlin Coroutine support
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# OkHttp rules
-keepattributes Signature
-keepattributes *Annotation*
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# Gson specific rules
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers enum * { *; }

# Explicitly preserve all serialization members
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Keep TypeToken and its generic signatures
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# Keep TypeToken and generic type information
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken

# Ensure ProGuard does not strip data models used in API responses
-keep class com.cbi.mobile_plantation.data.model.** { *; }

# Keep constructors for serialization
-keepclassmembers,allowobfuscation class com.cbi.mobile_plantation.data.model.** {
    public <init>();
}

# Keep Room database entities
-keepclassmembers class * {
    @androidx.room.Entity <fields>;
}

# Keep DAO methods
-keepclassmembers class * {
    @androidx.room.Dao <methods>;
}

# Keep all fields in Room models
-keepclassmembers class com.yourpackage.models.** {
    *;
}

# Keep the entire DatasetViewModel
-keep class com.cbi.mobile_plantation.ui.viewModel.DatasetViewModel { *; }

# Keep specific parsing method
-keepclassmembers class com.cbi.mobile_plantation.ui.viewModel.DatasetViewModel {
    private java.util.List parseStructuredJsonToList(java.lang.String, java.lang.Class);
}

# Keep all your models
-keep class com.cbi.mobile_plantation.data.model.** { *; }
-keep class com.cbi.mobile_plantation.domain.model.** { *; }
-keep class com.cbi.mobile_plantation.ui.model.** { *; }

# Keep Gson stuff
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Keep TypeToken stuff
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# Keep all constructors in your models
-keepclassmembers class com.cbi.mobile_plantation.** {
    <init>();
}

# Keep all fields in your models
-keepclassmembers class com.cbi.mobile_plantation.** {
    <fields>;
}

# Debug info for testing (remove in final release)
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep all Room-related classes
-keep class androidx.room.** { *; }
-keep class androidx.sqlite.** { *; }

# Keep the GZIP related classes
-keep class java.io.ByteArrayOutputStream { *; }
-keep class java.util.zip.GZIPInputStream { *; }
-keep class java.util.zip.GZIPOutputStream { *; }

# ===== ADD THESE RULES FOR SEALED CLASSES =====

# Keep all sealed classes and their subclasses
-keep class * extends kotlin.** { *; }
-keepclassmembers class * extends kotlin.** { *; }

# Keep your specific sealed class
-keep class com.cbi.mobile_plantation.ui.viewModel.InspectionViewModel$SaveDataInspectionDetailsState** { *; }
-keep class com.cbi.mobile_plantation.ui.viewModel.InspectionViewModel$SaveDataInspectionState** { *; }

# Keep all ViewModels and their nested classes
-keep class com.cbi.mobile_plantation.ui.viewModel.** { *; }
-keepclassmembers class com.cbi.mobile_plantation.ui.viewModel.** { *; }

# Keep sealed classes specifically
-keep class * extends kotlin.* { *; }
-keepclassmembers class * {
    synthetic <methods>;
}

# Keep Kotlin metadata for sealed classes
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeInvisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes RuntimeInvisibleParameterAnnotations
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# Keep Kotlin When mappings (crucial for sealed classes)
-keepclassmembers class **$WhenMappings {
    <fields>;
}

# Keep enum methods
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ===== ROOM DATABASE FIXES =====

# More specific Room rules (replace your existing Room rules with these)
-keep class androidx.room.** { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keep @androidx.room.Database class * { *; }

# Keep your specific Room models
-keep class com.cbi.mobile_plantation.data.local.entity.** { *; }
-keep class com.cbi.mobile_plantation.data.model.InspectionDetailModel { *; }

# Keep Room generated classes
-keep class com.cbi.mobile_plantation.data.local.dao.**_Impl { *; }
-keep class com.cbi.mobile_plantation.data.local.database.**_Impl { *; }

# ===== KOTLIN COROUTINES (Enhanced) =====
-keep class kotlinx.coroutines.** { *; }
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
