/*
 *     Copyright (C) 2023  Akane Foundation
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.akanework.symphonica

import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import org.akanework.symphonica.MainActivity.Companion.currentMusicDrawable
import org.akanework.symphonica.MainActivity.Companion.musicPlayer
import org.akanework.symphonica.logic.util.nextSong
import org.akanework.symphonica.logic.util.userChangedPlayerStatus


/**
 * Implementation of App Widget functionality.
 */
class PlayerPreviewWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        // TODO
    }

    override fun onDisabled(context: Context) {
        // TODO
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)
        val views = RemoteViews(context?.packageName, R.layout.player_preview_widget)
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val widget = ComponentName(context!!, PlayerPreviewWidget::class.java)
        when (intent?.action) {
            WIDGET_UPDATE_PLAYER_STATUS -> {
                userChangedPlayerStatus()
            }
            WIDGET_UPDATE_PLAYER_ALBUM_STATUS -> {
                if (currentMusicDrawable != null) {
                    views.setImageViewBitmap(R.id.widget_album_cover, currentMusicDrawable)
                    appWidgetManager.updateAppWidget(widget, views)
                }
            }
            WIDGET_UPDATE_PLAYER_BUTTON_STATUS -> {
                if (musicPlayer != null && musicPlayer!!.isPlaying) {
                    views.setImageViewResource(R.id.widget_button, R.drawable.ic_pause)
                } else {
                    views.setImageViewResource(R.id.widget_button, R.drawable.ic_sheet_play)
                }
                appWidgetManager.updateAppWidget(widget, views)
            }
            WIDGET_UPDATE_PLAYER_NEXT_SONG -> {
                nextSong()
                appWidgetManager.updateAppWidget(widget, views)
            }
        }
    }
}

internal fun updateAppWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int
) {
    // Construct the RemoteViews object
    val views = RemoteViews(context.packageName, R.layout.player_preview_widget)

    val intentChangeStatus = Intent(context, PlayerPreviewWidget::class.java)
    val intentNextSong = Intent(context, PlayerPreviewWidget::class.java)
    intentChangeStatus.action = WIDGET_UPDATE_PLAYER_STATUS
    intentNextSong.action = WIDGET_UPDATE_PLAYER_NEXT_SONG

    val pendingIntentStatus = PendingIntent.getBroadcast(context, 0,  intentChangeStatus, FLAG_IMMUTABLE)
    val pendingIntentNext = PendingIntent.getBroadcast(context, 0, intentNextSong, FLAG_IMMUTABLE)

    if (musicPlayer != null && musicPlayer!!.isPlaying) {
        views.setImageViewResource(R.id.widget_button, R.drawable.ic_pause)
    } else {
        views.setImageViewResource(R.id.widget_button, R.drawable.ic_sheet_play)
    }

    views.setOnClickPendingIntent(R.id.widget_button, pendingIntentStatus)
    views.setOnClickPendingIntent(R.id.widget_button_next, pendingIntentNext)

    // Instruct the widget manager to update the widget
    appWidgetManager.updateAppWidget(appWidgetId, views)
}