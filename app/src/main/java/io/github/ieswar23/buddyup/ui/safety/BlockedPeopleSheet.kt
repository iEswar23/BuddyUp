package io.github.ieswar23.buddyup.ui.safety

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.ieswar23.buddyup.R
import io.github.ieswar23.buddyup.domain.model.BlockedPerson
import io.github.ieswar23.buddyup.ui.components.GradientAvatar
import io.github.ieswar23.buddyup.util.Formatters

/** Bottom sheet listing everyone the user blocked, each with an "Unblock" button. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockedPeopleSheet(
    blocked: List<BlockedPerson>,
    now: Long,
    onUnblock: (BlockedPerson) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
        ) {
            Text(
                text = stringResource(R.string.blocked_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            Text(
                text = stringResource(R.string.blocked_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp),
            )
            if (blocked.isEmpty()) {
                Text(
                    text = "🕊️\n" + stringResource(R.string.blocked_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp, vertical = 32.dp),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 480.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(blocked, key = { it.person.id }) { entry ->
                        BlockedRow(entry, now, onUnblock = { onUnblock(entry) }, Modifier.animateItem())
                    }
                }
            }
        }
    }
}

@Composable
private fun BlockedRow(entry: BlockedPerson, now: Long, onUnblock: () -> Unit, modifier: Modifier = Modifier) {
    val person = entry.person
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GradientAvatar(person.initials, person.id, size = 44.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(person.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val detail = entry.report?.let { stringResource(R.string.blocked_reported, it.reason.label) }
                ?: stringResource(R.string.blocked_on, Formatters.relativeShort(entry.blockedAt, now))
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        OutlinedButton(onClick = onUnblock) { Text(stringResource(R.string.action_unblock)) }
    }
}
