package io.github.ieswar23.buddyup.domain.model

import java.util.concurrent.TimeUnit

/**
 * A member of the BuddyUp community as seen by the current user.
 *
 * [distanceKm] is always relative to the current user's home city and is computed in the
 * repository layer, so UI and scoring code never deal with coordinates directly.
 */
data class Person(
    val id: String,
    val name: String,
    val age: Int,
    val city: String,
    val neighborhood: String,
    val occupation: String,
    val bio: String,
    val interests: List<String>,
    val distanceKm: Double,
    val lastActiveAt: Long,
    val wavesBack: Boolean = false,
) {
    val firstName: String get() = name.substringBefore(' ')

    val initials: String
        get() = name.split(' ')
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercase() }

    fun minutesSinceActive(now: Long): Long =
        TimeUnit.MILLISECONDS.toMinutes((now - lastActiveAt).coerceAtLeast(0))

    fun isOnline(now: Long): Boolean = minutesSinceActive(now) <= ONLINE_WINDOW_MINUTES

    companion object {
        const val ONLINE_WINDOW_MINUTES = 15L
    }
}
