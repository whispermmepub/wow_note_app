package com.whispermmepub.wownote.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.whispermmepub.wownote.MainActivity
import com.whispermmepub.wownote.R
import com.whispermmepub.wownote.data.NoteStore

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val noteId = intent.getStringExtra(EXTRA_NOTE_ID) ?: return
        val note = NoteStore(context).get(noteId) ?: return
        createChannel(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val openIntent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(MainActivity.EXTRA_OPEN_NOTE_ID, noteId)
        val openPending = PendingIntent.getActivity(
            context,
            noteId.hashCode(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = note.title.ifBlank { "WoW Note Reminder" }
        val body = note.content.replace('\n', ' ').trim().take(180).ifBlank { "ဒီ note ကို ပြန်ကြည့်ရန် အချိန်ရောက်ပါပြီ။" }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openPending)
            .build()

        NotificationManagerCompat.from(context).notify(noteId.hashCode(), notification)
    }

    companion object {
        const val EXTRA_NOTE_ID = "note_id"
        private const val CHANNEL_ID = "wow_note_reminders"

        fun createChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val manager = context.getSystemService(NotificationManager::class.java)
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "WoW Note Reminders",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Calendar-linked reminders for WoW Note"
                    enableVibration(true)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }
}
