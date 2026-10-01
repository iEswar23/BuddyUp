package io.github.ieswar23.buddyup.domain.model

data class DiscoverFilters(
    val maxDistanceKm: Int = DEFAULT_DISTANCE_KM,
    val minAge: Int = MIN_AGE,
    val maxAge: Int = MAX_AGE,
    val interests: Set<String> = emptySet(),
) {
    /** The slider's top stop means "no distance limit". */
    val isAnywhere: Boolean get() = maxDistanceKm >= ANYWHERE_KM

    val isDefault: Boolean get() = this == DiscoverFilters()

    val activeCount: Int
        get() = listOf(
            maxDistanceKm != DEFAULT_DISTANCE_KM,
            minAge != MIN_AGE || maxAge != MAX_AGE,
            interests.isNotEmpty(),
        ).count { it }

    fun matches(person: Person): Boolean {
        if (!isAnywhere && person.distanceKm > maxDistanceKm) return false
        if (person.age !in minAge..maxAge) return false
        if (interests.isNotEmpty() && person.interests.none { it in interests }) return false
        return true
    }

    companion object {
        const val DEFAULT_DISTANCE_KM = 50
        const val MIN_DISTANCE_KM = 5
        const val ANYWHERE_KM = 100
        const val MIN_AGE = 18
        const val MAX_AGE = 60
    }
}
