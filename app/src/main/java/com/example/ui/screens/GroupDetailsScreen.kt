package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.Screen
import com.example.ui.AttendanceUiState
import com.example.ui.AttendanceViewModel

@Composable
fun GroupDetailsScreen(
  state: AttendanceUiState,
  viewModel: AttendanceViewModel,
  modifier: Modifier = Modifier
) {
  val group = state.groups.find { it.name == state.selectedGroup }
  var confirmDelete by remember { mutableStateOf(false) }

  if (group == null) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      Text("Group not found", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    return
  }

  val sessionCount = state.historySessions.count { it.groupId == group.id }

  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    Card(modifier = Modifier.fillMaxWidth()) {
      Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(group.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("👨‍🏫 Teacher: ${if (group.teacherName.isBlank()) "Not specified" else group.teacherName}")
        Text("📚 Subject: ${if (group.subjectName.isBlank()) "Not specified" else group.subjectName}")
        Text("👥 Students: ${group.studentCount}", style = MaterialTheme.typography.titleMedium)
        Text(
          "🗂 Saved sessions: $sessionCount",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }

    Button(
      onClick = {
        viewModel.selectAttendanceGroup(group.name)
        viewModel.navigateTo(Screen.ATTENDANCE)
      },
      modifier = Modifier.fillMaxWidth()
    ) {
      Icon(Icons.Default.PlayArrow, contentDescription = null)
      Spacer(modifier = Modifier.width(8.dp))
      Text("Take Attendance")
    }

    Button(
      onClick = {
        viewModel.setStudentGroupFilter(group.name)
        viewModel.navigateTo(Screen.STUDENTS)
      },
      modifier = Modifier.fillMaxWidth(),
      colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
    ) {
      Icon(Icons.Default.Group, contentDescription = null)
      Spacer(modifier = Modifier.width(8.dp))
      Text("👥 Manage Students")
    }

    OutlinedButton(
      onClick = {
        viewModel.setTargetGroupForImport(group.id)
        viewModel.navigateTo(Screen.IMPORT_STUDENTS)
      },
      modifier = Modifier.fillMaxWidth()
    ) {
      Icon(Icons.Default.Download, contentDescription = null)
      Spacer(modifier = Modifier.width(8.dp))
      Text("📥 Import Students from Telegram")
    }

    OutlinedButton(
      onClick = { confirmDelete = true },
      modifier = Modifier.fillMaxWidth(),
      colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
    ) {
      Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(20.dp))
      Spacer(modifier = Modifier.width(8.dp))
      Text("🗑 Delete Group")
    }
  }

  if (confirmDelete) {
    AlertDialog(
      onDismissRequest = { confirmDelete = false },
      title = { Text("Delete Group?", fontWeight = FontWeight.Bold) },
      text = {
        Text("Delete '${group.name}'? Its ${group.studentCount} students and $sessionCount saved sessions will be permanently removed. This cannot be undone.")
      },
      confirmButton = {
        Button(
          onClick = {
            confirmDelete = false
            viewModel.deleteGroup(group.id)
            viewModel.onBackPress()
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
          Text("Delete")
        }
      },
      dismissButton = {
        TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
      }
    )
  }
}
