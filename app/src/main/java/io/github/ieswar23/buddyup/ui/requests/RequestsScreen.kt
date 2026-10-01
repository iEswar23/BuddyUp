package io.github.ieswar23.buddyup.ui.requests

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.WavingHand
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.ieswar23.buddyup.R
import io.github.ieswar23.buddyup.ui.components.EmptyState
import io.github.ieswar23.buddyup.ui.components.GradientAvatar
import io.github.ieswar23.buddyup.ui.components.InterestChip
import io.github.ieswar23.buddyup.ui.components.ListItemSkeleton
import io.github.ieswar23.buddyup.util.Formatters
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun RequestsScreen(
    onGoToDiscover: () -> Unit,
    viewModel: RequestsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is RequestsEvent.ShowUndo -> {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    val result = snackbarHostState.showSnackbar(
                        message = event.message,
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Long,
                    )
                    if (result == SnackbarResult.ActionPerformed) viewModel.undo(event.action)
                }
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(stringResource(R.string.requests_title), style = MaterialTheme.typography.titleLarge) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                )
                PrimaryTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    containerColor = MaterialTheme.colorScheme.background,
                ) {
                    listOf(
                        stringResource(R.string.requests_incoming) to state.incoming.size,
                        stringResource(R.string.requests_sent) to state.sent.size,
                    ).forEachIndexed { index, (label, count) ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(label, style = MaterialTheme.typography.titleSmall)
                                    if (count > 0) {
                                        Spacer(Modifier.width(6.dp))
                                        Badge(
                                            containerColor = if (index == 0) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.secondaryContainer,
                                            contentColor = if (index == 0) MaterialTheme.colorScheme.onPrimary
                                            else MaterialTheme.colorScheme.onSecondaryContainer,
                                        ) { Text(count.toString()) }
                                    }
                                }
                            },
                        )
                    }
                }
            }
        },
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) { page ->
            when {
                state.isLoading -> Column(Modifier.fillMaxSize()) { repeat(5) { ListItemSkeleton() } }
                page == 0 -> IncomingList(
                    items = state.incoming,
                    onAccept = viewModel::accept,
                    onDecline = viewModel::decline,
                    onGoToDiscover = onGoToDiscover,
                )
                else -> SentList(
                    items = state.sent,
                    onWithdraw = viewModel::withdraw,
                    onGoToDiscover = onGoToDiscover,
                )
            }
        }
    }
}

@Composable
private fun IncomingList(
    items: List<RequestItem>,
    onAccept: (RequestItem) -> Unit,
    onDecline: (RequestItem) -> Unit,
    onGoToDiscover: () -> Unit,
) {
    if (items.isEmpty()) {
        EmptyState(
            emoji = "📭",
            title = stringResource(R.string.requests_empty_incoming_title),
            body = stringResource(R.string.requests_empty_incoming_body),
            primaryAction = stringResource(R.string.go_discover) to onGoToDiscover,
            modifier = Modifier.fillMaxSize(),
        )
        return
    }
    val now = remember(items) { System.currentTimeMillis() }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(items, key = { it.personId }) { item ->
            IncomingRequestCard(
                item = item,
                now = now,
                onAccept = { onAccept(item) },
                onDecline = { onDecline(item) },
                modifier = Modifier.animateItem(),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IncomingRequestCard(
    item: RequestItem,
    now: Long,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val person = item.request.person
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GradientAvatar(person.initials, person.id, size = 56.dp, isOnline = person.isOnline(now))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("${person.name}, ${person.age}", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = Formatters.place(person.neighborhood, person.city, person.distanceKm),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                    ) {
                        Text(
                            "${item.match.percent}% match",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        Formatters.relativeShort(item.request.createdAt, now),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item.request.note?.takeIf { it.isNotBlank() }?.let { note ->
                Surface(
                    shape = RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 18.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                ) {
                    Text(
                        text = "“$note”",
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    )
                }
            }
            if (item.match.sharedInterests.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    item.match.sharedInterests.take(4).forEach { InterestChip(it, highlighted = true) }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onDecline, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.action_decline))
                }
                Button(onClick = onAccept, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.WavingHand, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.action_accept))
                }
            }
        }
    }
}

@Composable
private fun SentList(
    items: List<RequestItem>,
    onWithdraw: (RequestItem) -> Unit,
    onGoToDiscover: () -> Unit,
) {
    if (items.isEmpty()) {
        EmptyState(
            emoji = "👋",
            title = stringResource(R.string.requests_empty_sent_title),
            body = stringResource(R.string.requests_empty_sent_body),
            primaryAction = stringResource(R.string.go_discover) to onGoToDiscover,
            modifier = Modifier.fillMaxSize(),
        )
        return
    }
    val now = remember(items) { System.currentTimeMillis() }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(items, key = { it.personId }) { item ->
            val person = item.request.person
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateItem(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GradientAvatar(person.initials, person.id, size = 48.dp, isOnline = person.isOnline(now))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(person.name, style = MaterialTheme.typography.titleSmall)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.HourglassTop,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.tertiary,
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "Waiting for a wave back · ${Formatters.relativeShort(item.request.createdAt, now)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    TextButton(onClick = { onWithdraw(item) }) { Text(stringResource(R.string.action_withdraw)) }
                }
            }
        }
        item {
            Box(Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.Center) {
                Text(
                    "Most people wave back within a day 🙂",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
