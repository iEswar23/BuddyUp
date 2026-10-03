package io.github.ieswar23.buddyup.ui

import com.google.common.truth.Truth.assertThat
import io.github.ieswar23.buddyup.MainDispatcherRule
import io.github.ieswar23.buddyup.data.repository.SyncStatus
import io.github.ieswar23.buddyup.domain.model.DiscoverFilters
import io.github.ieswar23.buddyup.domain.model.Report
import io.github.ieswar23.buddyup.domain.model.ReportReason
import io.github.ieswar23.buddyup.domain.scoring.MatchScorer
import io.github.ieswar23.buddyup.fakes.FakePeopleRepository
import io.github.ieswar23.buddyup.fakes.FakeSafetyRepository
import io.github.ieswar23.buddyup.fakes.FakeUserRepository
import io.github.ieswar23.buddyup.fakes.NOW
import io.github.ieswar23.buddyup.fakes.person
import io.github.ieswar23.buddyup.ui.discover.DiscoverEvent
import io.github.ieswar23.buddyup.ui.discover.DiscoverUiState
import io.github.ieswar23.buddyup.ui.discover.DiscoverViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DiscoverViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val bestMatch = person("best", interests = listOf("Hiking", "Coffee", "Chess", "Photography"), distanceKm = 1.0)
    private val okMatch = person("ok", interests = listOf("Hiking", "Cricket"), distanceKm = 8.0, age = 41)
    private val farAway = person("far", interests = listOf("Chess", "Yoga"), distanceKm = 1_200.0)
    private val safetyRepository = FakeSafetyRepository()

    private fun createViewModel(repo: FakePeopleRepository) =
        DiscoverViewModel(repo, FakeUserRepository(), safetyRepository, MatchScorer(), timeProvider = { NOW })

    private fun TestScope.observe(vm: DiscoverViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
    }

    @Test
    fun `cards are sorted by compatibility and far people hidden by default distance`() = runTest {
        val vm = createViewModel(FakePeopleRepository(listOf(okMatch, farAway, bestMatch)))
        observe(vm)

        val state = vm.uiState.value as DiscoverUiState.Content
        assertThat(state.cards.map { it.person.id }).containsExactly("best", "ok").inOrder()
        assertThat(state.cards.first().match.sharedInterests).containsExactly("Hiking", "Coffee", "Chess", "Photography")
    }

    @Test
    fun `pass removes the card, records the pass and enables undo`() = runTest {
        val repo = FakePeopleRepository(listOf(bestMatch, okMatch))
        val vm = createViewModel(repo)
        observe(vm)

        vm.onPass(bestMatch)

        assertThat(repo.passed).containsExactly("best")
        assertThat(vm.lastPassed.value?.id).isEqualTo("best")
        val state = vm.uiState.value as DiscoverUiState.Content
        assertThat(state.cards.map { it.person.id }).containsExactly("ok")
    }

    @Test
    fun `undo last pass brings the person back`() = runTest {
        val repo = FakePeopleRepository(listOf(bestMatch, okMatch))
        val vm = createViewModel(repo)
        observe(vm)

        vm.onPass(bestMatch)
        vm.undoLastPass()

        assertThat(vm.lastPassed.value).isNull()
        val state = vm.uiState.value as DiscoverUiState.Content
        assertThat(state.cards.map { it.person.id }).containsExactly("best", "ok").inOrder()
    }

    @Test
    fun `wave sends a wave and emits a confirmation event`() = runTest {
        val repo = FakePeopleRepository(listOf(bestMatch, okMatch))
        val vm = createViewModel(repo)
        observe(vm)

        vm.onWave(bestMatch)

        assertThat(repo.waved).containsExactly("best")
        assertThat(vm.events.first()).isEqualTo(DiscoverEvent.WaveSent("Test"))
        val state = vm.uiState.value as DiscoverUiState.Content
        assertThat(state.cards.map { it.person.id }).doesNotContain("best")
    }

    @Test
    fun `filters narrow the deck and empty state reports active filters`() = runTest {
        val vm = createViewModel(FakePeopleRepository(listOf(bestMatch, okMatch, farAway)))
        observe(vm)

        vm.applyFilters(DiscoverFilters(minAge = 35, maxAge = 60))
        assertThat((vm.uiState.value as DiscoverUiState.Content).cards.map { it.person.id }).containsExactly("ok")

        vm.applyFilters(DiscoverFilters(interests = setOf("Baking")))
        assertThat(vm.uiState.value).isEqualTo(DiscoverUiState.Empty(hasActiveFilters = true))
        assertThat(vm.countMatching(DiscoverFilters(maxDistanceKm = DiscoverFilters.ANYWHERE_KM))).isEqualTo(3)

        vm.searchAnywhere()
        assertThat((vm.uiState.value as DiscoverUiState.Content).cards.map { it.person.id }).contains("far")
    }

    @Test
    fun `shows loading while syncing and error when the first sync fails`() = runTest {
        val repo = FakePeopleRepository(emptyList()).apply { setSyncStatus(SyncStatus.Syncing) }
        val vm = createViewModel(repo)
        observe(vm)
        assertThat(vm.uiState.value).isEqualTo(DiscoverUiState.Loading)

        repo.setSyncStatus(SyncStatus.Failed("Offline"))
        assertThat(vm.uiState.value).isEqualTo(DiscoverUiState.Error("Offline"))

        repo.setSyncStatus(SyncStatus.Synced)
        assertThat(vm.uiState.value).isEqualTo(DiscoverUiState.Empty(hasActiveFilters = false))
    }

    @Test
    fun `blocking from a card hides it, records the block and clears undo for that person`() = runTest {
        val repo = FakePeopleRepository(listOf(bestMatch, okMatch))
        val vm = createViewModel(repo)
        observe(vm)
        vm.onPass(bestMatch)
        vm.undoLastPass()
        vm.onPass(okMatch)

        val report = Report(ReportReason.FAKE_PROFILE, note = "Same bio as another profile")
        vm.onBlock(okMatch, report)

        assertThat(safetyRepository.blocks.value).containsExactly("ok", report)
        assertThat(vm.lastPassed.value).isNull()
        val state = vm.uiState.value as DiscoverUiState.Content
        assertThat(state.cards.map { it.person.id }).containsExactly("best")
    }
}
