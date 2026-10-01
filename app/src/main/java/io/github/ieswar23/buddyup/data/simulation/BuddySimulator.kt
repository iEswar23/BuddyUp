package io.github.ieswar23.buddyup.data.simulation

import io.github.ieswar23.buddyup.data.local.dao.ConnectionDao
import io.github.ieswar23.buddyup.data.local.dao.MessageDao
import io.github.ieswar23.buddyup.data.local.dao.PersonDao
import io.github.ieswar23.buddyup.data.local.entity.ConnectionEntity
import io.github.ieswar23.buddyup.data.local.entity.MessageEntity
import io.github.ieswar23.buddyup.data.repository.UserRepository
import io.github.ieswar23.buddyup.data.repository.toDomain
import io.github.ieswar23.buddyup.di.ApplicationScope
import io.github.ieswar23.buddyup.domain.model.ConnectionState
import io.github.ieswar23.buddyup.domain.model.SupportedCities
import io.github.ieswar23.buddyup.domain.scoring.MatchScorer
import io.github.ieswar23.buddyup.util.TimeProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

data class SimulationConfig(
    val waveBackDelayMs: LongRange = 3_500L..7_000L,
    val readDelayMs: LongRange = 700L..1_400L,
    val typingDelayMs: LongRange = 1_600L..3_200L,
)

/**
 * Plays "the other side" of the community so the offline demo feels alive:
 *  - people flagged `wavesBack` accept the user's wave after a short delay and say hi;
 *  - friends show a typing indicator and then reply to the user's messages.
 *
 * Work runs on the application scope so it survives navigation between screens.
 */
@Singleton
class BuddySimulator @Inject constructor(
    @ApplicationScope private val scope: CoroutineScope,
    private val personDao: PersonDao,
    private val connectionDao: ConnectionDao,
    private val messageDao: MessageDao,
    private val userRepository: UserRepository,
    private val replyGenerator: ReplyGenerator,
    private val eventBus: AppEventBus,
    private val timeProvider: TimeProvider,
    private val config: SimulationConfig,
    private val random: Random,
) {
    private val _typing = MutableStateFlow<Set<String>>(emptySet())
    val typingFriendIds: StateFlow<Set<String>> = _typing.asStateFlow()

    private val replyJobs = ConcurrentHashMap<String, Job>()

    fun onWaveSent(personId: String) {
        scope.launch {
            val person = personDao.getById(personId) ?: return@launch
            if (!person.wavesBack) return@launch
            delay(random.nextLong(config.waveBackDelayMs))

            // The user may have withdrawn the wave in the meantime.
            if (connectionDao.get(personId)?.state != ConnectionState.WAVE_SENT) return@launch

            val now = timeProvider.now()
            val profile = userRepository.profile.first()
            val domainPerson = person.toDomain(SupportedCities.byName(profile.city))
            val shared = MatchScorer.sharedInterests(profile.interests, person.interests)

            connectionDao.upsert(
                ConnectionEntity(
                    personId = personId,
                    state = ConnectionState.FRIEND,
                    note = null,
                    updatedAt = now,
                    friendsSince = now,
                )
            )
            personDao.touchLastActive(personId, now)
            messageDao.insert(
                MessageEntity(
                    friendId = personId,
                    text = replyGenerator.waveBackGreeting(domainPerson, shared),
                    sentAt = now,
                    fromMe = false,
                    isRead = false,
                )
            )
            eventBus.emit(AppEvent.WavedBack(personId, domainPerson.firstName))
        }
    }

    /** Debounced per friend: rapid consecutive messages get a single reply to the latest one. */
    fun onMessageSent(friendId: String, text: String) {
        replyJobs.remove(friendId)?.cancel()
        val job = scope.launch {
            try {
                delay(random.nextLong(config.readDelayMs))
                _typing.update { it + friendId }
                delay(random.nextLong(config.typingDelayMs) + (text.length * 12L).coerceAtMost(1_500L))

                val person = personDao.getById(friendId) ?: return@launch
                val profile = userRepository.profile.first()
                val shared = MatchScorer.sharedInterests(profile.interests, person.interests)
                val reply = replyGenerator.replyTo(text, person.toDomain(SupportedCities.byName(profile.city)), shared)
                val now = timeProvider.now()
                messageDao.insert(
                    MessageEntity(friendId = friendId, text = reply, sentAt = now, fromMe = false, isRead = false)
                )
                personDao.touchLastActive(friendId, now)
            } finally {
                _typing.update { it - friendId }
            }
        }
        replyJobs[friendId] = job
        job.invokeOnCompletion { replyJobs.remove(friendId, job) }
    }

    private fun Random.nextLong(range: LongRange): Long =
        if (range.last > range.first) nextLong(range.first, range.last + 1) else range.first
}
