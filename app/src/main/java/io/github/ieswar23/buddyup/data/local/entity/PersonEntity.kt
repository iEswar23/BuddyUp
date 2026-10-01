package io.github.ieswar23.buddyup.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "people")
data class PersonEntity(
    @PrimaryKey val id: String,
    val name: String,
    val age: Int,
    val city: String,
    val neighborhood: String,
    val latitude: Double,
    val longitude: Double,
    val occupation: String,
    val bio: String,
    val interests: List<String>,
    val lastActiveAt: Long,
    val wavesBack: Boolean,
)
