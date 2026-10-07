package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.DriveFile
import com.example.ui.components.formatFileSize
import com.example.viewmodel.DriveViewModel
import com.example.viewmodel.MediaFilter

@Composable
fun MediaScreen(
  viewModel: DriveViewModel,
  onFileClick: (DriveFile) -> Unit,
  modifier: Modifier = Modifier
) {
  val mediaFiles by viewModel.mediaFiles.collectAsState()
  val currentFilter by viewModel.mediaFilter.collectAsState()

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp)
  ) {
    Spacer(modifier = Modifier.height(16.dp))

    Text(
      text = "Media Gallery",
      style = MaterialTheme.typography.headlineMedium,
      fontWeight = FontWeight.Bold
    )
    Text(
      text = "Photos and videos backed up to Telegram Cloud",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.outline
    )

    Spacer(modifier = Modifier.height(16.dp))

    // Filter Chips
    Row(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      FilterChip(
        selected = currentFilter == MediaFilter.ALL,
        onClick = { viewModel.setMediaFilter(MediaFilter.ALL) },
        label = { Text("All Media") },
        leadingIcon = { Icon(Icons.Default.PermMedia, contentDescription = null, modifier = Modifier.size(16.dp)) }
      )
      FilterChip(
        selected = currentFilter == MediaFilter.PHOTOS,
        onClick = { viewModel.setMediaFilter(MediaFilter.PHOTOS) },
        label = { Text("Photos") },
        leadingIcon = { Icon(Icons.Default.Photo, contentDescription = null, modifier = Modifier.size(16.dp)) }
      )
      FilterChip(
        selected = currentFilter == MediaFilter.VIDEOS,
        onClick = { viewModel.setMediaFilter(MediaFilter.VIDEOS) },
        label = { Text("Videos") },
        leadingIcon = { Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(16.dp)) }
      )
    }

    Spacer(modifier = Modifier.height(16.dp))

    if (mediaFiles.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(
            Icons.Default.PhotoLibrary,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outline
          )
          Spacer(modifier = Modifier.height(12.dp))
          Text(
            "No Media Found",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
          )
          Text(
            "Photos and videos you upload or auto-backup will show here",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
          )
        }
      }
    } else {
      LazyVerticalGrid(
        columns = GridCells.Adaptive(110.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 96.dp),
        modifier = Modifier.fillMaxSize()
      ) {
        items(mediaFiles, key = { it.id }) { item ->
          MediaThumbnailItem(
            file = item,
            onClick = { onFileClick(item) }
          )
        }
      }
    }
  }
}

@Composable
private fun MediaThumbnailItem(
  file: DriveFile,
  onClick: () -> Unit
) {
  Box(
    modifier = Modifier
      .aspectRatio(1f)
      .clip(RoundedCornerShape(12.dp))
      .background(MaterialTheme.colorScheme.surfaceVariant)
      .clickable(onClick = onClick)
      .testTag("media_thumbnail_${file.id}"),
    contentAlignment = Alignment.Center
  ) {
    if (!file.localUri.isNullOrBlank()) {
      AsyncImage(
        model = file.localUri,
        contentDescription = file.name,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize()
      )
    } else {
      // High quality placeholder with gradient
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(
            Brush.linearGradient(
              colors = if (file.isVideo) {
                listOf(Color(0xFF1E3A8A), Color(0xFF3B82F6))
              } else {
                listOf(Color(0xFF0F766E), Color(0xFF14B8A6))
              }
            )
          ),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          if (file.isVideo) Icons.Default.Videocam else Icons.Default.Image,
          contentDescription = null,
          tint = Color.White.copy(alpha = 0.8f),
          modifier = Modifier.size(36.dp)
        )
      }
    }

    // Gradient overlay at bottom with name & duration
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .align(Alignment.BottomCenter)
        .background(
          Brush.verticalGradient(
            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
          )
        )
        .padding(6.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = file.name,
          style = MaterialTheme.typography.labelSmall,
          color = Color.White,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f)
        )

        if (file.isVideo) {
          Surface(
            shape = RoundedCornerShape(4.dp),
            color = Color.Black.copy(alpha = 0.6f)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(10.dp)
              )
              Spacer(modifier = Modifier.width(2.dp))
              Text(
                text = "VID",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontSize = MaterialTheme.typography.labelSmall.fontSize * 0.85
              )
            }
          }
        }
      }
    }
  }
}
