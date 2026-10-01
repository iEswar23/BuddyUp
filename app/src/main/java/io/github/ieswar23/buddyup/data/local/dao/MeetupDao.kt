package io.github.ieswar23.buddyup.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import io.github.ieswar23.buddyup.data.local.entity.MeetupEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MeetupDao {

    @Query("SELECT * FROM meetups ORDER BY startsAt ASC")
    fun observeAll(): Flow<List<MeetupEntity>>

    @Query("SELECT * FROM meetups")
    suspend fun getAll(): List<MeetupEntity>

    @Query("SELECT * FROM meetups WHERE id = :id")
    suspend fun getById(id: String): MeetupEntity?

    @Upsert
    suspend fun upsertAll(meetups: List<MeetupEntity>)

    @Query("UPDATE meetups SET isJoined = :joined, attendeeCount = :attendeeCount WHERE id = :id")
    suspend fun updateAttendance(id: String, joined: Boolean, attendeeCount: Int)

    /** Atomically flips the joined flag and adjusts the attendee count; respects capacity. */
    @Transaction
    suspend fun toggleJoin(id: String): Boolean {
        val meetup = getById(id) ?: return false
        return if (meetup.isJoined) {
            updateAttendance(id, joined = false, attendeeCount = (meetup.attendeeCount - 1).coerceAtLeast(0))
            false
        } else {
            if (meetup.attendeeCount >= meetup.capacity) return false
            updateAttendance(id, joined = true, attendeeCount = meetup.attendeeCount + 1)
            true
        }
    }
}
