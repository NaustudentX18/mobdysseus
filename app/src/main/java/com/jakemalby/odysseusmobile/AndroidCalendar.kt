package com.jakemalby.odysseusmobile

import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import java.text.DateFormat
import java.util.Date

internal data class PhoneCalendarEvent(val id: Long, val title: String, val start: Long, val end: Long) {
    fun displayTime(): String = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(start))
}

internal fun upcomingPhoneEvents(context: Context, limit: Int = 12): List<PhoneCalendarEvent> {
    val now = System.currentTimeMillis()
    val horizon = now + 90L * 24L * 60L * 60L * 1000L
    val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
    android.content.ContentUris.appendId(builder, now)
    android.content.ContentUris.appendId(builder, horizon)
    val projection = arrayOf(
        CalendarContract.Instances.EVENT_ID,
        CalendarContract.Instances.TITLE,
        CalendarContract.Instances.BEGIN,
        CalendarContract.Instances.END,
    )
    return context.contentResolver.query(builder.build(), projection, null, null, "${CalendarContract.Instances.BEGIN} ASC")?.use { cursor ->
        val events = mutableListOf<PhoneCalendarEvent>()
        while (cursor.moveToNext() && events.size < limit) {
            events += PhoneCalendarEvent(cursor.getLong(0), cursor.getString(1) ?: "Untitled event", cursor.getLong(2), cursor.getLong(3))
        }
        events
    }.orEmpty()
}

internal fun newCalendarEventIntent(): Intent = Intent(Intent.ACTION_INSERT).setData(CalendarContract.Events.CONTENT_URI)
    .putExtra(CalendarContract.Events.TITLE, "Mobdysseus")
