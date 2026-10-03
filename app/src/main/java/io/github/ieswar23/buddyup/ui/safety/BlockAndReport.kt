package io.github.ieswar23.buddyup.ui.safety

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddComment
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.ieswar23.buddyup.R
import io.github.ieswar23.buddyup.domain.model.Report
import io.github.ieswar23.buddyup.domain.model.ReportReason

/** What the user picked from a "Block / Report" menu. */
enum class SafetyAction { BLOCK, REPORT }

/** "Block {name}" and "Report {name}" items for an overflow [androidx.compose.material3.DropdownMenu]. */
@Composable
fun SafetyMenuItems(firstName: String, onSelect: (SafetyAction) -> Unit) {
    DropdownMenuItem(
        text = { Text(stringResource(R.string.action_block, firstName)) },
        leadingIcon = { Icon(Icons.Outlined.Block, contentDescription = null) },
        onClick = { onSelect(SafetyAction.BLOCK) },
    )
    DropdownMenuItem(
        text = { Text(stringResource(R.string.action_report, firstName)) },
        leadingIcon = { Icon(Icons.Outlined.Flag, contentDescription = null) },
        onClick = { onSelect(SafetyAction.REPORT) },
    )
}

/**
 * Shows the confirmation for [action]: a dialog for [SafetyAction.BLOCK], or the report sheet for
 * [SafetyAction.REPORT]. [onConfirm] receives the report (null for a plain block); both block.
 */
@Composable
fun SafetyActionPrompt(
    action: SafetyAction,
    firstName: String,
    onConfirm: (report: Report?) -> Unit,
    onDismiss: () -> Unit,
) {
    when (action) {
        SafetyAction.BLOCK -> BlockConfirmDialog(firstName, onConfirm = { onConfirm(null) }, onDismiss = onDismiss)
        SafetyAction.REPORT -> ReportSheet(firstName, onSubmit = onConfirm, onDismiss = onDismiss)
    }
}

/** Asks the user to confirm blocking [firstName], explaining what blocking does. */
@Composable
fun BlockConfirmDialog(firstName: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.Block, contentDescription = null) },
        title = { Text(stringResource(R.string.block_title, firstName)) },
        text = { Text(stringResource(R.string.block_body, firstName), style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
            ) { Text(stringResource(R.string.block_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

/**
 * Bottom sheet for reporting [firstName]: a required reason and an optional note (revealed on
 * demand to keep the sheet short). Submitting also blocks them.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportSheet(firstName: String, onSubmit: (Report) -> Unit, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var reason by rememberSaveable { mutableStateOf<ReportReason?>(null) }
    var note by rememberSaveable { mutableStateOf("") }
    var showNote by rememberSaveable { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .navigationBarsPadding(),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Flag, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.report_title, firstName), style = MaterialTheme.typography.titleLarge)
            }
            Text(
                text = stringResource(R.string.report_body, firstName),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
            )
            Text(stringResource(R.string.report_reason_label), style = MaterialTheme.typography.titleSmall)
            Column(Modifier.padding(vertical = 4.dp).selectableGroup()) {
                ReportReason.entries.forEach { option ->
                    ReasonRow(label = option.label, selected = option == reason, onSelect = { reason = option })
                }
            }
            if (showNote) {
                NoteField(value = note, onValueChange = { note = it.take(Report.MAX_NOTE_LENGTH) })
            } else {
                TextButton(onClick = { showNote = true }) {
                    Icon(Icons.Outlined.AddComment, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.report_add_note))
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(52.dp),
                ) { Text(stringResource(R.string.cancel)) }
                Button(
                    onClick = { reason?.let { onSubmit(Report.of(it, note)) } },
                    enabled = reason != null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                    modifier = Modifier.weight(2f).height(52.dp),
                ) { Text(stringResource(R.string.report_submit)) }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ReasonRow(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(16.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

/** Free-text details; focused as soon as it appears because the user just asked for it. */
@Composable
private fun NoteField(value: String, onValueChange: (String) -> Unit) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(focusRequester) { focusRequester.requestFocus() }
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(stringResource(R.string.report_note_label)) },
        supportingText = {
            Text(
                text = stringResource(R.string.report_note_counter, value.length, Report.MAX_NOTE_LENGTH),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End,
            )
        },
        minLines = 2,
        maxLines = 4,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        shape = RoundedCornerShape(16.dp),
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .focusRequester(focusRequester),
    )
}
