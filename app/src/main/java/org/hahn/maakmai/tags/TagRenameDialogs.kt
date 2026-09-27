package org.hahn.maakmai.tags

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

fun conflictMessage(newTag: String) = "A folder named '$newTag' already exists next to this one."

@Composable
fun MergeTagConfirmDialog(
    oldTag: String,
    newTag: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Merge tags?") },
        text = { Text("'$oldTag' will be merged into '$newTag'. This can't be undone.") },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Merge") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
