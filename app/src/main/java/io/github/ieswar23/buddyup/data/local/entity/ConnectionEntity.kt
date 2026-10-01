package io.github.ieswar23.buddyup.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import io.github.ieswar23.buddyup.domain.model.ConnectionState

/**
 * The user's relationship with a person. People without a row are "new" and eligible for Discover.
 */
@Entity(tableName = "connections")
data class ConnectionEntity(
    @PrimaryKey val personId: String,
    val state: ConnectionState,
    val note: String?,
    val updatedAt: Long,
    val friendsSince: Long?,
)
