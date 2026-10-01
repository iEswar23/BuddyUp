package io.github.ieswar23.buddyup.ui.discover

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.ieswar23.buddyup.R
import io.github.ieswar23.buddyup.domain.model.DiscoverFilters
import io.github.ieswar23.buddyup.ui.components.InterestChip
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterSheet(
    current: DiscoverFilters,
    interests: List<String>,
    matchingCount: (DiscoverFilters) -> Int,
    onApply: (DiscoverFilters) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var draft by remember(current) { mutableStateOf(current) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(stringResource(R.string.filters_title), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))

            LabeledValue(
                label = stringResource(R.string.filters_distance),
                value = if (draft.isAnywhere) "Anywhere" else "${draft.maxDistanceKm} km",
            )
            Slider(
                value = draft.maxDistanceKm.toFloat(),
                onValueChange = { draft = draft.copy(maxDistanceKm = (it / 5f).roundToInt() * 5) },
                valueRange = DiscoverFilters.MIN_DISTANCE_KM.toFloat()..DiscoverFilters.ANYWHERE_KM.toFloat(),
                steps = (DiscoverFilters.ANYWHERE_KM - DiscoverFilters.MIN_DISTANCE_KM) / 5 - 1,
            )

            LabeledValue(
                label = stringResource(R.string.filters_age),
                value = "${draft.minAge} – ${draft.maxAge}${if (draft.maxAge >= DiscoverFilters.MAX_AGE) "+" else ""}",
            )
            RangeSlider(
                value = draft.minAge.toFloat()..draft.maxAge.toFloat(),
                onValueChange = { range ->
                    draft = draft.copy(minAge = range.start.roundToInt(), maxAge = range.endInclusive.roundToInt())
                },
                valueRange = DiscoverFilters.MIN_AGE.toFloat()..DiscoverFilters.MAX_AGE.toFloat(),
            )

            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.filters_interests), style = MaterialTheme.typography.titleSmall)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                interests.forEach { interest ->
                    InterestChip(
                        interest = interest,
                        highlighted = interest in draft.interests,
                        onClick = {
                            draft = draft.copy(
                                interests = if (interest in draft.interests) draft.interests - interest
                                else draft.interests + interest
                            )
                        },
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { draft = DiscoverFilters() },
                    modifier = Modifier.weight(1f).height(52.dp),
                ) { Text(stringResource(R.string.filters_reset)) }
                Button(
                    onClick = { onApply(draft) },
                    modifier = Modifier.weight(2f).height(52.dp),
                ) {
                    val count = matchingCount(draft)
                    Text("${stringResource(R.string.filters_apply)} ($count)")
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun LabeledValue(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    }
}
