package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun BiometricLockScreen(
  correctPin: String,
  onUnlocked: () -> Unit,
  modifier: Modifier = Modifier
) {
  var enteredPin by remember { mutableStateOf("") }
  var isError by remember { mutableStateOf(false) }

  fun handleDigit(d: String) {
    if (enteredPin.length < 4) {
      val next = enteredPin + d
      enteredPin = next
      if (next.length == 4) {
        if (next == correctPin || correctPin.isBlank()) {
          onUnlocked()
        } else {
          isError = true
          enteredPin = ""
        }
      }
    }
  }

  Surface(
    modifier = modifier.fillMaxSize(),
    color = MaterialTheme.colorScheme.background
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Box(
        modifier = Modifier
          .size(72.dp)
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          Icons.Default.Lock,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(36.dp)
        )
      }

      Spacer(modifier = Modifier.height(20.dp))

      Text(
        text = "TGDrive Locked",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold
      )

      Text(
        text = if (isError) "Incorrect PIN, try again" else "Enter 4-Digit Security PIN or Use Biometrics",
        style = MaterialTheme.typography.bodyMedium,
        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
      )

      Spacer(modifier = Modifier.height(28.dp))

      // PIN Dots
      Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        repeat(4) { idx ->
          val isFilled = enteredPin.length > idx
          Box(
            modifier = Modifier
              .size(16.dp)
              .clip(CircleShape)
              .background(
                if (isFilled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
              )
          )
        }
      }

      Spacer(modifier = Modifier.height(36.dp))

      // Numeric Keypad
      val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("BIO", "0", "DEL")
      )

      Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        rows.forEach { row ->
          Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            row.forEach { key ->
              when (key) {
                "BIO" -> {
                  IconButton(
                    onClick = { onUnlocked() }, // Biometric simulated success
                    modifier = Modifier.size(68.dp)
                  ) {
                    Icon(
                      Icons.Default.Fingerprint,
                      contentDescription = "Biometric Unlock",
                      tint = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.size(36.dp)
                    )
                  }
                }
                "DEL" -> {
                  IconButton(
                    onClick = {
                      if (enteredPin.isNotEmpty()) enteredPin = enteredPin.dropLast(1)
                      isError = false
                    },
                    modifier = Modifier.size(68.dp)
                  ) {
                    Icon(Icons.Default.Backspace, contentDescription = "Delete")
                  }
                }
                else -> {
                  Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                      .size(68.dp)
                      .clickable {
                        isError = false
                        handleDigit(key)
                      }
                      .testTag("pin_key_$key")
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Text(
                        text = key,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                      )
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}
