package io.github.ieswar23.buddyup.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "messages", indices = [Index("friendId"), Index("sentAt")])
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val friendId: String,
    val text: String,
    val sentAt: Long,
    val fromMe: Boolean,
    val isRead: Boolean,
)
