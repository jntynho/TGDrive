package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.telegram.TransferProgress

@Composable
fun UploadProgressBar(
  transfers: Map<Long, TransferProgress>,
  onPause: (Long) -> Unit,
  onResume: (Long) -> Unit,
  onCancel: (Long) -> Unit,
  modifier: Modifier = Modifier
) {
  AnimatedVisibility(
    visible = transfers.isNotEmpty(),
    enter = fadeIn(),
    exit = fadeOut(),
    modifier = modifier
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 6.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      transfers.values.forEach { transfer ->
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
          ),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("transfer_item_${transfer.fileId}")
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(12.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  if (transfer.isUpload) Icons.Default.CloudUpload else Icons.Default.CloudDownload,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                  Text(
                    text = transfer.fileName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                  Text(
                    text = if (transfer.isUpload) "Uploading to Saved Messages..." else "Downloading from Telegram...",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                  )
                }
              }

              Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                  onClick = {
                    if (transfer.isPaused) onResume(transfer.fileId) else onPause(transfer.fileId)
                  },
                  modifier = Modifier.size(32.dp)
                ) {
                  Icon(
                    if (transfer.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = "Pause/Resume",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                  )
                }

                IconButton(
                  onClick = { onCancel(transfer.fileId) },
                  modifier = Modifier.size(32.dp)
                ) {
                  Icon(
                    Icons.Default.Close,
                    contentDescription = "Cancel transfer",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress Indicator
            LinearProgressIndicator(
              progress = { transfer.progress },
              modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
              color = MaterialTheme.colorScheme.primary,
              trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text(
                text = "${(transfer.progress * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
              )
              Text(
                text = "${formatFileSize(transfer.bytesTransferred)} / ${formatFileSize(transfer.totalBytes)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
              )
            }
          }
        }
      }
    }
  }
}
