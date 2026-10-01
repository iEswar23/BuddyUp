package io.github.ieswar23.buddyup.ui.discover

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs

enum class SwipeDirection { LEFT, RIGHT }

/**
 * Holds the drag offset of the top Discover card. Exposes [progress] (-1…1) so the UI can fade in
 * "Wave"/"Pass" stamps and scale up the card underneath while the user drags.
 */
@Stable
class SwipeCardState {
    val offsetX = Animatable(0f)
    val offsetY = Animatable(0f)
    var widthPx by mutableStateOf(1f)
        internal set
    var isAnimatingOut by mutableStateOf(false)
        private set

    private val threshold: Float get() = widthPx * THRESHOLD_FRACTION

    /** Signed swipe progress where ±1 is the commit threshold. */
    val progress: Float get() = (offsetX.value / threshold).coerceIn(-1f, 1f)

    val rotation: Float get() = (offsetX.value / widthPx * MAX_ROTATION).coerceIn(-MAX_ROTATION, MAX_ROTATION)

    fun committedDirection(): SwipeDirection? = when {
        offsetX.value > threshold -> SwipeDirection.RIGHT
        offsetX.value < -threshold -> SwipeDirection.LEFT
        else -> null
    }

    suspend fun drag(dx: Float, dy: Float) {
        offsetX.snapTo(offsetX.value + dx)
        offsetY.snapTo(offsetY.value + dy)
    }

    suspend fun springBack() = coroutineScope {
        val spec = spring<Float>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
        launch { offsetX.animateTo(0f, spec) }
        launch { offsetY.animateTo(0f, spec) }
    }

    suspend fun flyOut(direction: SwipeDirection) = coroutineScope {
        isAnimatingOut = true
        val target = if (direction == SwipeDirection.RIGHT) widthPx * 1.6f else -widthPx * 1.6f
        launch { offsetX.animateTo(target, tween(durationMillis = 280, easing = FastOutLinearInEasing)) }
        launch { offsetY.animateTo(offsetY.value + widthPx * 0.1f, tween(durationMillis = 280)) }
    }

    private companion object {
        const val THRESHOLD_FRACTION = 0.32f
        const val MAX_ROTATION = 16f
    }
}

/** Wraps [content] with drag-to-swipe behaviour: rotation while dragging, spring back or fly out. */
@Composable
fun SwipeableCard(
    state: SwipeCardState,
    onSwiped: (SwipeDirection) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()
    Box(
        modifier = modifier
            .onSizeChanged { state.widthPx = it.width.toFloat().coerceAtLeast(1f) }
            .graphicsLayer {
                translationX = state.offsetX.value
                translationY = state.offsetY.value
                rotationZ = state.rotation
                transformOrigin = TransformOrigin(0.5f, 1.1f)
            }
            .pointerInput(state, enabled) {
                if (!enabled) return@pointerInput
                detectDragGestures(
                    onDragEnd = {
                        scope.launch {
                            val direction = state.committedDirection()
                            if (direction != null) {
                                state.flyOut(direction)
                                onSwiped(direction)
                            } else {
                                state.springBack()
                            }
                        }
                    },
                    onDragCancel = { scope.launch { state.springBack() } },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        scope.launch { state.drag(dragAmount.x, dragAmount.y * 0.4f) }
                    },
                )
            },
    ) {
        content()
    }
}

internal fun SwipeCardState.isIdle(): Boolean = !isAnimatingOut && abs(offsetX.value) < 1f
