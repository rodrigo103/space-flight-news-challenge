# Keep Retrofit interfaces
-keep,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**
-keepattributes Signature
-keepattributes Exceptions

# Keep kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.example.myandroidapp.**$$serializer { *; }
-keepclassmembers class com.example.myandroidapp.** {
    *** Companion;
}
-keepclasseswithmembers class com.example.myandroidapp.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# Keep Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Keep Coil
-keep class coil3.** { *; }
-dontwarn coil3.**

# Keep Lottie
-keep class com.airbnb.lottie.** { *; }
-dontwarn com.airbnb.lottie.**

# Keep OkHttp
-dontwarn okhttp3.internal.platform.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# Keep Timber
-keep class timber.log.** { *; }

# Keep Firebase
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Keep Paging 3
-keep class androidx.paging.** { *; }

# Keep Compose
-dontwarn androidx.compose.**
-keep class androidx.compose.** { *; }

# Keep Navigation
-keep class androidx.navigation.** { *; }

# Keep AndroidX lifecycle
-keep class androidx.lifecycle.** { *; }

# Keep domain models with @Serializable
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keep,includedescriptorclasses class com.example.myandroidapp.domain.model.** {
    <fields>;
}
