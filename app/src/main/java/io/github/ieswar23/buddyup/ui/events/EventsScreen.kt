package io.github.ieswar23.buddyup.ui.events

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.ieswar23.buddyup.R
import io.github.ieswar23.buddyup.domain.model.Meetup
import io.github.ieswar23.buddyup.domain.model.MeetupCategory
import io.github.ieswar23.buddyup.ui.components.CardSkeleton
import io.github.ieswar23.buddyup.ui.components.EmptyState
import io.github.ieswar23.buddyup.ui.components.Tag
import io.github.ieswar23.buddyup.ui.theme.gradientFor
import io.github.ieswar23.buddyup.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsScreen(viewModel: EventsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message ->
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
                        Text(stringResource(R.string.events_title), style = MaterialTheme.typography.titleLarge)
                        (state as? EventsUiState.Content)?.let {
                            Text(
                                text = if (it.goingCount > 0) "You're going to ${it.goingCount} meetup${if (it.goingCount == 1) "" else "s"}"
                                else "Group hangouts hosted by locals",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        when (val s = state) {
            EventsUiState.Loading -> Column(Modifier.padding(padding)) { CardSkeleton() }
            is EventsUiState.Content -> PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = viewModel::refresh,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 24.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    item(key = "filters") {
                        FilterRows(
                            city = s.city,
                            scope = s.scope,
                            category = s.category,
                            onScope = viewModel::selectScope,
                            onCategory = viewModel::selectCategory,
                        )
                    }
                    if (s.isEmpty) {
                        item(key = "empty") {
                            EmptyState(
                                emoji = if (s.scope == MeetupScope.GOING) "🗓️" else "🔍",
                                title = stringResource(R.string.events_empty_title),
                                body = if (s.scope == MeetupScope.GOING) stringResource(R.string.events_empty_body)
                                else "No meetups match these filters right now. Try another category or city.",
                                primaryAction = stringResource(R.string.events_browse_all) to {
                                    viewModel.selectCategory(null)
                                    viewModel.selectScope(MeetupScope.ALL)
                                },
                            )
                        }
                    }
                    s.sections.forEach { section ->
                        item(key = "header-${section.title}") {
                            Text(
                                text = section.title,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier
                                    .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp)
                                    .animateItem(),
                            )
                        }
                        items(section.meetups, key = { it.id }) { meetup ->
                            MeetupCard(
                                meetup = meetup,
                                now = s.now,
                                showCity = s.scope != MeetupScope.NEAR_ME,
                                onToggleJoin = { viewModel.toggleJoin(meetup) },
                                modifier = Modifier
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                                    .animateItem(),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterRows(
    city: String,
    scope: MeetupScope,
    category: MeetupCategory?,
    onScope: (MeetupScope) -> Unit,
    onCategory: (MeetupCategory?) -> Unit,
) {
    Column(Modifier.padding(top = 4.dp)) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val scopes = listOf(
                MeetupScope.NEAR_ME to "📍 $city",
                MeetupScope.GOING to "✅ Going",
                MeetupScope.ALL to "🌍 All cities",
            )
            items(scopes) { (value, label) ->
                FilterChip(
                    selected = scope == value,
                    onClick = { onScope(value) },
                    label = { Text(label) },
                    shape = CircleShape,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                )
            }
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(MeetupCategory.entries) { value ->
                FilterChip(
                    selected = category == value,
                    onClick = { onCategory(value) },
                    label = { Text("${value.emoji} ${value.label}") },
                    shape = CircleShape,
                )
            }
        }
    }
}

@Composable
private fun MeetupCard(
    meetup: Meetup,
    now: Long,
    showCity: Boolean,
    onToggleJoin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable(meetup.id) { mutableStateOf(false) }
    val (start, end) = gradientFor(meetup.category.name + meetup.id)
    val chevron by animateFloatAsState(if (expanded) 180f else 0f, label = "chevron")

    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = { expanded = !expanded },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
                .background(Brush.horizontalGradient(listOf(start, end))),
        ) {
            Text(
                text = meetup.emoji,
                fontSize = 54.sp,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 20.dp)
                    .rotate(-8f),
            )
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 18.dp),
            ) {
                Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.25f), contentColor = Color.White) {
                    Text(
                        text = "${meetup.category.emoji} ${meetup.category.label}",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = when {
                        meetup.isJoined -> "You're going ✓"
                        meetup.isFull -> "Fully booked"
                        meetup.spotsLeft <= 5 -> "Only ${meetup.spotsLeft} spot${if (meetup.spotsLeft == 1) "" else "s"} left!"
                        else -> "${meetup.spotsLeft} spots left"
                    },
                    color = Color.White,
                    style = MaterialTheme.typography.titleSmall,
                )
            }
        }

        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = meetup.title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Icon(
                    Icons.Outlined.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    modifier = Modifier.rotate(chevron),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            InfoRow(Icons.Outlined.CalendarMonth, Formatters.meetupDate(meetup.startsAt, now))
            InfoRow(Icons.Outlined.Place, if (showCity) "${meetup.venue} · ${meetup.city}" else meetup.venue)
            InfoRow(Icons.Outlined.Schedule, "${Formatters.duration(meetup.durationMinutes)} · Hosted by ${meetup.host}")

            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = meetup.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        meetup.tags.forEach { Tag(it) }
                    }
                }
            }

            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "${meetup.attendeeCount} going · ${meetup.capacity} max",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { meetup.fillRatio },
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .height(6.dp),
                        color = MaterialTheme.colorScheme.secondary,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        strokeCap = StrokeCap.Round,
                        gapSize = 0.dp,
                        drawStopIndicator = {},
                    )
                }
                if (meetup.isJoined) {
                    FilledTonalButton(onClick = onToggleJoin) {
                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.action_leave))
                    }
                } else {
                    Button(onClick = onToggleJoin, enabled = !meetup.isFull) {
                        Text(if (meetup.isFull) stringResource(R.string.events_full) else stringResource(R.string.action_join))
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
