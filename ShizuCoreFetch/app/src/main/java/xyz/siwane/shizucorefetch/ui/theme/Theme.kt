package xyz.siwane.shizucorefetch.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val Amethyst = Color(0xFF9B51E0)
val Emerald = Color(0xFF2ECC71)
val Sapphire = Color(0xFF4A90E2)
val Siwane = Color(0xFF14B8A6)
val Crimson = Color(0xFFE74C3C)
val Sunset = Color(0xFFF39C12)

@Composable
fun ShizuCoreFetchTheme(
    themeMode: String = "System",
    colorIndex: Int = 2,
    isDynamicColor: Boolean = false,
    useCustomColor: Boolean = false,
    customHexColor: String = "#4A90E2",
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    
    val darkTheme = when (themeMode) {
        "Dark" -> true
        "Light" -> false
        else -> isSystemInDarkTheme()
    }

    val primaryColor = if (useCustomColor) {
        try {
            Color(android.graphics.Color.parseColor(if (customHexColor.startsWith("#")) customHexColor else "#$customHexColor"))
        } catch (e: Exception) {
            Sapphire
        }
    } else {
        when (colorIndex) {
            0 -> Amethyst
            1 -> Emerald
            2 -> Sapphire
            3 -> Siwane
            4 -> Crimson
            5 -> Sunset
            else -> Sapphire
        }
    }

    val dynamicColorSupported = isDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    
    // تصميم التباين الواضح: إجبار M3 والألوان العادية على استخدام خلفيات نظيفة ومميزة
    val colorScheme = when {
        dynamicColorSupported && darkTheme -> {
            dynamicDarkColorScheme(context).copy(
                background = Color(0xFF000000), // خلفية سوداء نقية
                surface = Color(0xFF000000),
                surfaceVariant = Color(0xFF141414), // بطاقات رمادية داكنة للتباين
                surfaceContainer = Color(0xFF141414),
                onSurface = Color(0xFFE3E3E3),
                onSurfaceVariant = Color(0xFFC4C4C4)
            )
        }
        dynamicColorSupported && !darkTheme -> {
            dynamicLightColorScheme(context).copy(
                background = Color(0xFFF3F4F6), // خلفية رمادية فاتحة مريحة للعين
                surface = Color(0xFFF3F4F6),
                surfaceVariant = Color(0xFFFFFFFF), // بطاقات بيضاء ناصعة للتباين
                surfaceContainer = Color(0xFFFFFFFF),
                onSurface = Color(0xFF1F2937),
                onSurfaceVariant = Color(0xFF4B5563)
            )
        }
        darkTheme -> darkColorScheme(
            primary = primaryColor,
            onPrimary = Color.White,
            primaryContainer = primaryColor.copy(alpha = 0.25f),
            onPrimaryContainer = primaryColor,
            secondary = primaryColor,
            background = Color(0xFF000000),
            surface = Color(0xFF000000),
            surfaceVariant = Color(0xFF141414),
            onSurface = Color(0xFFE3E3E3),
            onSurfaceVariant = Color(0xFFC4C4C4),
            outline = Color(0xFF2C2C2C)
        )
        else -> lightColorScheme(
            primary = primaryColor,
            onPrimary = Color.White,
            primaryContainer = primaryColor.copy(alpha = 0.15f),
            onPrimaryContainer = primaryColor,
            secondary = primaryColor,
            background = Color(0xFFF3F4F6),
            surface = Color(0xFFF3F4F6),
            surfaceVariant = Color(0xFFFFFFFF),
            onSurface = Color(0xFF1F2937),
            onSurfaceVariant = Color(0xFF4B5563),
            outline = Color(0xFFE5E7EB)
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            window.decorView.setBackgroundColor(android.graphics.Color.parseColor(if (darkTheme) "#000000" else "#F3F4F6"))
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
