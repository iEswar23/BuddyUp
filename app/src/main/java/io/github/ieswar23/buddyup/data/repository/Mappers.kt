package io.github.ieswar23.buddyup.data.repository

import io.github.ieswar23.buddyup.data.local.entity.FriendRow
import io.github.ieswar23.buddyup.data.local.entity.MeetupEntity
import io.github.ieswar23.buddyup.data.local.entity.MessageEntity
import io.github.ieswar23.buddyup.data.local.entity.PersonEntity
import io.github.ieswar23.buddyup.data.local.entity.PersonWithConnection
import io.github.ieswar23.buddyup.data.remote.dto.MeetupDto
import io.github.ieswar23.buddyup.data.remote.dto.PersonDto
import io.github.ieswar23.buddyup.domain.model.ChatMessage
import io.github.ieswar23.buddyup.domain.model.City
import io.github.ieswar23.buddyup.domain.model.ConnectionState
import io.github.ieswar23.buddyup.domain.model.FriendRequest
import io.github.ieswar23.buddyup.domain.model.FriendSummary
import io.github.ieswar23.buddyup.domain.model.Meetup
import io.github.ieswar23.buddyup.domain.model.MeetupCategory
import io.github.ieswar23.buddyup.domain.model.Person
import io.github.ieswar23.buddyup.domain.model.RequestDirection
import io.github.ieswar23.buddyup.util.GeoDistance
import java.util.Calendar
import java.util.concurrent.TimeUnit

fun PersonEntity.toDomain(origin: City): Person = Person(
    id = id,
    name = name,
    age = age,
    city = city,
    neighborhood = neighborhood,
    occupation = occupation,
    bio = bio,
    interests = interests,
    distanceKm = GeoDistance.haversineKm(origin.latitude, origin.longitude, latitude, longitude),
    lastActiveAt = lastActiveAt,
    wavesBack = wavesBack,
)

fun PersonWithConnection.toRequest(origin: City): FriendRequest = FriendRequest(
    person = person.toDomain(origin),
    direction = if (connection.state == ConnectionState.WAVE_SENT) RequestDirection.OUTGOING else RequestDirection.INCOMING,
    note = connection.note,
    createdAt = connection.updatedAt,
)

fun FriendRow.toDomain(origin: City): FriendSummary = FriendSummary(
    person = person.toDomain(origin),
    friendsSince = connection.friendsSince ?: connection.updatedAt,
    lastMessage = lastMessage,
    lastMessageAt = lastMessageAt,
    lastMessageFromMe = lastMessageFromMe ?: false,
    unreadCount = unreadCount,
)

fun MessageEntity.toDomain(): ChatMessage = ChatMessage(
    id = id,
    friendId = friendId,
    text = text,
    sentAt = sentAt,
    fromMe = fromMe,
    isRead = isRead,
)

fun MeetupEntity.toDomain(): Meetup = Meetup(
    id = id,
    title = title,
    emoji = emoji,
    category = category,
    description = description,
    city = city,
    venue = venue,
    host = host,
    startsAt = startsAt,
    durationMinutes = durationMinutes,
    attendeeCount = attendeeCount,
    capacity = capacity,
    isJoined = isJoined,
    tags = tags,
)

fun PersonDto.toEntity(now: Long): PersonEntity = PersonEntity(
    id = id,
    name = name,
    age = age,
    city = city,
    neighborhood = neighborhood,
    latitude = latitude,
    longitude = longitude,
    occupation = occupation,
    bio = bio,
    interests = interests,
    lastActiveAt = now - TimeUnit.MINUTES.toMillis(lastActiveMinutesAgo.toLong()),
    wavesBack = wavesBack,
)

/** Resolves a relative "in N days at HH:mm" schedule into an absolute timestamp. */
fun MeetupDto.toEntity(now: Long): MeetupEntity {
    val (hour, minute) = startTime.split(":").let { parts ->
        (parts.getOrNull(0)?.toIntOrNull() ?: 9) to (parts.getOrNull(1)?.toIntOrNull() ?: 0)
    }
    val startsAt = Calendar.getInstance().apply {
        timeInMillis = now
        add(Calendar.DAY_OF_YEAR, dayOffset)
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    return MeetupEntity(
        id = id,
        title = title,
        emoji = emoji,
        category = MeetupCategory.fromKey(category),
        description = description,
        city = city,
        venue = venue,
        host = host,
        startsAt = startsAt,
        durationMinutes = durationMinutes,
        attendeeCount = attendeeCount,
        capacity = capacity,
        isJoined = joined,
        tags = tags,
    )
}
