package io.github.ieswar23.buddyup.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.WavingHand
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.WavingHand
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.ieswar23.buddyup.R

/** Every navigable destination in the app. */
sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object ProfileSetup : Screen("profile_setup")
    data object Discover : Screen("discover")
    data object Requests : Screen("requests")
    data object Friends : Screen("friends")
    data object Events : Screen("events")
    data object Profile : Screen("profile")
    data object EditProfile : Screen("edit_profile")

    data object Chat : Screen("chat/{friendId}") {
        const val ARG_FRIEND_ID = "friendId"
        fun createRoute(friendId: String): String = "chat/$friendId"
    }
}

/** Tabs shown in the bottom navigation bar. */
enum class TopLevelDestination(
    val screen: Screen,
    @StringRes val label: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    DISCOVER(Screen.Discover, R.string.nav_discover, Icons.Filled.Explore, Icons.Outlined.Explore),
    REQUESTS(Screen.Requests, R.string.nav_requests, Icons.Filled.WavingHand, Icons.Outlined.WavingHand),
    FRIENDS(Screen.Friends, R.string.nav_friends, Icons.Filled.People, Icons.Outlined.People),
    EVENTS(Screen.Events, R.string.nav_events, Icons.Filled.Event, Icons.Outlined.Event),
    PROFILE(Screen.Profile, R.string.nav_profile, Icons.Filled.Person, Icons.Outlined.Person);

    companion object {
        private val routes = entries.associateBy { it.screen.route }
        fun fromRoute(route: String?): TopLevelDestination? = routes[route]
    }
}
