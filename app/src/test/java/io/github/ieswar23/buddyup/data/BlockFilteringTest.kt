package io.github.ieswar23.buddyup.data

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import io.github.ieswar23.buddyup.data.local.BuddyUpDatabase
import io.github.ieswar23.buddyup.data.local.entity.ConnectionEntity
import io.github.ieswar23.buddyup.data.local.entity.MessageEntity
import io.github.ieswar23.buddyup.data.local.entity.PersonEntity
import io.github.ieswar23.buddyup.data.remote.BuddyUpApi
import io.github.ieswar23.buddyup.data.remote.dto.FriendshipDto
import io.github.ieswar23.buddyup.data.remote.dto.IncomingRequestDto
import io.github.ieswar23.buddyup.data.remote.dto.MeetupDto
import io.github.ieswar23.buddyup.data.remote.dto.PersonDto
import io.github.ieswar23.buddyup.data.repository.DataSynchronizer
import io.github.ieswar23.buddyup.data.repository.OfflineFirstChatRepository
import io.github.ieswar23.buddyup.data.repository.OfflineFirstFriendsRepository
import io.github.ieswar23.buddyup.data.repository.OfflineFirstPeopleRepository
import io.github.ieswar23.buddyup.data.repository.OfflineFirstRequestsRepository
import io.github.ieswar23.buddyup.data.repository.OfflineFirstSafetyRepository
import io.github.ieswar23.buddyup.data.simulation.AppEvent
import io.github.ieswar23.buddyup.data.simulation.AppEventBus
import io.github.ieswar23.buddyup.data.simulation.BuddySimulator
import io.github.ieswar23.buddyup.data.simulation.ReplyGenerator
import io.github.ieswar23.buddyup.data.simulation.SimulationConfig
import io.github.ieswar23.buddyup.domain.model.ConnectionState
import io.github.ieswar23.buddyup.domain.model.Report
import io.github.ieswar23.buddyup.domain.model.ReportReason
import io.github.ieswar23.buddyup.fakes.FakeUserRepository
import io.github.ieswar23.buddyup.fakes.NOW
import io.github.ieswar23.buddyup.fakes.minutesAgo
import io.github.ieswar23.buddyup.util.TimeProvider
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.random.Random

/**
 * Runs the real offline-first repositories and [BuddySimulator] against an in-memory Room database
 * to check that blocked people are filtered out everywhere and that the simulator respects blocks.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class BlockFilteringTest {

    private val io = Dispatchers.Unconfined
    private val timeProvider = TimeProvider { NOW }
    private val userRepository = FakeUserRepository()
    private val eventBus = AppEventBus()
    private val simulatorScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)

    private lateinit var db: BuddyUpDatabase
    private lateinit var simulator: BuddySimulator
    private lateinit var people: OfflineFirstPeopleRepository
    private lateinit var requests: OfflineFirstRequestsRepository
    private lateinit var friends: OfflineFirstFriendsRepository
    private lateinit var chat: OfflineFirstChatRepository
    private lateinit var safety: OfflineFirstSafetyRepository

    @Before
    fun setUp() = runBlocking<Unit> {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), BuddyUpDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        simulator = BuddySimulator(
            scope = simulatorScope,
            personDao = db.personDao(),
            connectionDao = db.connectionDao(),
            messageDao = db.messageDao(),
            blockDao = db.blockDao(),
            userRepository = userRepository,
            replyGenerator = ReplyGenerator(Random(7)),
            eventBus = eventBus,
            timeProvider = timeProvider,
            config = SimulationConfig(waveBackDelayMs = 0L..0L, readDelayMs = 0L..0L, typingDelayMs = 0L..0L),
            random = Random(7),
        )
        val synchronizer = DataSynchronizer(EmptyApi, db, timeProvider, io)
        people = OfflineFirstPeopleRepository(
            db.personDao(), db.connectionDao(), userRepository, synchronizer, simulator, timeProvider, io,
        )
        requests = OfflineFirstRequestsRepository(db.connectionDao(), userRepository, simulator, timeProvider, io)
        friends = OfflineFirstFriendsRepository(db.connectionDao(), userRepository, io)
        chat = OfflineFirstChatRepository(db.messageDao(), simulator, timeProvider, io)
        safety = OfflineFirstSafetyRepository(
            db.blockDao(), db.personDao(), userRepository, simulator, eventBus, timeProvider, io,
        )
        seed()
    }

    @After
    fun tearDown() {
        simulatorScope.cancel()
        db.close()
    }

    @Test
    fun `blocked people disappear from discover, friends, waves and badge counts`() = runBlocking<Unit> {
        assertThat(people.observeDiscoverable().first().map { it.id }).containsExactly(STRANGER)
        assertThat(friends.observeFriends().first().map { it.person.id }).containsExactly(FRIEND)
        assertThat(requests.observeIncoming().first().map { it.person.id }).containsExactly(INCOMING)
        assertThat(requests.observeSent().first().map { it.person.id }).containsExactly(SENT)

        listOf(STRANGER, FRIEND, INCOMING, SENT).forEach { safety.block(it) }

        assertThat(people.observeDiscoverable().first()).isEmpty()
        assertThat(friends.observeFriends().first()).isEmpty()
        assertThat(requests.observeIncoming().first()).isEmpty()
        assertThat(requests.observeSent().first()).isEmpty()
        assertThat(requests.observeIncomingCount().first()).isEqualTo(0)
        assertThat(db.messageDao().observeUnreadTotal().first()).isEqualTo(0)
        assertThat(safety.observeBlockedIds().first()).containsExactly(STRANGER, FRIEND, INCOMING, SENT)
    }

    @Test
    fun `unblocking restores the friendship and its chat history`() = runBlocking<Unit> {
        safety.block(FRIEND, Report(ReportReason.INAPPROPRIATE))
        assertThat(friends.observeFriends().first()).isEmpty()

        safety.unblock(FRIEND)

        val friend = friends.observeFriends().first().single()
        assertThat(friend.person.id).isEqualTo(FRIEND)
        assertThat(friend.lastMessage).isEqualTo("See you Saturday?")
        assertThat(friend.unreadCount).isEqualTo(1)
        assertThat(chat.observeConversation(FRIEND).first()).hasSize(2)
        assertThat(safety.observeBlocked().first()).isEmpty()
    }

    @Test
    fun `blocked list joins profiles, keeps the report and confirms with an event`() = runBlocking<Unit> {
        val event = async(start = CoroutineStart.UNDISPATCHED) { eventBus.events.first() }

        safety.block(INCOMING, Report(ReportReason.SPAM, note = "Asked me to join a paid group"))

        val blocked = safety.observeBlocked().first().single()
        assertThat(blocked.person.name).isEqualTo("Sneha Kulkarni")
        assertThat(blocked.report).isEqualTo(Report(ReportReason.SPAM, note = "Asked me to join a paid group"))
        assertThat(blocked.blockedAt).isEqualTo(NOW)
        assertThat(event.await()).isEqualTo(AppEvent.Blocked(INCOMING, "Sneha", reported = true))
    }

    @Test
    fun `the simulator never waves back for someone who was blocked`() = runBlocking<Unit> {
        safety.block(SENT)

        simulator.onWaveSent(SENT)
        awaitSimulator()

        assertThat(db.connectionDao().get(SENT)?.state).isEqualTo(ConnectionState.WAVE_SENT)
        assertThat(chat.observeConversation(SENT).first()).isEmpty()

        // Control: an unblocked person who waves back still becomes a buddy.
        people.wave(STRANGER)
        awaitSimulator()
        assertThat(db.connectionDao().get(STRANGER)?.state).isEqualTo(ConnectionState.FRIEND)
    }

    @Test
    fun `blocked friends never reply or show a typing indicator`() = runBlocking<Unit> {
        safety.block(FRIEND)

        chat.send(FRIEND, "Hey, are you around?")
        awaitSimulator()

        val conversation = chat.observeConversation(FRIEND).first()
        assertThat(conversation.last().fromMe).isTrue()
        assertThat(conversation).hasSize(3)
        assertThat(simulator.typingFriendIds.value).isEmpty()
    }

    private suspend fun awaitSimulator() {
        simulatorScope.coroutineContext.job.children.toList().forEach { it.join() }
    }

    private suspend fun seed() {
        db.personDao().upsertAll(
            listOf(
                personEntity(STRANGER, "Ananya Rao", wavesBack = true),
                personEntity(FRIEND, "Kavya Iyer"),
                personEntity(INCOMING, "Sneha Kulkarni"),
                personEntity(SENT, "Emily Carter", wavesBack = true),
            )
        )
        db.connectionDao().upsertAll(
            listOf(
                ConnectionEntity(FRIEND, ConnectionState.FRIEND, null, minutesAgo(600), minutesAgo(600)),
                ConnectionEntity(INCOMING, ConnectionState.WAVE_RECEIVED, "Hi!", minutesAgo(25), null),
                ConnectionEntity(SENT, ConnectionState.WAVE_SENT, null, minutesAgo(5), null),
            )
        )
        db.messageDao().insertAll(
            listOf(
                MessageEntity(friendId = FRIEND, text = "Hey!", sentAt = minutesAgo(90), fromMe = true, isRead = true),
                MessageEntity(friendId = FRIEND, text = "See you Saturday?", sentAt = minutesAgo(30), fromMe = false, isRead = false),
            )
        )
    }

    private fun personEntity(id: String, name: String, wavesBack: Boolean = false) = PersonEntity(
        id = id,
        name = name,
        age = 27,
        city = "Bengaluru",
        neighborhood = "Indiranagar",
        latitude = 12.9784,
        longitude = 77.6408,
        occupation = "Designer",
        bio = "Hello there",
        interests = listOf("Hiking", "Coffee"),
        lastActiveAt = minutesAgo(5),
        wavesBack = wavesBack,
    )

    private object EmptyApi : BuddyUpApi {
        override suspend fun nearbyPeople(): List<PersonDto> = emptyList()
        override suspend fun incomingWaves(): List<IncomingRequestDto> = emptyList()
        override suspend fun friends(): List<FriendshipDto> = emptyList()
        override suspend fun meetups(): List<MeetupDto> = emptyList()
    }

    private companion object {
        const val STRANGER = "p01"
        const val FRIEND = "p03"
        const val INCOMING = "p05"
        const val SENT = "p23"
    }
}
