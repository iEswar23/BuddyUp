package io.github.ieswar23.buddyup.fakes

import io.github.ieswar23.buddyup.data.repository.ChatRepository
import io.github.ieswar23.buddyup.data.repository.FriendsRepository
import io.github.ieswar23.buddyup.data.repository.MeetupRepository
import io.github.ieswar23.buddyup.data.repository.PeopleRepository
import io.github.ieswar23.buddyup.data.repository.RequestsRepository
import io.github.ieswar23.buddyup.data.repository.SafetyRepository
import io.github.ieswar23.buddyup.data.repository.SyncStatus
import io.github.ieswar23.buddyup.data.repository.UserRepository
import io.github.ieswar23.buddyup.domain.model.AppSettings
import io.github.ieswar23.buddyup.domain.model.BlockedPerson
import io.github.ieswar23.buddyup.domain.model.ChatMessage
import io.github.ieswar23.buddyup.domain.model.FriendRequest
import io.github.ieswar23.buddyup.domain.model.FriendSummary
import io.github.ieswar23.buddyup.domain.model.Meetup
import io.github.ieswar23.buddyup.domain.model.Person
import io.github.ieswar23.buddyup.domain.model.Report
import io.github.ieswar23.buddyup.domain.model.RequestDirection
import io.github.ieswar23.buddyup.domain.model.ThemeMode
import io.github.ieswar23.buddyup.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeUserRepository(initial: UserProfile = testProfile) : UserRepository {
    private val _profile = MutableStateFlow(initial)
    private val _settings = MutableStateFlow(AppSettings())
    private val _onboarding = MutableStateFlow(false)

    override val profile: Flow<UserProfile> = _profile
    override val settings: Flow<AppSettings> = _settings
    override val onboardingComplete: Flow<Boolean> = _onboarding
    override val memberSince: Flow<Long?> = MutableStateFlow(NOW)

    override suspend fun saveProfile(profile: UserProfile) { _profile.value = profile }
    override suspend fun completeOnboarding() { _onboarding.value = true }
    override suspend fun setThemeMode(mode: ThemeMode) { _settings.update { it.copy(themeMode = mode) } }
    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        _settings.update { it.copy(notificationsEnabled = enabled) }
    }
}

class FakePeopleRepository(people: List<Person> = emptyList()) : PeopleRepository {
    val unseen = MutableStateFlow(people)
    val passed = mutableListOf<String>()
    val waved = mutableListOf<String>()
    var refreshResult: Result<Unit> = Result.success(Unit)
    private val all = people.associateBy { it.id }.toMutableMap()

    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Synced)
    override val syncStatus: StateFlow<SyncStatus> = _syncStatus

    fun setSyncStatus(status: SyncStatus) { _syncStatus.value = status }

    override fun observeDiscoverable(): Flow<List<Person>> = unseen
    override fun observePerson(personId: String): Flow<Person?> = unseen.map { all[personId] }

    override suspend fun pass(personId: String) {
        passed += personId
        unseen.update { list -> list.filterNot { it.id == personId } }
    }

    override suspend fun wave(personId: String) {
        waved += personId
        unseen.update { list -> list.filterNot { it.id == personId } }
    }

    override suspend fun undoPass(personId: String) {
        if (passed.remove(personId)) {
            all[personId]?.let { p -> unseen.update { it + p } }
        }
    }

    override suspend fun resetPassed() {
        val restored = passed.mapNotNull { all[it] }
        passed.clear()
        unseen.update { it + restored }
    }

    override suspend fun syncIfNeeded() = Unit
    override suspend fun refresh(): Result<Unit> = refreshResult
}

class FakeRequestsRepository(
    incoming: List<FriendRequest> = emptyList(),
    sent: List<FriendRequest> = emptyList(),
) : RequestsRepository {
    val incoming = MutableStateFlow(incoming)
    val sent = MutableStateFlow(sent)
    val friends = MutableStateFlow<List<Person>>(emptyList())
    val declined = mutableListOf<String>()

    override fun observeIncoming(): Flow<List<FriendRequest>> = incoming
    override fun observeSent(): Flow<List<FriendRequest>> = sent
    override fun observeIncomingCount(): Flow<Int> = incoming.map { it.size }

    override suspend fun accept(personId: String) {
        val request = incoming.value.first { it.person.id == personId }
        incoming.update { list -> list - request }
        friends.update { it + request.person }
    }

    override suspend fun decline(personId: String) {
        incoming.update { list -> list.filterNot { it.person.id == personId } }
        declined += personId
    }

    override suspend fun withdraw(personId: String) {
        sent.update { list -> list.filterNot { it.person.id == personId } }
    }

    override suspend fun restore(request: FriendRequest) {
        friends.update { list -> list.filterNot { it.id == request.person.id } }
        declined.remove(request.person.id)
        when (request.direction) {
            RequestDirection.INCOMING -> incoming.update { list -> (list + request).sortedByDescending { it.createdAt } }
            RequestDirection.OUTGOING -> sent.update { list -> (list + request).sortedByDescending { it.createdAt } }
        }
    }
}

class FakeChatRepository(initial: Map<String, List<ChatMessage>> = emptyMap()) : ChatRepository {
    val conversations = MutableStateFlow(initial)
    val markedRead = mutableListOf<String>()
    private val _typing = MutableStateFlow<Set<String>>(emptySet())
    override val typingFriendIds: StateFlow<Set<String>> = _typing
    private var nextId = 1_000L

    fun setTyping(ids: Set<String>) { _typing.value = ids }

    override fun observeConversation(friendId: String): Flow<List<ChatMessage>> =
        conversations.map { it[friendId].orEmpty() }

    override suspend fun send(friendId: String, text: String) {
        val message = ChatMessage(nextId++, friendId, text, NOW, fromMe = true, isRead = true)
        conversations.update { it + (friendId to (it[friendId].orEmpty() + message)) }
    }

    override suspend fun markRead(friendId: String) {
        markedRead += friendId
        conversations.update { map ->
            map + (friendId to map[friendId].orEmpty().map { it.copy(isRead = true) })
        }
    }
}

class FakeSafetyRepository(people: List<Person> = emptyList()) : SafetyRepository {
    private val all = people.associateBy { it.id }
    /** Blocked person id → report, in blocking order. */
    val blocks = MutableStateFlow<Map<String, Report?>>(emptyMap())

    override fun observeBlocked(): Flow<List<BlockedPerson>> = blocks.map { map ->
        map.entries.reversed().mapNotNull { (id, report) -> all[id]?.let { BlockedPerson(it, report, NOW) } }
    }

    override fun observeBlockedIds(): Flow<Set<String>> = blocks.map { it.keys }

    override suspend fun block(personId: String, report: Report?) {
        blocks.update { it + (personId to report) }
    }

    override suspend fun unblock(personId: String) {
        blocks.update { it - personId }
    }
}

class FakeFriendsRepository(friends: List<FriendSummary> = emptyList()) : FriendsRepository {
    val friends = MutableStateFlow(friends)
    override fun observeFriends(): Flow<List<FriendSummary>> = friends
}

class FakeMeetupRepository(meetups: List<Meetup> = emptyList()) : MeetupRepository {
    val meetups = MutableStateFlow(meetups)
    override fun observeMeetups(): Flow<List<Meetup>> = meetups
    override suspend fun toggleJoin(meetupId: String): Boolean = false
    override suspend fun refresh(): Result<Unit> = Result.success(Unit)
}
