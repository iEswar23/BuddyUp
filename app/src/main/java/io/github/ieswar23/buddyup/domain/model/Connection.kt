package io.github.ieswar23.buddyup.domain.model

/** Relationship between the current user and another [Person]. No row means "not seen yet". */
enum class ConnectionState {
    /** The user skipped this person in Discover. */
    PASSED,

    /** The user waved; waiting on the other side. */
    WAVE_SENT,

    /** The other person waved at the user. */
    WAVE_RECEIVED,

    /** Both sides waved — they can chat. */
    FRIEND,

    /** The user declined an incoming wave. */
    DECLINED,
}

enum class RequestDirection { INCOMING, OUTGOING }

data class FriendRequest(
    val person: Person,
    val direction: RequestDirection,
    val note: String?,
    val createdAt: Long,
)

data class FriendSummary(
    val person: Person,
    val friendsSince: Long,
    val lastMessage: String?,
    val lastMessageAt: Long?,
    val lastMessageFromMe: Boolean,
    val unreadCount: Int,
) {
    /** Used for "recent activity" ordering in the friends list. */
    val lastActivityAt: Long get() = maxOf(lastMessageAt ?: 0L, friendsSince)
}
