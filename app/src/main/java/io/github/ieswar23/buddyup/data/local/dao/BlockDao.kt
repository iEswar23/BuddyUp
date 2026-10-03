package io.github.ieswar23.buddyup.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import io.github.ieswar23.buddyup.data.local.entity.BlockedPersonEntity
import io.github.ieswar23.buddyup.data.local.entity.BlockedPersonRow
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockDao {

    @Query(
        """
        SELECT people.*,
            blocked_people.personId AS b_personId,
            blocked_people.reason AS b_reason,
            blocked_people.note AS b_note,
            blocked_people.blockedAt AS b_blockedAt
        FROM people INNER JOIN blocked_people ON blocked_people.personId = people.id
        ORDER BY blocked_people.blockedAt DESC
        """
    )
    fun observeBlocked(): Flow<List<BlockedPersonRow>>

    @Query("SELECT personId FROM blocked_people")
    fun observeBlockedIds(): Flow<List<String>>

    @Query("SELECT EXISTS(SELECT 1 FROM blocked_people WHERE personId = :personId)")
    suspend fun isBlocked(personId: String): Boolean

    @Upsert
    suspend fun upsert(block: BlockedPersonEntity)

    @Query("DELETE FROM blocked_people WHERE personId = :personId")
    suspend fun delete(personId: String)
}
