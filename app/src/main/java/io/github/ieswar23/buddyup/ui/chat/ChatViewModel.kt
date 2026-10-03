package io.github.ieswar23.buddyup.ui.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.ieswar23.buddyup.data.repository.ChatRepository
import io.github.ieswar23.buddyup.data.repository.MeetupRepository
import io.github.ieswar23.buddyup.data.repository.PeopleRepository
import io.github.ieswar23.buddyup.data.repository.SafetyRepository
import io.github.ieswar23.buddyup.data.repository.UserRepository
import io.github.ieswar23.buddyup.domain.icebreakers.Icebreaker
import io.github.ieswar23.buddyup.domain.icebreakers.IcebreakerGenerator
import io.github.ieswar23.buddyup.domain.model.ChatMessage
import io.github.ieswar23.buddyup.domain.model.Person
import io.github.ieswar23.buddyup.domain.model.Report
import io.github.ieswar23.buddyup.domain.model.UserProfile
import io.github.ieswar23.buddyup.domain.scoring.MatchScorer
import io.github.ieswar23.buddyup.ui.navigation.Screen
import io.github.ieswar23.buddyup.util.TimeProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

data class ChatUiState(
    val isLoading: Boolean = true,
    val friend: Person? = null,
    val items: List<ChatItem> = emptyList(),
    val isTyping: Boolean = false,
    val sharedInterests: List<String> = emptyList(),
    /**
     * The current set of conversation starters. Offered in new, short or quiet conversations only;
     * empty once a chat is in full swing.
     */
    val icebreakers: List<Icebreaker> = emptyList(),
    /** Whether refreshing leads to a different set of [icebreakers]. */
    val canRefreshIcebreakers: Boolean = false,
    /**
     * Whether the icebreaker row above the composer is open. It starts open when the chat has gone
     * quiet and closed otherwise; an empty chat shows its icebreakers in the conversation instead.
     */
    val icebreakersExpanded: Boolean = false,
    /** Quick replies (sent as-is), shown after their latest message while the icebreaker row is closed. */
    val suggestions: List<String> = emptyList(),
    /** True once the user blocked this person — the screen closes itself. */
    val isBlocked: Boolean = false,
    val now: Long = 0L,
) {
    val lastMessageKey: String? get() = items.lastOrNull { it is ChatItem.Bubble }?.key

    /** No messages yet: the conversation area itself offers the icebreakers. */
    val isNewConversation: Boolean get() = !isLoading && items.isEmpty()

    /** A short conversation with the row closed: a "Need an icebreaker?" prompt sits above the composer. */
    val showIcebreakerPrompt: Boolean get() = !isNewConversation && !icebreakersExpanded && icebreakers.isNotEmpty()
}

@HiltViewModel
class ChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val chatRepository: ChatRepository,
    peopleRepository: PeopleRepository,
    userRepository: UserRepository,
    meetupRepository: MeetupRepository,
    private val safetyRepository: SafetyRepository,
    private val icebreakerGenerator: IcebreakerGenerator,
    private val timeProvider: TimeProvider,
) : ViewModel() {

    val friendId: String = checkNotNull(savedStateHandle[Screen.Chat.ARG_FRIEND_ID]) {
        "ChatViewModel requires a friendId argument"
    }

    private val _input = MutableStateFlow("")
    val input: StateFlow<String> = _input.asStateFlow()

    /** Which icebreaker set is showing; refreshing moves to the next one. */
    private val icebreakerRound = MutableStateFlow(0)

    /** The user's explicit open/closed choice for the icebreaker row; null follows [isQuiet]. */
    private val icebreakersExpandedOverride = MutableStateFlow<Boolean?>(null)

    private val messages = chatRepository.observeConversation(friendId)
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), replay = 1)

    private val snapshots = combine(
        peopleRepository.observePerson(friendId),
        messages,
        chatRepository.typingFriendIds.map { friendId in it },
        userRepository.profile,
        safetyRepository.observeBlockedIds().map { friendId in it },
    ) { friend, conversation, typing, me, blocked -> ConversationSnapshot(friend, conversation, typing, me, blocked) }

    val uiState: StateFlow<ChatUiState> = combine(
        snapshots,
        meetupRepository.observeMeetups(),
        icebreakerRound,
        icebreakersExpandedOverride,
    ) { snapshot, meetups, round, expandedOverride ->
        val (friend, conversation, typing, me, blocked) = snapshot
        val now = timeProvider.now()
        val shared = friend?.let { MatchScorer.sharedInterests(me.interests, it.interests) }.orEmpty()
        val lastFromThem = conversation.lastOrNull()?.fromMe == false
        val quiet = isQuiet(conversation, now)
        val offersIcebreakers = friend != null && (conversation.size < SHORT_CONVERSATION_MESSAGES || quiet)
        val expanded = offersIcebreakers && conversation.isNotEmpty() && (expandedOverride ?: quiet)
        val upcoming = meetups.filter { it.startsAt > now }
        ChatUiState(
            isLoading = false,
            friend = friend,
            items = ChatGrouping.build(conversation, now),
            isTyping = typing,
            sharedInterests = shared,
            // offersIcebreakers implies friend != null, so friend is smart-cast below.
            icebreakers = if (offersIcebreakers) icebreakerGenerator.generate(me, friend, upcoming, round) else emptyList(),
            canRefreshIcebreakers = offersIcebreakers && icebreakerGenerator.setCount(me, friend, upcoming) > 1,
            icebreakersExpanded = expanded,
            suggestions = if (!expanded && lastFromThem) suggestionsFor(friend, shared) else emptyList(),
            isBlocked = blocked,
            now = now,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChatUiState())

    init {
        // Opening the conversation (and any reply that lands while it's open) marks messages as read.
        messages
            .onEach { conversation ->
                if (conversation.any { !it.fromMe && !it.isRead }) chatRepository.markRead(friendId)
            }
            .launchIn(viewModelScope)
    }

    fun onInputChange(value: String) {
        _input.value = value.take(MAX_MESSAGE_LENGTH)
    }

    fun send() {
        val text = _input.value.trim()
        if (text.isEmpty()) return
        _input.value = ""
        // The conversation is active again, so the icebreaker row goes back to its default (closed).
        icebreakersExpandedOverride.value = null
        viewModelScope.launch { chatRepository.send(friendId, text) }
    }

    /** Quick replies are sent straight away. */
    fun sendSuggestion(text: String) {
        viewModelScope.launch { chatRepository.send(friendId, text) }
    }

    /** Icebreakers go into the composer so the user can edit them before sending. */
    fun useIcebreaker(text: String) {
        onInputChange(text)
    }

    /** Shows the next set of icebreakers, wrapping around to the first after the last. */
    fun refreshIcebreakers() {
        icebreakerRound.update { it + 1 }
    }

    fun showIcebreakers() {
        icebreakersExpandedOverride.value = true
    }

    fun hideIcebreakers() {
        icebreakersExpandedOverride.value = false
    }

    /** Blocks this person, optionally reporting them. The screen closes once the block lands. */
    fun block(report: Report?) {
        viewModelScope.launch { safetyRepository.block(friendId, report) }
    }

    private fun isQuiet(conversation: List<ChatMessage>, now: Long): Boolean {
        val last = conversation.lastOrNull() ?: return true
        return now - last.sentAt >= QUIET_AFTER_MS
    }

    private fun suggestionsFor(friend: Person?, shared: List<String>): List<String> {
        if (friend == null) return emptyList()
        val interest = shared.firstOrNull()?.lowercase()
        return buildList {
            add("Hey ${friend.firstName}! 👋")
            if (interest != null) add("Any $interest plans this weekend?")
            add("Coffee sometime this week? ☕")
            add("How's your week going?")
        }
    }

    private data class ConversationSnapshot(
        val friend: Person?,
        val messages: List<ChatMessage>,
        val isTyping: Boolean,
        val me: UserProfile,
        val isBlocked: Boolean,
    )

    companion object {
        const val MAX_MESSAGE_LENGTH = 1_000

        /** Conversations with fewer messages than this still offer icebreakers. */
        const val SHORT_CONVERSATION_MESSAGES = 6

        /** A conversation with no messages for this long counts as quiet: the icebreaker row starts open. */
        val QUIET_AFTER_MS: Long = TimeUnit.HOURS.toMillis(24)
    }
}
