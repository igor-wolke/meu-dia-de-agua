package com.meudiaagua.app

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("water_cycle", MODE_PRIVATE) }
    private val fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    private fun anchor(): LocalDate {
        val stored = prefs.getString("anchor_date", null)
        if (stored == null) {
            val today = LocalDate.of(2026, 11, 12) // Data informada pelo usuário na configuração inicial
            prefs.edit().putString("anchor_date", today.toString()).apply()
            return today
        }
        return LocalDate.parse(stored)
    }

    private fun isWaterDay(date: LocalDate = LocalDate.now()): Boolean {
        val start = anchor()
        if (date.isBefore(start)) return false
        val days = java.time.temporal.ChronoUnit.DAYS.between(start, date)
        return days % 2L == 0L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel()
        requestNotificationPermission()
        scheduleNextAlarm(this)
        render()
    }

    private fun render() {
        val today = LocalDate.now()
        val waterToday = isWaterDay(today)
        val nextWater = generateSequence(today) { it.plusDays(1) }.first { isWaterDay(it) }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(36, 48, 36, 32)
            setBackgroundColor(0xFFEAF7FF.toInt())
        }
        val title = TextView(this).apply {
            text = "💧 Meu Dia de Água"
            textSize = 28f
            setTextColor(0xFF075985.toInt())
            gravity = Gravity.CENTER
        }
        val status = TextView(this).apply {
            text = if (waterToday) "HOJE TEM ÁGUA!" else "Hoje não é dia de água"
            textSize = 25f
            setTextColor(if (waterToday) 0xFF047857.toInt() else 0xFF334155.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 42, 0, 12)
        }
        val details = TextView(this).apply {
            text = "Hoje: ${today.format(fmt)}\\nPróximo dia de água: ${nextWater.format(fmt)}\\nAlarme: todos os dias às 07:00 (avisa nos dias de água)"
            textSize = 17f
            setTextColor(0xFF1E293B.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 12, 0, 32)
        }
        root.addView(title)
        root.addView(status)
        root.addView(details)
        root.addView(makeButton("🔔 Testar alarme agora") {
            WaterAlarmReceiver.showNotification(this, "Teste do alarme", "O Meu Dia de Água está funcionando.")
        })
        root.addView(makeButton("💧 Chegou água hoje — reiniciar ciclo") {
            prefs.edit()
                .putString("anchor_date", LocalDate.now().toString())
                .remove("last_missed_date")
                .apply()
            scheduleNextAlarm(this)
            Toast.makeText(this, "Ciclo reiniciado: hoje foi marcado como dia de água.", Toast.LENGTH_LONG).show()
            render()
        })
        root.addView(makeButton("⚠️ Não chegou água hoje") {
            prefs.edit().putString("last_missed_date", LocalDate.now().toString()).apply()
            Toast.makeText(this, "Dia sem abastecimento registrado. Você pode reprogramar a próxima data.", Toast.LENGTH_LONG).show()
            render()
        })
        root.addView(makeButton("🔄 Reprogramar a próxima data de água") {
            val suggested = LocalDate.now().plusDays(1)
            DatePickerDialog(this, { _, y, m, day ->
                val newExpectedDate = LocalDate.of(y, m + 1, day)
                prefs.edit()
                    .putString("anchor_date", newExpectedDate.toString())
                    .putString("last_rescheduled_date", newExpectedDate.toString())
                    .apply()
                scheduleNextAlarm(this)
                Toast.makeText(this, "Próximo abastecimento reprogramado para ${newExpectedDate.format(fmt)}. O ciclo de 48 horas começará nessa data.", Toast.LENGTH_LONG).show()
                render()
            }, suggested.year, suggested.monthValue - 1, suggested.dayOfMonth).show()
        })
        root.addView(makeButton("📅 Corrigir a última data de abastecimento") {
            val d = anchor()
            DatePickerDialog(this, { _, y, m, day ->
                prefs.edit()
                    .putString("anchor_date", LocalDate.of(y, m + 1, day).toString())
                    .remove("last_missed_date")
                    .apply()
                scheduleNextAlarm(this)
                render()
            }, d.year, d.monthValue - 1, d.dayOfMonth).show()
        })
        root.addView(makeButton("⏰ Configurar permissão do alarme") {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
                if (!am.canScheduleExactAlarms()) {
                    startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                        Uri.parse("package:$packageName")))
                } else {
                    Toast.makeText(this, "Permissão de alarme exato já está ativa.", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(this, "Seu Android permite o agendamento de alarmes.", Toast.LENGTH_LONG).show()
            }
        })
        setContentView(root)
    }

    private fun makeButton(label: String, action: () -> Unit): Button =
        Button(this).apply {
            text = label
            textSize = 15f
            isAllCaps = false
            setOnClickListener { action() }
            val p = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            p.setMargins(0, 8, 0, 8)
            layoutParams = p
        }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                WaterAlarmReceiver.CHANNEL_ID,
                "Alarme de abastecimento",
                android.app.NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Avisos às 07:00 nos dias de abastecimento"
                enableVibration(true)
                setSound(android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI,
                    android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                        .build())
            }
            getSystemService(android.app.NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    companion object {
        fun scheduleNextAlarm(context: Context) {
            val prefs = context.getSharedPreferences("water_cycle", Context.MODE_PRIVATE)
            val anchorString = prefs.getString("anchor_date", null) ?: "2026-11-12"
            val anchor = LocalDate.parse(anchorString)
            val today = LocalDate.now()
            var target = today.atTime(7, 0)
            if (!target.isAfter(java.time.LocalDateTime.now())) target = today.plusDays(1).atTime(7, 0)

            val intent = Intent(context, WaterAlarmReceiver::class.java)
            val pending = PendingIntent.getBroadcast(
                context, 7001, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val trigger = target.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarm.canScheduleExactAlarms()) {
                    alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pending)
                } else {
                    alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pending)
                }
            } catch (_: SecurityException) {
                alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pending)
            }
        }
    }
}
