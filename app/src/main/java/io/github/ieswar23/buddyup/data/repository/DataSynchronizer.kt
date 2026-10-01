package io.github.ieswar23.buddyup.data.repository

import androidx.room.withTransaction
import io.github.ieswar23.buddyup.data.local.BuddyUpDatabase
import io.github.ieswar23.buddyup.data.local.entity.ConnectionEntity
import io.github.ieswar23.buddyup.data.local.entity.MeetupEntity
import io.github.ieswar23.buddyup.data.local.entity.MessageEntity
import io.github.ieswar23.buddyup.data.remote.BuddyUpApi
import io.github.ieswar23.buddyup.di.IoDispatcher
import io.github.ieswar23.buddyup.domain.model.ConnectionState
import io.github.ieswar23.buddyup.util.TimeProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

/**
 * Pulls data from the (mocked) BuddyUp API and caches it in Room, which stays the single source
 * of truth for every screen. The first sync also seeds existing friendships and incoming waves;
 * later refreshes only update profile/meetup data and never clobber local relationship state.
 */
@Singleton
class DataSynchronizer @Inject constructor(
    private val api: BuddyUpApi,
    private val database: BuddyUpDatabase,
    private val timeProvider: TimeProvider,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    private val mutex = Mutex()
    private val _status = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val status: StateFlow<SyncStatus> = _status.asStateFlow()

    private val personDao get() = database.personDao()
    private val connectionDao get() = database.connectionDao()
    private val messageDao get() = database.messageDao()
    private val meetupDao get() = database.meetupDao()

    suspend fun syncIfNeeded() = withContext(ioDispatcher) {
        mutex.withLock {
            if (personDao.count() > 0) {
                _status.value = SyncStatus.Synced
                return@withLock
            }
            runSync { initialSync() }
        }
    }

    suspend fun refresh(): Result<Unit> = withContext(ioDispatcher) {
        mutex.withLock {
            val firstRun = personDao.count() == 0
            runSync { if (firstRun) initialSync() else incrementalSync() }
        }
    }

    suspend fun refreshMeetups(): Result<Unit> = withContext(ioDispatcher) {
        try {
            mergeMeetups(api.meetups().map { it.toEntity(timeProvider.now()) })
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun runSync(block: suspend () -> Unit): Result<Unit> {
        _status.value = SyncStatus.Syncing
        return try {
            block()
            _status.value = SyncStatus.Synced
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _status.value = SyncStatus.Failed(e.message ?: "Something went wrong")
            Result.failure(e)
        }
    }

    private suspend fun initialSync() = coroutineScope {
        val people = async { api.nearbyPeople() }
        val incoming = async { api.incomingWaves() }
        val friends = async { api.friends() }
        val meetups = async { api.meetups() }

        val now = timeProvider.now()
        val personEntities = people.await().map { it.toEntity(now) }
        val knownIds = personEntities.mapTo(HashSet()) { it.id }

        val requestRows = incoming.await().filter { it.personId in knownIds }.map {
            ConnectionEntity(
                personId = it.personId,
                state = ConnectionState.WAVE_RECEIVED,
                note = it.note,
                updatedAt = now - TimeUnit.MINUTES.toMillis(it.minutesAgo.toLong()),
                friendsSince = null,
            )
        }
        val friendships = friends.await().filter { it.personId in knownIds }
        val friendRows = friendships.map {
            val since = now - TimeUnit.DAYS.toMillis(it.friendsSinceDays.toLong())
            ConnectionEntity(it.personId, ConnectionState.FRIEND, null, since, since)
        }
        val messages = friendships.flatMap { friendship ->
            friendship.messages.map {
                MessageEntity(
                    friendId = friendship.personId,
                    text = it.text,
                    sentAt = now - TimeUnit.MINUTES.toMillis(it.minutesAgo.toLong()),
                    fromMe = it.fromMe,
                    isRead = it.fromMe || it.read,
                )
            }
        }
        val meetupEntities = meetups.await().map { it.toEntity(now) }

        database.withTransaction {
            personDao.upsertAll(personEntities)
            connectionDao.upsertAll(requestRows + friendRows)
            messageDao.insertAll(messages)
            meetupDao.upsertAll(meetupEntities)
        }
    }

    private suspend fun incrementalSync() = coroutineScope {
        val people = async { api.nearbyPeople() }
        val meetups = async { api.meetups() }
        val now = timeProvider.now()
        val personEntities = people.await().map { it.toEntity(now) }
        val meetupEntities = meetups.await().map { it.toEntity(now) }
        database.withTransaction {
            personDao.upsertAll(personEntities)
        }
        mergeMeetups(meetupEntities)
    }

    /** Keeps the user's local RSVP state when fresh meetup data arrives. */
    private suspend fun mergeMeetups(remote: List<MeetupEntity>) {
        database.withTransaction {
            val local = meetupDao.getAll().associateBy { it.id }
            val merged = remote.map { fresh ->
                val existing = local[fresh.id] ?: return@map fresh
                val adjustment = when {
                    existing.isJoined && !fresh.isJoined -> 1
                    !existing.isJoined && fresh.isJoined -> -1
                    else -> 0
                }
                fresh.copy(
                    isJoined = existing.isJoined,
                    attendeeCount = (fresh.attendeeCount + adjustment).coerceIn(0, fresh.capacity),
                )
            }
            meetupDao.upsertAll(merged)
        }
    }
}
