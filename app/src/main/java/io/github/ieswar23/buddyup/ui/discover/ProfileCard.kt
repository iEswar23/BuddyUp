package io.github.ieswar23.buddyup.ui.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.ieswar23.buddyup.ui.components.CompatibilityRing
import io.github.ieswar23.buddyup.ui.components.InterestChip
import io.github.ieswar23.buddyup.ui.theme.OnlineGreen
import io.github.ieswar23.buddyup.ui.theme.gradientFor
import io.github.ieswar23.buddyup.util.Formatters

/** The full Discover card: gradient hero with initials + match ring, then details and interests. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileCard(
    card: DiscoverCard,
    now: Long,
    modifier: Modifier = Modifier,
    swipeProgress: Float = 0f,
) {
    val person = card.person
    val (start, end) = gradientFor(person.id)
    val shared = card.match.sharedInterests.toSet()
    val orderedInterests = person.interests.sortedByDescending { it in shared }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
        Column(Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.42f)
                    .background(Brush.linearGradient(listOf(start, end))),
            ) {
                // Soft decorative bubbles
                Box(
                    Modifier
                        .size(180.dp)
                        .align(Alignment.TopEnd)
                        .graphicsLayer { translationX = 60.dp.toPx(); translationY = (-50).dp.toPx() }
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f))
                )
                Box(
                    Modifier
                        .size(120.dp)
                        .align(Alignment.BottomStart)
                        .graphicsLayer { translationX = (-30).dp.toPx(); translationY = 40.dp.toPx() }
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.10f))
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(116.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.22f))
                        .border(3.dp, Color.White.copy(alpha = 0.85f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = person.initials,
                        color = Color.White,
                        fontSize = 44.sp,
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.displaySmall,
                    )
                }
                ActivityPill(
                    text = Formatters.lastActive(person.lastActiveAt, now),
                    online = person.isOnline(now),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp),
                )
                CompatibilityRing(
                    percent = card.match.percent,
                    size = 66.dp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(14.dp),
                )
                SwipeStamp(
                    text = "WAVE 👋",
                    color = MaterialTheme.colorScheme.secondary,
                    alpha = swipeProgress.coerceAtLeast(0f),
                    rotation = -14f,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(20.dp),
                )
                SwipeStamp(
                    text = "PASS",
                    color = MaterialTheme.colorScheme.error,
                    alpha = (-swipeProgress).coerceAtLeast(0f),
                    rotation = 14f,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(20.dp),
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.58f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = person.name,
                        style = MaterialTheme.typography.headlineSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = person.age.toString(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                MetaRow(
                    icon = { Icon(Icons.Outlined.LocationOn, null, Modifier.size(16.dp)) },
                    text = "${person.neighborhood}, ${person.city} · ${Formatters.distance(person.distanceKm)}",
                )
                MetaRow(
                    icon = { Icon(Icons.Outlined.WorkOutline, null, Modifier.size(16.dp)) },
                    text = person.occupation,
                )
                Text(
                    text = person.bio,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = if (shared.isEmpty()) "Interests" else "${shared.size} shared interest${if (shared.size == 1) "" else "s"} · ${card.match.label}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    orderedInterests.forEach { interest ->
                        InterestChip(interest = interest, highlighted = interest in shared)
                    }
                }
            }
        }
    }
}

@Composable
private fun MetaRow(icon: @Composable () -> Unit, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(18.dp), contentAlignment = Alignment.Center) {
            CompositionLocalProvider(
                LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant,
                content = icon,
            )
        }
        Spacer(Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ActivityPill(text: String, online: Boolean, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = Color.Black.copy(alpha = 0.22f),
        contentColor = Color.White,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (online) OnlineGreen else Color.White.copy(alpha = 0.6f))
            )
            Spacer(Modifier.width(6.dp))
            Text(text, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun SwipeStamp(text: String, color: Color, alpha: Float, rotation: Float, modifier: Modifier = Modifier) {
    if (alpha <= 0.01f) return
    Box(
        modifier = modifier
            .graphicsLayer { this.alpha = alpha }
            .rotate(rotation)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(3.dp, color, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        Text(text = text, color = color, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp)
    }
}
