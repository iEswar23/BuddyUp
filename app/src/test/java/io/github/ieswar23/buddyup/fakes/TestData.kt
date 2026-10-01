package io.github.ieswar23.buddyup.fakes

import io.github.ieswar23.buddyup.domain.model.AgeRange
import io.github.ieswar23.buddyup.domain.model.Person
import io.github.ieswar23.buddyup.domain.model.UserProfile
import java.util.concurrent.TimeUnit

const val NOW = 1_780_000_000_000L

fun minutesAgo(minutes: Long): Long = NOW - TimeUnit.MINUTES.toMillis(minutes)

fun person(
    id: String,
    name: String = "Test Person $id",
    age: Int = 28,
    distanceKm: Double = 3.0,
    interests: List<String> = listOf("Hiking", "Coffee"),
    lastActiveMinutesAgo: Long = 5,
    wavesBack: Boolean = false,
) = Person(
    id = id,
    name = name,
    age = age,
    city = "Bengaluru",
    neighborhood = "Indiranagar",
    occupation = "Designer",
    bio = "Hello there",
    interests = interests,
    distanceKm = distanceKm,
    lastActiveAt = minutesAgo(lastActiveMinutesAgo),
    wavesBack = wavesBack,
)

val testProfile = UserProfile(
    name = "Chandra",
    ageRange = AgeRange.AGE_25_34,
    city = "Bengaluru",
    bio = "Weekend trekker",
    interests = listOf("Hiking", "Coffee", "Chess", "Photography"),
)
