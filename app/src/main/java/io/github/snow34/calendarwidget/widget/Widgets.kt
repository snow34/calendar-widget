package io.github.snow34.calendarwidget.widget

import android.content.Context
import androidx.glance.appwidget.updateAll

object Widgets {
    /** Redraws every placed widget (Upcoming and Month). */
    suspend fun updateAll(context: Context) {
        CalendarWidget().updateAll(context)
        MonthWidget().updateAll(context)
    }
}
