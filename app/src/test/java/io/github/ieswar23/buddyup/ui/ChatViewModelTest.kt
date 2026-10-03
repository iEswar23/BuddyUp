package io.github.ieswar23.buddyup.ui

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import io.github.ieswar23.buddyup.MainDispatcherRule
import io.github.ieswar23.buddyup.domain.icebreakers.Icebreaker
import io.github.ieswar23.buddyup.domain.icebreakers.IcebreakerGenerator
import io.github.ieswar23.buddyup.domain.model.ChatMessage
import io.github.ieswar23.buddyup.domain.model.MeetupCategory
import io.github.ieswar23.buddyup.domain.model.Report
import io.github.ieswar23.buddyup.domain.model.ReportReason
import io.github.ieswar23.buddyup.fakes.FakeChatRepository
import io.github.ieswar23.buddyup.fakes.FakeMeetupRepository
import io.github.ieswar23.buddyup.fakes.FakePeopleRepository
import io.github.ieswar23.buddyup.fakes.FakeSafetyRepository
import io.github.ieswar23.buddyup.fakes.FakeUserRepository
import io.github.ieswar23.buddyup.fakes.NOW
import io.github.ieswar23.buddyup.fakes.meetup
import io.github.ieswar23.buddyup.fakes.minutesAgo
import io.github.ieswar23.buddyup.fakes.person
import io.github.ieswar23.buddyup.fakes.testProfile
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
    private val coffeeWalk = meetup("e01", "Sunrise Coffee Walk", MeetupCategory.COFFEE)
    private val pastCoffeeWalk = meetup("e00", "Last Week's Coffee Walk", MeetupCategory.COFFEE, startsInMinutes = -7 * 24 * 60)

    private lateinit var chatRepository: FakeChatRepository
    private lateinit var safetyRepository: FakeSafetyRepository

    private fun TestScope.createViewModel(conversation: List<ChatMessage> = listOf(unreadReply)): ChatViewModel {
        chatRepository = FakeChatRepository(mapOf("p03" to conversation))
        safetyRepository = FakeSafetyRepository(listOf(kavya))
        val vm = ChatViewModel(
            savedStateHandle = SavedStateHandle(mapOf(Screen.Chat.ARG_FRIEND_ID to "p03")),
            chatRepository = chatRepository,
            peopleRepository = FakePeopleRepository(listOf(kavya)),
            userRepository = FakeUserRepository(),
            meetupRepository = FakeMeetupRepository(listOf(pastCoffeeWalk, coffeeWalk)),
            safetyRepository = safetyRepository,
            icebreakerGenerator = IcebreakerGenerator(),
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

    @Test
    fun `an empty chat offers icebreakers from shared interests instead of quick replies`() = runTest {
        val vm = createViewModel(conversation = emptyList())
        val state = vm.uiState.value

        assertThat(state.isNewConversation).isTrue()
        assertThat(state.icebreakers).hasSize(3)
        assertThat(state.icebreakers.first().source).isEqualTo(Icebreaker.Source.SHARED_INTEREST)
        assertThat(state.icebreakers.first().interest).isEqualTo("Coffee")
        assertThat(state.canRefreshIcebreakers).isTrue()
        assertThat(state.suggestions).isEmpty()
    }

    @Test
    fun `icebreakers invite them to an upcoming meetup that matches a shared interest`() = runTest {
        val vm = createViewModel(conversation = emptyList())

        val meetupStarter = vm.uiState.value.icebreakers.single { it.source == Icebreaker.Source.MEETUP }

        assertThat(meetupStarter.text).contains("Sunrise Coffee Walk")
        assertThat(vm.uiState.value.icebreakers.none { "Last Week" in it.text }).isTrue()
    }

    @Test
    fun `a short chat shows the icebreaker prompt and a quiet one opens the row`() = runTest {
        val short = createViewModel(conversation = listOf(unreadReply))
        assertThat(short.uiState.value.isNewConversation).isFalse()
        assertThat(short.uiState.value.icebreakersExpanded).isFalse()
        assertThat(short.uiState.value.showIcebreakerPrompt).isTrue()
        assertThat(short.uiState.value.icebreakers).hasSize(3)
        assertThat(short.uiState.value.suggestions).isNotEmpty()

        val dayAndAHalfAgo = ChatMessage(1, "p03", "Nice meeting you!", minutesAgo(36 * 60), fromMe = true, isRead = true)
        val quiet = createViewModel(conversation = listOf(dayAndAHalfAgo))
        assertThat(quiet.uiState.value.icebreakersExpanded).isTrue()
        assertThat(quiet.uiState.value.showIcebreakerPrompt).isFalse()
    }

    @Test
    fun `a chat in full swing no longer offers icebreakers`() = runTest {
        val busy = (1..ChatViewModel.SHORT_CONVERSATION_MESSAGES).map { index ->
            ChatMessage(index.toLong(), "p03", "Message $index", minutesAgo(60L - index), fromMe = index % 2 == 0, isRead = true)
        }
        val vm = createViewModel(conversation = busy)

        assertThat(vm.uiState.value.icebreakers).isEmpty()
        assertThat(vm.uiState.value.showIcebreakerPrompt).isFalse()
        vm.showIcebreakers()
        assertThat(vm.uiState.value.icebreakersExpanded).isFalse()
    }

    @Test
    fun `opening the icebreaker row hides quick replies and sending closes it again`() = runTest {
        val vm = createViewModel()

        vm.showIcebreakers()
        assertThat(vm.uiState.value.icebreakersExpanded).isTrue()
        assertThat(vm.uiState.value.showIcebreakerPrompt).isFalse()
        assertThat(vm.uiState.value.suggestions).isEmpty()

        vm.hideIcebreakers()
        assertThat(vm.uiState.value.icebreakersExpanded).isFalse()
        assertThat(vm.uiState.value.suggestions).isNotEmpty()

        vm.showIcebreakers()
        vm.onInputChange("Sure, which one?")
        vm.send()
        assertThat(vm.uiState.value.icebreakersExpanded).isFalse()
    }

    @Test
    fun `quick replies are sent straight away`() = runTest {
        val vm = createViewModel()

        vm.sendSuggestion("Hey Kavya! 👋")

        assertThat(chatRepository.conversations.value["p03"]?.last()?.text).isEqualTo("Hey Kavya! 👋")
        assertThat(vm.input.value).isEmpty()
    }

    @Test
    fun `refreshing cycles through every icebreaker set and wraps around`() = runTest {
        val vm = createViewModel(conversation = emptyList())
        val first = vm.uiState.value.icebreakers
        val setCount = IcebreakerGenerator().setCount(testProfile, kavya, listOf(coffeeWalk))
        assertThat(setCount).isGreaterThan(1)

        val seen = mutableListOf(first)
        repeat(setCount - 1) {
            vm.refreshIcebreakers()
            seen += vm.uiState.value.icebreakers
        }
        assertThat(seen.toSet()).hasSize(setCount)
        assertThat(seen.all { it.size == 3 }).isTrue()

        vm.refreshIcebreakers()
        assertThat(vm.uiState.value.icebreakers).isEqualTo(first)
    }

    @Test
    fun `picking an icebreaker fills the composer without sending it`() = runTest {
        val vm = createViewModel(conversation = emptyList())
        val icebreaker = vm.uiState.value.icebreakers.first()

        vm.useIcebreaker(icebreaker.text)

        assertThat(vm.input.value).isEqualTo(icebreaker.text)
        assertThat(chatRepository.conversations.value["p03"]).isEmpty()

        vm.onInputChange(icebreaker.text + " Would love tips!")
        vm.send()
        val sent = vm.uiState.value.items.filterIsInstance<ChatItem.Bubble>().single().message
        assertThat(sent.text).isEqualTo(icebreaker.text + " Would love tips!")
        assertThat(vm.uiState.value.isNewConversation).isFalse()
        assertThat(vm.uiState.value.icebreakersExpanded).isFalse()
    }

    @Test
    fun `blocking files the report and marks the chat as blocked`() = runTest {
        val vm = createViewModel()
        assertThat(vm.uiState.value.isBlocked).isFalse()

        val report = Report(ReportReason.SPAM, note = "Promoting a paid course")
        vm.block(report)

        assertThat(safetyRepository.blocks.value).containsExactly("p03", report)
        assertThat(vm.uiState.value.isBlocked).isTrue()
    }
}
