package io.github.ieswar23.buddyup.ui.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.ieswar23.buddyup.data.repository.PeopleRepository
import io.github.ieswar23.buddyup.data.repository.SafetyRepository
import io.github.ieswar23.buddyup.data.repository.SyncStatus
import io.github.ieswar23.buddyup.data.repository.UserRepository
import io.github.ieswar23.buddyup.domain.model.DiscoverFilters
import io.github.ieswar23.buddyup.domain.model.Interests
import io.github.ieswar23.buddyup.domain.model.Person
import io.github.ieswar23.buddyup.domain.model.Report
import io.github.ieswar23.buddyup.domain.scoring.MatchResult
import io.github.ieswar23.buddyup.domain.scoring.MatchScorer
import io.github.ieswar23.buddyup.util.TimeProvider
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DiscoverCard(val person: Person, val match: MatchResult)

sealed interface DiscoverUiState {
    data object Loading : DiscoverUiState
    data class Error(val message: String) : DiscoverUiState
    data class Empty(val hasActiveFilters: Boolean) : DiscoverUiState
    data class Content(val cards: List<DiscoverCard>) : DiscoverUiState
}

sealed interface DiscoverEvent {
    data class WaveSent(val firstName: String) : DiscoverEvent
    data class Message(val text: String) : DiscoverEvent
}

data class DiscoverHeader(val city: String = "", val nearbyCount: Int = 0)

@HiltViewModel
class DiscoverViewModel @Inject constructor(
    private val peopleRepository: PeopleRepository,
    userRepository: UserRepository,
    private val safetyRepository: SafetyRepository,
    private val matchScorer: MatchScorer,
    private val timeProvider: TimeProvider,
) : ViewModel() {

    private val _filters = MutableStateFlow(DiscoverFilters())
    val filters: StateFlow<DiscoverFilters> = _filters.asStateFlow()

    /** Ids acted on locally; hides cards instantly before Room emits the update. */
    private val dismissedIds = MutableStateFlow<Set<String>>(emptySet())

    private val _lastPassed = MutableStateFlow<Person?>(null)
    val lastPassed: StateFlow<Person?> = _lastPassed.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _events = Channel<DiscoverEvent>(Channel.BUFFERED)
    val events: Flow<DiscoverEvent> = _events.receiveAsFlow()

    private val profile = userRepository.profile

    private val scoredPeople: StateFlow<List<DiscoverCard>?> =
        combine(peopleRepository.observeDiscoverable(), profile, dismissedIds) { people, me, dismissed ->
            val now = timeProvider.now()
            people.asSequence()
                .filterNot { it.id in dismissed }
                .map { DiscoverCard(it, matchScorer.score(me.interests, it, now)) }
                .sortedWith(compareByDescending<DiscoverCard> { it.match.percent }.thenBy { it.person.distanceKm })
                .toList()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), null)

    val header: StateFlow<DiscoverHeader> =
        combine(scoredPeople, profile, _filters) { cards, me, filters ->
            DiscoverHeader(city = me.city, nearbyCount = cards.orEmpty().count { filters.matches(it.person) })
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), DiscoverHeader())

    val uiState: StateFlow<DiscoverUiState> =
        combine(scoredPeople, _filters, peopleRepository.syncStatus) { scored, filters, sync ->
            val cards = scored ?: return@combine DiscoverUiState.Loading
            val visible = cards.filter { filters.matches(it.person) }
            when {
                visible.isNotEmpty() -> DiscoverUiState.Content(visible)
                cards.isEmpty() && (sync is SyncStatus.Syncing || sync is SyncStatus.Idle) -> DiscoverUiState.Loading
                cards.isEmpty() && sync is SyncStatus.Failed -> DiscoverUiState.Error(sync.message)
                else -> DiscoverUiState.Empty(hasActiveFilters = !filters.isDefault)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), DiscoverUiState.Loading)

    /** Interests to offer in the filter sheet — the user's own first. */
    val filterInterests: StateFlow<List<String>> = profile.map { me ->
        val mine = me.interests
        mine + Interests.all.map { it.name }.filterNot { it in mine }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), emptyList())

    /** Live count for the filter sheet's "Show buddies (N)" button. */
    fun countMatching(filters: DiscoverFilters): Int = scoredPeople.value.orEmpty().count { filters.matches(it.person) }

    fun onPass(person: Person) {
        dismissedIds.update { it + person.id }
        _lastPassed.value = person
        viewModelScope.launch { peopleRepository.pass(person.id) }
    }

    fun onWave(person: Person) {
        dismissedIds.update { it + person.id }
        _lastPassed.value = null
        viewModelScope.launch {
            peopleRepository.wave(person.id)
            _events.send(DiscoverEvent.WaveSent(person.firstName))
        }
    }

    /** Blocks (and optionally reports) someone straight from their card; they never come back here. */
    fun onBlock(person: Person, report: Report?) {
        dismissedIds.update { it + person.id }
        if (_lastPassed.value?.id == person.id) _lastPassed.value = null
        viewModelScope.launch { safetyRepository.block(person.id, report) }
    }

    fun undoLastPass() {
        val person = _lastPassed.value ?: return
        _lastPassed.value = null
        viewModelScope.launch {
            peopleRepository.undoPass(person.id)
            dismissedIds.update { it - person.id }
        }
    }

    fun applyFilters(filters: DiscoverFilters) {
        _filters.value = filters
    }

    fun resetFilters() {
        _filters.value = DiscoverFilters()
    }

    fun searchAnywhere() {
        _filters.update { DiscoverFilters(maxDistanceKm = DiscoverFilters.ANYWHERE_KM, interests = emptySet()) }
    }

    fun revisitPassed() {
        _lastPassed.value = null
        viewModelScope.launch {
            peopleRepository.resetPassed()
            dismissedIds.value = emptySet()
        }
    }

    fun retry() = refresh()

    fun refresh() {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            val result = peopleRepository.refresh()
            _isRefreshing.value = false
            if (result.isFailure) {
                _events.send(DiscoverEvent.Message("Couldn't refresh. Check your connection and try again."))
            }
        }
    }

    private companion object {
        const val STOP_TIMEOUT = 5_000L
    }
}
