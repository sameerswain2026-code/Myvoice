# Myvoice R8 rules

# --- kotlinx.serialization ---
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.myvoice.app.**$$serializer { *; }
-keepclassmembers class com.myvoice.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.myvoice.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# --- OkHttp / Okio ---
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# --- Coroutines debug metadata ---
-dontwarn kotlinx.coroutines.debug.**
