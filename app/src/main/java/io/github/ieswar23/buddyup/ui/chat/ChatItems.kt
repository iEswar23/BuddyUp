package io.github.ieswar23.buddyup.ui.chat

import io.github.ieswar23.buddyup.domain.model.ChatMessage
import io.github.ieswar23.buddyup.util.Formatters
import java.util.concurrent.TimeUnit

/** Rows rendered by the chat list: day separators and message bubbles. */
sealed interface ChatItem {
    val key: String

    data class DayHeader(val label: String, val dayStart: Long) : ChatItem {
        override val key: String get() = "day-$dayStart"
    }

    data class Bubble(
        val message: ChatMessage,
        /** First bubble of a run of consecutive messages from the same sender. */
        val isFirstInGroup: Boolean,
        /** Last bubble of the run — shows the timestamp and the "tail" corner. */
        val isLastInGroup: Boolean,
    ) : ChatItem {
        override val key: String get() = "msg-${message.id}"
    }
}

object ChatGrouping {
    /** Messages from the same sender within this window are visually grouped. */
    private val GROUP_WINDOW_MS = TimeUnit.MINUTES.toMillis(5)

    fun build(messages: List<ChatMessage>, now: Long): List<ChatItem> {
        if (messages.isEmpty()) return emptyList()
        val sorted = messages.sortedWith(compareBy<ChatMessage> { it.sentAt }.thenBy { it.id })
        val result = mutableListOf<ChatItem>()
        var currentDay = Long.MIN_VALUE
        sorted.forEachIndexed { index, message ->
            val day = Formatters.startOfDay(message.sentAt)
            if (day != currentDay) {
                currentDay = day
                result += ChatItem.DayHeader(Formatters.dayHeader(message.sentAt, now), day)
            }
            val previous = sorted.getOrNull(index - 1)
            val next = sorted.getOrNull(index + 1)
            result += ChatItem.Bubble(
                message = message,
                isFirstInGroup = previous == null || !sameGroup(previous, message),
                isLastInGroup = next == null || !sameGroup(message, next),
            )
        }
        return result
    }

    private fun sameGroup(a: ChatMessage, b: ChatMessage): Boolean =
        a.fromMe == b.fromMe &&
            b.sentAt - a.sentAt <= GROUP_WINDOW_MS &&
            Formatters.startOfDay(a.sentAt) == Formatters.startOfDay(b.sentAt)
}
