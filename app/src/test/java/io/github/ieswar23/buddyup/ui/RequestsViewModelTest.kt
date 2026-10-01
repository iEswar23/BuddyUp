package io.github.ieswar23.buddyup.ui

import com.google.common.truth.Truth.assertThat
import io.github.ieswar23.buddyup.MainDispatcherRule
import io.github.ieswar23.buddyup.domain.model.FriendRequest
import io.github.ieswar23.buddyup.domain.model.RequestDirection
import io.github.ieswar23.buddyup.domain.scoring.MatchScorer
import io.github.ieswar23.buddyup.fakes.FakeRequestsRepository
import io.github.ieswar23.buddyup.fakes.FakeUserRepository
import io.github.ieswar23.buddyup.fakes.NOW
import io.github.ieswar23.buddyup.fakes.minutesAgo
import io.github.ieswar23.buddyup.fakes.person
import io.github.ieswar23.buddyup.ui.requests.RequestsEvent
import io.github.ieswar23.buddyup.ui.requests.RequestsViewModel
import io.github.ieswar23.buddyup.ui.requests.UndoableAction
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RequestsViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val sneha = FriendRequest(person("p05", name = "Sneha Kulkarni"), RequestDirection.INCOMING, "Hi!", minutesAgo(25))
    private val karthik = FriendRequest(person("p10", name = "Karthik Reddy"), RequestDirection.INCOMING, null, minutesAgo(180))
    private val emily = FriendRequest(person("p23", name = "Emily Carter"), RequestDirection.OUTGOING, null, minutesAgo(10))

    private lateinit var repo: FakeRequestsRepository

    private fun TestScope.createViewModel(): RequestsViewModel {
        repo = FakeRequestsRepository(incoming = listOf(sneha, karthik), sent = listOf(emily))
        val vm = RequestsViewModel(repo, FakeUserRepository(), MatchScorer(), timeProvider = { NOW })
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        return vm
    }

    @Test
    fun `exposes incoming and sent requests with match scores`() = runTest {
        val vm = createViewModel()
        val state = vm.uiState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.incoming.map { it.personId }).containsExactly("p05", "p10").inOrder()
        assertThat(state.sent.map { it.personId }).containsExactly("p23")
        assertThat(state.incoming.first().match.percent).isGreaterThan(0)
    }

    @Test
    fun `accepting makes them a friend and offers undo`() = runTest {
        val vm = createViewModel()
        vm.accept(vm.uiState.value.incoming.first())

        assertThat(repo.friends.value.map { it.id }).containsExactly("p05")
        assertThat(vm.uiState.value.incoming.map { it.personId }).containsExactly("p10")
        val event = vm.events.first() as RequestsEvent.ShowUndo
        assertThat(event.action).isInstanceOf(UndoableAction.Accepted::class.java)
        assertThat(event.message).contains("Sneha")
    }

    @Test
    fun `declining removes the request and undo restores it`() = runTest {
        val vm = createViewModel()
        vm.decline(vm.uiState.value.incoming.last())

        assertThat(repo.declined).containsExactly("p10")
        assertThat(vm.uiState.value.incoming.map { it.personId }).containsExactly("p05")

        val event = vm.events.first() as RequestsEvent.ShowUndo
        vm.undo(event.action)

        assertThat(repo.declined).isEmpty()
        assertThat(vm.uiState.value.incoming.map { it.personId }).containsExactly("p05", "p10").inOrder()
    }

    @Test
    fun `undoing an accept puts the request back in incoming`() = runTest {
        val vm = createViewModel()
        vm.accept(vm.uiState.value.incoming.first())
        val event = vm.events.first() as RequestsEvent.ShowUndo

        vm.undo(event.action)

        assertThat(repo.friends.value).isEmpty()
        assertThat(vm.uiState.value.incoming.first().request.note).isEqualTo("Hi!")
    }

    @Test
    fun `withdrawing a sent wave removes it from the sent tab`() = runTest {
        val vm = createViewModel()
        vm.withdraw(vm.uiState.value.sent.single())
        assertThat(vm.uiState.value.sent).isEmpty()
    }
}
