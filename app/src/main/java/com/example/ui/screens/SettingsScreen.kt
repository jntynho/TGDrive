package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.telegram.TelegramClient
import com.example.localization.StringResources
import com.example.ui.components.UsageStatsCard
import com.example.ui.components.formatFileSize
import com.example.ui.theme.ThemeMode
import com.example.viewmodel.DriveViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
  viewModel: DriveViewModel,
  onOpenTrashDialog: () -> Unit,
  onOpenAboutScreen: () -> Unit,
  onLogout: () -> Unit,
  modifier: Modifier = Modifier
) {
  val coroutineScope = rememberCoroutineScope()
  val strings = StringResources.get()

  val currentUser = viewModel.sessionManager.getUser()
  val savedAccounts by viewModel.savedAccounts.collectAsState()
  val allFiles by viewModel.allActiveFiles.collectAsState()
  val trashFiles by viewModel.trashFiles.collectAsState()

  // Advanced toggles
  val clientEncryption by viewModel.clientEncryptionEnabled.collectAsState()
  val passphrase by viewModel.encryptionPassphrase.collectAsState()
  val autoBackupDcim by viewModel.autoBackupDcim.collectAsState()
  val backupWifiOnly by viewModel.backupWifiOnly.collectAsState()
  val nightSyncOnly by viewModel.nightSyncOnly.collectAsState()
  val bandwidthLimit by viewModel.bandwidthLimitKbps.collectAsState()
  val compressMedia by viewModel.compressMediaBeforeUpload.collectAsState()
  val compressQuality by viewModel.compressionQuality.collectAsState()
  val biometricLock by viewModel.biometricLockEnabled.collectAsState()
  val appPin by viewModel.appPinCode.collectAsState()
  val autoArchiveDays by viewModel.autoArchiveDays.collectAsState()
  val currentTheme by viewModel.themeMode.collectAsState()
  val apiId by viewModel.apiId.collectAsState()
  val apiHash by viewModel.apiHash.collectAsState()

  // Dialog triggers
  var showAccountDialog by remember { mutableStateOf(false) }
  var showEncryptionDialog by remember { mutableStateOf(false) }
  var showBandwidthDialog by remember { mutableStateOf(false) }
  var showPinDialog by remember { mutableStateOf(false) }
  var showExportDialog by remember { mutableStateOf(false) }
  var showImportDialog by remember { mutableStateOf(false) }
  var exportedJsonText by remember { mutableStateOf("") }
  var showApiDialog by remember { mutableStateOf(false) }
  var showDiagnosticsDialog by remember { mutableStateOf(false) }
  var showLogoutConfirm by remember { mutableStateOf(false) }

  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    Spacer(modifier = Modifier.height(12.dp))

    Text(
      text = strings.settingsTab,
      style = MaterialTheme.typography.headlineMedium,
      fontWeight = FontWeight.Bold
    )

    // 1. Multi-Account Section
    Card(
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
      ),
      modifier = Modifier
        .fillMaxWidth()
        .testTag("account_card")
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = currentUser.firstName.take(1).ifBlank { "T" },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
              )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  text = currentUser.fullName,
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                ) {
                  Text(
                    text = currentUser.accountLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                }
              }
              Text(
                text = currentUser.phoneNumber.ifBlank { "+1 202 555 0198" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
              )
            }
          }

          OutlinedButton(
            onClick = { showAccountDialog = true },
            modifier = Modifier.testTag("switch_account_btn")
          ) {
            Icon(Icons.Default.SupervisorAccount, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(strings.switchAccount)
          }
        }
      }
    }

    // 2. Client-Side Encryption (AES-256)
    Card(
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
      ),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.EnhancedEncryption, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(strings.encryption, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
              Text(strings.encryptionDesc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
          }
          Switch(
            checked = clientEncryption,
            onCheckedChange = { isChecked ->
              if (isChecked) {
                showEncryptionDialog = true
              } else {
                viewModel.setClientEncryption(false, passphrase)
              }
            },
            modifier = Modifier.testTag("encryption_switch")
          )
        }

        if (clientEncryption) {
          Spacer(modifier = Modifier.height(8.dp))
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
            modifier = Modifier
              .fillMaxWidth()
              .clickable { showEncryptionDialog = true }
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text("Key: derived via PBKDF2 (SHA-256)", style = MaterialTheme.typography.labelSmall)
              Text("Edit Passphrase", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }

    // 3. Smart Sync & Scheduling
    Card(
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
      ),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text(strings.smartSync, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(10.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(strings.wifiOnly, style = MaterialTheme.typography.bodyMedium)
          Switch(
            checked = backupWifiOnly,
            onCheckedChange = { viewModel.setSmartSync(it, nightSyncOnly, bandwidthLimit) }
          )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(strings.nightSyncOnly, style = MaterialTheme.typography.bodyMedium)
          Switch(
            checked = nightSyncOnly,
            onCheckedChange = { viewModel.setSmartSync(backupWifiOnly, it, bandwidthLimit) }
          )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable { showBandwidthDialog = true },
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(strings.bandwidthLimit, style = MaterialTheme.typography.bodyMedium)
            Text(
              if (bandwidthLimit == 0) "Unlimited (Maximum)" else "$bandwidthLimit KB/s",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.primary,
              fontWeight = FontWeight.Bold
            )
          }
          Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
        }
      }
    }

    // 4. Media Compression
    Card(
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
      ),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(strings.mediaCompression, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(strings.compressionDesc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
          }
          Switch(
            checked = compressMedia,
            onCheckedChange = { viewModel.setCompression(it, compressQuality) }
          )
        }

        if (compressMedia) {
          Spacer(modifier = Modifier.height(10.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("Quality Level: $compressQuality%", style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              FilterChip(
                selected = compressQuality == 70,
                onClick = { viewModel.setCompression(true, 70) },
                label = { Text("70%") }
              )
              FilterChip(
                selected = compressQuality == 80,
                onClick = { viewModel.setCompression(true, 80) },
                label = { Text("80%") }
              )
              FilterChip(
                selected = compressQuality == 90,
                onClick = { viewModel.setCompression(true, 90) },
                label = { Text("90%") }
              )
            }
          }
        }
      }
    }

    // 5. Biometric Lock
    Card(
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
      ),
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(strings.biometricLock, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
          Text(strings.biometricDesc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        }
        Switch(
          checked = biometricLock,
          onCheckedChange = { enabled ->
            if (enabled) {
              showPinDialog = true
            } else {
              viewModel.setBiometricLock(false, appPin)
            }
          }
        )
      }
    }

    // 6. Smart Archiving
    Card(
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
      ),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text(strings.smartArchive, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        Text(strings.smartArchiveDesc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        Spacer(modifier = Modifier.height(10.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("Archive older than: $autoArchiveDays days", style = MaterialTheme.typography.bodySmall)
          OutlinedButton(onClick = { viewModel.archiveOldFilesNow() }) {
            Icon(Icons.Default.CloudDone, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(strings.archiveNow)
          }
        }
      }
    }

    // 7. Usage Statistics & Charts Card
    UsageStatsCard(files = allFiles)

    // 8. Export / Import Settings & Index
    Card(
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
      ),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text("Backup & Transfer Index", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(10.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Button(
            onClick = {
              coroutineScope.launch {
                exportedJsonText = viewModel.exportDatabaseIndex(if (clientEncryption) passphrase else null)
                showExportDialog = true
              }
            },
            modifier = Modifier.weight(1f)
          ) {
            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(strings.exportBackup)
          }

          OutlinedButton(
            onClick = { showImportDialog = true },
            modifier = Modifier.weight(1f)
          ) {
            Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(strings.importBackup)
          }
        }
      }
    }

    // 9. Trash Bin Card
    Card(
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
      ),
      modifier = Modifier
        .fillMaxWidth()
        .clickable(onClick = onOpenTrashDialog)
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
          Spacer(modifier = Modifier.width(12.dp))
          Text("${strings.trash} (${trashFiles.size} items)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null)
      }
    }

    // 10. About Screen & Transparency
    Card(
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
      ),
      modifier = Modifier
        .fillMaxWidth()
        .clickable(onClick = onOpenAboutScreen)
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
          Spacer(modifier = Modifier.width(12.dp))
          Text(strings.aboutTitle, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null)
      }
    }

    // Telegram MTProto Credentials
    Card(
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
      ),
      modifier = Modifier
        .fillMaxWidth()
        .clickable { showApiDialog = true }
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text("Telegram MTProto API Credentials", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
          Text("API ID: ${apiId.ifBlank { "Not set" }} • my.telegram.org", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null)
      }
    }

    // Diagnostics & TDLib Logs
    Card(
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
      ),
      modifier = Modifier
        .fillMaxWidth()
        .clickable { showDiagnosticsDialog = true }
        .testTag("diagnostics_card")
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.BugReport, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
          Spacer(modifier = Modifier.width(12.dp))
          Column {
            Text("Diagnostic Log Report (TGDriveAuth)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text("View & copy last 50 TDLib auth events", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
          }
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null)
      }
    }

    // Theme Mode
    Card(
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
      ),
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text("Theme Mode", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          FilterChip(selected = currentTheme == ThemeMode.SYSTEM, onClick = { viewModel.setThemeMode(ThemeMode.SYSTEM) }, label = { Text("Auto") })
          FilterChip(selected = currentTheme == ThemeMode.LIGHT, onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) }, label = { Text("Light") })
          FilterChip(selected = currentTheme == ThemeMode.DARK, onClick = { viewModel.setThemeMode(ThemeMode.DARK) }, label = { Text("Dark") })
        }
      }
    }

    // Sign Out Button
    Button(
      onClick = { showLogoutConfirm = true },
      colors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer
      ),
      shape = RoundedCornerShape(14.dp),
      modifier = Modifier
        .fillMaxWidth()
        .height(50.dp)
        .testTag("logout_button")
    ) {
      Icon(Icons.Default.ExitToApp, contentDescription = null)
      Spacer(modifier = Modifier.width(8.dp))
      Text(strings.logout)
    }

    Spacer(modifier = Modifier.height(80.dp))
  }

  // Multi-Account Dialog
  if (showAccountDialog) {
    var newAccountLabel by remember { mutableStateOf("Work") }
    var newAccountPhone by remember { mutableStateOf("+12025550199") }
    var isAdding by remember { mutableStateOf(false) }

    AlertDialog(
      onDismissRequest = { showAccountDialog = false },
      title = { Text(strings.multiAccount) },
      text = {
        Column {
          Text("Switch between accounts or add a new Telegram profile for separate Saved Messages vaults.")
          Spacer(modifier = Modifier.height(12.dp))

          savedAccounts.forEach { acc ->
            val isCurrent = acc.userId == currentUser.userId
            Card(
              shape = RoundedCornerShape(10.dp),
              colors = CardDefaults.cardColors(
                containerColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
              ),
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clickable {
                  viewModel.switchAccount(acc.userId)
                  showAccountDialog = false
                }
            ) {
              Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column {
                  Text(acc.fullName, fontWeight = FontWeight.Bold)
                  Text("${acc.accountLabel} • ${acc.phoneNumber}", style = MaterialTheme.typography.bodySmall)
                }
                if (isCurrent) {
                  Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
              }
            }
          }

          if (isAdding) {
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
              value = newAccountLabel,
              onValueChange = { newAccountLabel = it },
              label = { Text("Label (e.g. Work / Family)") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
              value = newAccountPhone,
              onValueChange = { newAccountPhone = it },
              label = { Text("Phone Number") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )
          }
        }
      },
      confirmButton = {
        if (!isAdding) {
          Button(onClick = { isAdding = true }) {
            Text(strings.addAccount)
          }
        } else {
          Button(onClick = {
            viewModel.addNewSimulatedAccount(newAccountLabel, newAccountPhone)
            showAccountDialog = false
          }) {
            Text("Confirm Add")
          }
        }
      },
      dismissButton = {
        TextButton(onClick = { showAccountDialog = false }) {
          Text("Close")
        }
      }
    )
  }

  // Encryption Passphrase Dialog
  if (showEncryptionDialog) {
    var editPassphrase by remember { mutableStateOf(passphrase) }

    AlertDialog(
      onDismissRequest = { showEncryptionDialog = false },
      title = { Text("Set AES-256 Passphrase") },
      text = {
        Column {
          Text("Files will be encrypted on device before uploading. Keep your passphrase safe; without it, encrypted files cannot be restored.")
          Spacer(modifier = Modifier.height(12.dp))
          OutlinedTextField(
            value = editPassphrase,
            onValueChange = { editPassphrase = it },
            label = { Text("Encryption Passphrase") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (editPassphrase.isNotBlank()) {
              viewModel.setClientEncryption(true, editPassphrase)
              showEncryptionDialog = false
            }
          }
        ) {
          Text("Enable Encryption")
        }
      },
      dismissButton = {
        TextButton(onClick = { showEncryptionDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }

  // Bandwidth limit dialog
  if (showBandwidthDialog) {
    AlertDialog(
      onDismissRequest = { showBandwidthDialog = false },
      title = { Text(strings.bandwidthLimit) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          listOf(0 to "Unlimited (Fastest)", 256 to "256 KB/s", 512 to "512 KB/s", 1024 to "1024 KB/s (1 MB/s)").forEach { (limit, title) ->
            Card(
              shape = RoundedCornerShape(10.dp),
              colors = CardDefaults.cardColors(
                containerColor = if (bandwidthLimit == limit) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
              ),
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  viewModel.setSmartSync(backupWifiOnly, nightSyncOnly, limit)
                  showBandwidthDialog = false
                }
            ) {
              Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text(title, fontWeight = FontWeight.SemiBold)
                if (bandwidthLimit == limit) Icon(Icons.Default.Check, null)
              }
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showBandwidthDialog = false }) { Text("Close") }
      }
    )
  }

  // Pin setup dialog
  if (showPinDialog) {
    var editPin by remember { mutableStateOf(appPin) }

    AlertDialog(
      onDismissRequest = { showPinDialog = false },
      title = { Text("Configure Security PIN") },
      text = {
        Column {
          Text("Enter 4 digits for your app security lock.")
          Spacer(modifier = Modifier.height(12.dp))
          OutlinedTextField(
            value = editPin,
            onValueChange = { if (it.length <= 4) editPin = it },
            label = { Text("4-Digit PIN") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(onClick = {
          if (editPin.length == 4) {
            viewModel.setBiometricLock(true, editPin)
            showPinDialog = false
          }
        }) {
          Text("Set Lock")
        }
      },
      dismissButton = {
        TextButton(onClick = { showPinDialog = false }) { Text("Cancel") }
      }
    )
  }

  // Export Dialog
  if (showExportDialog) {
    AlertDialog(
      onDismissRequest = { showExportDialog = false },
      title = { Text(strings.exportBackup) },
      text = {
        Column {
          Text("Index JSON copied or ready for export:")
          Spacer(modifier = Modifier.height(10.dp))
          OutlinedTextField(
            value = exportedJsonText.take(400) + if (exportedJsonText.length > 400) "...\n[Total ${exportedJsonText.length} characters]" else "",
            onValueChange = {},
            readOnly = true,
            maxLines = 6,
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(onClick = {
          viewModel.showMessage("Index JSON exported successfully!")
          showExportDialog = false
        }) {
          Text("Done")
        }
      }
    )
  }

  // Import Dialog
  if (showImportDialog) {
    var importInput by remember { mutableStateOf("") }

    AlertDialog(
      onDismissRequest = { showImportDialog = false },
      title = { Text(strings.importBackup) },
      text = {
        Column {
          Text("Paste exported JSON payload:")
          Spacer(modifier = Modifier.height(10.dp))
          OutlinedTextField(
            value = importInput,
            onValueChange = { importInput = it },
            label = { Text("JSON Payload") },
            maxLines = 6,
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(onClick = {
          coroutineScope.launch {
            viewModel.importDatabaseIndex(importInput, if (clientEncryption) passphrase else null)
            showImportDialog = false
          }
        }) {
          Text("Import Now")
        }
      },
      dismissButton = {
        TextButton(onClick = { showImportDialog = false }) { Text("Cancel") }
      }
    )
  }

  // Telegram API Dialog
  if (showApiDialog) {
    var editApiId by remember { mutableStateOf(apiId) }
    var editApiHash by remember { mutableStateOf(apiHash) }

    AlertDialog(
      onDismissRequest = { showApiDialog = false },
      title = { Text("Telegram MTProto API Credentials") },
      text = {
        Column {
          Text("Obtain your personal api_id and api_hash from my.telegram.org to use custom client credentials.")
          Spacer(modifier = Modifier.height(12.dp))
          OutlinedTextField(
            value = editApiId,
            onValueChange = { editApiId = it },
            label = { Text("API ID") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(
            value = editApiHash,
            onValueChange = { editApiHash = it },
            label = { Text("API Hash") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            viewModel.updateApiCredentials(editApiId.trim(), editApiHash.trim())
            showApiDialog = false
          }
        ) {
          Text("Save")
        }
      },
      dismissButton = {
        TextButton(onClick = { showApiDialog = false }) { Text("Cancel") }
      }
    )
  }

  // Logout Confirmation Dialog
  if (showLogoutConfirm) {
    var purgeLocalDatabase by remember { mutableStateOf(false) }

    AlertDialog(
      onDismissRequest = { showLogoutConfirm = false },
      title = { Text("Log Out from TGDrive?") },
      text = {
        Column {
          Text("Your active MTProto session will be closed. Files in your Telegram Saved Messages will remain safe and intact on Telegram servers.")
          Spacer(modifier = Modifier.height(12.dp))
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
          ) {
            Checkbox(
              checked = purgeLocalDatabase,
              onCheckedChange = { purgeLocalDatabase = it }
            )
            Text(
              text = "Purge local TDLib cache directory (recommended if experiencing auth/connection errors)",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            showLogoutConfirm = false
            viewModel.logout(purgeDatabase = purgeLocalDatabase)
            onLogout()
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
          Text("Log Out")
        }
      },
      dismissButton = {
        TextButton(onClick = { showLogoutConfirm = false }) { Text("Cancel") }
      }
    )
  }

  // Diagnostics Dialog
  if (showDiagnosticsDialog) {
    val clipboardManager = LocalClipboardManager.current
    var logs by remember { mutableStateOf(TelegramClient.getDiagnosticLogs()) }

    AlertDialog(
      onDismissRequest = { showDiagnosticsDialog = false },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.BugReport, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
          Spacer(modifier = Modifier.width(8.dp))
          Text("TGDriveAuth Diagnostics")
        }
      },
      text = {
        Column {
          Text(
            text = "Latest TDLib events & connection log (${logs.size} lines):",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
          )
          Spacer(modifier = Modifier.height(8.dp))
          Card(
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            modifier = Modifier
              .fillMaxWidth()
              .height(280.dp)
          ) {
            val logScrollState = rememberScrollState()
            Column(
              modifier = Modifier
                .fillMaxSize()
                .verticalScroll(logScrollState)
                .padding(8.dp)
            ) {
              if (logs.isEmpty()) {
                Text(
                  text = "No diagnostic events recorded yet.",
                  style = MaterialTheme.typography.bodySmall,
                  fontFamily = FontFamily.Monospace
                )
              } else {
                logs.forEach { line ->
                  Text(
                    text = line,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace
                  )
                  Spacer(modifier = Modifier.height(2.dp))
                }
              }
            }
          }
          Spacer(modifier = Modifier.height(8.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            TextButton(onClick = {
              TelegramClient.clearDiagnosticLogs()
              logs = emptyList()
            }) {
              Text("Clear Logs")
            }
            Button(onClick = {
              val allText = logs.joinToString("\n")
              clipboardManager.setText(AnnotatedString(allText))
              viewModel.showMessage("Copied ${logs.size} diagnostic lines to clipboard")
            }) {
              Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Copy All")
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showDiagnosticsDialog = false }) { Text("Close") }
      }
    )
  }
}
