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

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var notifListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
            with(NotificationManagerCompat.from(this)) {
                notify((System.currentTimeMillis() % 100000).toInt(), builder.build())
            }
        })
    }
}