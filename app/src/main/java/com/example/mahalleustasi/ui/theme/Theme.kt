package com.example.mahalleustasi.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Şimdilik sadece Light (Aydınlık) modu senin renklerine göre ayarlıyoruz
private val LightColorScheme = lightColorScheme(
    primary = MahalleTeal,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    secondary = MahalleOrange,
    onSecondary = MahalleTextPrimary,
    background = MahalleBackground,
    surface = MahalleSurface,
    onSurface = MahalleTextPrimary,
    onBackground = MahalleTextPrimary,
)

@Composable
fun MahalleUstasiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color genelde Android 12+ için duvar kağıdı rengini alır,
    // biz senin renklerin baskın olsun diye false yapıyoruz:
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme
    // Not: İleride Dark Mode istersen buraya 'if(darkTheme) DarkColorScheme else LightColorScheme' yazabiliriz.

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Status bar (Bildirim çubuğu) rengini ana rengimiz yapalım
            window.statusBarColor = colorScheme.primary.toArgb()
            // Status bar ikonları beyaz olsun (çünkü zemin koyu teal)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // Varsayılan Typography dosyası varsa
        content = content
    )
}