package xyz.siwane.shizucorefetch

import android.graphics.drawable.Drawable
import java.io.File

data class LocalApkModel(
    val file: File,
    val appName: String,
    val packageName: String,
    val versionName: String,
    val fileSize: String,
    val icon: Drawable? = null // التعديل هنا: إضافة متغير الأيقونة
)
