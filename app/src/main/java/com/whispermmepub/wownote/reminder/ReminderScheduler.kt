package com.whispermmepub.wownote.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.whispermmepub.wownote.model.Note

object ReminderScheduler {
    fun schedule(context: Context, note: Note) {
        val at = note.reminderAt ?: return cancel(context, note.id)
        if (at <= System.currentTimeMillis()) return
        val alarm = context.getSystemService(AlarmManager::class.java)
        val pending = reminderPendingIntent(context, note.id)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarm.canScheduleExactAlarms()) {
            alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
        } else {
            alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
        }
    }

    fun cancel(context: Context, noteId: String) {
        context.getSystemService(AlarmManager::class.java).cancel(reminderPendingIntent(context, noteId))
    }

    private fun reminderPendingIntent(context: Context, noteId: String): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
            .setAction("com.whispermmepub.wownote.REMINDER")
            .putExtra(ReminderReceiver.EXTRA_NOTE_ID, noteId)
        return PendingIntent.getBroadcast(
            context,
            noteId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
