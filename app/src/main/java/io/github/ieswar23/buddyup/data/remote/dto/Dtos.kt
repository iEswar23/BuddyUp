package io.github.ieswar23.buddyup.data.remote.dto

import com.google.gson.annotations.SerializedName

data class PersonDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("age") val age: Int,
    @SerializedName("city") val city: String,
    @SerializedName("neighborhood") val neighborhood: String,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("occupation") val occupation: String,
    @SerializedName("bio") val bio: String,
    @SerializedName("interests") val interests: List<String>,
    @SerializedName("lastActiveMinutesAgo") val lastActiveMinutesAgo: Int,
    @SerializedName("wavesBack") val wavesBack: Boolean,
)

data class IncomingRequestDto(
    @SerializedName("personId") val personId: String,
    @SerializedName("note") val note: String?,
    @SerializedName("minutesAgo") val minutesAgo: Int,
)

data class FriendshipDto(
    @SerializedName("personId") val personId: String,
    @SerializedName("friendsSinceDays") val friendsSinceDays: Int,
    @SerializedName("messages") val messages: List<MessageDto>,
)

data class MessageDto(
    @SerializedName("fromMe") val fromMe: Boolean,
    @SerializedName("text") val text: String,
    @SerializedName("minutesAgo") val minutesAgo: Int,
    @SerializedName("read") val read: Boolean,
)

data class MeetupDto(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("emoji") val emoji: String,
    @SerializedName("category") val category: String,
    @SerializedName("city") val city: String,
    @SerializedName("venue") val venue: String,
    @SerializedName("host") val host: String,
    @SerializedName("dayOffset") val dayOffset: Int,
    @SerializedName("startTime") val startTime: String,
    @SerializedName("durationMinutes") val durationMinutes: Int,
    @SerializedName("attendeeCount") val attendeeCount: Int,
    @SerializedName("capacity") val capacity: Int,
    @SerializedName("joined") val joined: Boolean,
    @SerializedName("description") val description: String,
    @SerializedName("tags") val tags: List<String>,
)
