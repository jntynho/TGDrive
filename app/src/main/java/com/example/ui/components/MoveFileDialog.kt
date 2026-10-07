package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.DriveFile
import com.example.data.VirtualFolder

@Composable
fun MoveFileDialog(
  file: DriveFile,
  folders: List<VirtualFolder>,
  onDismiss: () -> Unit,
  onFolderSelected: (String) -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    shape = RoundedCornerShape(20.dp),
    icon = {
      Icon(
        Icons.Default.DriveFileMove,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary
      )
    },
    title = {
      Text("Move '${file.name}'")
    },
    text = {
      Column(modifier = Modifier.fillMaxWidth()) {
        Text(
          "Current folder: ${file.virtualFolder}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.height(12.dp))
        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 240.dp)
        ) {
          items(folders) { folder ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  onFolderSelected(folder.id)
                  onDismiss()
                }
                .padding(vertical = 10.dp, horizontal = 8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                Icons.Default.Folder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
              )
              Spacer(modifier = Modifier.width(12.dp))
              Text(
                text = folder.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (folder.id == file.virtualFolder) FontWeight.Bold else FontWeight.Normal,
                color = if (folder.id == file.virtualFolder) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
              )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}
