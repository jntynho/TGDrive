package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.DriveFile

@Composable
fun TrashDialog(
  trashFiles: List<DriveFile>,
  onDismiss: () -> Unit,
  onRestore: (DriveFile) -> Unit,
  onDeletePermanently: (DriveFile) -> Unit,
  onEmptyTrash: () -> Unit
) {
  var showEmptyConfirm by remember { mutableStateOf(false) }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      modifier = Modifier
        .fillMaxWidth(0.92f)
        .fillMaxHeight(0.75f)
        .clip(RoundedCornerShape(24.dp))
        .testTag("trash_dialog"),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 6.dp
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(20.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = "Trash & Recycle Bin",
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "Items are permanently deleted after 30 days",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.outline
            )
          }

          if (trashFiles.isNotEmpty()) {
            IconButton(
              onClick = { showEmptyConfirm = true },
              modifier = Modifier.testTag("empty_trash_button")
            ) {
              Icon(
                Icons.Default.DeleteSweep,
                contentDescription = "Empty Trash",
                tint = MaterialTheme.colorScheme.error
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (trashFiles.isEmpty()) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .weight(1f),
            contentAlignment = Alignment.Center
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Icon(
                Icons.Default.DeleteForever,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.outline
              )
              Spacer(modifier = Modifier.height(12.dp))
              Text(
                "Trash is Empty",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
              )
              Text(
                "Deleted files will appear here for 30 days",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
              )
            }
          }
        } else {
          LazyColumn(
            modifier = Modifier
              .fillMaxWidth()
              .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            items(trashFiles) { file ->
              val daysRemaining = remember(file.trashedDate) {
                val trashedTime = file.trashedDate ?: System.currentTimeMillis()
                val elapsedDays = (System.currentTimeMillis() - trashedTime) / (1000L * 60 * 60 * 24)
                (30 - elapsedDays).coerceAtLeast(1)
              }

              Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                  containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = file.name,
                      style = MaterialTheme.typography.bodyMedium,
                      fontWeight = FontWeight.SemiBold,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                    Text(
                      text = "${formatFileSize(file.size)} • $daysRemaining days left",
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.error
                    )
                  }

                  Row {
                    IconButton(onClick = { onRestore(file) }) {
                      Icon(
                        Icons.Default.RestoreFromTrash,
                        contentDescription = "Restore",
                        tint = MaterialTheme.colorScheme.primary
                      )
                    }
                    IconButton(onClick = { onDeletePermanently(file) }) {
                      Icon(
                        Icons.Default.DeleteForever,
                        contentDescription = "Delete permanently",
                        tint = MaterialTheme.colorScheme.error
                      )
                    }
                  }
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
          onClick = onDismiss,
          modifier = Modifier.fillMaxWidth()
        ) {
          Text("Close")
        }
      }
    }
  }

  if (showEmptyConfirm) {
    AlertDialog(
      onDismissRequest = { showEmptyConfirm = false },
      title = { Text("Empty Trash?") },
      text = { Text("All ${trashFiles.size} items in trash will be permanently deleted from Telegram and your device.") },
      confirmButton = {
        Button(
          onClick = {
            onEmptyTrash()
            showEmptyConfirm = false
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
          Text("Empty Trash")
        }
      },
      dismissButton = {
        TextButton(onClick = { showEmptyConfirm = false }) {
          Text("Cancel")
        }
      }
    )
  }
}
