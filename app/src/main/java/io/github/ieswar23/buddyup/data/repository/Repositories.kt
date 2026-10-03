package io.github.ieswar23.buddyup.data.repository

import io.github.ieswar23.buddyup.domain.model.AppSettings
import io.github.ieswar23.buddyup.domain.model.BlockedPerson
import io.github.ieswar23.buddyup.domain.model.ChatMessage
import io.github.ieswar23.buddyup.domain.model.FriendRequest
import io.github.ieswar23.buddyup.domain.model.FriendSummary
import io.github.ieswar23.buddyup.domain.model.Meetup
import io.github.ieswar23.buddyup.domain.model.Person
import io.github.ieswar23.buddyup.domain.model.Report
import io.github.ieswar23.buddyup.domain.model.ThemeMode
import io.github.ieswar23.buddyup.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

sealed interface SyncStatus {
    data object Idle : SyncStatus
    data object Syncing : SyncStatus
    data object Synced : SyncStatus
    data class Failed(val message: String) : SyncStatus
}

/** The signed-in user's own profile and app settings (backed by DataStore). */
interface UserRepository {
    val profile: Flow<UserProfile>
    val settings: Flow<AppSettings>
    val onboardingComplete: Flow<Boolean>
    val memberSince: Flow<Long?>

    suspend fun saveProfile(profile: UserProfile)
    suspend fun completeOnboarding()
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setNotificationsEnabled(enabled: Boolean)
}

/** People nearby and the Discover actions the user can take on them. */
interface PeopleRepository {
    val syncStatus: StateFlow<SyncStatus>

    /** People the user hasn't passed on, waved at or connected with yet. */
    fun observeDiscoverable(): Flow<List<Person>>

    fun observePerson(personId: String): Flow<Person?>

    suspend fun pass(personId: String)
    suspend fun wave(personId: String)

    /** Reverts a pass so the person shows up in Discover again. */
    suspend fun undoPass(personId: String)

    /** Clears every pass so all skipped profiles can be reviewed again. */
    suspend fun resetPassed()

    suspend fun syncIfNeeded()
    suspend fun refresh(): Result<Unit>
}

interface RequestsRepository {
    fun observeIncoming(): Flow<List<FriendRequest>>
    fun observeSent(): Flow<List<FriendRequest>>
    fun observeIncomingCount(): Flow<Int>

    suspend fun accept(personId: String)
    suspend fun decline(personId: String)
    suspend fun withdraw(personId: String)

    /** Puts a request back exactly as it was — used by snackbar "Undo". */
    suspend fun restore(request: FriendRequest)
}

interface FriendsRepository {
    fun observeFriends(): Flow<List<FriendSummary>>
}

interface ChatRepository {
    /** Friend ids for whom a "typing…" indicator should currently be shown. */
    val typingFriendIds: StateFlow<Set<String>>

    fun observeConversation(friendId: String): Flow<List<ChatMessage>>
    suspend fun send(friendId: String, text: String)
    suspend fun markRead(friendId: String)
}

interface MeetupRepository {
    fun observeMeetups(): Flow<List<Meetup>>

    /** Joins or leaves a meetup. Returns true when the user is now attending. */
    suspend fun toggleJoin(meetupId: String): Boolean
    suspend fun refresh(): Result<Unit>
}

/**
 * Blocking and reporting. Blocked people are filtered out of Discover, Waves, Friends and chats
 * by the other repositories, and the [io.github.ieswar23.buddyup.data.simulation.BuddySimulator]
 * never acts on their behalf.
 */
interface SafetyRepository {
    /** Blocked people, most recently blocked first. */
    fun observeBlocked(): Flow<List<BlockedPerson>>
    fun observeBlockedIds(): Flow<Set<String>>

    /** Blocks [personId], optionally filing a [report] about them. */
    suspend fun block(personId: String, report: Report? = null)
    suspend fun unblock(personId: String)
}
