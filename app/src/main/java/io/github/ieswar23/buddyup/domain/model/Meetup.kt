package io.github.ieswar23.buddyup.domain.model

enum class MeetupCategory(val label: String, val emoji: String) {
    COFFEE("Coffee", "☕"),
    OUTDOORS("Outdoors", "🥾"),
    GAMES("Games", "🎲"),
    MUSIC("Music", "🎸"),
    FOOD("Food", "🍛"),
    FITNESS("Fitness", "🏃"),
    CREATIVE("Creative", "🎨"),
    NETWORKING("Networking", "🚀");

    companion object {
        fun fromKey(key: String): MeetupCategory =
            entries.firstOrNull { it.name.equals(key, ignoreCase = true) } ?: COFFEE
    }
}

data class Meetup(
    val id: String,
    val title: String,
    val emoji: String,
    val category: MeetupCategory,
    val description: String,
    val city: String,
    val venue: String,
    val host: String,
    val startsAt: Long,
    val durationMinutes: Int,
    val attendeeCount: Int,
    val capacity: Int,
    val isJoined: Boolean,
    val tags: List<String>,
) {
    val spotsLeft: Int get() = (capacity - attendeeCount).coerceAtLeast(0)
    val isFull: Boolean get() = spotsLeft == 0 && !isJoined
    val fillRatio: Float get() = if (capacity == 0) 0f else (attendeeCount.toFloat() / capacity).coerceIn(0f, 1f)
}
