package io.github.ieswar23.buddyup.ui.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.ieswar23.buddyup.data.repository.MeetupRepository
import io.github.ieswar23.buddyup.data.repository.UserRepository
import io.github.ieswar23.buddyup.domain.model.Meetup
import io.github.ieswar23.buddyup.domain.model.MeetupCategory
import io.github.ieswar23.buddyup.util.Formatters
import io.github.ieswar23.buddyup.util.TimeProvider
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

enum class MeetupScope { NEAR_ME, GOING, ALL }

data class MeetupSection(val title: String, val meetups: List<Meetup>)

sealed interface EventsUiState {
    data object Loading : EventsUiState
    data class Content(
        val sections: List<MeetupSection>,
        val city: String,
        val scope: MeetupScope,
        val category: MeetupCategory?,
        val goingCount: Int,
        val now: Long,
    ) : EventsUiState {
        val isEmpty: Boolean get() = sections.isEmpty()
    }
}

@HiltViewModel
class EventsViewModel @Inject constructor(
    private val meetupRepository: MeetupRepository,
    userRepository: UserRepository,
    private val timeProvider: TimeProvider,
) : ViewModel() {

    private val _scope = MutableStateFlow(MeetupScope.NEAR_ME)
    private val _category = MutableStateFlow<MeetupCategory?>(null)

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages: Flow<String> = _messages.receiveAsFlow()

    val uiState: StateFlow<EventsUiState> = combine(
        meetupRepository.observeMeetups(),
        userRepository.profile,
        _scope,
        _category,
    ) { meetups, me, scope, category ->
        val now = timeProvider.now()
        val upcoming = meetups.filter { it.startsAt + it.durationMinutes * 60_000L >= now }
        val filtered = upcoming
            .filter {
                when (scope) {
                    MeetupScope.NEAR_ME -> it.city.equals(me.city, ignoreCase = true)
                    MeetupScope.GOING -> it.isJoined
                    MeetupScope.ALL -> true
                }
            }
            .filter { category == null || it.category == category }
            .sortedBy { it.startsAt }
        val sections = filtered
            .groupBy { Formatters.startOfDay(it.startsAt) }
            .map { (day, items) -> MeetupSection(sectionTitle(day, now), items) }
        EventsUiState.Content(
            sections = sections,
            city = me.city,
            scope = scope,
            category = category,
            goingCount = upcoming.count { it.isJoined },
            now = now,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EventsUiState.Loading)

    fun selectScope(scope: MeetupScope) {
        _scope.value = scope
    }

    fun selectCategory(category: MeetupCategory?) {
        _category.value = if (_category.value == category) null else category
    }

    fun toggleJoin(meetup: Meetup) {
        if (meetup.isFull) {
            viewModelScope.launch { _messages.send("${meetup.title} is full — try another one!") }
            return
        }
        viewModelScope.launch {
            val joined = meetupRepository.toggleJoin(meetup.id)
            _messages.send(if (joined) "You're going to ${meetup.title} 🎉" else "You left ${meetup.title}")
        }
    }

    fun refresh() {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            val result = meetupRepository.refresh()
            _isRefreshing.value = false
            if (result.isFailure) _messages.send("Couldn't refresh meetups right now")
        }
    }

    private fun sectionTitle(dayStart: Long, now: Long): String =
        when (Formatters.daysBetween(now, dayStart)) {
            0 -> "Today"
            1 -> "Tomorrow"
            else -> SimpleDateFormat("EEEE, d MMM", Locale.getDefault()).format(Date(dayStart))
        }
}
