package com.example.ui.screens

import android.app.KeyguardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.AttendanceUiState
import com.example.ui.AttendanceViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
  state: AttendanceUiState,
  viewModel: AttendanceViewModel,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var showClearDataDialog by remember { mutableStateOf(false) }
  var reportLink by remember { mutableStateOf(state.reportTelegramLink) }

  // Export: pick where to save, then write the JSON that the ViewModel prepared.
  val createDocumentLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument("application/json")
  ) { uri ->
    val json = viewModel.pendingBackupString
    if (uri != null && json != null) {
      try {
        context.contentResolver.openOutputStream(uri)?.use { out ->
          out.write(json.toByteArray(Charsets.UTF_8))
        }
        val pb = viewModel.pendingBackupPreview
        val msg = if (pb != null) {
          "✅ Backup created\n\nTeachers: ${pb.teachers.size}\nGroups: ${pb.groups.size}\nStudents: ${pb.students.size}\nSessions: ${pb.sessions.size}\nRecords: ${pb.records.size}"
        } else {
          "✅ Backup created"
        }
        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
      } catch (e: Exception) {
        Toast.makeText(context, "Failed to save backup: ${e.message}", Toast.LENGTH_LONG).show()
      }
    }
    viewModel.pendingBackupString = null
    viewModel.pendingBackupPreview = null
  }

  // Import: read the chosen file and show a preview before anything is replaced.
  val openDocumentLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri ->
    if (uri != null) {
      try {
        val jsonString = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        if (jsonString != null) {
          val preview = viewModel.getBackupPreview(jsonString)
          if (preview != null) {
            viewModel.setImportPreviewData(jsonString, preview)
          } else {
            Toast.makeText(context, "❌ Unable to import backup. The file is invalid or incompatible.", Toast.LENGTH_LONG).show()
          }
        }
      } catch (e: Exception) {
        Toast.makeText(context, "Failed to read backup: ${e.message}", Toast.LENGTH_LONG).show()
      }
    }
  }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .testTag("settings_screen"),
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // 1. Appearance
    item {
      SettingsSectionHeader(icon = Icons.Default.Brightness4, title = "Appearance")
      Spacer(modifier = Modifier.height(8.dp))
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text(
            text = "Theme Mode",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
          )
          Spacer(modifier = Modifier.height(8.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            listOf("System", "Light", "Dark").forEach { theme ->
              FilterChip(
                selected = state.selectedTheme == theme,
                onClick = { viewModel.setTheme(theme) },
                label = { Text(theme) },
                modifier = Modifier.weight(1f)
              )
            }
          }
        }
      }
    }

    // 2. Attendance Settings
    item {
      SettingsSectionHeader(icon = Icons.Default.Tune, title = "Attendance Preferences")
      Spacer(modifier = Modifier.height(8.dp))
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
      ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
          SettingsSwitchRow(
            title = "Start with everyone Present",
            subtitle = if (state.defaultAllPresent) {
              "A new roll call starts with all students ✅ Present"
            } else {
              "A new roll call starts with all students ❌ — tick who arrived"
            },
            checked = state.defaultAllPresent,
            onCheckedChange = { viewModel.toggleDefaultAllPresent(it) }
          )

          SettingsSwitchRow(
            title = "Auto-save drafts",
            subtitle = "Remember unsaved marks for today if the app is closed",
            checked = state.autoSaveEnabled,
            onCheckedChange = { viewModel.toggleAutoSave(it) }
          )

          Spacer(modifier = Modifier.height(8.dp))

          OutlinedTextField(
            value = reportLink,
            onValueChange = { reportLink = it },
            label = { Text("Report chat link") },
            placeholder = { Text("https://t.me/your_group") },
            supportingText = { Text("Opened by “Copy & open report chat” when you save attendance") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
          )
          if (reportLink.trim() != state.reportTelegramLink) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
              TextButton(onClick = { reportLink = state.reportTelegramLink }) { Text("Reset") }
              TextButton(onClick = {
                viewModel.updateReportLink(reportLink)
                Toast.makeText(context, "Report link saved", Toast.LENGTH_SHORT).show()
              }) { Text("Save link") }
            }
          }
          Spacer(modifier = Modifier.height(8.dp))
        }
      }
    }

    // 3. Data Management (Backup, Restore, Clear)
    item {
      SettingsSectionHeader(icon = Icons.Default.Backup, title = "Backup & Restore")
      Spacer(modifier = Modifier.height(8.dp))
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
      ) {
        Column {
          SettingsRowAction(
            title = "Export All Data",
            icon = Icons.Default.Backup,
            onClick = {
              scope.launch {
                val pair = viewModel.getBackupDataAndJson()
                if (pair != null) {
                  viewModel.pendingBackupPreview = pair.first
                  viewModel.pendingBackupString = pair.second
                  val stamp = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                  createDocumentLauncher.launch("AttendanceApp_Backup_$stamp.json")
                } else {
                  Toast.makeText(context, "Failed to generate backup.", Toast.LENGTH_SHORT).show()
                }
              }
            }
          )
          SettingsRowAction(
            title = "Import All Data",
            icon = Icons.Default.Restore,
            onClick = { openDocumentLauncher.launch(arrayOf("application/json", "*/*")) }
          )
          SettingsRowAction(
            title = "Clear All Data",
            icon = Icons.Default.DeleteForever,
            iconTint = MaterialTheme.colorScheme.error,
            textColor = MaterialTheme.colorScheme.error,
            onClick = { showClearDataDialog = true }
          )
        }
      }
    }

    // 4. Security (App Lock)
    item {
      SettingsSectionHeader(icon = Icons.Default.Lock, title = "Security")
      Spacer(modifier = Modifier.height(8.dp))
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
      ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
          SettingsSwitchRow(
            title = "App Lock",
            subtitle = "Ask for your phone's PIN, pattern or fingerprint when the app is opened",
            checked = state.appLockEnabled,
            onCheckedChange = { enable ->
              if (enable) {
                val keyguard = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
                if (keyguard.isDeviceSecure) {
                  viewModel.toggleAppLock(true)
                } else {
                  Toast.makeText(
                    context,
                    "Set a screen lock (PIN, pattern or fingerprint) in your phone's settings first.",
                    Toast.LENGTH_LONG
                  ).show()
                }
              } else {
                viewModel.toggleAppLock(false)
              }
            }
          )
        }
      }
    }
  }

  // Clear Data Confirmation Dialog
  if (showClearDataDialog) {
    AlertDialog(
      onDismissRequest = { showClearDataDialog = false },
      icon = {
        Icon(
          imageVector = Icons.Default.DeleteForever,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.error,
          modifier = Modifier.size(32.dp)
        )
      },
      title = { Text("Clear All Data?", fontWeight = FontWeight.Bold) },
      text = {
        Text("This permanently removes all teachers, groups, students and attendance history from this device. Export a backup first if you may need it. Are you sure?")
      },
      confirmButton = {
        Button(
          onClick = {
            viewModel.clearAllData()
            showClearDataDialog = false
            Toast.makeText(context, "All data cleared", Toast.LENGTH_SHORT).show()
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
          Text("Yes, Clear Everything")
        }
      },
      dismissButton = {
        TextButton(onClick = { showClearDataDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }

  // Import Backup Dialog
  val preview = state.pendingImportPreview
  val pendingJson = state.pendingImportJson
  if (preview != null && pendingJson != null) {
    AlertDialog(
      onDismissRequest = { viewModel.setImportPreviewData(null, null) },
      icon = {
        Icon(
          Icons.Default.Restore,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(32.dp)
        )
      },
      title = { Text("📦 Backup Found", fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text("Backup date:\n${preview.date}")
          Spacer(Modifier.height(4.dp))
          Text("Teachers: ${preview.teachers.size}")
          Text("Groups: ${preview.groups.size}")
          Text("Students: ${preview.students.size}")
          Text("Attendance sessions: ${preview.sessions.size}")
          Text("Attendance records: ${preview.records.size}")
          Spacer(Modifier.height(4.dp))
          Text(
            "Warning: importing this backup will replace ALL current data on this device.",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            scope.launch {
              val success = viewModel.restoreFromBackupJson(pendingJson)
              if (success) {
                val msg = "✅ Data restored\n\nTeachers: ${preview.teachers.size}\nGroups: ${preview.groups.size}\nStudents: ${preview.students.size}\nSessions: ${preview.sessions.size}\nRecords: ${preview.records.size}"
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
              } else {
                Toast.makeText(context, "❌ Failed to restore data.", Toast.LENGTH_LONG).show()
              }
              viewModel.setImportPreviewData(null, null)
            }
          }
        ) {
          Text("Import & Restore")
        }
      },
      dismissButton = {
        TextButton(onClick = { viewModel.setImportPreviewData(null, null) }) {
          Text("Cancel")
        }
      }
    )
  }
}

@Composable
private fun SettingsSwitchRow(
  title: String,
  subtitle: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 10.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
    Spacer(modifier = Modifier.width(12.dp))
    Switch(checked = checked, onCheckedChange = onCheckedChange)
  }
}

@Composable
fun SettingsRowAction(
  title: String,
  icon: ImageVector,
  iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
  textColor: Color = MaterialTheme.colorScheme.onSurface,
  onClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() }
      .padding(horizontal = 16.dp, vertical = 14.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = iconTint,
      modifier = Modifier.size(24.dp)
    )
    Spacer(modifier = Modifier.width(16.dp))
    Text(
      text = title,
      style = MaterialTheme.typography.bodyLarge,
      color = textColor,
      modifier = Modifier.weight(1f)
    )
    Icon(
      imageVector = Icons.Default.ChevronRight,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
      modifier = Modifier.size(20.dp)
    )
  }
}

@Composable
private fun SettingsSectionHeader(icon: ImageVector, title: String) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier.padding(horizontal = 4.dp)
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.primary,
      modifier = Modifier.size(18.dp)
    )
    Spacer(modifier = Modifier.width(8.dp))
    Text(
      text = title,
      style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
      color = MaterialTheme.colorScheme.onBackground
    )
  }
}
