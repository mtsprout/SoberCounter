package com.csprout.sobercounter

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import java.time.ZoneId

private const val CHANNEL_ID = "goals"
private const val EXTRA_LABEL = "label"

object GoalScheduler {
    private fun pendingIntent(context: Context, goal: Goal): PendingIntent {
        val intent = Intent(context, GoalReceiver::class.java)
            .putExtra(EXTRA_LABEL, goal.label)
            .putExtra("id", goal.id)
        return PendingIntent.getBroadcast(
            context, goal.id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** Re-arms one alarm per goal from the stored sobriety date. Safe to call repeatedly. */
    fun scheduleAll(context: Context) {
        val store = Store(context)
        val start = store.soberStart
        val alarms = context.getSystemService(AlarmManager::class.java)
        val now = System.currentTimeMillis()
        store.goals.forEach { goal ->
            val pi = pendingIntent(context, goal)
            alarms.cancel(pi)
            if (start == null) return@forEach
            val at = goal.targetFrom(start).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            if (at > now) {
                // Inexact (no special permission needed); may fire a few minutes late in Doze.
                alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            }
        }
    }

    fun cancel(context: Context, goal: Goal) {
        context.getSystemService(AlarmManager::class.java).cancel(pendingIntent(context, goal))
    }
}

class GoalReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val label = intent.getStringExtra(EXTRA_LABEL) ?: return
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Goals", NotificationManager.IMPORTANCE_HIGH)
        )
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val n = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Goal reached: $label")
            .setContentText("You made it. Be proud of yourself.")
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        // Silently skipped if the user has not granted notification permission.
        runCatching { nm.notify(intent.getIntExtra("id", 0), n) }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) GoalScheduler.scheduleAll(context)
    }
}
