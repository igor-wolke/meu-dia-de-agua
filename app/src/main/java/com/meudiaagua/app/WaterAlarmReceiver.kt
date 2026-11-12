package com.meudiaagua.app

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class WaterAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val prefs = context.getSharedPreferences("water_cycle", Context.MODE_PRIVATE)
        val anchor = LocalDate.parse(prefs.getString("anchor_date", "2026-11-12"))
        val today = LocalDate.now()
        val days = ChronoUnit.DAYS.between(anchor, today)
        val isWaterDay = Math.floorMod(days, 2L) == 0L

        if (isWaterDay) {
            showNotification(context, "HOJE TEM ÁGUA! 💧", "São 7h! Hoje é dia de abastecimento. Não esqueça de verificar sua caixa.")
        }
        MainActivity.scheduleNextAlarm(context)
    }

    companion object {
        const val CHANNEL_ID = "water_alarm_channel"

        fun showNotification(context: Context, title: String, message: String) {
            val openApp = PendingIntent.getActivity(
                context, 1, Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Notification.Builder(context, CHANNEL_ID)
            } else {
                @Suppress("DEPRECATION")
                Notification.Builder(context)
                    .setPriority(Notification.PRIORITY_HIGH)
                    .setSound(android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI)
            }.setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(Notification.BigTextStyle().bigText(message))
                .setContentIntent(openApp)
                .setAutoCancel(true)
                .build()

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(7001, notification)
        }
    }
}
