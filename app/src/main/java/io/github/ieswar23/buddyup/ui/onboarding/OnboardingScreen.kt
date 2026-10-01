package io.github.ieswar23.buddyup.ui.onboarding

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.ieswar23.buddyup.R
import io.github.ieswar23.buddyup.ui.theme.AvatarGradients
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

private data class OnboardingPage(
    val heroEmoji: String,
    val orbitEmojis: List<String>,
    @StringRes val title: Int,
    @StringRes val body: Int,
    val gradient: Pair<Color, Color>,
)

private val pages = listOf(
    OnboardingPage("🤝", listOf("☕", "🎲", "🥾"), R.string.onboarding_title_1, R.string.onboarding_body_1, AvatarGradients[0]),
    OnboardingPage("👋", listOf("💬", "😊", "✨"), R.string.onboarding_title_2, R.string.onboarding_body_2, AvatarGradients[1]),
    OnboardingPage("🗓️", listOf("🎸", "🏃", "🍛"), R.string.onboarding_title_3, R.string.onboarding_body_3, AvatarGradients[3]),
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == pages.lastIndex

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onFinished) { Text(stringResource(R.string.onboarding_skip)) }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
        ) { index ->
            val page = pages[index]
            val pageOffset = (pagerState.currentPage - index + pagerState.currentPageOffsetFraction).absoluteValue
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = 1f - (pageOffset * 0.6f).coerceIn(0f, 1f)
                        val scale = 1f - (pageOffset * 0.1f).coerceIn(0f, 0.1f)
                        scaleX = scale
                        scaleY = scale
                    }
                    .padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                HeroIllustration(page)
                Spacer(Modifier.height(40.dp))
                Text(
                    text = stringResource(page.title),
                    style = MaterialTheme.typography.headlineLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = stringResource(page.body),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        PagerIndicator(
            pageCount = pages.size,
            currentPage = pagerState.currentPage,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(vertical = 20.dp),
        )

        Button(
            onClick = {
                if (isLastPage) onFinished()
                else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 20.dp)
                .height(56.dp),
        ) {
            AnimatedContent(
                targetState = isLastPage,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "ctaLabel",
            ) { last ->
                Text(
                    text = stringResource(if (last) R.string.onboarding_get_started else R.string.onboarding_next),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

@Composable
private fun HeroIllustration(page: OnboardingPage) {
    Box(modifier = Modifier.size(260.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(220.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(page.gradient.first, page.gradient.second))),
        )
        Box(
            modifier = Modifier
                .size(170.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.18f)),
        )
        Text(text = page.heroEmoji, fontSize = 92.sp)
        val positions = listOf((-96).dp to (-80).dp, 100.dp to (-48).dp, (-70).dp to 100.dp)
        page.orbitEmojis.zip(positions).forEachIndexed { i, (emoji, offset) ->
            Surface(
                modifier = Modifier
                    .offset(offset.first, offset.second)
                    .rotate(if (i % 2 == 0) -8f else 8f),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 6.dp,
            ) {
                Text(text = emoji, fontSize = 28.sp, modifier = Modifier.padding(12.dp))
            }
        }
    }
}

@Composable
fun PagerIndicator(pageCount: Int, currentPage: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(pageCount) { index ->
            val selected = index == currentPage
            val width by animateDpAsState(if (selected) 28.dp else 8.dp, label = "dotWidth")
            val color by animateColorAsState(
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                label = "dotColor",
            )
            val alpha by animateFloatAsState(if (selected) 1f else 0.8f, label = "dotAlpha")
            Box(
                modifier = Modifier
                    .height(8.dp)
                    .width(width)
                    .graphicsLayer { this.alpha = alpha }
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}
