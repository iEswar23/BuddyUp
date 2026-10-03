package io.github.ieswar23.buddyup.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.ieswar23.buddyup.data.repository.FriendsRepository
import io.github.ieswar23.buddyup.data.repository.MeetupRepository
import io.github.ieswar23.buddyup.data.repository.RequestsRepository
import io.github.ieswar23.buddyup.data.repository.SafetyRepository
import io.github.ieswar23.buddyup.data.repository.UserRepository
import io.github.ieswar23.buddyup.domain.model.AppSettings
import io.github.ieswar23.buddyup.domain.model.BlockedPerson
import io.github.ieswar23.buddyup.domain.model.ThemeMode
import io.github.ieswar23.buddyup.domain.model.UserProfile
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileStats(val buddies: Int = 0, val wavesSent: Int = 0, val meetupsGoing: Int = 0)

data class ProfileUiState(
    val isLoading: Boolean = true,
    val profile: UserProfile = UserProfile.EMPTY,
    val settings: AppSettings = AppSettings(),
    val stats: ProfileStats = ProfileStats(),
    val memberSince: Long? = null,
    val blocked: List<BlockedPerson> = emptyList(),
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    friendsRepository: FriendsRepository,
    requestsRepository: RequestsRepository,
    meetupRepository: MeetupRepository,
    private val safetyRepository: SafetyRepository,
) : ViewModel() {

    private val stats = combine(
        friendsRepository.observeFriends().map { it.size },
        requestsRepository.observeSent().map { it.size },
        meetupRepository.observeMeetups().map { meetups -> meetups.count { it.isJoined } },
    ) { buddies, sent, going -> ProfileStats(buddies, sent, going) }

    val uiState: StateFlow<ProfileUiState> = combine(
        userRepository.profile,
        userRepository.settings,
        userRepository.memberSince,
        stats,
        safetyRepository.observeBlocked(),
    ) { profile, settings, since, stats, blocked ->
        ProfileUiState(
            isLoading = false,
            profile = profile,
            settings = settings,
            stats = stats,
            memberSince = since,
            blocked = blocked,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState())

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { userRepository.setThemeMode(mode) }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { userRepository.setNotificationsEnabled(enabled) }
    }

    fun unblock(personId: String) {
        viewModelScope.launch { safetyRepository.unblock(personId) }
    }
}
