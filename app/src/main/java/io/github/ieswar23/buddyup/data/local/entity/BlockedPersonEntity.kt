package io.github.ieswar23.buddyup.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import io.github.ieswar23.buddyup.domain.model.ReportReason

/**
 * A person the user blocked, with the optional report ([reason] plus free-text [note]). Kept
 * separate from `connections` so the relationship (and chat history) is untouched and comes back
 * if the user unblocks.
 */
@Entity(tableName = "blocked_people")
data class BlockedPersonEntity(
    @PrimaryKey val personId: String,
    val reason: ReportReason?,
    val note: String?,
    val blockedAt: Long,
)

/** A blocked person joined with their profile (block columns prefixed with `b_`). */
data class BlockedPersonRow(
    @Embedded val person: PersonEntity,
    @Embedded(prefix = "b_") val block: BlockedPersonEntity,
)
