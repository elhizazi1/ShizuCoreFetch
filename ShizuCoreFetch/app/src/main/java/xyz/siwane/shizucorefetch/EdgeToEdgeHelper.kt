package xyz.siwane.shizucorefetch

import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

object EdgeToEdgeHelper {
    
    fun setup(activity: ComponentActivity, rootView: View) {
        // 1. تفعيل الميزة: المكتبة ذكية وتعمل حسب إصدار هاتف المستخدم
        activity.enableEdgeToEdge()

        // 2. حساب المسافات الآمنة (Safe Insets) وتطبيقها بدقة لمنع تداخل المحتوى
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}
