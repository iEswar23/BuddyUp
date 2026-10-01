package io.github.ieswar23.buddyup.data.simulation

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

sealed interface AppEvent {
    /** Someone the user waved at waved back — they are now friends. */
    data class WavedBack(val personId: String, val firstName: String) : AppEvent
}

/** App-wide one-off events (e.g. in-app alerts) that outlive any single screen. */
@Singleton
class AppEventBus @Inject constructor() {
    private val _events = MutableSharedFlow<AppEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<AppEvent> = _events.asSharedFlow()

    fun emit(event: AppEvent) {
        _events.tryEmit(event)
    }
}
