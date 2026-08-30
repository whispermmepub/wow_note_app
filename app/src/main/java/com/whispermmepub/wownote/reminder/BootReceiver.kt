package com.whispermmepub.wownote.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.whispermmepub.wownote.data.NoteStore

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        NoteStore(context).dueReminders().forEach { ReminderScheduler.schedule(context, it) }
    }
}
