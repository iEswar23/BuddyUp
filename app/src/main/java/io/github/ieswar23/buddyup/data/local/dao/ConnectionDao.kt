package io.github.ieswar23.buddyup.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import io.github.ieswar23.buddyup.data.local.entity.ConnectionEntity
import io.github.ieswar23.buddyup.data.local.entity.FriendRow
import io.github.ieswar23.buddyup.data.local.entity.PersonWithConnection
import io.github.ieswar23.buddyup.domain.model.ConnectionState
import kotlinx.coroutines.flow.Flow

@Dao
interface ConnectionDao {

    @Query(
        """
        SELECT people.*,
            connections.personId AS c_personId,
            connections.state AS c_state,
            connections.note AS c_note,
            connections.updatedAt AS c_updatedAt,
            connections.friendsSince AS c_friendsSince
        FROM people INNER JOIN connections ON connections.personId = people.id
        WHERE connections.state = :state
        ORDER BY connections.updatedAt DESC
        """
    )
    fun observeByState(state: ConnectionState): Flow<List<PersonWithConnection>>

    @Query("SELECT COUNT(*) FROM connections WHERE state = :state")
    fun observeCount(state: ConnectionState): Flow<Int>

    @Query(
        """
        SELECT people.*,
            connections.personId AS c_personId,
            connections.state AS c_state,
            connections.note AS c_note,
            connections.updatedAt AS c_updatedAt,
            connections.friendsSince AS c_friendsSince,
            (SELECT text FROM messages WHERE friendId = people.id ORDER BY sentAt DESC, id DESC LIMIT 1) AS lastMessage,
            (SELECT sentAt FROM messages WHERE friendId = people.id ORDER BY sentAt DESC, id DESC LIMIT 1) AS lastMessageAt,
            (SELECT fromMe FROM messages WHERE friendId = people.id ORDER BY sentAt DESC, id DESC LIMIT 1) AS lastMessageFromMe,
            (SELECT COUNT(*) FROM messages WHERE friendId = people.id AND fromMe = 0 AND isRead = 0) AS unreadCount
        FROM people INNER JOIN connections ON connections.personId = people.id
        WHERE connections.state = 'FRIEND'
        """
    )
    fun observeFriends(): Flow<List<FriendRow>>

    @Query("SELECT * FROM connections WHERE personId = :personId")
    suspend fun get(personId: String): ConnectionEntity?

    @Query("SELECT COUNT(*) FROM connections")
    suspend fun count(): Int

    @Upsert
    suspend fun upsert(connection: ConnectionEntity)

    @Upsert
    suspend fun upsertAll(connections: List<ConnectionEntity>)

    @Query("DELETE FROM connections WHERE personId = :personId")
    suspend fun delete(personId: String)

    @Query("DELETE FROM connections WHERE state = :state")
    suspend fun deleteByState(state: ConnectionState)
}
