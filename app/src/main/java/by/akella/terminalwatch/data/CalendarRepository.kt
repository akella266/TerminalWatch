package by.akella.terminalwatch.data

import android.content.Context
import android.icu.util.Calendar
import android.provider.CalendarContract

data class EventModel(
    val title: String,
    val startTime: Long,
)

class CalendarRepository(
    private val context: Context
) {

    fun getNearestEvent(): EventModel? {
        val contentResolver = context.contentResolver

        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND
        )

        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis

        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val endOfDay = calendar.timeInMillis

        val selection = "(${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.DTSTART} <= ?) OR (${CalendarContract.Events.DTEND} >= ? AND ${CalendarContract.Events.DTEND} <= ?)"
        val selectionArgs = arrayOf(startOfDay.toString(), endOfDay.toString(), startOfDay.toString(), endOfDay.toString())

        val cursor = contentResolver.query(
            CalendarContract.Events.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )

        val currentTimeInMillis = System.currentTimeMillis()

        cursor?.use {
            while (it.moveToNext()) {
                val startTime = it.getLong(it.getColumnIndexOrThrow(CalendarContract.Events.DTSTART))

                if (startTime > currentTimeInMillis) {
                    val title = it.getString(it.getColumnIndexOrThrow(CalendarContract.Events.TITLE))
                    return EventModel(title, startTime)
                }
            }
        }
        return null
    }

//    fun getCalendarId(): Long? {
//        val projection = arrayOf(
//            CalendarContract.Calendars._ID,
//            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME
//        )
//
//        val cursor = contentResolver.query(
//            CalendarContract.Calendars.CONTENT_URI,
//            projection,
//            null,
//            null,
//            null
//        )
//
//        cursor?.use {
//            if (it.moveToFirst()) {
//                val calID = it.getLong(it.getColumnIndexOrThrow(CalendarContract.Calendars._ID))
//                // ... use the calendar ID
//            }
//        }
//    }
}