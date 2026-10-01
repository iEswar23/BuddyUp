package io.github.ieswar23.buddyup.data.local.entity

import androidx.room.Embedded

/** A person joined with their connection row (columns prefixed with `c_`). */
data class PersonWithConnection(
    @Embedded val person: PersonEntity,
    @Embedded(prefix = "c_") val connection: ConnectionEntity,
)

/** A friend row enriched with the latest message and unread count for the friends list. */
data class FriendRow(
    @Embedded val person: PersonEntity,
    @Embedded(prefix = "c_") val connection: ConnectionEntity,
    val lastMessage: String?,
    val lastMessageAt: Long?,
    val lastMessageFromMe: Boolean?,
    val unreadCount: Int,
)
