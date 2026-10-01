package io.github.ieswar23.buddyup.domain.model

data class Interest(val name: String, val emoji: String)

/** Canonical interest catalog shared by onboarding, filters and seed data. */
object Interests {
    val all: List<Interest> = listOf(
        Interest("Hiking", "🥾"),
        Interest("Coffee", "☕"),
        Interest("Board games", "🎲"),
        Interest("Chess", "♟️"),
        Interest("Photography", "📷"),
        Interest("Cricket", "🏏"),
        Interest("Cooking", "🍳"),
        Interest("Indie music", "🎸"),
        Interest("Startups", "🚀"),
        Interest("Running", "🏃"),
        Interest("Yoga", "🧘"),
        Interest("Reading", "📚"),
        Interest("Travel", "✈️"),
        Interest("Gaming", "🎮"),
        Interest("Cycling", "🚴"),
        Interest("Painting", "🎨"),
        Interest("Football", "⚽"),
        Interest("Basketball", "🏀"),
        Interest("Tennis", "🎾"),
        Interest("Badminton", "🏸"),
        Interest("Movies", "🎬"),
        Interest("Tech", "💻"),
        Interest("Volunteering", "🤝"),
        Interest("Dogs", "🐶"),
        Interest("Baking", "🧁"),
        Interest("Podcasts", "🎧"),
        Interest("Writing", "✍️"),
        Interest("Gardening", "🌱"),
        Interest("Dancing", "💃"),
        Interest("Stand-up comedy", "🎤"),
    )

    private val byName = all.associateBy { it.name.lowercase() }

    fun emojiFor(name: String): String = byName[name.lowercase()]?.emoji ?: "✨"

    fun label(name: String): String = "${emojiFor(name)} $name"
}
