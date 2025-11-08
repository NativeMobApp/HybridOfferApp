package com.example.OfferApp.data.firebase
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.example.OfferApp.R
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
class Notifications: FirebaseMessagingService() {
    private val CHANNEL_ID = "post_notifications"
    private val CHANNEL_NAME = "Notificaciones de Posts"
    // Se llama cada vez que el token se genera o se actualiza
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM_TOKEN", "Nuevo Token: $token")

        // Aquí debes enviar el nuevo token a tu servidor de aplicaciones para mantener el registro actualizado.
    }

    // Se llama cuando llega un mensaje de notificación de FCM
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // Los datos se envían a través del mapa de datos (data payload)
        // Puedes pasar datos como el título, cuerpo y el ID del post o del autor.
        remoteMessage.data.isNotEmpty().let {
            val title = remoteMessage.data["title"] ?: "Nuevo Post"
            val body = remoteMessage.data["body"] ?: "Alguien que sigues ha publicado una oferta."

            // Muestra la notificación solo si la app está en primer plano
            sendNotification(title, body)
        }
    }

    private fun sendNotification(title: String, body: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Crear canal de notificación para Android 8.0 (Oreo) y superior
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Canal para notificaciones de nuevos posts."
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Construir la notificación
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.offerapplogo) // **¡Reemplaza 'ic_notification' con el ícono de tu app!**
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        // Mostrar la notificación (usando un ID único)
        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }
}