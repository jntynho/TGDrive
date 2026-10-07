package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun AboutScreen(
  onBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 20.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    Spacer(modifier = Modifier.height(12.dp))

    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.fillMaxWidth()
    ) {
      IconButton(onClick = onBack) {
        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
      }
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = "About TGDrive CloudGram",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
      )
    }

    // Hero Badge
    Card(
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.primaryContainer
      ),
      modifier = Modifier
        .fillMaxWidth()
        .testTag("about_card")
    ) {
      Column(
        modifier = Modifier.padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Box(
          modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            Icons.Default.Security,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(36.dp)
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
          text = "Zero-Server Privacy Guarantee",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onPrimaryContainer
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          text = "TGDrive operates without any intermediate servers, databases, or cloud proxies. Your device connects directly to official Telegram MTProto datacenters using your own Telegram credentials.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
        )
      }
    }

    Text(
      text = "Security Architecture",
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.Bold
    )

    ArchitectureItem(
      icon = Icons.Default.VpnKey,
      title = "Client-Side AES-256 Encryption",
      desc = "Optional encryption scrambles files on your device with your private passphrase before transmission. Telegram only ever sees encrypted binary blobs."
    )

    ArchitectureItem(
      icon = Icons.Default.AllInclusive,
      title = "2GB – 4GB Free Storage",
      desc = "Every standard Telegram user gets 2GB per file. Telegram Premium accounts get 4GB per file with unlimited cumulative capacity."
    )

    ArchitectureItem(
      icon = Icons.Default.Lock,
      title = "Encrypted Local Keystore",
      desc = "Your MTProto session and cryptographic keys never leave your device. They are stored securely using Android Keystore."
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
      text = "Permissions Explained",
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.Bold
    )

    PermissionItem("READ_MEDIA_IMAGES / VIDEO", "Allows you to select photos/videos to backup to Telegram Saved Messages.")
    PermissionItem("POST_NOTIFICATIONS", "Displays upload/download progress and backup completion notifications.")
    PermissionItem("FOREGROUND_SERVICE", "Ensures large file transfers continue smoothly in background without Android killing the process.")
    PermissionItem("INTERNET", "Required to send packets to Telegram MTProto datacenters (DC1–DC5).")

    Spacer(modifier = Modifier.height(32.dp))
  }
}

@Composable
private fun ArchitectureItem(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  title: String,
  desc: String
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.Top
  ) {
    Box(
      modifier = Modifier
        .size(40.dp)
        .clip(RoundedCornerShape(10.dp))
        .background(MaterialTheme.colorScheme.surfaceVariant),
      contentAlignment = Alignment.Center
    ) {
      Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
    }
    Spacer(modifier = Modifier.width(14.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
      Text(text = desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
  }
}

@Composable
private fun PermissionItem(name: String, reason: String) {
  Card(
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    ),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Text(text = name, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
      Spacer(modifier = Modifier.height(4.dp))
      Text(text = reason, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
    }
  }
}
