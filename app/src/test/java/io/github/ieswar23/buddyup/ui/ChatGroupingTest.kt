package io.github.ieswar23.buddyup.ui

import com.google.common.truth.Truth.assertThat
import io.github.ieswar23.buddyup.domain.model.ChatMessage
import io.github.ieswar23.buddyup.fakes.NOW
import io.github.ieswar23.buddyup.fakes.minutesAgo
import io.github.ieswar23.buddyup.ui.chat.ChatGrouping
import io.github.ieswar23.buddyup.ui.chat.ChatItem
import org.junit.Test

class ChatGroupingTest {

    private fun msg(id: Long, minutesAgo: Long, fromMe: Boolean) =
        ChatMessage(id, "p1", "m$id", minutesAgo(minutesAgo), fromMe, isRead = true)

    @Test
    fun `inserts a day header whenever the calendar day changes`() {
        val items = ChatGrouping.build(
            listOf(msg(1, 60L * 24 * 3, true), msg(2, 2, false), msg(3, 1, true)),
            NOW,
        )
        val headers = items.filterIsInstance<ChatItem.DayHeader>()
        assertThat(headers).hasSize(2)
        assertThat(headers.last().label).isEqualTo("Today")
        assertThat(items.first()).isInstanceOf(ChatItem.DayHeader::class.java)
    }

    @Test
    fun `consecutive messages from the same sender are grouped`() {
        val bubbles = ChatGrouping.build(
            listOf(msg(1, 3, false), msg(2, 2, false), msg(3, 1, true)),
            NOW,
        ).filterIsInstance<ChatItem.Bubble>()
        assertThat(bubbles[0].isFirstInGroup).isTrue()
        assertThat(bubbles[0].isLastInGroup).isFalse()
        assertThat(bubbles[1].isFirstInGroup).isFalse()
        assertThat(bubbles[1].isLastInGroup).isTrue()
        assertThat(bubbles[2].isFirstInGroup).isTrue()
        assertThat(bubbles[2].isLastInGroup).isTrue()
    }
}
