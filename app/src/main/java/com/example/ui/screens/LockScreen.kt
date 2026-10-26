package com.example.ui.screens

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Shown while App Lock is on. Uses the phone's own screen lock (PIN / pattern / password /
 * fingerprint / face) through the system "confirm credential" screen - no extra library needed.
 */
@Composable
fun LockScreen(onUnlock: () -> Unit) {
  val context = LocalContext.current
  val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
    if (result.resultCode == Activity.RESULT_OK) onUnlock()
  }

  val authenticate: () -> Unit = {
    val keyguard = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
    val intent = keyguard.createConfirmDeviceCredentialIntent(
      "Attendance is locked",
      "Confirm your screen lock to continue"
    )
    // No screen lock on the phone any more -> there is nothing to verify against.
    if (intent == null) onUnlock() else launcher.launch(intent)
  }

  LaunchedEffect(Unit) { authenticate() }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(32.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Icon(
      imageVector = Icons.Default.Lock,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.primary,
      modifier = Modifier.size(56.dp)
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text(
      text = "Attendance is locked",
      style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
      color = MaterialTheme.colorScheme.onBackground
    )
    Spacer(modifier = Modifier.height(24.dp))
    Button(onClick = authenticate) { Text("Unlock") }
  }
}
