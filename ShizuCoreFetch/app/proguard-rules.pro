# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# ==========================================
# قواعد الحماية الخاصة بتطبيق Shizu CoreFetch
# ==========================================

# 1. حماية كلاسات البيانات الخاصة بك (مهم جداً للـ API والكاش لتجنب العمى البرمجي)
-keep class xyz.siwane.shizucorefetch.AppModel { *; }
-keep class xyz.siwane.shizucorefetch.LocalApkModel { *; }
-keep class xyz.siwane.shizucorefetch.ReleaseModel { *; }
-keep class xyz.siwane.shizucorefetch.AssetModel { *; }
-keep class xyz.siwane.shizucorefetch.CommentModel { *; }
# (قاعدة ذكية): حماية أي كلاس بيانات ينتهي اسمه بكلمة Model داخل حزمتك بشكل تلقائي
-keep class xyz.siwane.shizucorefetch.**Model { *; }

# 2. حماية مكتبة جلب البيانات (Retrofit & OkHttp)
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
# السطر التالي مهم جداً للحفاظ على هيكلة القوائم (Generics)
-keepattributes Signature, Exceptions, *Annotation*, InnerClasses, EnclosingMethod
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# 3. حماية مكتبة فك تشفير البيانات (Gson) وقراءة القوائم من الكاش (TypeToken)
-keep class sun.misc.Unsafe { *; }
-keep class com.google.gson.** { *; }
-keep class com.google.gson.stream.** { *; }
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# 4. حماية مكتبة التحميل والصور (Coil & Okio)
-dontwarn okio.**
-keep class coil.** { *; }

# 5. حماية محرك التطبيق الأساسي (Shizuku)
-keep class rikka.shizuku.** { *; }

# 6. حماية مكتبة الماركداون (Markwon) لضمان عرض التعليقات ووصف التطبيقات بشكل سليم
-keep class io.noties.markwon.** { *; }
