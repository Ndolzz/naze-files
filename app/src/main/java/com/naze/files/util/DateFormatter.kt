package com.naze.files.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Formats a last-modified timestamp as "Today", "Yesterday", or a date. */
fun formatModifiedDate(millis: Long): String {
    val target = Calendar.getInstance().apply { timeInMillis = millis }
    val today = Calendar.getInstance()
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

    return when {
        isSameDay(target, today) -> "Today"
        isSameDay(target, yesterday) -> "Yesterday"
        target.get(Calendar.YEAR) == today.get(Calendar.YEAR) ->
            SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(millis))
        else ->
            SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(millis))
    }
}

private fun isSameDay(a: Calendar, b: Calendar): Boolean =
    a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
