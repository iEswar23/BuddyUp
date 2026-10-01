package io.github.ieswar23.buddyup.domain.model

data class City(val name: String, val country: String, val latitude: Double, val longitude: Double)

/** BuddyUp is live in a handful of launch cities; distance is measured from the user's city centre. */
object SupportedCities {
    val all: List<City> = listOf(
        City("Bengaluru", "India", 12.9716, 77.5946),
        City("Hyderabad", "India", 17.3850, 78.4867),
        City("Chicago", "USA", 41.8781, -87.6298),
        City("Austin", "USA", 30.2672, -97.7431),
    )

    val DEFAULT: City = all.first()

    fun byName(name: String?): City = all.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: DEFAULT
}
