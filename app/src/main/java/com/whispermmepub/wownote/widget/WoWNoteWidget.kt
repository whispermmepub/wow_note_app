package com.whispermmepub.wownote.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.whispermmepub.wownote.MainActivity
import com.whispermmepub.wownote.R
import com.whispermmepub.wownote.data.NoteStore

class WoWNoteWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { id -> manager.updateAppWidget(id, buildViews(context)) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) refresh(context)
    }

    companion object {
        private const val ACTION_REFRESH = "com.whispermmepub.wownote.WIDGET_REFRESH"

        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, WoWNoteWidget::class.java)
            manager.getAppWidgetIds(component).forEach { id ->
                manager.updateAppWidget(id, buildViews(context))
            }
        }

        private fun buildViews(context: Context): RemoteViews {
            val notes = NoteStore(context).all().filter { !it.deleted && !it.archived }.take(3)
            val views = RemoteViews(context.packageName, R.layout.widget_wow_note)
            val open = PendingIntent.getActivity(
                context,
                9000,
                Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_title, open)

            val quick = PendingIntent.getActivity(
                context,
                9001,
                Intent(context, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    .putExtra(MainActivity.EXTRA_QUICK_NEW, true),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_add, quick)

            val ids = intArrayOf(R.id.widget_note_1, R.id.widget_note_2, R.id.widget_note_3)
            ids.forEachIndexed { index, viewId ->
                val note = notes.getOrNull(index)
                if (note == null) {
                    views.setViewVisibility(viewId, View.GONE)
                } else {
                    views.setViewVisibility(viewId, View.VISIBLE)
                    val text = note.title.ifBlank { note.content.lineSequence().firstOrNull().orEmpty().ifBlank { "Untitled" } }
                    views.setTextViewText(viewId, text)
                    val pending = PendingIntent.getActivity(
                        context,
                        9100 + index,
                        Intent(context, MainActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            .putExtra(MainActivity.EXTRA_OPEN_NOTE_ID, note.id),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(viewId, pending)
                }
            }
            return views
        }
    }
}
