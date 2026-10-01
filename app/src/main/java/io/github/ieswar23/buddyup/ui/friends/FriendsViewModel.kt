package io.github.ieswar23.buddyup.ui.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.ieswar23.buddyup.data.repository.FriendsRepository
import io.github.ieswar23.buddyup.domain.model.FriendSummary
import io.github.ieswar23.buddyup.util.TimeProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface FriendsUiState {
    data object Loading : FriendsUiState
    data object Empty : FriendsUiState
    data class Content(
        val friends: List<FriendSummary>,
        val onlineNow: List<FriendSummary>,
        val query: String,
        val now: Long,
    ) : FriendsUiState
}

@HiltViewModel
class FriendsViewModel @Inject constructor(
    friendsRepository: FriendsRepository,
    private val timeProvider: TimeProvider,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val uiState: StateFlow<FriendsUiState> =
        combine(friendsRepository.observeFriends(), _query) { friends, query ->
            if (friends.isEmpty()) return@combine FriendsUiState.Empty
            val now = timeProvider.now()
            val needle = query.trim()
            val filtered = if (needle.isEmpty()) friends else friends.filter { friend ->
                friend.person.name.contains(needle, ignoreCase = true) ||
                    friend.person.neighborhood.contains(needle, ignoreCase = true) ||
                    friend.person.interests.any { it.contains(needle, ignoreCase = true) }
            }
            FriendsUiState.Content(
                friends = filtered,
                onlineNow = if (needle.isEmpty()) friends.filter { it.person.isOnline(now) } else emptyList(),
                query = query,
                now = now,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FriendsUiState.Loading)

    fun onQueryChange(query: String) {
        _query.value = query
    }
}
