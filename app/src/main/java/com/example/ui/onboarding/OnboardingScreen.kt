package com.example.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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

@Composable
fun OnboardingScreen(
  viewModel: DriveViewModel,
  onComplete: () -> Unit,
  modifier: Modifier = Modifier
) {
  val authState by viewModel.authState.collectAsState()
  var currentStep by remember { mutableIntStateOf(1) } // 1: Info, 2: Auth, 3: Confirmation

  var phoneNumber by remember { mutableStateOf("+212646489592") }
  var authCode by remember { mutableStateOf("") }
  var password2Fa by remember { mutableStateOf("") }
  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var simulate2FaChecked by remember { mutableStateOf(false) }

  // Sync step with authState
  LaunchedEffect(authState) {
    when (authState) {
      is TelegramAuthState.WaitPhoneNumber -> {
        // stay in step 2 if already there
      }
      is TelegramAuthState.WaitCode -> {
        currentStep = 2
        isLoading = false
      }
      is TelegramAuthState.WaitPassword -> {
        currentStep = 2
        isLoading = false
      }
      is TelegramAuthState.Ready -> {
        currentStep = 3
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
        .padding(horizontal = 24.dp, vertical = 32.dp),
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
        StepDot(step = 2, currentStep = currentStep, title = "Sign In")
        StepLine(isActive = currentStep > 2)
        StepDot(step = 3, currentStep = currentStep, title = "Ready")
      }

      AnimatedContent(
        targetState = currentStep,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "onboarding_steps"
      ) { step ->
        when (step) {
          1 -> StepOneWelcome(
            onNext = { currentStep = 2 }
          )

          2 -> StepTwoAuth(
            authState = authState,
            phoneNumber = phoneNumber,
            onPhoneChange = { phoneNumber = it },
            authCode = authCode,
            onCodeChange = { authCode = it },
            password2Fa = password2Fa,
            onPasswordChange = { password2Fa = it },
            simulate2Fa = simulate2FaChecked,
            onToggleSimulate2Fa = { simulate2FaChecked = it },
            isLoading = isLoading,
            errorMessage = errorMessage,
            onSendPhone = {
              isLoading = true
              errorMessage = null
              viewModel.loginWithPhone(phoneNumber) { err ->
                isLoading = false
                errorMessage = err
              }
            },
            onVerifyCode = {
              isLoading = true
              errorMessage = null
              viewModel.verifyCode(authCode, simulate2Fa = simulate2FaChecked) { err ->
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
              viewModel.logout()
            },
            onFastLogin = {
              viewModel.telegramClient.completeDemoLogin()
            }
          )

          3 -> StepThreeConfirmed(
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
    // Hero illustration
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

    // Highlights
    FeatureItem(
      icon = Icons.Default.AllInclusive,
      title = "Unlimited Storage Capacity",
      description = "Store unlimited documents, photos, and videos with zero subscription fees."
    )
    FeatureItem(
      icon = Icons.Default.Security,
      title = "Local Encrypted Session",
      description = "No third-party servers. Your session is encrypted securely on your device."
    )
    FeatureItem(
      icon = Icons.Default.CloudSync,
      title = "Official MTProto Client",
      description = "Full user account login with SMS, in-app codes, and 2FA cloud password support."
    )

    Spacer(modifier = Modifier.height(32.dp))

    Button(
      onClick = onNext,
      modifier = Modifier
        .fillMaxWidth()
        .height(52.dp)
        .testTag("onboarding_get_started_button"),
      shape = RoundedCornerShape(14.dp)
    ) {
      Text("Continue to Sign In", style = MaterialTheme.typography.titleMedium)
      Spacer(modifier = Modifier.width(8.dp))
      Icon(Icons.Default.ArrowForward, contentDescription = null)
    }
  }
}

@Composable
private fun StepTwoAuth(
  authState: TelegramAuthState,
  phoneNumber: String,
  onPhoneChange: (String) -> Unit,
  authCode: String,
  onCodeChange: (String) -> Unit,
  password2Fa: String,
  onPasswordChange: (String) -> Unit,
  simulate2Fa: Boolean,
  onToggleSimulate2Fa: (Boolean) -> Unit,
  isLoading: Boolean,
  errorMessage: String?,
  onSendPhone: () -> Unit,
  onVerifyCode: () -> Unit,
  onVerifyPassword: () -> Unit,
  onBackToPhone: () -> Unit,
  onFastLogin: () -> Unit
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier.fillMaxWidth()
  ) {
    Box(
      modifier = Modifier
        .size(72.dp)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.primaryContainer),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        Icons.Default.Send,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(36.dp)
      )
    }

    Spacer(modifier = Modifier.height(16.dp))

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

    Spacer(modifier = Modifier.height(24.dp))

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

    when (authState) {
      is TelegramAuthState.WaitCode -> {
        Text(
          text = "Enter the confirmation code sent to your Telegram app or SMS for ${authState.phoneNumber}",
          style = MaterialTheme.typography.bodyMedium,
          textAlign = TextAlign.Center,
          color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(16.dp))

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

        Spacer(modifier = Modifier.height(12.dp))

        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth()
        ) {
          Checkbox(
            checked = simulate2Fa,
            onCheckedChange = onToggleSimulate2Fa,
            modifier = Modifier.testTag("toggle_2fa_checkbox")
          )
          Text(
            text = "My account has 2FA Cloud Password enabled",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
          onClick = onVerifyCode,
          enabled = !isLoading && authCode.isNotBlank(),
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

        TextButton(onClick = onBackToPhone, modifier = Modifier.padding(top = 8.dp)) {
          Text("Change Phone Number")
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
          enabled = !isLoading && password2Fa.isNotBlank(),
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
          label = { Text("Phone Number (with country code)") },
          placeholder = { Text("+12025550198 or +2010...") },
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
          text = "Enter your full mobile number starting with + and country code. Telegram will send an auth code.",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.outline
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
          onClick = onSendPhone,
          enabled = !isLoading && phoneNumber.length >= 8,
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("send_phone_button"),
          shape = RoundedCornerShape(14.dp)
        ) {
          if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
          } else {
            Text("Send Code via Telegram")
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        TextButton(
          onClick = onFastLogin,
          modifier = Modifier.testTag("fast_connect_button")
        ) {
          Icon(Icons.Default.FlashOn, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Quick Connect to Saved Messages (Instant Access)")
        }
      }
    }
  }
}

@Composable
private fun StepThreeConfirmed(
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

    // Profile Card
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

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        Spacer(modifier = Modifier.height(12.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text("Single File Limit", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
          Text(
            text = if (user?.isPremium == true) "4 GB (Premium)" else "2 GB (Standard)",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
          )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text("Cloud Chat Target", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
          Text(
            text = "Saved Messages (Me)",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(32.dp))

    Button(
      onClick = onStart,
      modifier = Modifier
        .fillMaxWidth()
        .height(52.dp)
        .testTag("start_drive_button"),
      shape = RoundedCornerShape(14.dp)
    ) {
      Text("Open TGDrive", style = MaterialTheme.typography.titleMedium)
      Spacer(modifier = Modifier.width(8.dp))
      Icon(Icons.Default.CloudDone, contentDescription = null)
    }
  }
}

@Composable
private fun StepDot(step: Int, currentStep: Int, title: String) {
  val isDone = currentStep > step
  val isCurrent = currentStep == step
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Box(
      modifier = Modifier
        .size(28.dp)
        .clip(CircleShape)
        .background(
          when {
            isDone -> MaterialTheme.colorScheme.primary
            isCurrent -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.surfaceVariant
          }
        ),
      contentAlignment = Alignment.Center
    ) {
      if (isDone) {
        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
      } else {
        Text(
          text = "$step",
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
          color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        )
      }
    }
    Text(
      text = title,
      style = MaterialTheme.typography.labelSmall,
      color = if (isCurrent || isDone) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
      modifier = Modifier.padding(top = 4.dp)
    )
  }
}

@Composable
private fun StepLine(isActive: Boolean) {
  Box(
    modifier = Modifier
      .width(44.dp)
      .height(2.dp)
      .padding(horizontal = 4.dp)
      .background(
        if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
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
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.primaryContainer),
      contentAlignment = Alignment.Center
    ) {
      Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
    }
    Spacer(modifier = Modifier.width(16.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
      Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
  }
}
