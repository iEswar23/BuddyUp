package io.github.ieswar23.buddyup.ui

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import io.github.ieswar23.buddyup.MainDispatcherRule
import io.github.ieswar23.buddyup.domain.model.ChatMessage
import io.github.ieswar23.buddyup.fakes.FakeChatRepository
import io.github.ieswar23.buddyup.fakes.FakePeopleRepository
import io.github.ieswar23.buddyup.fakes.FakeUserRepository
import io.github.ieswar23.buddyup.fakes.NOW
import io.github.ieswar23.buddyup.fakes.minutesAgo
import io.github.ieswar23.buddyup.fakes.person
import io.github.ieswar23.buddyup.ui.chat.ChatItem
import io.github.ieswar23.buddyup.ui.chat.ChatViewModel
import io.github.ieswar23.buddyup.ui.navigation.Screen
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val kavya = person("p03", name = "Kavya Iyer", interests = listOf("Reading", "Coffee"))
    private val unreadReply = ChatMessage(1, "p03", "Want to come to the bookstore?", minutesAgo(10), fromMe = false, isRead = false)

    private lateinit var chatRepository: FakeChatRepository

    private fun TestScope.createViewModel(): ChatViewModel {
        chatRepository = FakeChatRepository(mapOf("p03" to listOf(unreadReply)))
        val vm = ChatViewModel(
            savedStateHandle = SavedStateHandle(mapOf(Screen.Chat.ARG_FRIEND_ID to "p03")),
            chatRepository = chatRepository,
            peopleRepository = FakePeopleRepository(listOf(kavya)),
            userRepository = FakeUserRepository(),
            timeProvider = { NOW },
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        return vm
    }

    @Test
    fun `loads friend, groups messages and marks conversation read`() = runTest {
        val vm = createViewModel()
        val state = vm.uiState.value

        assertThat(state.friend?.name).isEqualTo("Kavya Iyer")
        assertThat(state.items.first()).isInstanceOf(ChatItem.DayHeader::class.java)
        assertThat(state.sharedInterests).containsExactly("Coffee")
        assertThat(chatRepository.markedRead).contains("p03")
    }

    @Test
    fun `sending a message appends it and clears the input`() = runTest {
        val vm = createViewModel()
        vm.onInputChange("  Sounds great, see you Saturday!  ")
        vm.send()

        assertThat(vm.input.value).isEmpty()
        val bubbles = vm.uiState.value.items.filterIsInstance<ChatItem.Bubble>()
        assertThat(bubbles.last().message.text).isEqualTo("Sounds great, see you Saturday!")
        assertThat(bubbles.last().message.fromMe).isTrue()
    }

    @Test
    fun `blank messages are not sent`() = runTest {
        val vm = createViewModel()
        vm.onInputChange("   ")
        vm.send()

        assertThat(chatRepository.conversations.value["p03"]).hasSize(1)
    }

    @Test
    fun `typing indicator follows the repository and suggestions appear after their message`() = runTest {
        val vm = createViewModel()
        assertThat(vm.uiState.value.isTyping).isFalse()
        assertThat(vm.uiState.value.suggestions).isNotEmpty()

        chatRepository.setTyping(setOf("p03"))
        assertThat(vm.uiState.value.isTyping).isTrue()

        vm.onInputChange("Yes!")
        vm.send()
        assertThat(vm.uiState.value.suggestions).isEmpty()
    }
}
