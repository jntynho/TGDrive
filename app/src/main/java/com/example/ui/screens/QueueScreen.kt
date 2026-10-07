package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.localization.StringResources
import com.example.telegram.TransferProgress
import com.example.ui.components.formatFileSize
import com.example.viewmodel.DriveViewModel

@Composable
fun QueueScreen(
  viewModel: DriveViewModel,
  modifier: Modifier = Modifier
) {
  val activeTransfers by viewModel.activeTransfers.collectAsState()
  val backupWifiOnly by viewModel.backupWifiOnly.collectAsState()
  val bandwidthLimit by viewModel.bandwidthLimitKbps.collectAsState()
  val strings = StringResources.get()

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp)
  ) {
    Spacer(modifier = Modifier.height(16.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column {
        Text(
          text = strings.queueTab,
          style = MaterialTheme.typography.headlineMedium,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = if (bandwidthLimit > 0) "Throttled: ${bandwidthLimit} KB/s" else "Unlimited Speed • MTProto DC4",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.primary
        )
      }

      if (backupWifiOnly) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.secondaryContainer
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Wi-Fi Only", style = MaterialTheme.typography.labelSmall)
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    if (activeTransfers.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(
            Icons.Default.CheckCircleOutline,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
          )
          Spacer(modifier = Modifier.height(12.dp))
          Text(
            text = strings.queueEmpty,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
          )
          Text(
            text = "Files you upload or download will appear here with live progress.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
          )
        }
      }
    } else {
      LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
      ) {
        items(activeTransfers.values.toList(), key = { it.fileId }) { transfer ->
          Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            ),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("queue_item_${transfer.fileId}")
          ) {
            Column(modifier = Modifier.padding(14.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = transfer.fileName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                  Text(
                    text = if (transfer.isPaused) "Paused" else "${(transfer.progress * 100).toInt()}% • ${formatFileSize(transfer.bytesTransferred)} / ${formatFileSize(transfer.totalBytes)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (transfer.isPaused) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                  )
                }

                Row {
                  IconButton(onClick = {
                    if (transfer.isPaused) {
                      viewModel.telegramClient.resumeTransfer(transfer.fileId)
                    } else {
                      viewModel.telegramClient.pauseTransfer(transfer.fileId)
                    }
                  }) {
                    Icon(
                      if (transfer.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                      contentDescription = "Pause/Resume"
                    )
                  }

                  IconButton(onClick = {
                    viewModel.telegramClient.cancelTransfer(transfer.fileId)
                  }) {
                    Icon(
                      Icons.Default.Close,
                      contentDescription = "Cancel",
                      tint = MaterialTheme.colorScheme.error
                    )
                  }
                }
              }

              Spacer(modifier = Modifier.height(10.dp))

              LinearProgressIndicator(
                progress = { transfer.progress },
                modifier = Modifier
                  .fillMaxWidth()
                  .height(8.dp)
                  .clip(RoundedCornerShape(4.dp)),
                color = if (transfer.isPaused) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary
              )
            }
          }
        }
      }
    }
  }
}
