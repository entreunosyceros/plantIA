package com.plantia.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.plantia.MainActivity
import com.plantia.R
import java.util.concurrent.TimeUnit

object NotificationHelper {
    const val CHANNEL_WATER = "plantia_water"
    private const val NOTIFICATION_ID_BASE = 4000

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_WATER,
            "Recordatorios de riego",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Avisos para regar tus plantas"
        }
        manager.createNotificationChannel(channel)
    }

    fun showWaterReminder(context: Context, plantId: Int, plantName: String) {
        ensureChannels(context)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_OPEN_PLANT_ID, plantId)
        }
        val pending = PendingIntent.getActivity(
            context,
            plantId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_WATER)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("¿Has regado $plantName?")
            .setContentText("Toca para abrir el cuaderno de la planta.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_BASE + plantId, notification)
    }

    fun daysSince(millis: Long): Long =
        TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - millis)
}
