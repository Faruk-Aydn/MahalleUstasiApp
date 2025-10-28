package com.example.mahalleustasi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.mahalleustasi.ui.navigation.AppNavigation
import com.example.mahalleustasi.ui.theme.MahalleUstasiTheme
import dagger.hilt.android.AndroidEntryPoint
import com.example.mahalleustasi.data.repository.NotificationsRepository
import com.google.firebase.firestore.ListenerRegistration
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.Manifest
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var notifListener: ListenerRegistration? = null
    
    // Bildirim izni için launcher
    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                Log.d("MainActivity", "Notification permission granted")
            } else {
                Log.w("MainActivity", "Notification permission denied")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Android 13+ için bildirim izni kontrolü ve isteği
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // İzin zaten verilmiş
                    Log.d("MainActivity", "Notification permission already granted")
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    // Kullanıcıya neden izin gerektiğini açıklayabilirsiniz
                    // Şimdilik direkt izin isteği yapıyoruz
                    requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                else -> {
                    // İlk kez izin isteniyor
                    requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
        
        setContent {
            MahalleUstasiTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        startNotificationsListener()
    }

    override fun onStop() {
        super.onStop()
        notifListener?.remove()
        notifListener = null
    }

    private fun startNotificationsListener() {
        if (notifListener != null) return
        val repo = NotificationsRepository()
        notifListener = repo.listenMyNotifications(onAdded = { notif ->
            val builder = NotificationCompat.Builder(this, MahalleUstasiApp.NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(notif.title.ifBlank { getString(R.string.app_name) })
                .setContentText(notif.body)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            
            // Android 13+ (API 33+) için POST_NOTIFICATIONS iznini kontrol et
            val hasPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else {
                true // Android 13 öncesi için izin gerekmez
            }

            if (hasPermission) {
                try {
                    with(NotificationManagerCompat.from(this)) {
                        notify((System.currentTimeMillis() % 100000).toInt(), builder.build())
                    }
                } catch (e: SecurityException) {
                    Log.w("MainActivity", "Failed to show notification: permission denied", e)
                }
            } else {
                Log.w("MainActivity", "POST_NOTIFICATIONS permission not granted, notification not shown")
            }
        })
    }
}