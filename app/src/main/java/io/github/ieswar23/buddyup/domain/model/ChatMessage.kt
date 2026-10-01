package io.github.ieswar23.buddyup.domain.model

data class ChatMessage(
    val id: Long,
    val friendId: String,
    val text: String,
    val sentAt: Long,
    val fromMe: Boolean,
    val isRead: Boolean,
)
