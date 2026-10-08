package com.example.ui.onboarding

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.TelegramUser
import com.example.telegram.TelegramAuthState
import com.example.viewmodel.DriveViewModel
import kotlinx.coroutines.delay

@Composable
fun OnboardingScreen(
  viewModel: DriveViewModel,
  onComplete: () -> Unit,
  modifier: Modifier = Modifier
) {
  val authState by viewModel.authState.collectAsState()
  val floodWaitSeconds by viewModel.floodWaitSeconds.collectAsState()
  val apiId by viewModel.apiId.collectAsState()
  val apiHash by viewModel.apiHash.collectAsState()

  // 1: Info, 2: Setup API credentials (mandatory if empty), 3: Auth Phone/Code, 4: Confirmation
  var currentStep by remember { mutableIntStateOf(1) }

  var phoneNumber by remember { mutableStateOf("") }
  var authCode by remember { mutableStateOf("") }
  var password2Fa by remember { mutableStateOf("") }
  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  // Sync step with authState
  LaunchedEffect(authState) {
    when (authState) {
      is TelegramAuthState.WaitPhoneNumber -> {
        // stay in step 3 if already there
      }
      is TelegramAuthState.WaitCode -> {
        currentStep = 3
        isLoading = false
      }
      is TelegramAuthState.WaitPassword -> {
        currentStep = 3
        isLoading = false
      }
      is TelegramAuthState.Ready -> {
        currentStep = 4
        isLoading = false
      }
      is TelegramAuthState.Error -> {
        isLoading = false
        errorMessage = (authState as TelegramAuthState.Error).message
      }
      else -> {}
    }
  }

  Surface(
    modifier = modifier.fillMaxSize(),
    color = MaterialTheme.colorScheme.background
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 24.dp, vertical = 28.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      // Top Step Indicator
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 24.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
      ) {
        StepDot(step = 1, currentStep = currentStep, title = "Welcome")
        StepLine(isActive = currentStep > 1)
        StepDot(step = 2, currentStep = currentStep, title = "API Keys")
        StepLine(isActive = currentStep > 2)
        StepDot(step = 3, currentStep = currentStep, title = "Sign In")
        StepLine(isActive = currentStep > 3)
        StepDot(step = 4, currentStep = currentStep, title = "Ready")
      }

      AnimatedContent(
        targetState = currentStep,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "onboarding_steps"
      ) { step ->
        when (step) {
          1 -> StepOneWelcome(
            onNext = {
              if (apiId.isBlank() || apiHash.isBlank()) {
                currentStep = 2
              } else {
                currentStep = 3
              }
            }
          )

          2 -> StepApiSetup(
            initialApiId = apiId,
            initialApiHash = apiHash,
            onSaveAndContinue = { newId, newHash ->
              viewModel.updateApiCredentials(newId, newHash)
              errorMessage = null
              currentStep = 3
            },
            onBack = { currentStep = 1 }
          )

          3 -> StepThreeAuth(
            authState = authState,
            apiId = apiId,
            apiHash = apiHash,
            phoneNumber = phoneNumber,
            onPhoneChange = { phoneNumber = it },
            authCode = authCode,
            onCodeChange = { authCode = it },
            password2Fa = password2Fa,
            onPasswordChange = { password2Fa = it },
            isLoading = isLoading,
            errorMessage = errorMessage,
            floodWaitSeconds = floodWaitSeconds,
            onChangeApiKeys = { currentStep = 2 },
            onSendPhone = {
              if (apiId.isBlank() || apiHash.isBlank()) {
                errorMessage = "API ID and API Hash are required from my.telegram.org. Please set them first."
                currentStep = 2
                return@StepThreeAuth
              }
              isLoading = true
              errorMessage = null
              viewModel.loginWithPhone(phoneNumber) { err ->
                isLoading = false
                errorMessage = err
              }
            },
            onResendCode = {
              isLoading = true
              errorMessage = null
              viewModel.resendAuthenticationCode { err ->
                isLoading = false
                errorMessage = err
              }
            },
            onVerifyCode = {
              isLoading = true
              errorMessage = null
              viewModel.verifyCode(authCode) { err ->
                isLoading = false
                errorMessage = err
              }
            },
            onVerifyPassword = {
              isLoading = true
              errorMessage = null
              viewModel.verifyPassword(password2Fa) { err ->
                isLoading = false
                errorMessage = err
              }
            },
            onBackToPhone = {
              viewModel.logout(purgeDatabase = false)
            }
          )

          4 -> StepFourConfirmed(
            user = (authState as? TelegramAuthState.Ready)?.user,
            onStart = onComplete
          )
        }
      }
    }
  }
}

@Composable
private fun StepOneWelcome(onNext: () -> Unit) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier.fillMaxWidth()
  ) {
    Card(
      shape = RoundedCornerShape(24.dp),
      elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
      modifier = Modifier
        .fillMaxWidth()
        .height(200.dp)
    ) {
      Image(
        painter = painterResource(id = R.drawable.img_onboarding_hero_1791412250819),
        contentDescription = "TGDrive Cloud Hero",
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize()
      )
    }

    Spacer(modifier = Modifier.height(24.dp))

    Text(
      text = "Unlimited Cloud Storage with Telegram",
      style = MaterialTheme.typography.headlineSmall,
      fontWeight = FontWeight.Bold,
      textAlign = TextAlign.Center,
      color = MaterialTheme.colorScheme.onBackground
    )

    Spacer(modifier = Modifier.height(12.dp))

    Text(
      text = "TGDrive uses the official Telegram MTProto protocol to connect directly with your Telegram account, turning your \"Saved Messages\" chat into a secure personal cloud drive with 2GB–4GB per file.",
      style = MaterialTheme.typography.bodyMedium,
      textAlign = TextAlign.Center,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(24.dp))

    FeatureItem(
      icon = Icons.Default.AllInclusive,
      title = "Unlimited Storage Capacity",
      description = "Store unlimited documents, photos, and videos with zero subscription fees."
    )
    FeatureItem(
      icon = Icons.Default.Security,
      title = "Zero-Server Direct Connection",
      description = "Connects directly from your device to Telegram MTProto datacenters using your own API credentials."
    )
    FeatureItem(
      icon = Icons.Default.Key,
      title = "Private API Credentials",
      description = "Use your own api_id & api_hash from my.telegram.org for high security and unthrottled access."
    )

    Spacer(modifier = Modifier.height(28.dp))

    Button(
      onClick = onNext,
      modifier = Modifier
        .fillMaxWidth()
        .height(52.dp)
        .testTag("onboarding_get_started_button"),
      shape = RoundedCornerShape(14.dp)
    ) {
      Text("Get Started", style = MaterialTheme.typography.titleMedium)
      Spacer(modifier = Modifier.width(8.dp))
      Icon(Icons.Default.ArrowForward, contentDescription = null)
    }
  }
}

@Composable
private fun StepApiSetup(
  initialApiId: String,
  initialApiHash: String,
  onSaveAndContinue: (String, String) -> Unit,
  onBack: () -> Unit
) {
  val context = LocalContext.current
  var inputApiId by remember { mutableStateOf(initialApiId) }
  var inputApiHash by remember { mutableStateOf(initialApiHash) }
  var validationError by remember { mutableStateOf<String?>(null) }

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier.fillMaxWidth()
  ) {
    Box(
      modifier = Modifier
        .size(68.dp)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.primaryContainer),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        Icons.Default.VpnKey,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(34.dp)
      )
    }

    Spacer(modifier = Modifier.height(16.dp))

    Text(
      text = "Telegram API Configuration",
      style = MaterialTheme.typography.headlineSmall,
      fontWeight = FontWeight.Bold,
      textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
      text = "To guarantee privacy and prevent banned shared keys, enter your personal API credentials from Telegram.",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(16.dp))

    // Helper banner linking to my.telegram.org
    Card(
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
      ),
      modifier = Modifier
        .fillMaxWidth()
        .clickable {
          val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://my.telegram.org/auth?to=apps"))
          context.startActivity(intent)
        }
    ) {
      Row(
        modifier = Modifier.padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(Icons.Default.OpenInNew, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Obtain from my.telegram.org/apps",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
          )
          Text(
            text = "Tap to open Telegram developer portal in browser to create your App API ID & Hash.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    validationError?.let { err ->
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 12.dp)
      ) {
        Row(
          modifier = Modifier.padding(10.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
          Spacer(modifier = Modifier.width(8.dp))
          Text(err, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
        }
      }
    }

    OutlinedTextField(
      value = inputApiId,
      onValueChange = { inputApiId = it; validationError = null },
      label = { Text("API ID (Numeric)") },
      placeholder = { Text("e.g. 12345678") },
      singleLine = true,
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
      modifier = Modifier
        .fillMaxWidth()
        .testTag("api_id_input"),
      shape = RoundedCornerShape(14.dp)
    )

    Spacer(modifier = Modifier.height(12.dp))

    OutlinedTextField(
      value = inputApiHash,
      onValueChange = { inputApiHash = it; validationError = null },
      label = { Text("API Hash (32 characters)") },
      placeholder = { Text("e.g. 1a2b3c4d5e6f...") },
      singleLine = true,
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Done),
      modifier = Modifier
        .fillMaxWidth()
        .testTag("api_hash_input"),
      shape = RoundedCornerShape(14.dp)
    )

    Spacer(modifier = Modifier.height(24.dp))

    Button(
      onClick = {
        val cleanId = inputApiId.trim()
        val cleanHash = inputApiHash.trim()
        val idVal = cleanId.toIntOrNull()
        if (idVal == null || idVal <= 0) {
          validationError = "Please enter a valid positive numeric API ID."
          return@Button
        }
        if (cleanHash.length < 10) {
          validationError = "Please enter a valid API Hash."
          return@Button
        }
        onSaveAndContinue(cleanId, cleanHash)
      },
      modifier = Modifier
        .fillMaxWidth()
        .height(50.dp)
        .testTag("save_api_keys_button"),
      shape = RoundedCornerShape(14.dp)
    ) {
      Text("Save Credentials & Continue")
    }

    Spacer(modifier = Modifier.height(8.dp))

    TextButton(onClick = onBack) {
      Text("Back")
    }
  }
}

@Composable
private fun StepThreeAuth(
  authState: TelegramAuthState,
  apiId: String,
  apiHash: String,
  phoneNumber: String,
  onPhoneChange: (String) -> Unit,
  authCode: String,
  onCodeChange: (String) -> Unit,
  password2Fa: String,
  onPasswordChange: (String) -> Unit,
  isLoading: Boolean,
  errorMessage: String?,
  floodWaitSeconds: Int,
  onChangeApiKeys: () -> Unit,
  onSendPhone: () -> Unit,
  onResendCode: () -> Unit,
  onVerifyCode: () -> Unit,
  onVerifyPassword: () -> Unit,
  onBackToPhone: () -> Unit
) {
  var remainingSeconds by remember(floodWaitSeconds) { mutableIntStateOf(floodWaitSeconds) }

  LaunchedEffect(floodWaitSeconds) {
    remainingSeconds = floodWaitSeconds
    while (remainingSeconds > 0) {
      delay(1000)
      remainingSeconds--
    }
  }

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier.fillMaxWidth()
  ) {
    Box(
      modifier = Modifier
        .size(68.dp)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.primaryContainer),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        Icons.Default.Send,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(34.dp)
      )
    }

    Spacer(modifier = Modifier.height(14.dp))

    Text(
      text = "Sign in with Telegram",
      style = MaterialTheme.typography.headlineSmall,
      fontWeight = FontWeight.Bold,
      textAlign = TextAlign.Center
    )

    Text(
      text = "Authenticate via MTProto to access your Saved Messages",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center
    )

    // Current API Key in use banner
    Card(
      shape = RoundedCornerShape(10.dp),
      colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
      ),
      modifier = Modifier
        .fillMaxWidth()
        .padding(top = 10.dp, bottom = 14.dp)
        .clickable { onChangeApiKeys() }
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Column {
          Text(
            text = "Using API ID: ${if (apiId.isBlank()) "None (Tap to set)" else apiId}",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
          )
          Text(
            text = "Tap to change API credentials",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline
          )
        }
        Icon(Icons.Default.Edit, contentDescription = "Edit API Credentials", modifier = Modifier.size(16.dp))
      }
    }

    errorMessage?.let { error ->
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 16.dp)
      ) {
        Row(
          modifier = Modifier.padding(12.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
          Spacer(modifier = Modifier.width(8.dp))
          Text(text = error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
        }
      }
    }

    if (remainingSeconds > 0) {
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 16.dp)
      ) {
        Row(
          modifier = Modifier.padding(12.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(Icons.Default.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Telegram Rate Limit (FLOOD_WAIT): Please wait $remainingSeconds seconds before retrying.",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onTertiaryContainer
          )
        }
      }
    }

    when (authState) {
      is TelegramAuthState.WaitCode -> {
        // Clear guidance notice explaining where the code was delivered
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
          ),
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Code Sent via: ${authState.deliveryType}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
              )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "IMPORTANT: Check your official Telegram app on your phone, PC, or tablet! Telegram delivers login codes inside the Telegram app (Service Notifications chat) and only falls back to SMS if you have no active sessions.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onPrimaryContainer
            )
          }
        }

        Text(
          text = "Confirmation code for ${authState.phoneNumber}",
          style = MaterialTheme.typography.bodyMedium,
          textAlign = TextAlign.Center,
          color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
          value = authCode,
          onValueChange = onCodeChange,
          label = { Text("5-Digit Verification Code") },
          placeholder = { Text("12345") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
          keyboardActions = KeyboardActions(onDone = { onVerifyCode() }),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("auth_code_input"),
          shape = RoundedCornerShape(14.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
          onClick = onVerifyCode,
          enabled = !isLoading && authCode.isNotBlank() && remainingSeconds == 0,
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("confirm_code_button"),
          shape = RoundedCornerShape(14.dp)
        ) {
          if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
          } else {
            Text("Verify Code")
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          TextButton(
            onClick = onResendCode,
            enabled = !isLoading && remainingSeconds == 0,
            modifier = Modifier.testTag("resend_code_button")
          ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Resend Code")
          }

          TextButton(
            onClick = onBackToPhone,
            modifier = Modifier.testTag("change_phone_button")
          ) {
            Text("Change Phone")
          }
        }
      }

      is TelegramAuthState.WaitPassword -> {
        Text(
          text = "Two-Step Verification (2FA)",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = "Enter your Telegram Cloud Password to complete sign in.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
          value = password2Fa,
          onValueChange = onPasswordChange,
          label = { Text("2FA Cloud Password") },
          visualTransformation = PasswordVisualTransformation(),
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
          keyboardActions = KeyboardActions(onDone = { onVerifyPassword() }),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("password_2fa_input"),
          shape = RoundedCornerShape(14.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
          onClick = onVerifyPassword,
          enabled = !isLoading && password2Fa.isNotBlank() && remainingSeconds == 0,
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("confirm_2fa_button"),
          shape = RoundedCornerShape(14.dp)
        ) {
          if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
          } else {
            Text("Unlock Account")
          }
        }
      }

      else -> {
        // Phone number input
        OutlinedTextField(
          value = phoneNumber,
          onValueChange = onPhoneChange,
          label = { Text("Phone Number (with + and country code)") },
          placeholder = { Text("+12025550198 or +212646489592") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
          keyboardActions = KeyboardActions(onNext = { onSendPhone() }),
          leadingIcon = {
            Icon(Icons.Default.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
          },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("phone_number_input"),
          shape = RoundedCornerShape(14.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          text = "Required format: + followed by country code and phone number without spaces. E.g.: +212646489592",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.outline
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
          onClick = onSendPhone,
          enabled = !isLoading && phoneNumber.length >= 8 && remainingSeconds == 0,
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("send_phone_button"),
          shape = RoundedCornerShape(14.dp)
        ) {
          if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
          } else {
            Text(if (remainingSeconds > 0) "Wait $remainingSeconds s" else "Request Login Code via Telegram")
          }
        }
      }
    }
  }
}

@Composable
private fun StepFourConfirmed(
  user: TelegramUser?,
  onStart: () -> Unit
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier.fillMaxWidth()
  ) {
    Box(
      modifier = Modifier
        .size(80.dp)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.primary),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        Icons.Default.Check,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onPrimary,
        modifier = Modifier.size(44.dp)
      )
    }

    Spacer(modifier = Modifier.height(20.dp))

    Text(
      text = "Saved Messages Connected!",
      style = MaterialTheme.typography.headlineSmall,
      fontWeight = FontWeight.Bold,
      textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
      text = "Your Telegram cloud vault is verified and ready for unlimited storage.",
      style = MaterialTheme.typography.bodyMedium,
      textAlign = TextAlign.Center,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(24.dp))

    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
      ),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth()
        ) {
          Box(
            modifier = Modifier
              .size(48.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = user?.firstName?.take(1) ?: "T",
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary
            )
          }

          Spacer(modifier = Modifier.width(12.dp))

          Column {
            Text(
              text = user?.fullName ?: "Telegram User",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "@${user?.username ?: "tg_user"}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.outline
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(28.dp))

    Button(
      onClick = onStart,
      modifier = Modifier
        .fillMaxWidth()
        .height(52.dp)
        .testTag("onboarding_finish_button"),
      shape = RoundedCornerShape(14.dp)
    ) {
      Text("Open TGDrive Cloud", style = MaterialTheme.typography.titleMedium)
    }
  }
}

@Composable
private fun StepDot(step: Int, currentStep: Int, title: String) {
  val isActive = currentStep >= step
  val isCurrent = currentStep == step

  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Box(
      modifier = Modifier
        .size(if (isCurrent) 32.dp else 26.dp)
        .clip(CircleShape)
        .background(
          if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
        ),
      contentAlignment = Alignment.Center
    ) {
      if (currentStep > step) {
        Icon(
          Icons.Default.Check,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onPrimary,
          modifier = Modifier.size(16.dp)
        )
      } else {
        Text(
          text = "$step",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
          color = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.outline
        )
      }
    }
    Spacer(modifier = Modifier.height(4.dp))
    Text(
      text = title,
      style = MaterialTheme.typography.labelSmall,
      color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    )
  }
}

@Composable
private fun StepLine(isActive: Boolean) {
  Box(
    modifier = Modifier
      .width(28.dp)
      .height(2.dp)
      .padding(horizontal = 4.dp)
      .background(
        if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
      )
  )
}

@Composable
private fun FeatureItem(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  title: String,
  description: String
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically
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
      Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
  }
}
