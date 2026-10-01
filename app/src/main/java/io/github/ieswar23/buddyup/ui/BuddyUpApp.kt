package io.github.ieswar23.buddyup.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.ieswar23.buddyup.data.simulation.AppEvent
import io.github.ieswar23.buddyup.ui.chat.ChatScreen
import io.github.ieswar23.buddyup.ui.discover.DiscoverScreen
import io.github.ieswar23.buddyup.ui.events.EventsScreen
import io.github.ieswar23.buddyup.ui.friends.FriendsScreen
import io.github.ieswar23.buddyup.ui.navigation.Screen
import io.github.ieswar23.buddyup.ui.navigation.TopLevelDestination
import io.github.ieswar23.buddyup.ui.onboarding.OnboardingScreen
import io.github.ieswar23.buddyup.ui.profile.ProfileScreen
import io.github.ieswar23.buddyup.ui.requests.RequestsScreen
import io.github.ieswar23.buddyup.ui.setup.ProfileFormScreen

/** Root composable: app chrome (bottom bar, global alerts) around the navigation graph. */
@Composable
fun BuddyUpApp(
    onboardingComplete: Boolean,
    viewModel: MainViewModel,
    navController: NavHostController = rememberNavController(),
) {
    val chrome by viewModel.chrome.collectAsStateWithLifecycle()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = TopLevelDestination.fromRoute(backStackEntry?.destination?.route)
    val snackbarHostState = remember { SnackbarHostState() }
    val startDestination = remember { if (onboardingComplete) Screen.Discover.route else Screen.Onboarding.route }

    LaunchedEffect(viewModel) {
        viewModel.alerts.collect { event ->
            when (event) {
                is AppEvent.WavedBack -> {
                    val result = snackbarHostState.showSnackbar(
                        message = "${event.firstName} waved back! You're now buddies 🎉",
                        actionLabel = "Say hi",
                        duration = SnackbarDuration.Long,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        navController.navigate(Screen.Chat.createRoute(event.personId))
                    }
                }
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            AnimatedVisibility(
                visible = currentDestination != null,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
            ) {
                BuddyBottomBar(
                    current = currentDestination,
                    incomingWaves = chrome.incomingWaves,
                    unreadMessages = chrome.unreadMessages,
                    onNavigate = { navController.navigateToTab(it) },
                )
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding()),
            enterTransition = { fadeIn(tween(220)) },
            exitTransition = { fadeOut(tween(160)) },
        ) {
            composable(Screen.Onboarding.route) {
                OnboardingScreen(onFinished = { navController.navigate(Screen.ProfileSetup.route) })
            }
            composable(
                Screen.ProfileSetup.route,
                enterTransition = { slideInHorizontally { it / 3 } + fadeIn() },
            ) {
                ProfileFormScreen(
                    isEditing = false,
                    onBack = { navController.popBackStack() },
                    onDone = {
                        navController.navigate(Screen.Discover.route) {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    },
                )
            }
            composable(Screen.Discover.route) { DiscoverScreen() }
            composable(Screen.Requests.route) {
                RequestsScreen(onGoToDiscover = { navController.navigateToTab(TopLevelDestination.DISCOVER) })
            }
            composable(Screen.Friends.route) {
                FriendsScreen(
                    onOpenChat = { navController.navigate(Screen.Chat.createRoute(it)) },
                    onGoToDiscover = { navController.navigateToTab(TopLevelDestination.DISCOVER) },
                )
            }
            composable(Screen.Events.route) { EventsScreen() }
            composable(Screen.Profile.route) {
                ProfileScreen(onEditProfile = { navController.navigate(Screen.EditProfile.route) })
            }
            composable(
                Screen.EditProfile.route,
                enterTransition = { slideInVertically { it / 4 } + fadeIn() },
                popExitTransition = { slideOutVertically { it / 4 } + fadeOut() },
            ) {
                ProfileFormScreen(
                    isEditing = true,
                    onBack = { navController.popBackStack() },
                    onDone = { navController.popBackStack() },
                )
            }
            composable(
                route = Screen.Chat.route,
                arguments = listOf(navArgument(Screen.Chat.ARG_FRIEND_ID) { type = NavType.StringType }),
                enterTransition = { slideInHorizontally { it } },
                exitTransition = { ExitTransition.None },
                popEnterTransition = { EnterTransition.None },
                popExitTransition = { slideOutHorizontally { it } },
            ) {
                ChatScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

@Composable
private fun BuddyBottomBar(
    current: TopLevelDestination?,
    incomingWaves: Int,
    unreadMessages: Int,
    onNavigate: (TopLevelDestination) -> Unit,
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        tonalElevation = 0.dp,
    ) {
        TopLevelDestination.entries.forEach { destination ->
            val selected = destination == current
            val badge = when (destination) {
                TopLevelDestination.REQUESTS -> incomingWaves
                TopLevelDestination.FRIENDS -> unreadMessages
                else -> 0
            }
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(destination) },
                icon = {
                    BadgedBox(badge = { if (badge > 0) Badge { Text(if (badge > 9) "9+" else badge.toString()) } }) {
                        Icon(
                            imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
                            contentDescription = null,
                        )
                    }
                },
                label = { Text(stringResource(destination.label), style = MaterialTheme.typography.labelSmall) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            )
        }
    }
}

private fun NavHostController.navigateToTab(destination: TopLevelDestination) {
    navigate(destination.screen.route) {
        // Discover is always the root of the main back stack (both for returning users and right
        // after onboarding, which clears the stack), so tabs never pile up behind each other.
        popUpTo(Screen.Discover.route) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
