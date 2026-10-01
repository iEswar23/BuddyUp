package io.github.ieswar23.buddyup.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import io.github.ieswar23.buddyup.data.local.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    @Query("SELECT * FROM messages WHERE friendId = :friendId ORDER BY sentAt ASC, id ASC")
    fun observeConversation(friendId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE friendId = :friendId ORDER BY sentAt DESC, id DESC LIMIT :limit")
    suspend fun recent(friendId: String, limit: Int): List<MessageEntity>

    @Insert
    suspend fun insert(message: MessageEntity): Long

    @Insert
    suspend fun insertAll(messages: List<MessageEntity>)

    @Query("UPDATE messages SET isRead = 1 WHERE friendId = :friendId AND fromMe = 0 AND isRead = 0")
    suspend fun markConversationRead(friendId: String)

    @Query("SELECT COUNT(*) FROM messages WHERE fromMe = 0 AND isRead = 0")
    fun observeUnreadTotal(): Flow<Int>
}
