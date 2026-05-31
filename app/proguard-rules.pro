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

# Keep annotations and generic signatures for Gson/Retrofit reflection
-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*, RuntimeVisibleAnnotations, RuntimeInvisibleAnnotations, RuntimeVisibleParameterAnnotations, RuntimeInvisibleParameterAnnotations

# Keep Gson serialization/deserialization classes
-keep class com.example.moneypad.data.model.** { *; }

# Keep Retrofit interfaces and their annotated parameters/methods
-keep class com.example.moneypad.data.remote.** { *; }
-keepclassmembers class * {
    @retrofit2.http.* <methods>;
}

# Keep OkHttp components
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**

# Keep Retrofit classes
-keep class retrofit2.** { *; }
-dontwarn retrofit2.**

# Keep Kotlin coroutine classes and continuation signatures
-keep class kotlin.coroutines.Continuation { *; }
-keep class kotlin.coroutines.jvm.internal.DebugMetadata { *; }

# Keep Room database and its generated implementation class
-keep class * extends androidx.room.RoomDatabase { *; }
-dontwarn androidx.room.paging.**

# Keep DAO classes and interfaces
-keep interface com.example.moneypad.data.dao.** { *; }
-keep class * implements com.example.moneypad.data.dao.** { *; }

# Keep Repository and Retrofit Client
-keep class com.example.moneypad.data.MoneyPadRepository { *; }
-keep class com.example.moneypad.data.remote.RetrofitClient { *; }

# Keep ViewModels and ViewModelFactory (used for reflection creation)
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keep class com.example.moneypad.ui.ViewModelFactory { *; }

# Keep AdManager and Google Play Services Ads SDK
-keep class com.example.moneypad.ads.AdManager { *; }
-keep class com.google.android.gms.ads.** { *; }
-dontwarn com.google.android.gms.ads.**
