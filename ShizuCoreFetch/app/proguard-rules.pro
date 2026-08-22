# ==========================================
# قواعد الحماية الخاصة بتطبيق Shizu CoreFetch
# ==========================================

# 1. حماية كلاسات البيانات الفعلية في التطبيق
-keep class xyz.siwane.shizucorefetch.data.** { *; }
-keep class xyz.siwane.shizucorefetch.network.** { *; }
-keep class xyz.siwane.shizucorefetch.ui.screens.DummyApp { *; }

# 2. حماية مكتبة جلب البيانات (Retrofit & OkHttp)
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature, Exceptions, *Annotation*, InnerClasses, EnclosingMethod
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# 3. حماية مكتبة Moshi (المستخدمة فعلياً لفك التشفير)
-dontwarn com.squareup.moshi.**
-keep class com.squareup.moshi.** { *; }
-keep class * extends com.squareup.moshi.JsonAdapter { *; }
-keep @com.squareup.moshi.JsonClass class * { *; }
-keepnames class * { @com.squareup.moshi.Json <fields>; }

# 4. حماية مكتبة التحميل والصور (Coil & Okio)
-dontwarn okio.**
-keep class coil.** { *; }

# 5. حماية محرك التطبيق الأساسي (Shizuku)
-keep class rikka.shizuku.** { *; }
