package com.example.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.DriveFile
import com.example.data.VirtualFolder
import com.example.ui.components.formatFileSize
import com.example.ui.components.formatTimestamp
import com.example.viewmodel.DriveViewModel
import com.example.viewmodel.SortOption

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FilesScreen(
  viewModel: DriveViewModel,
  onFileClick: (DriveFile) -> Unit,
  onPickFile: () -> Unit,
  onOpenNewFolderDialog: () -> Unit,
  onMoveFile: (DriveFile) -> Unit,
  modifier: Modifier = Modifier
) {
  val files by viewModel.filesInCurrentFolder.collectAsState()
  val folders by viewModel.allFolders.collectAsState()
  val selectedFolder by viewModel.selectedFolder.collectAsState()
  val searchQuery by viewModel.searchQuery.collectAsState()
  val isGridView by viewModel.isGridView.collectAsState()
  val sortOption by viewModel.sortOption.collectAsState()

  var showSortMenu by remember { mutableStateOf(false) }

  Scaffold(
    floatingActionButton = {
      ExtendedFloatingActionButton(
        onClick = onPickFile,
        icon = { Icon(Icons.Default.CloudUpload, contentDescription = null) },
        text = { Text("Upload") },
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        modifier = Modifier.testTag("files_upload_fab")
      )
    },
    modifier = modifier.fillMaxSize()
  ) { paddingValues ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .padding(horizontal = 16.dp)
    ) {
      Spacer(modifier = Modifier.height(12.dp))

      // Search Bar (Instant search-as-you-type)
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { viewModel.setSearchQuery(it) },
        placeholder = { Text("Search files by name or extension...") },
        leadingIcon = {
          Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        },
        trailingIcon = {
          if (searchQuery.isNotBlank()) {
            IconButton(onClick = { viewModel.setSearchQuery("") }) {
              Icon(Icons.Default.Clear, contentDescription = "Clear search")
            }
          }
        },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
          focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
          unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("file_search_input")
      )

      Spacer(modifier = Modifier.height(12.dp))

      // Folder Chips Row
      LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        items(folders) { folder ->
          val isSelected = folder.id == selectedFolder
          FilterChip(
            selected = isSelected,
            onClick = { viewModel.setSelectedFolder(folder.id) },
            label = { Text(folder.name) },
            leadingIcon = {
              Icon(
                when (folder.iconName) {
                  "photo_camera" -> Icons.Default.PhotoCamera
                  "description" -> Icons.Default.Description
                  "perm_media" -> Icons.Default.PermMedia
                  "work" -> Icons.Default.Work
                  else -> Icons.Default.Folder
                },
                contentDescription = null,
                modifier = Modifier.size(16.dp)
              )
            },
            shape = RoundedCornerShape(12.dp)
          )
        }

        item {
          IconButton(
            onClick = onOpenNewFolderDialog,
            modifier = Modifier.size(36.dp)
          ) {
            Icon(
              Icons.Default.CreateNewFolder,
              contentDescription = "New Folder",
              tint = MaterialTheme.colorScheme.primary
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Controls Bar: item count, sort, grid/list view toggle
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "${files.size} items",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.outline
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
          Box {
            IconButton(onClick = { showSortMenu = true }) {
              Icon(Icons.Default.Sort, contentDescription = "Sort files")
            }

            DropdownMenu(
              expanded = showSortMenu,
              onDismissRequest = { showSortMenu = false }
            ) {
              DropdownMenuItem(
                text = { Text("Date (Newest First)") },
                onClick = {
                  viewModel.setSortOption(SortOption.DATE_DESC)
                  showSortMenu = false
                },
                leadingIcon = { if (sortOption == SortOption.DATE_DESC) Icon(Icons.Default.Check, null) }
              )
              DropdownMenuItem(
                text = { Text("Date (Oldest First)") },
                onClick = {
                  viewModel.setSortOption(SortOption.DATE_ASC)
                  showSortMenu = false
                },
                leadingIcon = { if (sortOption == SortOption.DATE_ASC) Icon(Icons.Default.Check, null) }
              )
              DropdownMenuItem(
                text = { Text("Name (A to Z)") },
                onClick = {
                  viewModel.setSortOption(SortOption.NAME_ASC)
                  showSortMenu = false
                },
                leadingIcon = { if (sortOption == SortOption.NAME_ASC) Icon(Icons.Default.Check, null) }
              )
              DropdownMenuItem(
                text = { Text("Size (Largest First)") },
                onClick = {
                  viewModel.setSortOption(SortOption.SIZE_DESC)
                  showSortMenu = false
                },
                leadingIcon = { if (sortOption == SortOption.SIZE_DESC) Icon(Icons.Default.Check, null) }
              )
            }
          }

          IconButton(onClick = { viewModel.toggleGridView() }) {
            Icon(
              if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
              contentDescription = "Toggle Grid/List View"
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(4.dp))

      // File Browser Content
      if (files.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
          contentAlignment = Alignment.Center
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
              Icons.Default.FolderOpen,
              contentDescription = null,
              modifier = Modifier.size(64.dp),
              tint = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
              if (searchQuery.isNotBlank()) "No files match '$searchQuery'" else "No files in $selectedFolder",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.SemiBold
            )
            Text(
              "Tap '+' to upload files to this virtual folder",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.outline
            )
          }
        }
      } else if (isGridView) {
        LazyVerticalGrid(
          columns = GridCells.Fixed(2),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
          contentPadding = PaddingValues(bottom = 96.dp),
          modifier = Modifier.fillMaxSize()
        ) {
          items(files, key = { it.id }) { file ->
            FileGridItem(
              file = file,
              onClick = { onFileClick(file) },
              onDownload = { viewModel.downloadFile(file) },
              onMove = { onMoveFile(file) },
              onDelete = { viewModel.moveToTrash(file) }
            )
          }
        }
      } else {
        LazyColumn(
          verticalArrangement = Arrangement.spacedBy(8.dp),
          contentPadding = PaddingValues(bottom = 96.dp),
          modifier = Modifier.fillMaxSize()
        ) {
          items(files, key = { it.id }) { file ->
            FileListItem(
              file = file,
              onClick = { onFileClick(file) },
              onDownload = { viewModel.downloadFile(file) },
              onMove = { onMoveFile(file) },
              onDelete = { viewModel.moveToTrash(file) }
            )
          }
        }
      }
    }
  }
}

@Composable
private fun FileListItem(
  file: DriveFile,
  onClick: () -> Unit,
  onDownload: () -> Unit,
  onMove: () -> Unit,
  onDelete: () -> Unit
) {
  var showMenu by remember { mutableStateOf(false) }

  Surface(
    shape = RoundedCornerShape(14.dp),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
    modifier = Modifier
      .fillMaxWidth()
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
          .size(44.dp)
          .clip(RoundedCornerShape(12.dp))
          .background(MaterialTheme.colorScheme.primaryContainer),
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
          modifier = Modifier.size(24.dp)
        )
      }

      Spacer(modifier = Modifier.width(12.dp))

      Column(
        modifier = Modifier
          .weight(1f)
          .clickable(onClick = onClick)
      ) {
        Text(
          text = file.name,
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.SemiBold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          text = "${formatFileSize(file.size)} • ${formatTimestamp(file.uploadDate)}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.outline
        )
      }

      Box {
        IconButton(onClick = { showMenu = true }) {
          Icon(Icons.Default.MoreVert, contentDescription = "More actions")
        }

        DropdownMenu(
          expanded = showMenu,
          onDismissRequest = { showMenu = false }
        ) {
          DropdownMenuItem(
            text = { Text("Preview") },
            onClick = {
              showMenu = false
              onClick()
            },
            leadingIcon = { Icon(Icons.Default.Visibility, null) }
          )
          DropdownMenuItem(
            text = { Text("Download") },
            onClick = {
              showMenu = false
              onDownload()
            },
            leadingIcon = { Icon(Icons.Default.Download, null) }
          )
          DropdownMenuItem(
            text = { Text("Move to Folder") },
            onClick = {
              showMenu = false
              onMove()
            },
            leadingIcon = { Icon(Icons.Default.DriveFileMove, null) }
          )
          DropdownMenuItem(
            text = { Text("Move to Trash") },
            onClick = {
              showMenu = false
              onDelete()
            },
            leadingIcon = { Icon(Icons.Default.DeleteOutline, null, tint = MaterialTheme.colorScheme.error) }
          )
        }
      }
    }
  }
}

@Composable
private fun FileGridItem(
  file: DriveFile,
  onClick: () -> Unit,
  onDownload: () -> Unit,
  onMove: () -> Unit,
  onDelete: () -> Unit
) {
  var showMenu by remember { mutableStateOf(false) }

  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ),
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .testTag("file_grid_item_${file.id}")
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.primaryContainer),
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
            modifier = Modifier.size(20.dp)
          )
        }

        Box {
          IconButton(
            onClick = { showMenu = true },
            modifier = Modifier.size(28.dp)
          ) {
            Icon(Icons.Default.MoreVert, contentDescription = "More", modifier = Modifier.size(18.dp))
          }

          DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
          ) {
            DropdownMenuItem(
              text = { Text("Preview") },
              onClick = {
                showMenu = false
                onClick()
              },
              leadingIcon = { Icon(Icons.Default.Visibility, null) }
            )
            DropdownMenuItem(
              text = { Text("Download") },
              onClick = {
                showMenu = false
                onDownload()
              },
              leadingIcon = { Icon(Icons.Default.Download, null) }
            )
            DropdownMenuItem(
              text = { Text("Move") },
              onClick = {
                showMenu = false
                onMove()
              },
              leadingIcon = { Icon(Icons.Default.DriveFileMove, null) }
            )
            DropdownMenuItem(
              text = { Text("Trash") },
              onClick = {
                showMenu = false
                onDelete()
              },
              leadingIcon = { Icon(Icons.Default.DeleteOutline, null) }
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      Text(
        text = file.name,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )

      Spacer(modifier = Modifier.height(4.dp))

      Text(
        text = formatFileSize(file.size),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.outline
      )
    }
  }
}
