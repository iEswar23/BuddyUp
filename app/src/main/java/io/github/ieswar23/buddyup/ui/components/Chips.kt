package io.github.ieswar23.buddyup.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.ieswar23.buddyup.domain.model.Interests

/**
 * Pill showing an interest with its emoji. [highlighted] marks interests the user shares;
 * when [onClick] is set the chip becomes a toggle (used in onboarding and filters).
 */
@Composable
fun InterestChip(
    interest: String,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    val container by animateColorAsState(
        if (highlighted) colors.primary else colors.surfaceContainerHigh,
        label = "chipContainer",
    )
    val content by animateColorAsState(
        if (highlighted) colors.onPrimary else colors.onSurface,
        label = "chipContent",
    )
    val border = if (highlighted) null else BorderStroke(1.dp, colors.outlineVariant)
    val text = Interests.label(interest)

    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = modifier,
            shape = CircleShape,
            color = container,
            contentColor = content,
            border = border,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            )
        }
    } else {
        Surface(
            modifier = modifier,
            shape = CircleShape,
            color = container,
            contentColor = content,
            border = border,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }
    }
}

/** Small rounded tag, e.g. "Free" or "Beginner friendly". */
@Composable
fun Tag(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
) {
    Surface(modifier = modifier, shape = CircleShape, color = containerColor, contentColor = contentColor) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}
