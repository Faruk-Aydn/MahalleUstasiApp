package com.example.mahalleustasi.ui.theme

import androidx.compose.ui.graphics.Color

// --- Ana Marka Renkleri ---
val MahalleTeal = Color(0xFF00695C)       // Ana renk (Primary)
val MahalleTealDark = Color(0xFF004D40)   // Gradient veya koyu mod için
val MahalleOrange = Color(0xFFFFB74D)     // Vurgu rengi (Secondary)
val MahalleOrangeDark = Color(0xFFEF6C00) // Turuncu üzerindeki koyu metinler için

// --- Arkaplan ve Zemin ---
val MahalleBackground = Color(0xFFF7F9FC) // O sevdiğin açık gri-mavi zemin
val MahalleSurface = Color.White          // Kartlar ve alt barlar için

// --- Metin Renkleri ---
val MahalleTextPrimary = Color(0xFF1A1C1E)   // Koyu başlıklar
val MahalleTextSecondary = Color(0xFF546E7A) // Açıklama metinleri (Gri-Mavi)

// --- Durum (Status) Renkleri ---
// Bekliyor
val StatusPendingBg = Color(0xFFFFF8E1)
val StatusPendingText = Color(0xFFF57C00)

// Aktif / Atandı
val StatusActiveBg = Color(0xFFE0F2F1)
val StatusActiveText = MahalleTeal

// Tamamlandı
val StatusCompletedBg = Color(0xFFE8F5E9)
val StatusCompletedText = Color(0xFF2E7D32)