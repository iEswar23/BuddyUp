package io.github.ieswar23.buddyup.ui

import com.google.common.truth.Truth.assertThat
import io.github.ieswar23.buddyup.MainDispatcherRule
import io.github.ieswar23.buddyup.domain.model.Report
import io.github.ieswar23.buddyup.domain.model.ReportReason
import io.github.ieswar23.buddyup.fakes.FakeFriendsRepository
import io.github.ieswar23.buddyup.fakes.FakeMeetupRepository
import io.github.ieswar23.buddyup.fakes.FakeRequestsRepository
import io.github.ieswar23.buddyup.fakes.FakeSafetyRepository
import io.github.ieswar23.buddyup.fakes.FakeUserRepository
import io.github.ieswar23.buddyup.fakes.person
import io.github.ieswar23.buddyup.ui.profile.ProfileViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val rahul = person("p14", name = "Rahul Varma")
    private val marcus = person("p24", name = "Marcus Johnson")
    private val safetyRepository = FakeSafetyRepository(listOf(rahul, marcus))

    private fun TestScope.createViewModel(): ProfileViewModel {
        val vm = ProfileViewModel(
            userRepository = FakeUserRepository(),
            friendsRepository = FakeFriendsRepository(),
            requestsRepository = FakeRequestsRepository(),
            meetupRepository = FakeMeetupRepository(),
            safetyRepository = safetyRepository,
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        return vm
    }

    @Test
    fun `blocked people are listed newest first with their report reason`() = runTest {
        safetyRepository.block("p14", report = null)
        safetyRepository.block("p24", Report(ReportReason.HARASSMENT, note = "Kept messaging after I said no"))
        val vm = createViewModel()

        val blocked = vm.uiState.value.blocked
        assertThat(blocked.map { it.person.name }).containsExactly("Marcus Johnson", "Rahul Varma").inOrder()
        assertThat(blocked.first().report?.reason).isEqualTo(ReportReason.HARASSMENT)
        assertThat(blocked.first().report?.note).isEqualTo("Kept messaging after I said no")
        assertThat(blocked.last().report).isNull()
    }

    @Test
    fun `unblocking removes the person from the blocked list`() = runTest {
        safetyRepository.block("p14", report = null)
        val vm = createViewModel()
        assertThat(vm.uiState.value.blocked).hasSize(1)

        vm.unblock("p14")

        assertThat(vm.uiState.value.blocked).isEmpty()
        assertThat(safetyRepository.blocks.value).isEmpty()
    }
}
