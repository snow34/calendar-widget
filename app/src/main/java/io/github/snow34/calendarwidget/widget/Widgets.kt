package io.github.snow34.calendarwidget.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.glance.appwidget.updateAll
import io.github.snow34.calendarwidget.ui.MainActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

object Widgets {
    private const val PACKAGE = "io.github.snow34.calendarwidget"

    /** Intent that opens the app, optionally on a given `calendarwidget://day/<date>` link. */
    fun openAppIntent(data: Uri? = null): Intent =
        Intent(if (data == null) Intent.ACTION_MAIN else Intent.ACTION_VIEW, data)
            .setClassName(PACKAGE, MainActivity::class.java.name)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)

    private val _revision = MutableStateFlow(0)

    /**
     * Bumped whenever widget inputs (settings, events, shown month) change. A Glance widget session can stay alive
     * between update() calls, and update() then only recomposes without re-running provideGlance, so anything read
     * once there goes stale. Widgets read their data inside the composition, keyed on this value.
     */
    val revision: StateFlow<Int> = _revision

    fun requestRefresh() = _revision.update { it + 1 }

    /** Redraws every placed widget (Upcoming and Month). */
    suspend fun updateAll(context: Context) {
        requestRefresh()
        CalendarWidget().updateAll(context)
        MonthWidget().updateAll(context)
    }
}
