package io.github.ieswar23.buddyup.ui.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.ieswar23.buddyup.data.repository.ChatRepository
import io.github.ieswar23.buddyup.data.repository.PeopleRepository
import io.github.ieswar23.buddyup.data.repository.UserRepository
import io.github.ieswar23.buddyup.domain.model.Person
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
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val isLoading: Boolean = true,
    val friend: Person? = null,
    val items: List<ChatItem> = emptyList(),
    val isTyping: Boolean = false,
    val sharedInterests: List<String> = emptyList(),
    val suggestions: List<String> = emptyList(),
    val now: Long = 0L,
) {
    val lastMessageKey: String? get() = items.lastOrNull { it is ChatItem.Bubble }?.key
}

@HiltViewModel
class ChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val chatRepository: ChatRepository,
    peopleRepository: PeopleRepository,
    userRepository: UserRepository,
    private val timeProvider: TimeProvider,
) : ViewModel() {

    val friendId: String = checkNotNull(savedStateHandle[Screen.Chat.ARG_FRIEND_ID]) {
        "ChatViewModel requires a friendId argument"
    }

    private val _input = MutableStateFlow("")
    val input: StateFlow<String> = _input.asStateFlow()

    private val messages = chatRepository.observeConversation(friendId)
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), replay = 1)

    val uiState: StateFlow<ChatUiState> = combine(
        peopleRepository.observePerson(friendId),
        messages,
        chatRepository.typingFriendIds.map { friendId in it },
        userRepository.profile,
    ) { friend, conversation, typing, me ->
        val now = timeProvider.now()
        val shared = friend?.let { MatchScorer.sharedInterests(me.interests, it.interests) }.orEmpty()
        val lastFromThem = conversation.lastOrNull()?.fromMe == false
        ChatUiState(
            isLoading = false,
            friend = friend,
            items = ChatGrouping.build(conversation, now),
            isTyping = typing,
            sharedInterests = shared,
            suggestions = if (conversation.isEmpty() || lastFromThem) suggestionsFor(friend, shared) else emptyList(),
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
        viewModelScope.launch { chatRepository.send(friendId, text) }
    }

    fun sendSuggestion(text: String) {
        viewModelScope.launch { chatRepository.send(friendId, text) }
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

    companion object {
        const val MAX_MESSAGE_LENGTH = 1_000
    }
}
