package com.example.mahalleustasi.notifications

import android.util.Log
import com.example.mahalleustasi.data.model.AppNotification
import com.example.mahalleustasi.data.repository.NotificationsRepository
import com.example.mahalleustasi.data.repository.UsersRepository
import com.google.firebase.auth.ktx.auth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.mahalleustasi.MahalleUstasiApp
import com.example.mahalleustasi.R
import android.app.PendingIntent
import android.content.Intent
import com.example.mahalleustasi.MainActivity

@AndroidEntryPoint
class FcmService : FirebaseMessagingService() {

    @Inject lateinit var notificationsRepository: NotificationsRepository
    @Inject lateinit var usersRepository: UsersRepository

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FcmService", "New FCM token: $token")
        // Token'i Firestore'daki users/{uid} alanına kaydet
        CoroutineScope(Dispatchers.IO).launch {
            runCatching { usersRepository.updateFcmToken(token) }
                .onFailure { Log.w("FcmService", "Failed to save FCM token: ${it.message}") }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val uid = Firebase.auth.currentUser?.uid
        val title = message.notification?.title ?: message.data["title"] ?: "Mahalle Ustası"
        val body = message.notification?.body ?: message.data["body"] ?: ""
        val type = message.data["type"] ?: "generic"
        val data = message.data

        if (uid != null) {
            val appNotif = AppNotification(
                userId = uid,
                type = type,
                title = title,
                body = body,
                data = data,
            )
            // Hata olursa sadece logla, foreground'da çakılmasın
            CoroutineScope(Dispatchers.IO).launch {
                runCatching { notificationsRepository.saveNotification(appNotif) }
                    .onFailure { Log.w("FcmService", "Failed to persist notification: ${it.message}") }
            }
        }
        // Local bildirim göster
        showLocalNotification(title, body, data)
    }

    private fun showLocalNotification(title: String, body: String, data: Map<String, String>) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            data["jobId"]?.let { putExtra("jobId", it) }
            data["type"]?.let { putExtra("type", it) }
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, MahalleUstasiApp.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)

        with(NotificationManagerCompat.from(this)) {
            notify((System.currentTimeMillis() % 100000).toInt(), builder.build())
        }
    }
}
