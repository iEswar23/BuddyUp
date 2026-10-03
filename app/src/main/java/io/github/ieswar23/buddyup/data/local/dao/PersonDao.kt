package io.github.ieswar23.buddyup.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import io.github.ieswar23.buddyup.data.local.entity.PersonEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonDao {

    /** People the user hasn't interacted with (or blocked) yet. */
    @Query(
        """
        SELECT * FROM people
        WHERE id NOT IN (SELECT personId FROM connections)
            AND id NOT IN (SELECT personId FROM blocked_people)
        ORDER BY lastActiveAt DESC
        """
    )
    fun observeUnseen(): Flow<List<PersonEntity>>

    @Query("SELECT * FROM people WHERE id = :id")
    suspend fun getById(id: String): PersonEntity?

    @Query("SELECT * FROM people WHERE id = :id")
    fun observeById(id: String): Flow<PersonEntity?>

    @Query("SELECT COUNT(*) FROM people")
    suspend fun count(): Int

    @Upsert
    suspend fun upsertAll(people: List<PersonEntity>)

    @Query("UPDATE people SET lastActiveAt = :timestamp WHERE id = :id")
    suspend fun touchLastActive(id: String, timestamp: Long)
}
