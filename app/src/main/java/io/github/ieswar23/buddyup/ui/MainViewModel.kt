package io.github.ieswar23.buddyup.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.ieswar23.buddyup.data.repository.FriendsRepository
import io.github.ieswar23.buddyup.data.repository.PeopleRepository
import io.github.ieswar23.buddyup.data.repository.RequestsRepository
import io.github.ieswar23.buddyup.data.repository.SafetyRepository
import io.github.ieswar23.buddyup.data.repository.UserRepository
import io.github.ieswar23.buddyup.data.simulation.AppEvent
import io.github.ieswar23.buddyup.data.simulation.AppEventBus
import io.github.ieswar23.buddyup.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface LaunchState {
    data object Loading : LaunchState
    data class Ready(val onboardingComplete: Boolean) : LaunchState
}

data class AppChromeState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val incomingWaves: Int = 0,
    val unreadMessages: Int = 0,
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val peopleRepository: PeopleRepository,
    requestsRepository: RequestsRepository,
    friendsRepository: FriendsRepository,
    private val safetyRepository: SafetyRepository,
    eventBus: AppEventBus,
) : ViewModel() {

    /** Resolved once at launch — keeps the splash screen up until we know where to start. */
    val launchState: StateFlow<LaunchState> = userRepository.onboardingComplete
        .map<Boolean, LaunchState> { LaunchState.Ready(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, LaunchState.Loading)

    val chrome: StateFlow<AppChromeState> = combine(
        userRepository.settings,
        requestsRepository.observeIncomingCount(),
        friendsRepository.observeFriends().map { friends -> friends.sumOf { it.unreadCount } },
    ) { settings, incoming, unread ->
        AppChromeState(settings.themeMode, incoming, unread)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, AppChromeState())

    /**
     * In-app alerts. "Waved back" alerts are suppressed when the user turned them off in settings;
     * confirmations of the user's own actions (like blocking) always show.
     */
    val alerts: Flow<AppEvent> = eventBus.events.filter { event ->
        event !is AppEvent.WavedBack || userRepository.settings.first().notificationsEnabled
    }

    /** Snackbar "Undo" after blocking someone. */
    fun undoBlock(personId: String) {
        viewModelScope.launch { safetyRepository.unblock(personId) }
    }

    init {
        viewModelScope.launch { peopleRepository.syncIfNeeded() }
    }
}
