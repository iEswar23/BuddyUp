package io.github.ieswar23.buddyup.domain.model

enum class AgeRange(val label: String, val min: Int, val max: Int) {
    AGE_18_24("18–24", 18, 24),
    AGE_25_34("25–34", 25, 34),
    AGE_35_44("35–44", 35, 44),
    AGE_45_PLUS("45+", 45, 99);

    companion object {
        fun fromKey(key: String?): AgeRange = entries.firstOrNull { it.name == key } ?: AGE_25_34
    }
}

data class UserProfile(
    val name: String,
    val ageRange: AgeRange,
    val city: String,
    val bio: String,
    val interests: List<String>,
) {
    val initials: String
        get() = name.trim().split(' ').filter { it.isNotBlank() }.take(2)
            .joinToString("") { it.first().uppercase() }
            .ifEmpty { "🙂" }

    companion object {
        const val MIN_INTERESTS = 3
        const val MAX_INTERESTS = 8
        const val MAX_BIO_LENGTH = 160
        const val MAX_NAME_LENGTH = 30

        val EMPTY = UserProfile(
            name = "",
            ageRange = AgeRange.AGE_25_34,
            city = SupportedCities.DEFAULT.name,
            bio = "",
            interests = emptyList(),
        )
    }
}

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val notificationsEnabled: Boolean = true,
)
