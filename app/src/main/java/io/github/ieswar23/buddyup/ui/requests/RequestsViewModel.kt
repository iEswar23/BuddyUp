package io.github.ieswar23.buddyup.ui.requests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.ieswar23.buddyup.data.repository.RequestsRepository
import io.github.ieswar23.buddyup.data.repository.UserRepository
import io.github.ieswar23.buddyup.domain.model.FriendRequest
import io.github.ieswar23.buddyup.domain.scoring.MatchResult
import io.github.ieswar23.buddyup.domain.scoring.MatchScorer
import io.github.ieswar23.buddyup.util.TimeProvider
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RequestItem(val request: FriendRequest, val match: MatchResult) {
    val personId: String get() = request.person.id
}

data class RequestsUiState(
    val isLoading: Boolean = true,
    val incoming: List<RequestItem> = emptyList(),
    val sent: List<RequestItem> = emptyList(),
)

/** An action that can be reverted from the snackbar. */
sealed interface UndoableAction {
    val request: FriendRequest

    data class Accepted(override val request: FriendRequest) : UndoableAction
    data class Declined(override val request: FriendRequest) : UndoableAction
    data class Withdrawn(override val request: FriendRequest) : UndoableAction
}

sealed interface RequestsEvent {
    data class ShowUndo(val message: String, val action: UndoableAction) : RequestsEvent
}

@HiltViewModel
class RequestsViewModel @Inject constructor(
    private val requestsRepository: RequestsRepository,
    userRepository: UserRepository,
    private val matchScorer: MatchScorer,
    private val timeProvider: TimeProvider,
) : ViewModel() {

    /** Ids hidden optimistically while the database write is in flight. */
    private val pendingIds = MutableStateFlow<Set<String>>(emptySet())

    private val _events = Channel<RequestsEvent>(Channel.BUFFERED)
    val events: Flow<RequestsEvent> = _events.receiveAsFlow()

    val uiState: StateFlow<RequestsUiState> = combine(
        requestsRepository.observeIncoming(),
        requestsRepository.observeSent(),
        userRepository.profile,
        pendingIds,
    ) { incoming, sent, me, pending ->
        val now = timeProvider.now()
        fun List<FriendRequest>.toItems() = filterNot { it.person.id in pending }
            .map { RequestItem(it, matchScorer.score(me.interests, it.person, now)) }
        RequestsUiState(isLoading = false, incoming = incoming.toItems(), sent = sent.toItems())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RequestsUiState())

    fun accept(item: RequestItem) = perform(item, UndoableAction.Accepted(item.request), "You and ${item.request.person.firstName} are now buddies 🎉") {
        requestsRepository.accept(item.personId)
    }

    fun decline(item: RequestItem) = perform(item, UndoableAction.Declined(item.request), "Wave from ${item.request.person.firstName} dismissed") {
        requestsRepository.decline(item.personId)
    }

    fun withdraw(item: RequestItem) = perform(item, UndoableAction.Withdrawn(item.request), "Wave to ${item.request.person.firstName} withdrawn") {
        requestsRepository.withdraw(item.personId)
    }

    fun undo(action: UndoableAction) {
        viewModelScope.launch {
            requestsRepository.restore(action.request)
        }
    }

    private fun perform(item: RequestItem, action: UndoableAction, message: String, block: suspend () -> Unit) {
        pendingIds.update { it + item.personId }
        viewModelScope.launch {
            try {
                block()
            } finally {
                pendingIds.update { it - item.personId }
            }
            _events.send(RequestsEvent.ShowUndo(message, action))
        }
    }
}
