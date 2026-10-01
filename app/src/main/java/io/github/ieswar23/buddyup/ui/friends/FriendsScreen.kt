package io.github.ieswar23.buddyup.ui.friends

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.ieswar23.buddyup.R
import io.github.ieswar23.buddyup.domain.model.FriendSummary
import io.github.ieswar23.buddyup.ui.components.EmptyState
import io.github.ieswar23.buddyup.ui.components.GradientAvatar
import io.github.ieswar23.buddyup.ui.components.ListItemSkeleton
import io.github.ieswar23.buddyup.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    onOpenChat: (String) -> Unit,
    onGoToDiscover: () -> Unit,
    viewModel: FriendsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.friends_title), style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        when (val s = state) {
            FriendsUiState.Loading -> Column(Modifier.padding(padding)) { repeat(6) { ListItemSkeleton() } }
            FriendsUiState.Empty -> EmptyState(
                emoji = "🫶",
                title = stringResource(R.string.friends_empty_title),
                body = stringResource(R.string.friends_empty_body),
                primaryAction = stringResource(R.string.go_discover) to onGoToDiscover,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )
            is FriendsUiState.Content -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                item(key = "search") {
                    SearchField(
                        query = query,
                        onQueryChange = viewModel::onQueryChange,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                if (s.onlineNow.isNotEmpty()) {
                    item(key = "online") {
                        OnlineNowRow(friends = s.onlineNow, onOpenChat = onOpenChat)
                    }
                }
                item(key = "header") {
                    Text(
                        text = if (s.query.isBlank()) "Recent" else "Results",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 4.dp),
                    )
                }
                if (s.friends.isEmpty()) {
                    item(key = "noResults") {
                        Text(
                            text = stringResource(R.string.friends_no_results, s.query.trim()),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                        )
                    }
                }
                items(s.friends, key = { it.person.id }) { friend ->
                    Column(Modifier.animateItem()) {
                        FriendRow(friend = friend, now = s.now, onClick = { onOpenChat(friend.person.id) })
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 88.dp, end = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(stringResource(R.string.friends_search)) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            AnimatedVisibility(visible = query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Filled.Close, contentDescription = "Clear search")
                }
            }
        },
        singleLine = true,
        shape = CircleShape,
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    )
}

@Composable
private fun OnlineNowRow(friends: List<FriendSummary>, onOpenChat: (String) -> Unit) {
    Column {
        Text(
            text = "Online now",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 8.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(friends, key = { it.person.id }) { friend ->
                Surface(onClick = { onOpenChat(friend.person.id) }, shape = MaterialTheme.shapes.medium, color = Color.Transparent) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(4.dp).widthIn(max = 72.dp),
                    ) {
                        GradientAvatar(friend.person.initials, friend.person.id, size = 60.dp, isOnline = true)
                        Text(
                            text = friend.person.firstName,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FriendRow(friend: FriendSummary, now: Long, onClick: () -> Unit) {
    val person = friend.person
    val hasUnread = friend.unreadCount > 0
    ListItem(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp),
        leadingContent = {
            GradientAvatar(person.initials, person.id, size = 56.dp, isOnline = person.isOnline(now))
        },
        headlineContent = {
            Text(
                text = person.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = {
            val preview = when {
                friend.lastMessage == null -> "You're now buddies — say hi 👋"
                friend.lastMessageFromMe -> "You: ${friend.lastMessage}"
                else -> friend.lastMessage
            }
            Text(
                text = preview,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (hasUnread) FontWeight.Bold else FontWeight.Normal,
                color = if (hasUnread) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = Formatters.relativeShort(friend.lastActivityAt, now),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (hasUnread) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.size(6.dp))
                if (hasUnread) {
                    Badge(containerColor = MaterialTheme.colorScheme.primary) { Text(friend.unreadCount.toString()) }
                } else {
                    Spacer(Modifier.width(16.dp))
                }
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}
