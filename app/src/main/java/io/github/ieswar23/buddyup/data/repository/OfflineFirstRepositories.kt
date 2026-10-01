package io.github.ieswar23.buddyup.data.repository

import io.github.ieswar23.buddyup.data.local.dao.ConnectionDao
import io.github.ieswar23.buddyup.data.local.dao.MeetupDao
import io.github.ieswar23.buddyup.data.local.dao.MessageDao
import io.github.ieswar23.buddyup.data.local.dao.PersonDao
import io.github.ieswar23.buddyup.data.local.entity.ConnectionEntity
import io.github.ieswar23.buddyup.data.local.entity.MessageEntity
import io.github.ieswar23.buddyup.data.simulation.BuddySimulator
import io.github.ieswar23.buddyup.di.IoDispatcher
import io.github.ieswar23.buddyup.domain.model.ChatMessage
import io.github.ieswar23.buddyup.domain.model.City
import io.github.ieswar23.buddyup.domain.model.ConnectionState
import io.github.ieswar23.buddyup.domain.model.FriendRequest
import io.github.ieswar23.buddyup.domain.model.FriendSummary
import io.github.ieswar23.buddyup.domain.model.Meetup
import io.github.ieswar23.buddyup.domain.model.Person
import io.github.ieswar23.buddyup.domain.model.RequestDirection
import io.github.ieswar23.buddyup.domain.model.SupportedCities
import io.github.ieswar23.buddyup.util.TimeProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** Emits the user's home city so distances can be computed relative to it. */
private fun UserRepository.originCity(): Flow<City> =
    profile.map { SupportedCities.byName(it.city) }.distinctUntilChanged()

@Singleton
class OfflineFirstPeopleRepository @Inject constructor(
    private val personDao: PersonDao,
    private val connectionDao: ConnectionDao,
    private val userRepository: UserRepository,
    private val synchronizer: DataSynchronizer,
    private val simulator: BuddySimulator,
    private val timeProvider: TimeProvider,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : PeopleRepository {

    override val syncStatus: StateFlow<SyncStatus> = synchronizer.status

    override fun observeDiscoverable(): Flow<List<Person>> =
        combine(personDao.observeUnseen(), userRepository.originCity()) { people, origin ->
            people.map { it.toDomain(origin) }
        }.flowOn(ioDispatcher)

    override fun observePerson(personId: String): Flow<Person?> =
        combine(personDao.observeById(personId), userRepository.originCity()) { person, origin ->
            person?.toDomain(origin)
        }.flowOn(ioDispatcher)

    override suspend fun pass(personId: String) = withContext(ioDispatcher) {
        connectionDao.upsert(ConnectionEntity(personId, ConnectionState.PASSED, null, timeProvider.now(), null))
    }

    override suspend fun wave(personId: String) {
        withContext(ioDispatcher) {
            val existing = connectionDao.get(personId)
            val now = timeProvider.now()
            if (existing?.state == ConnectionState.WAVE_RECEIVED) {
                // They already waved at us — waving back makes us friends right away.
                connectionDao.upsert(existing.copy(state = ConnectionState.FRIEND, updatedAt = now, friendsSince = now))
                return@withContext
            }
            connectionDao.upsert(ConnectionEntity(personId, ConnectionState.WAVE_SENT, null, now, null))
            simulator.onWaveSent(personId)
        }
    }

    override suspend fun undoPass(personId: String) = withContext(ioDispatcher) {
        if (connectionDao.get(personId)?.state == ConnectionState.PASSED) connectionDao.delete(personId)
    }

    override suspend fun resetPassed() = withContext(ioDispatcher) {
        connectionDao.deleteByState(ConnectionState.PASSED)
    }

    override suspend fun syncIfNeeded() = synchronizer.syncIfNeeded()

    override suspend fun refresh(): Result<Unit> = synchronizer.refresh()
}

@Singleton
class OfflineFirstRequestsRepository @Inject constructor(
    private val connectionDao: ConnectionDao,
    private val userRepository: UserRepository,
    private val simulator: BuddySimulator,
    private val timeProvider: TimeProvider,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : RequestsRepository {

    override fun observeIncoming(): Flow<List<FriendRequest>> = observe(ConnectionState.WAVE_RECEIVED)

    override fun observeSent(): Flow<List<FriendRequest>> = observe(ConnectionState.WAVE_SENT)

    override fun observeIncomingCount(): Flow<Int> =
        connectionDao.observeCount(ConnectionState.WAVE_RECEIVED).distinctUntilChanged()

    private fun observe(state: ConnectionState): Flow<List<FriendRequest>> =
        combine(connectionDao.observeByState(state), userRepository.originCity()) { rows, origin ->
            rows.map { it.toRequest(origin) }
        }.flowOn(ioDispatcher)

    override suspend fun accept(personId: String) = withContext(ioDispatcher) {
        val now = timeProvider.now()
        val existing = connectionDao.get(personId)
        connectionDao.upsert(
            ConnectionEntity(personId, ConnectionState.FRIEND, existing?.note, now, now)
        )
    }

    override suspend fun decline(personId: String) = withContext(ioDispatcher) {
        val existing = connectionDao.get(personId)
        connectionDao.upsert(
            ConnectionEntity(personId, ConnectionState.DECLINED, existing?.note, timeProvider.now(), null)
        )
    }

    override suspend fun withdraw(personId: String) = withContext(ioDispatcher) {
        if (connectionDao.get(personId)?.state == ConnectionState.WAVE_SENT) connectionDao.delete(personId)
    }

    override suspend fun restore(request: FriendRequest) {
        withContext(ioDispatcher) {
            val state = when (request.direction) {
                RequestDirection.INCOMING -> ConnectionState.WAVE_RECEIVED
                RequestDirection.OUTGOING -> ConnectionState.WAVE_SENT
            }
            connectionDao.upsert(ConnectionEntity(request.person.id, state, request.note, request.createdAt, null))
        }
        if (request.direction == RequestDirection.OUTGOING) simulator.onWaveSent(request.person.id)
    }
}

@Singleton
class OfflineFirstFriendsRepository @Inject constructor(
    private val connectionDao: ConnectionDao,
    private val userRepository: UserRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : FriendsRepository {

    override fun observeFriends(): Flow<List<FriendSummary>> =
        combine(connectionDao.observeFriends(), userRepository.originCity()) { rows, origin ->
            rows.map { it.toDomain(origin) }.sortedByDescending { it.lastActivityAt }
        }.flowOn(ioDispatcher)
}

@Singleton
class OfflineFirstChatRepository @Inject constructor(
    private val messageDao: MessageDao,
    private val simulator: BuddySimulator,
    private val timeProvider: TimeProvider,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ChatRepository {

    override val typingFriendIds: StateFlow<Set<String>> = simulator.typingFriendIds

    override fun observeConversation(friendId: String): Flow<List<ChatMessage>> =
        messageDao.observeConversation(friendId)
            .map { rows -> rows.map { it.toDomain() } }
            .flowOn(ioDispatcher)

    override suspend fun send(friendId: String, text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        withContext(ioDispatcher) {
            messageDao.insert(
                MessageEntity(friendId = friendId, text = trimmed, sentAt = timeProvider.now(), fromMe = true, isRead = true)
            )
        }
        simulator.onMessageSent(friendId, trimmed)
    }

    override suspend fun markRead(friendId: String) = withContext(ioDispatcher) {
        messageDao.markConversationRead(friendId)
    }
}

@Singleton
class OfflineFirstMeetupRepository @Inject constructor(
    private val meetupDao: MeetupDao,
    private val synchronizer: DataSynchronizer,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : MeetupRepository {

    override fun observeMeetups(): Flow<List<Meetup>> =
        meetupDao.observeAll().map { rows -> rows.map { it.toDomain() } }.flowOn(ioDispatcher)

    override suspend fun toggleJoin(meetupId: String): Boolean = withContext(ioDispatcher) {
        meetupDao.toggleJoin(meetupId)
    }

    override suspend fun refresh(): Result<Unit> = synchronizer.refreshMeetups()
}
