package io.github.ieswar23.buddyup.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import io.github.ieswar23.buddyup.domain.model.MeetupCategory

@Entity(tableName = "meetups")
data class MeetupEntity(
    @PrimaryKey val id: String,
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
)
