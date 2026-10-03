package io.github.ieswar23.buddyup.ui.chat

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.TipsAndUpdates
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.ieswar23.buddyup.R
import io.github.ieswar23.buddyup.domain.icebreakers.Icebreaker
import io.github.ieswar23.buddyup.domain.model.Person
import io.github.ieswar23.buddyup.ui.components.GradientAvatar
import io.github.ieswar23.buddyup.ui.components.InterestChip
import io.github.ieswar23.buddyup.ui.safety.SafetyAction
import io.github.ieswar23.buddyup.ui.safety.SafetyActionPrompt
import io.github.ieswar23.buddyup.ui.safety.SafetyMenuItems
import io.github.ieswar23.buddyup.ui.theme.AvatarGradients
import io.github.ieswar23.buddyup.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onBack: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val input by viewModel.input.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    var showProfile by rememberSaveable { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var safetyAction by rememberSaveable { mutableStateOf<SafetyAction?>(null) }

    // Once blocked (from here or anywhere else) the conversation is hidden, so leave it.
    LaunchedEffect(state.isBlocked) {
        if (state.isBlocked) onBack()
    }

    // reverseLayout = true, so index 0 is the bottom of the conversation.
    val reversedItems = remember(state.items) { state.items.asReversed() }
    LaunchedEffect(state.lastMessageKey, state.isTyping) {
        if (state.items.isNotEmpty()) listState.animateScrollToItem(0)
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { state.friend?.let { ChatTitle(it, state.isTyping, state.now) } },
                actions = {
                    IconButton(onClick = { showProfile = true }, enabled = state.friend != null) {
                        Icon(Icons.Outlined.Info, contentDescription = stringResource(R.string.chat_view_profile))
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }, enabled = state.friend != null) {
                            Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.chat_more))
                        }
                        state.friend?.let { friend ->
                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                SafetyMenuItems(firstName = friend.firstName) { action ->
                                    showMenu = false
                                    safetyAction = action
                                }
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            )
        },
        bottomBar = {
            Column(
                Modifier
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                AnimatedVisibility(
                    visible = !state.isNewConversation && state.icebreakersExpanded && state.icebreakers.isNotEmpty(),
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    IcebreakerRow(
                        icebreakers = state.icebreakers,
                        canRefresh = state.canRefreshIcebreakers,
                        onPick = viewModel::useIcebreaker,
                        onRefresh = viewModel::refreshIcebreakers,
                        onHide = viewModel::hideIcebreakers,
                    )
                }
                AnimatedVisibility(
                    visible = (state.showIcebreakerPrompt || state.suggestions.isNotEmpty()) && input.isEmpty(),
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 8.dp),
                    ) {
                        if (state.showIcebreakerPrompt) {
                            item(key = "icebreakerPrompt") { IcebreakerPrompt(onClick = viewModel::showIcebreakers) }
                        }
                        items(state.suggestions) { suggestion ->
                            SuggestionChip(
                                onClick = { viewModel.sendSuggestion(suggestion) },
                                label = { Text(suggestion) },
                                shape = CircleShape,
                            )
                        }
                    }
                }
                MessageInput(
                    value = input,
                    onValueChange = viewModel::onInputChange,
                    onSend = viewModel::send,
                )
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
        ) {
            LazyColumn(
                state = listState,
                reverseLayout = true,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                item(key = "typing") {
                    AnimatedVisibility(
                        visible = state.isTyping,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically(),
                    ) {
                        TypingBubble(Modifier.padding(top = 6.dp))
                    }
                }
                items(reversedItems, key = { it.key }, contentType = { it::class }) { item ->
                    when (item) {
                        is ChatItem.DayHeader -> DayHeader(item.label, Modifier.animateItem())
                        is ChatItem.Bubble -> MessageBubble(item, Modifier.animateItem())
                    }
                }
            }
            // Drawn above the (empty) message list so its icebreakers receive taps.
            if (state.isNewConversation) {
                EmptyConversation(
                    friend = state.friend,
                    shared = state.sharedInterests,
                    icebreakers = state.icebreakers,
                    canRefresh = state.canRefreshIcebreakers,
                    onPick = viewModel::useIcebreaker,
                    onRefresh = viewModel::refreshIcebreakers,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }
    }

    if (showProfile) {
        state.friend?.let { friend ->
            ModalBottomSheet(onDismissRequest = { showProfile = false }) {
                FriendProfileSheet(
                    friend = friend,
                    shared = state.sharedInterests,
                    now = state.now,
                    onSafetyAction = { action ->
                        showProfile = false
                        safetyAction = action
                    },
                )
            }
        }
    }

    safetyAction?.let { action ->
        state.friend?.let { friend ->
            SafetyActionPrompt(
                action = action,
                firstName = friend.firstName,
                onConfirm = { report ->
                    safetyAction = null
                    viewModel.block(report)
                },
                onDismiss = { safetyAction = null },
            )
        }
    }
}

/** Collapsible row of conversation starters above the composer. Tapping one fills the composer. */
@Composable
private fun IcebreakerRow(
    icebreakers: List<Icebreaker>,
    canRefresh: Boolean,
    onPick: (String) -> Unit,
    onRefresh: () -> Unit,
    onHide: () -> Unit,
) {
    Column(Modifier.padding(top = 6.dp)) {
        IcebreakerHeader(
            caption = icebreakerCaption(icebreakers),
            canRefresh = canRefresh,
            onRefresh = onRefresh,
            onHide = onHide,
            modifier = Modifier.padding(start = 16.dp, end = 4.dp),
        )
        AnimatedContent(
            targetState = icebreakers,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "icebreakerRow",
        ) { shown ->
            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(shown, key = { it.text }) { icebreaker ->
                    SuggestionChip(
                        onClick = { onPick(icebreaker.text) },
                        label = { Text(icebreaker.text) },
                        shape = CircleShape,
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                            labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                        border = null,
                    )
                }
            }
        }
    }
}

/** "Icebreakers · you both like …", naming the shared interests the current set is about. */
@Composable
private fun icebreakerCaption(icebreakers: List<Icebreaker>): String {
    val sharedInterests = icebreakers
        .filter { it.source == Icebreaker.Source.SHARED_INTEREST }
        .mapNotNull { it.interest?.lowercase() }
        .distinct()
    return if (sharedInterests.isEmpty()) {
        stringResource(R.string.icebreakers_title)
    } else {
        stringResource(R.string.icebreakers_shared, joinNaturally(sharedInterests))
    }
}

/**
 * Light-bulb caption with a refresh button that cycles to the next set of icebreakers, plus a
 * close button when [onHide] is set.
 */
@Composable
private fun IcebreakerHeader(
    caption: String,
    canRefresh: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    onHide: (() -> Unit)? = null,
) {
    Row(modifier = modifier.heightIn(min = 40.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Outlined.TipsAndUpdates,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = caption,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (canRefresh) {
            IconButton(onClick = onRefresh) {
                Icon(
                    Icons.Outlined.Refresh,
                    contentDescription = stringResource(R.string.icebreakers_refresh),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
        if (onHide != null) {
            IconButton(onClick = onHide) {
                Icon(
                    Icons.Outlined.Close,
                    contentDescription = stringResource(R.string.icebreakers_hide),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Small "Need an icebreaker?" chip above the composer that opens the icebreaker row. */
@Composable
private fun IcebreakerPrompt(onClick: () -> Unit) {
    SuggestionChip(
        onClick = onClick,
        label = { Text(stringResource(R.string.icebreakers_prompt)) },
        icon = {
            Icon(
                Icons.Outlined.TipsAndUpdates,
                contentDescription = null,
                modifier = Modifier.size(SuggestionChipDefaults.IconSize),
            )
        },
        shape = CircleShape,
        colors = SuggestionChipDefaults.suggestionChipColors(
            labelColor = MaterialTheme.colorScheme.primary,
            iconContentColor = MaterialTheme.colorScheme.primary,
        ),
        border = SuggestionChipDefaults.suggestionChipBorder(
            enabled = true,
            borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
        ),
    )
}

private fun joinNaturally(items: List<String>): String = when (items.size) {
    0 -> ""
    1 -> items.first()
    else -> items.dropLast(1).joinToString(", ") + " & " + items.last()
}

@Composable
private fun ChatTitle(friend: Person, isTyping: Boolean, now: Long) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        GradientAvatar(friend.initials, friend.id, size = 40.dp, isOnline = friend.isOnline(now) || isTyping)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(friend.name, style = MaterialTheme.typography.titleMedium, maxLines = 1)
            AnimatedContent(
                targetState = isTyping,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "chatSubtitle",
            ) { typing ->
                Text(
                    text = when {
                        typing -> stringResource(R.string.chat_typing)
                        friend.isOnline(now) -> stringResource(R.string.chat_online)
                        else -> Formatters.lastActive(friend.lastActiveAt, now)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (typing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DayHeader(label: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun MessageBubble(item: ChatItem.Bubble, modifier: Modifier = Modifier) {
    val message = item.message
    val mine = message.fromMe
    val big = 20.dp
    val small = 6.dp
    val shape = if (mine) {
        RoundedCornerShape(
            topStart = big,
            topEnd = if (item.isFirstInGroup) big else small,
            bottomEnd = small,
            bottomStart = big,
        )
    } else {
        RoundedCornerShape(
            topStart = if (item.isFirstInGroup) big else small,
            topEnd = big,
            bottomEnd = big,
            bottomStart = small,
        )
    }
    val (start, end) = AvatarGradients[0]
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = if (item.isFirstInGroup) 8.dp else 2.dp),
        horizontalAlignment = if (mine) Alignment.End else Alignment.Start,
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 290.dp)
                .clip(shape)
                .then(
                    if (mine) Modifier.background(Brush.linearGradient(listOf(start, end)))
                    else Modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest)
                )
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyLarge,
                color = if (mine) Color.White else MaterialTheme.colorScheme.onSurface,
            )
        }
        if (item.isLastInGroup) {
            Text(
                text = Formatters.time(message.sentAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, start = 6.dp, end = 6.dp),
            )
        }
    }
}

@Composable
private fun TypingBubble(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "typing")
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomEnd = 20.dp, bottomStart = 6.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            repeat(3) { index ->
                val offset by transition.animateFloat(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 450, delayMillis = index * 150),
                        repeatMode = RepeatMode.Reverse,
                    ),
                    label = "dot$index",
                )
                Box(
                    Modifier
                        .size(8.dp)
                        .graphicsLayer {
                            translationY = -5.dp.toPx() * offset
                            alpha = 0.4f + 0.6f * offset
                        }
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant),
                )
            }
        }
    }
}

@Composable
private fun MessageInput(value: String, onValueChange: (String) -> Unit, onSend: () -> Unit) {
    val canSend = value.isNotBlank()
    val sendScale by animateFloatAsState(if (canSend) 1f else 0.85f, label = "sendScale")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text(stringResource(R.string.chat_hint)) },
            shape = RoundedCornerShape(24.dp),
            maxLines = 4,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Send,
            ),
            keyboardActions = KeyboardActions(onSend = { onSend() }),
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            ),
        )
        Spacer(Modifier.width(8.dp))
        FilledIconButton(
            onClick = onSend,
            enabled = canSend,
            modifier = Modifier
                .size(52.dp)
                .scale(sendScale),
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary),
        ) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = stringResource(R.string.chat_send))
        }
    }
}

/** A brand-new conversation: who they are, what you share, and a card of icebreakers to start with. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EmptyConversation(
    friend: Person?,
    shared: List<String>,
    icebreakers: List<Icebreaker>,
    canRefresh: Boolean,
    onPick: (String) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (friend != null) GradientAvatar(friend.initials, friend.id, size = 88.dp)
        Spacer(Modifier.height(16.dp))
        Text(
            text = if (shared.isNotEmpty() || friend == null) {
                stringResource(R.string.chat_empty)
            } else {
                stringResource(R.string.chat_empty_no_shared, friend.firstName)
            },
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (shared.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)) {
                shared.forEach { InterestChip(it, highlighted = true) }
            }
        }
        if (icebreakers.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            IcebreakerCard(icebreakers, canRefresh, onPick, onRefresh)
        }
    }
}

@Composable
private fun IcebreakerCard(
    icebreakers: List<Icebreaker>,
    canRefresh: Boolean,
    onPick: (String) -> Unit,
    onRefresh: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        modifier = Modifier.widthIn(max = 420.dp),
    ) {
        Column(Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 16.dp)) {
            // The shared interests are already listed above the card, so the title stays short.
            IcebreakerHeader(
                caption = stringResource(R.string.icebreakers_title),
                canRefresh = canRefresh,
                onRefresh = onRefresh,
            )
            Text(
                text = stringResource(R.string.icebreakers_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 24.dp, bottom = 12.dp),
            )
            AnimatedContent(
                targetState = icebreakers,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "icebreakerCard",
            ) { shown ->
                Column(
                    modifier = Modifier.padding(end = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    shown.forEach { icebreaker ->
                        Surface(
                            onClick = { onPick(icebreaker.text) },
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = icebreaker.text,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FriendProfileSheet(
    friend: Person,
    shared: List<String>,
    now: Long,
    onSafetyAction: (SafetyAction) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        GradientAvatar(friend.initials, friend.id, size = 96.dp, isOnline = friend.isOnline(now))
        Text("${friend.name}, ${friend.age}", style = MaterialTheme.typography.headlineSmall)
        Text(
            "${friend.occupation} · ${friend.neighborhood}, ${friend.city}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Text(friend.bio, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            friend.interests.forEach { InterestChip(it, highlighted = it in shared) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 6.dp)) {
            SafetyButton(Icons.Outlined.Block, stringResource(R.string.action_block, friend.firstName)) {
                onSafetyAction(SafetyAction.BLOCK)
            }
            SafetyButton(Icons.Outlined.Flag, stringResource(R.string.action_report, friend.firstName)) {
                onSafetyAction(SafetyAction.REPORT)
            }
        }
    }
}

@Composable
private fun SafetyButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(label)
    }
}
