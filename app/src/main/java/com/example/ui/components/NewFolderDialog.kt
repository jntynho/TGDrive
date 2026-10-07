package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun NewFolderDialog(
  onDismiss: () -> Unit,
  onCreateFolder: (String) -> Unit
) {
  var folderName by remember { mutableStateOf("") }
  var isError by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    shape = RoundedCornerShape(20.dp),
    icon = {
      Icon(
        Icons.Default.CreateNewFolder,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(28.dp)
      )
    },
    title = {
      Text("Create Virtual Folder")
    },
    text = {
      Column {
        Text(
          "Virtual folders help you organize your Telegram Saved Messages locally without altering chat files.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
          value = folderName,
          onValueChange = {
            folderName = it
            isError = false
          },
          label = { Text("Folder Name") },
          singleLine = true,
          isError = isError,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("folder_name_input"),
          shape = RoundedCornerShape(12.dp)
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (folderName.trim().isNotBlank()) {
            onCreateFolder(folderName.trim())
            onDismiss()
          } else {
            isError = true
          }
        },
        modifier = Modifier.testTag("confirm_create_folder")
      ) {
        Text("Create")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}
