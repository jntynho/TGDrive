package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.DriveFile
import com.example.telegram.TransferProgress
import com.example.ui.components.formatFileSize
import com.example.ui.components.formatTimestamp
import com.example.viewmodel.DriveViewModel

@Composable
fun HomeScreen(
  viewModel: DriveViewModel,
  onFileClick: (DriveFile) -> Unit,
  onPickFile: () -> Unit,
  onOpenNewFolderDialog: () -> Unit,
  onNavigateToFiles: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  val totalStorage by viewModel.totalStorageBytes.collectAsState()
  val fileCount by viewModel.fileCount.collectAsState()
  val recentFiles by viewModel.recentFiles.collectAsState()
  val activeTransfers by viewModel.activeTransfers.collectAsState()

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Header Greeting
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "TGDrive Cloud",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "Saved Messages Vault",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
          )
        }

        Surface(
          shape = RoundedCornerShape(20.dp),
          color = MaterialTheme.colorScheme.primaryContainer,
          tonalElevation = 2.dp
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(Color(0xFF10B981))
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "MTProto Connected",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onPrimaryContainer
            )
          }
        }
      }
    }

    // Storage Status Card
    item {
      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("storage_card")
      ) {
        Column(modifier = Modifier.padding(18.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(36.dp)
                  .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  Icons.Default.Cloud,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onPrimary,
                  modifier = Modifier.size(20.dp)
                )
              }
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text = "Storage Used",
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.outline
                )
                Text(
                  text = formatFileSize(totalStorage),
                  style = MaterialTheme.typography.titleLarge,
                  fontWeight = FontWeight.Bold
                )
              }
            }

            Surface(
              shape = RoundedCornerShape(12.dp),
              color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            ) {
              Text(
                text = "Unlimited Plan",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          // Visual capacity progress
          LinearProgressIndicator(
            progress = { 0.12f }, // Visual representation of active usage in unlimited cloud
            modifier = Modifier
              .fillMaxWidth()
              .height(8.dp)
              .clip(RoundedCornerShape(4.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
          )

          Spacer(modifier = Modifier.height(10.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(
              text = "$fileCount files uploaded",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
              text = "Up to 4GB / file",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.primary,
              fontWeight = FontWeight.SemiBold
            )
          }
        }
      }
    }

    // Quick Action Buttons
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        QuickActionButton(
          icon = Icons.Default.CloudUpload,
          label = "Upload File",
          onClick = onPickFile,
          modifier = Modifier
            .weight(1f)
            .testTag("home_upload_file_btn")
        )
        QuickActionButton(
          icon = Icons.Default.PhotoCamera,
          label = "Backup DCIM",
          onClick = { viewModel.triggerDcimScanAndBackup() },
          modifier = Modifier
            .weight(1f)
            .testTag("home_backup_dcim_btn")
        )
        QuickActionButton(
          icon = Icons.Default.CreateNewFolder,
          label = "New Folder",
          onClick = onOpenNewFolderDialog,
          modifier = Modifier
            .weight(1f)
            .testTag("home_new_folder_btn")
        )
      }
    }

    // Active Transfers Section (if any)
    if (activeTransfers.isNotEmpty()) {
      item {
        Text(
          text = "Active Cloud Transfers (${activeTransfers.size})",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold
        )
      }
      items(activeTransfers.values.toList()) { transfer ->
        TransferItemCard(
          transfer = transfer,
          onPause = { viewModel.telegramClient.pauseTransfer(transfer.fileId) },
          onResume = { viewModel.telegramClient.resumeTransfer(transfer.fileId) },
          onCancel = { viewModel.telegramClient.cancelTransfer(transfer.fileId) }
        )
      }
    }

    // Recent Files Section
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Recent Uploads",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold
        )
        TextButton(onClick = { onNavigateToFiles("Root") }) {
          Text("View All")
        }
      }
    }

    if (recentFiles.isEmpty()) {
      item {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
          ),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Icon(
              Icons.Default.CloudQueue,
              contentDescription = null,
              modifier = Modifier.size(48.dp),
              tint = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("No recent files", style = MaterialTheme.typography.bodyMedium)
            Text(
              "Tap 'Upload File' to add your first file to Telegram Saved Messages",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.outline
            )
          }
        }
      }
    } else {
      items(recentFiles) { file ->
        RecentFileRow(
          file = file,
          onClick = { onFileClick(file) },
          onDelete = { viewModel.moveToTrash(file) }
        )
      }
    }
  }
}

@Composable
private fun QuickActionButton(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Surface(
    onClick = onClick,
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    tonalElevation = 1.dp,
    modifier = modifier.height(72.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(8.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

@Composable
private fun TransferItemCard(
  transfer: TransferProgress,
  onPause: () -> Unit,
  onResume: () -> Unit,
  onCancel: () -> Unit
) {
  Card(
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(transfer.fileName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 1)
          Text(
            if (transfer.isUpload) "Uploading to Saved Messages" else "Downloading from Telegram",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
          )
        }
        Row {
          IconButton(onClick = { if (transfer.isPaused) onResume() else onPause() }) {
            Icon(if (transfer.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause, contentDescription = null)
          }
          IconButton(onClick = onCancel) {
            Icon(Icons.Default.Close, contentDescription = null, tint = MaterialTheme.colorScheme.error)
          }
        }
      }
      LinearProgressIndicator(
        progress = { transfer.progress },
        modifier = Modifier
          .fillMaxWidth()
          .height(6.dp)
          .clip(RoundedCornerShape(3.dp))
      )
    }
  }
}

@Composable
private fun RecentFileRow(
  file: DriveFile,
  onClick: () -> Unit,
  onDelete: () -> Unit
) {
  Surface(
    shape = RoundedCornerShape(14.dp),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .testTag("file_item_${file.id}")
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(42.dp)
          .clip(RoundedCornerShape(10.dp))
          .background(
            when {
              file.isImage -> MaterialTheme.colorScheme.primaryContainer
              file.isVideo -> MaterialTheme.colorScheme.secondaryContainer
              file.isPdf -> MaterialTheme.colorScheme.errorContainer
              else -> MaterialTheme.colorScheme.surfaceVariant
            }
          ),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          when {
            file.isImage -> Icons.Default.Image
            file.isVideo -> Icons.Default.Videocam
            file.isPdf -> Icons.Default.PictureAsPdf
            file.isAudio -> Icons.Default.Audiotrack
            file.isArchive -> Icons.Default.Archive
            else -> Icons.Default.InsertDriveFile
          },
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(22.dp)
        )
      }

      Spacer(modifier = Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = file.name,
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.SemiBold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          text = "${formatFileSize(file.size)} • ${file.virtualFolder}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.outline
        )
      }

      Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
      ) {
        Icon(
          Icons.Default.CloudDone,
          contentDescription = "Synced to Telegram",
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier
            .padding(6.dp)
            .size(16.dp)
        )
      }
    }
  }
}
