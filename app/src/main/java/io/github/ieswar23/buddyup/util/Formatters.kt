package io.github.ieswar23.buddyup.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object Formatters {

    fun distance(km: Double): String = when {
        km < 1.0 -> "< 1 km away"
        km < 100 -> "${km.toInt()} km away"
        else -> "${NumberFormat.getIntegerInstance(Locale.US).format(km.toLong())} km away"
    }

    /**
     * Short location line for list rows: "Indiranagar · 5 km away" for people nearby, or
     * "Jubilee Hills, Hyderabad" for someone in another city, where a raw km figure means little.
     */
    fun place(neighborhood: String, city: String, km: Double): String =
        if (km < OTHER_CITY_KM) "$neighborhood · ${distance(km)}" else "$neighborhood, $city"

    private const val OTHER_CITY_KM = 100.0

    /** "now", "5m", "3h", "2d", or a short date for anything older than a week. */
    fun relativeShort(timestamp: Long, now: Long): String {
        val diff = (now - timestamp).coerceAtLeast(0)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
        return when {
            minutes < 1 -> "now"
            minutes < 60 -> "${minutes}m"
            minutes < 60 * 24 -> "${minutes / 60}h"
            minutes < 60 * 24 * 7 -> "${minutes / (60 * 24)}d"
            else -> SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(timestamp))
        }
    }

    fun lastActive(timestamp: Long, now: Long): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes((now - timestamp).coerceAtLeast(0))
        return when {
            minutes <= 15 -> "Active now"
            minutes < 60 -> "Active ${minutes}m ago"
            minutes < 60 * 24 -> "Active ${minutes / 60}h ago"
            else -> "Active ${minutes / (60 * 24)}d ago"
        }
    }

    fun time(timestamp: Long): String =
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestamp))

    /** Header label used to group chat messages by day. */
    fun dayHeader(timestamp: Long, now: Long): String {
        val dayDiff = daysBetween(timestamp, now)
        return when (dayDiff) {
            0 -> "Today"
            1 -> "Yesterday"
            in 2..6 -> SimpleDateFormat("EEEE", Locale.getDefault()).format(Date(timestamp))
            else -> SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(Date(timestamp))
        }
    }

    fun meetupDate(timestamp: Long, now: Long): String {
        val dayDiff = daysBetween(now, timestamp)
        val time = time(timestamp)
        return when (dayDiff) {
            0 -> "Today · $time"
            1 -> "Tomorrow · $time"
            else -> SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(Date(timestamp)) + " · " + time
        }
    }

    fun duration(minutes: Int): String = when {
        minutes < 60 -> "$minutes min"
        minutes % 60 == 0 -> "${minutes / 60} h"
        else -> "${minutes / 60} h ${minutes % 60} min"
    }

    fun memberSince(timestamp: Long): String =
        SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(Date(timestamp))

    /** Whole calendar days from [from] to [to] (positive when [to] is later). */
    fun daysBetween(from: Long, to: Long): Int {
        val start = startOfDay(from)
        val end = startOfDay(to)
        return TimeUnit.MILLISECONDS.toDays(end - start).toInt()
    }

    fun startOfDay(timestamp: Long): Long = Calendar.getInstance().apply {
        timeInMillis = timestamp
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
