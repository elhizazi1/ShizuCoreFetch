plugins {
    id("com.android.application")
    id("kotlin-android")
    // إضافة بلاجن خدمات جوجل لفايربيس
    id("com.google.gms.google-services")
}

android {
    namespace = "xyz.siwane.shizucorefetch"
    compileSdk = 36
    ndkVersion = "28.2.13676358"
    
    defaultConfig {
        applicationId = "xyz.siwane.shizucorefetch"
        minSdk = 24
        targetSdk = 36
        versionCode = 5
        versionName = "1.5"
        
        vectorDrawables { 
            useSupportLibrary = true
        }
        
        // ملاحظة: إذا لم تكن تنوي كتابة أكواد C/C++ في هذا التطبيق، 
        // يمكنك إزالة قسم externalNativeBuild لتسريع عملية البناء (Build) بشكل كبير في AndroidIDE.
        externalNativeBuild {
            ndkBuild {
                abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a", "x86_64", "x86"))
            }
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    
    externalNativeBuild {
        ndkBuild {
            path = file("src/main/jni/Android.mk")
        }
    }
    
    buildFeatures {
        viewBinding = true
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.fromTarget("17"))
    }
}

dependencies {
    // مكتبات الواجهة الأساسية
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("com.google.android.material:material:1.12.0")

    // 1. Shizuku API (للتثبيت الصامت وإدارة الصلاحيات)
    val shizukuVersion = "13.1.5"
    implementation("dev.rikka.shizuku:api:$shizukuVersion")
    implementation("dev.rikka.shizuku:provider:$shizukuVersion")

    // 2. Retrofit & Gson (للاتصال بالـ Backend وجلب ملفات JSON)
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")

    // 3. Kotlin Coroutines (لإدارة المهام في الخلفية)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")

    // 4. Coil (لتحميل وعرض صور الأيقونات من الروابط بسلاسة)
    implementation("io.coil-kt:coil:2.6.0")
    
    implementation("io.noties.markwon:core:4.6.2")

    // 5. Firebase & GitHub Authentication
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-auth")
    
    implementation("androidx.browser:browser:1.8.0")
    
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    
    implementation("com.google.firebase:firebase-firestore")
    
}
