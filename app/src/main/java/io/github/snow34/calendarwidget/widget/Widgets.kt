package io.github.snow34.calendarwidget.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.glance.appwidget.updateAll
import io.github.snow34.calendarwidget.ui.MainActivity

object Widgets {
    private const val PACKAGE = "io.github.snow34.calendarwidget"

    /** Intent that opens the app, optionally on a given `calendarwidget://day/<date>` link. */
    fun openAppIntent(data: Uri? = null): Intent =
        Intent(if (data == null) Intent.ACTION_MAIN else Intent.ACTION_VIEW, data)
            .setClassName(PACKAGE, MainActivity::class.java.name)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)

    /** Redraws every placed widget (Upcoming and Month). */
    suspend fun updateAll(context: Context) {
        CalendarWidget().updateAll(context)
        MonthWidget().updateAll(context)
    }
}
