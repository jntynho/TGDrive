package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.DriveFile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FilePreviewDialog(
  file: DriveFile,
  onDismiss: () -> Unit,
  onDownload: () -> Unit,
  onMoveToFolder: () -> Unit,
  onDelete: () -> Unit,
  onToggleFavorite: () -> Unit
) {
  var isPlayingVideo by remember { mutableStateOf(false) }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      modifier = Modifier
        .fillMaxWidth(0.94f)
        .fillMaxHeight(0.85f)
        .clip(RoundedCornerShape(24.dp))
        .testTag("file_preview_dialog"),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 6.dp
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(20.dp)
      ) {
        // Top Bar
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          IconButton(
            onClick = onDismiss,
            modifier = Modifier.testTag("close_preview_button")
          ) {
            Icon(Icons.Default.Close, contentDescription = "Close preview")
          }

          Text(
            text = file.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
              .weight(1f)
              .padding(horizontal = 8.dp)
          )

          IconButton(
            onClick = onToggleFavorite,
            modifier = Modifier.testTag("favorite_button")
          ) {
            Icon(
              if (file.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
              contentDescription = "Toggle favorite",
              tint = if (file.isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Preview Box Area
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
          contentAlignment = Alignment.Center
        ) {
          when {
            file.isImage -> {
              if (!file.localUri.isNullOrBlank()) {
                AsyncImage(
                  model = file.localUri,
                  contentDescription = file.name,
                  modifier = Modifier.fillMaxSize()
                )
              } else {
                Column(
                  horizontalAlignment = Alignment.CenterHorizontally,
                  modifier = Modifier.padding(16.dp)
                ) {
                  Icon(
                    Icons.Default.Image,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                    tint = MaterialTheme.colorScheme.primary
                  )
                  Spacer(modifier = Modifier.height(8.dp))
                  Text(
                    text = "High-Resolution Image Preview",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                  Text(
                    text = "Stored on Telegram Cloud (DC4)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                  )
                }
              }
            }

            file.isVideo -> {
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(16.dp)
              ) {
                Box(
                  modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                  contentAlignment = Alignment.Center
                ) {
                  IconButton(onClick = { isPlayingVideo = !isPlayingVideo }) {
                    Icon(
                      if (isPlayingVideo) Icons.Default.Pause else Icons.Default.PlayArrow,
                      contentDescription = "Play video",
                      tint = MaterialTheme.colorScheme.onPrimary,
                      modifier = Modifier.size(44.dp)
                    )
                  }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                  text = if (isPlayingVideo) "Streaming from Telegram MTProto..." else "Tap to Play Video",
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = FontWeight.SemiBold,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                  text = "Telegram Saved Messages Stream Engine",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.outline
                )
              }
            }

            file.isPdf -> {
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp)
              ) {
                Icon(
                  Icons.Default.PictureAsPdf,
                  contentDescription = null,
                  modifier = Modifier.size(72.dp),
                  tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                  text = "PDF Document",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold
                )
                Text(
                  text = "${formatFileSize(file.size)} • ${file.name}",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.outline,
                  textAlign = TextAlign.Center
                )
              }
            }

            else -> {
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp)
              ) {
                Icon(
                  Icons.Default.InsertDriveFile,
                  contentDescription = null,
                  modifier = Modifier.size(72.dp),
                  tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                  text = file.extension.uppercase().ifBlank { "FILE" },
                  style = MaterialTheme.typography.titleLarge,
                  fontWeight = FontWeight.Bold
                )
                Text(
                  text = formatFileSize(file.size),
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.outline
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // File Metadata Card
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
          ),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            MetadataRow("Size", formatFileSize(file.size))
            MetadataRow("Folder", file.virtualFolder)
            MetadataRow("Telegram Message ID", "#${file.telegramMessageId}")
            if (file.isEncrypted) {
              MetadataRow("Security", "🔒 AES-256 GCM Client-Encrypted", isSuccess = true)
            }
            MetadataRow("Uploaded", formatTimestamp(file.uploadDate))
            MetadataRow("Status", "Synced to Saved Messages", isSuccess = true)
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action Buttons Row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Button(
            onClick = onDownload,
            modifier = Modifier
              .weight(1f)
              .testTag("download_file_button"),
            colors = ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary
            )
          ) {
            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Download")
          }

          OutlinedButton(
            onClick = onMoveToFolder,
            modifier = Modifier.testTag("move_folder_button")
          ) {
            Icon(Icons.Default.DriveFileMove, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Move")
          }

          OutlinedButton(
            onClick = onDelete,
            colors = ButtonDefaults.outlinedButtonColors(
              contentColor = MaterialTheme.colorScheme.error
            ),
            modifier = Modifier.testTag("delete_file_button")
          ) {
            Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
          }
        }
      }
    }
  }
}

@Composable
private fun MetadataRow(label: String, value: String, isSuccess: Boolean = false) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 3.dp),
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Text(
      text = value,
      style = MaterialTheme.typography.bodySmall,
      fontWeight = FontWeight.SemiBold,
      color = if (isSuccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    )
  }
}

fun formatFileSize(bytes: Long): String {
  if (bytes <= 0) return "0 B"
  val units = arrayOf("B", "KB", "MB", "GB", "TB")
  val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
  return String.format(Locale.US, "%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}

fun formatTimestamp(timestamp: Long): String {
  val sdf = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())
  return sdf.format(Date(timestamp))
}
