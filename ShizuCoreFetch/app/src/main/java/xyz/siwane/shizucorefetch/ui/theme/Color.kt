package xyz.siwane.shizucorefetch.ui.theme

import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

fun parseHexColor(hexString: String, defaultColor: Color = Color(0xFF6750A4)): Color {
    val cleanHex = hexString.replace("#", "").trim()
    return try {
        when (cleanHex.length) {
            6 -> Color(android.graphics.Color.parseColor("#FF$cleanHex"))
            8 -> Color(android.graphics.Color.parseColor("#$cleanHex"))
            else -> defaultColor
        }
    } catch (e: Exception) {
        defaultColor
    }
}
