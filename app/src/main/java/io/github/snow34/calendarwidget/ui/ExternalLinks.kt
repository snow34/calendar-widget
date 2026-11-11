package io.github.snow34.calendarwidget.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/** Opens [url] in the default web browser. Only called with links built by `GoogleCalendarLinks`. */
fun openInBrowser(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "No web browser found on this phone", Toast.LENGTH_LONG).show()
    }
}
