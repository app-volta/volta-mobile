# ==============================================================================
# VOLTA — Regras de Ofuscação e Minificação (R8 / ProGuard)
# Fase 27 — Hardening de Segurança para Release
# ==============================================================================

# 1. Preservar stack trace legível e anotações essenciais
-keepattributes SourceFile,LineNumberTable
-keepattributes Signature,InnerClasses,EnclosingMethod
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations

# 2. GSON — Preservar campos serializados e modelos de dados
-keepattributes *Annotation*
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
    @com.google.gson.annotations.Expose <fields>;
}
-keep class com.google.gson.** { *; }
-keep class com.aula.volta.data.model.** { *; }
-keep class com.aula.volta.data.sync.** { *; }

# 3. Retrofit 2 & OkHttp
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**

# 4. Navigation Component e Fragments instanciados por reflexão
-keep class androidx.navigation.** { *; }
-keep class androidx.fragment.app.Fragment { *; }
-keep public class * extends androidx.fragment.app.Fragment {
    public <init>();
}

# 5. AndroidX Material Design
-keep class com.google.android.material.** { *; }
-dontwarn com.google.android.material.**

# 6. Remoção de logs sensíveis em builds de Release
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
}