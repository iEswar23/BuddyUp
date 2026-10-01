package io.github.ieswar23.buddyup.ui.discover

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WavingHand
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.ieswar23.buddyup.R
import io.github.ieswar23.buddyup.domain.model.Person
import io.github.ieswar23.buddyup.ui.components.CardSkeleton
import io.github.ieswar23.buddyup.ui.components.EmptyState
import io.github.ieswar23.buddyup.ui.theme.AvatarGradients
import kotlinx.coroutines.launch
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(viewModel: DiscoverViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val filters by viewModel.filters.collectAsStateWithLifecycle()
    val header by viewModel.header.collectAsStateWithLifecycle()
    val lastPassed by viewModel.lastPassed.collectAsStateWithLifecycle()
    val filterInterests by viewModel.filterInterests.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showFilters by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            val message = when (event) {
                is DiscoverEvent.WaveSent -> "Wave sent to ${event.firstName} 👋"
                is DiscoverEvent.Message -> event.text
            }
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.discover_title), style = MaterialTheme.typography.titleLarge)
                        if (header.city.isNotBlank()) {
                            Text(
                                text = "📍 ${header.city} · ${header.nearbyCount} ${if (header.nearbyCount == 1) "buddy" else "buddies"} to meet",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showFilters = true }) {
                        BadgedBox(badge = {
                            if (filters.activeCount > 0) Badge { Text(filters.activeCount.toString()) }
                        }) {
                            Icon(Icons.Filled.Tune, contentDescription = stringResource(R.string.action_filters))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            AnimatedContent(
                targetState = uiState,
                contentKey = { it::class },
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "discoverState",
            ) { state ->
                when (state) {
                    DiscoverUiState.Loading -> CardSkeleton(Modifier.fillMaxSize())
                    is DiscoverUiState.Error -> EmptyState(
                        emoji = "📡",
                        title = stringResource(R.string.discover_error_title),
                        body = state.message,
                        primaryAction = stringResource(R.string.retry) to viewModel::retry,
                        modifier = Modifier.fillMaxSize(),
                    )
                    is DiscoverUiState.Empty -> EmptyState(
                        emoji = "🎉",
                        title = stringResource(R.string.discover_empty_title),
                        body = stringResource(R.string.discover_empty_body),
                        primaryAction = if (filters.isAnywhere && state.hasActiveFilters) {
                            stringResource(R.string.filters_reset) to viewModel::resetFilters
                        } else {
                            stringResource(R.string.discover_expand) to viewModel::searchAnywhere
                        },
                        secondaryAction = stringResource(R.string.discover_start_over) to viewModel::revisitPassed,
                        modifier = Modifier.fillMaxSize(),
                    )
                    is DiscoverUiState.Content -> CardDeck(
                        cards = state.cards,
                        canRewind = lastPassed != null,
                        onPass = viewModel::onPass,
                        onWave = viewModel::onWave,
                        onRewind = viewModel::undoLastPass,
                        onFilters = { showFilters = true },
                    )
                }
            }
        }
    }

    if (showFilters) {
        FilterSheet(
            current = filters,
            interests = filterInterests,
            matchingCount = viewModel::countMatching,
            onApply = {
                viewModel.applyFilters(it)
                showFilters = false
            },
            onDismiss = { showFilters = false },
        )
    }
}

@Composable
private fun CardDeck(
    cards: List<DiscoverCard>,
    canRewind: Boolean,
    onPass: (Person) -> Unit,
    onWave: (Person) -> Unit,
    onRewind: () -> Unit,
    onFilters: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val now = remember(cards.firstOrNull()?.person?.id) { System.currentTimeMillis() }
    val top = cards.first()
    val topState = remember(top.person.id) { SwipeCardState() }
    val visible = cards.take(3)

    fun commit(direction: SwipeDirection) {
        if (direction == SwipeDirection.RIGHT) onWave(top.person) else onPass(top.person)
    }

    fun swipeFromButton(direction: SwipeDirection) {
        if (!topState.isIdle()) return
        scope.launch {
            topState.flyOut(direction)
            commit(direction)
        }
    }

    Column(Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 8.dp),
        ) {
            val dragProgress = abs(topState.progress)
            for (index in visible.indices.reversed()) {
                val card = visible[index]
                key(card.person.id) {
                    if (index == 0) {
                        SwipeableCard(
                            state = topState,
                            onSwiped = ::commit,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            ProfileCard(card = card, now = now, swipeProgress = topState.progress, modifier = Modifier.fillMaxSize())
                        }
                    } else {
                        // Cards underneath peek out and grow as the top card is dragged away.
                        val depth = index - dragProgress
                        val scale = 1f - 0.05f * depth
                        ProfileCard(
                            card = card,
                            now = now,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    translationY = 18.dp.toPx() * depth
                                    alpha = if (index >= 2) (1f - 0.4f * depth).coerceIn(0f, 1f) else 1f
                                },
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp, top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundActionButton(
                size = 48.dp,
                onClick = onRewind,
                enabled = canRewind,
                description = stringResource(R.string.action_rewind),
            ) {
                Icon(Icons.AutoMirrored.Filled.Undo, null, tint = MaterialTheme.colorScheme.tertiary)
            }
            RoundActionButton(
                size = 68.dp,
                onClick = { swipeFromButton(SwipeDirection.LEFT) },
                description = stringResource(R.string.action_pass),
            ) {
                Icon(Icons.Filled.Close, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(32.dp))
            }
            RoundActionButton(
                size = 76.dp,
                onClick = { swipeFromButton(SwipeDirection.RIGHT) },
                description = stringResource(R.string.action_wave),
                gradient = AvatarGradients[0],
            ) {
                Icon(Icons.Filled.WavingHand, null, tint = Color.White, modifier = Modifier.size(34.dp))
            }
            RoundActionButton(
                size = 48.dp,
                onClick = onFilters,
                description = stringResource(R.string.action_filters),
            ) {
                Icon(Icons.Filled.Tune, null, tint = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

@Composable
private fun RoundActionButton(
    size: Dp,
    onClick: () -> Unit,
    description: String,
    enabled: Boolean = true,
    gradient: Pair<Color, Color>? = null,
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.88f else 1f, label = "pressScale")
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = if (gradient == null) MaterialTheme.colorScheme.surfaceContainerLowest else Color.Transparent,
        shadowElevation = if (enabled) 6.dp else 0.dp,
        interactionSource = interaction,
        modifier = Modifier
            .size(size)
            .scale(scale)
            .graphicsLayer { alpha = if (enabled) 1f else 0.45f }
            .semantics { contentDescription = description },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (gradient != null) {
                        Modifier
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(gradient.first, gradient.second)))
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center,
        ) { content() }
    }
}
