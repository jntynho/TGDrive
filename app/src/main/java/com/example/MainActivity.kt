package com.example

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.DriveFile
import com.example.localization.StringResources
import com.example.telegram.TelegramAuthState
import com.example.ui.components.*
import com.example.ui.onboarding.OnboardingScreen
import com.example.ui.screens.*
import com.example.ui.theme.TGDriveTheme
import com.example.viewmodel.DriveViewModel

enum class MainTab {
  HOME, FILES, MEDIA, QUEUE, SETTINGS, ABOUT
}

class MainActivity : ComponentActivity() {

  private val viewModel: DriveViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    setContent {
      val themeMode by viewModel.themeMode.collectAsState()
      val strings = StringResources.get()
      val layoutDirection = StringResources.getLayoutDirection()

      val authState by viewModel.authState.collectAsState()
      val userMessage by viewModel.userMessage.collectAsState()
      val activeTransfers by viewModel.activeTransfers.collectAsState()
      val folders by viewModel.allFolders.collectAsState()
      val trashFiles by viewModel.trashFiles.collectAsState()

      // Biometric lock state
      val biometricEnabled by viewModel.biometricLockEnabled.collectAsState()
      val appPin by viewModel.appPinCode.collectAsState()
      var isUnlocked by remember { mutableStateOf(!biometricEnabled) }

      val snackbarHostState = remember { SnackbarHostState() }
      var currentTab by remember { mutableStateOf(MainTab.HOME) }
      var showOnboarding by remember {
        mutableStateOf(!viewModel.sessionManager.isLoggedIn)
      }

      // Dialog states
      var previewFile by remember { mutableStateOf<DriveFile?>(null) }
      var moveFileTarget by remember { mutableStateOf<DriveFile?>(null) }
      var showNewFolderDialog by remember { mutableStateOf(false) }
      var showTrashDialog by remember { mutableStateOf(false) }

      // System File Picker for manual uploads (supports all files: docs, audio, video, archives, etc.)
      val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
      ) { uri: Uri? ->
        uri?.let { fileUri ->
          val (name, size) = queryFileInfo(this, fileUri)
          val mime = contentResolver.getType(fileUri) ?: "application/octet-stream"
          viewModel.uploadFile(
            uri = fileUri,
            name = name,
            size = size,
            mimeType = mime
          )
        }
      }

      // Sync auth state with onboarding
      LaunchedEffect(authState) {
        if (authState is TelegramAuthState.Ready) {
          showOnboarding = false
        }
      }

      // Handle user feedback messages
      LaunchedEffect(userMessage) {
        userMessage?.let { msg ->
          snackbarHostState.showSnackbar(msg)
          viewModel.clearUserMessage()
        }
      }

      TGDriveTheme(themeMode = themeMode) {
        CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
          Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
          ) {
            if (biometricEnabled && !isUnlocked) {
              BiometricLockScreen(
                correctPin = appPin,
                onUnlocked = { isUnlocked = true }
              )
            } else if (showOnboarding) {
              OnboardingScreen(
                viewModel = viewModel,
                onComplete = { showOnboarding = false },
                modifier = Modifier.systemBarsPadding()
              )
            } else {
              // Main Application Shell
              Scaffold(
                contentWindowInsets = WindowInsets.systemBars,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                  // Persistent transfer progress notification bar
                  UploadProgressBar(
                    transfers = activeTransfers,
                    onPause = { viewModel.telegramClient.pauseTransfer(it) },
                    onResume = { viewModel.telegramClient.resumeTransfer(it) },
                    onCancel = { viewModel.telegramClient.cancelTransfer(it) }
                  )
                },
                bottomBar = {
                  if (currentTab != MainTab.ABOUT) {
                    NavigationBar(
                      tonalElevation = 8.dp,
                      modifier = Modifier.testTag("bottom_nav_bar")
                    ) {
                      NavigationBarItem(
                        selected = currentTab == MainTab.HOME,
                        onClick = { currentTab = MainTab.HOME },
                        icon = { Icon(Icons.Default.Home, contentDescription = strings.homeTab) },
                        label = { Text(strings.homeTab) },
                        modifier = Modifier.testTag("nav_tab_home")
                      )
                      NavigationBarItem(
                        selected = currentTab == MainTab.FILES,
                        onClick = { currentTab = MainTab.FILES },
                        icon = { Icon(Icons.Default.Folder, contentDescription = strings.filesTab) },
                        label = { Text(strings.filesTab) },
                        modifier = Modifier.testTag("nav_tab_files")
                      )
                      NavigationBarItem(
                        selected = currentTab == MainTab.MEDIA,
                        onClick = { currentTab = MainTab.MEDIA },
                        icon = { Icon(Icons.Default.PermMedia, contentDescription = strings.mediaTab) },
                        label = { Text(strings.mediaTab) },
                        modifier = Modifier.testTag("nav_tab_media")
                      )
                      NavigationBarItem(
                        selected = currentTab == MainTab.QUEUE,
                        onClick = { currentTab = MainTab.QUEUE },
                        icon = {
                          BadgedBox(
                            badge = {
                              if (activeTransfers.isNotEmpty()) {
                                Badge { Text("${activeTransfers.size}") }
                              }
                            }
                          ) {
                            Icon(Icons.Default.CloudSync, contentDescription = strings.queueTab)
                          }
                        },
                        label = { Text(strings.queueTab) },
                        modifier = Modifier.testTag("nav_tab_queue")
                      )
                      NavigationBarItem(
                        selected = currentTab == MainTab.SETTINGS,
                        onClick = { currentTab = MainTab.SETTINGS },
                        icon = { Icon(Icons.Default.Settings, contentDescription = strings.settingsTab) },
                        label = { Text(strings.settingsTab) },
                        modifier = Modifier.testTag("nav_tab_settings")
                      )
                    }
                  }
                }
              ) { innerPadding ->
                Box(
                  modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                ) {
                  when (currentTab) {
                    MainTab.HOME -> HomeScreen(
                      viewModel = viewModel,
                      onFileClick = { previewFile = it },
                      onPickFile = { filePickerLauncher.launch(arrayOf("*/*")) },
                      onOpenNewFolderDialog = { showNewFolderDialog = true },
                      onNavigateToFiles = { folder ->
                        viewModel.setSelectedFolder(folder)
                        currentTab = MainTab.FILES
                      }
                    )

                    MainTab.FILES -> {
                      BackHandler { currentTab = MainTab.HOME }
                      FilesScreen(
                        viewModel = viewModel,
                        onFileClick = { previewFile = it },
                        onPickFile = { filePickerLauncher.launch(arrayOf("*/*")) },
                        onOpenNewFolderDialog = { showNewFolderDialog = true },
                        onMoveFile = { moveFileTarget = it }
                      )
                    }

                    MainTab.MEDIA -> {
                      BackHandler { currentTab = MainTab.HOME }
                      MediaScreen(
                        viewModel = viewModel,
                        onFileClick = { previewFile = it }
                      )
                    }

                    MainTab.QUEUE -> {
                      BackHandler { currentTab = MainTab.HOME }
                      QueueScreen(
                        viewModel = viewModel
                      )
                    }

                    MainTab.SETTINGS -> {
                      BackHandler { currentTab = MainTab.HOME }
                      SettingsScreen(
                        viewModel = viewModel,
                        onOpenTrashDialog = { showTrashDialog = true },
                        onOpenAboutScreen = { currentTab = MainTab.ABOUT },
                        onLogout = { showOnboarding = true }
                      )
                    }

                    MainTab.ABOUT -> {
                      BackHandler { currentTab = MainTab.SETTINGS }
                      AboutScreen(
                        onBack = { currentTab = MainTab.SETTINGS }
                      )
                    }
                  }
                }
              }
            }

            // File Preview Dialog
            previewFile?.let { file ->
              FilePreviewDialog(
                file = file,
                onDismiss = { previewFile = null },
                onDownload = {
                  viewModel.downloadFile(file)
                  previewFile = null
                },
                onMoveToFolder = {
                  moveFileTarget = file
                  previewFile = null
                },
                onDelete = {
                  viewModel.moveToTrash(file)
                  previewFile = null
                },
                onToggleFavorite = {
                  viewModel.toggleFavorite(file)
                }
              )
            }

            // New Folder Dialog
            if (showNewFolderDialog) {
              NewFolderDialog(
                onDismiss = { showNewFolderDialog = false },
                onCreateFolder = { folderName ->
                  viewModel.createVirtualFolder(folderName)
                }
              )
            }

            // Move File Dialog
            moveFileTarget?.let { file ->
              MoveFileDialog(
                file = file,
                folders = folders,
                onDismiss = { moveFileTarget = null },
                onFolderSelected = { newFolder ->
                  viewModel.moveFileToFolder(file.id, newFolder)
                  moveFileTarget = null
                }
              )
            }

            // Trash Dialog
            if (showTrashDialog) {
              TrashDialog(
                trashFiles = trashFiles,
                onDismiss = { showTrashDialog = false },
                onRestore = { viewModel.restoreFromTrash(it) },
                onDeletePermanently = { viewModel.deletePermanently(it) },
                onEmptyTrash = { viewModel.emptyTrash() }
              )
            }
          }
        }
      }
    }
  }

  private fun queryFileInfo(context: Context, uri: Uri): Pair<String, Long> {
    var name = "uploaded_file"
    var size = 0L
    try {
      context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
        if (cursor.moveToFirst()) {
          if (nameIndex != -1) name = cursor.getString(nameIndex) ?: name
          if (sizeIndex != -1) size = cursor.getLong(sizeIndex)
        }
      }
    } catch (_: Exception) {}
    return Pair(name, size)
  }
}
