package io.github.ieswar23.buddyup.fakes

import io.github.ieswar23.buddyup.domain.model.AgeRange
import io.github.ieswar23.buddyup.domain.model.Meetup
import io.github.ieswar23.buddyup.domain.model.MeetupCategory
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
    city: String = "Bengaluru",
    neighborhood: String = "Indiranagar",
    bio: String = "Hello there",
) = Person(
    id = id,
    name = name,
    age = age,
    city = city,
    neighborhood = neighborhood,
    occupation = "Designer",
    bio = bio,
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

fun meetup(
    id: String,
    title: String,
    category: MeetupCategory,
    city: String = "Bengaluru",
    startsInMinutes: Long = 24 * 60,
    attendeeCount: Int = 10,
    capacity: Int = 20,
    isJoined: Boolean = false,
) = Meetup(
    id = id,
    title = title,
    emoji = category.emoji,
    category = category,
    description = "A friendly meetup",
    city = city,
    venue = "Cubbon Park",
    host = "BuddyUp",
    startsAt = NOW + TimeUnit.MINUTES.toMillis(startsInMinutes),
    durationMinutes = 90,
    attendeeCount = attendeeCount,
    capacity = capacity,
    isJoined = isJoined,
    tags = emptyList(),
)
