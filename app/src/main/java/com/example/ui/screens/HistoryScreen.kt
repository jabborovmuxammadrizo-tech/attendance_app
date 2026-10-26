package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.AttendanceSession
import com.example.model.AttendanceStatus
import com.example.model.StudentAttendanceRecord
import com.example.ui.AttendanceUiState
import com.example.ui.AttendanceViewModel
import com.example.ui.theme.HomeIndigo
import com.example.ui.theme.HomeIndigoContainer
import com.example.ui.theme.LateRed
import com.example.ui.theme.LateRedContainer
import com.example.ui.theme.LeftAmber
import com.example.ui.theme.LeftAmberContainer
import com.example.ui.theme.OnHomeIndigoContainer
import com.example.ui.theme.OnLateRedContainer
import com.example.ui.theme.OnLeftAmberContainer
import com.example.ui.theme.OnPresentGreenContainer
import com.example.ui.theme.PresentGreen
import com.example.ui.theme.PresentGreenContainer
import com.example.utils.ReportUtils
import com.example.utils.ShareUtils

private fun statusColor(status: AttendanceStatus): Color = when (status) {
  AttendanceStatus.PRESENT -> PresentGreen
  AttendanceStatus.LATE -> LateRed
  AttendanceStatus.LEFT -> LeftAmber
  AttendanceStatus.OTHER_GROUP -> HomeIndigo
}

@Composable
fun HistoryScreen(
  state: AttendanceUiState,
  viewModel: AttendanceViewModel,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  var groupFilter by remember { mutableStateOf<String?>(null) } // group id, null = all
  var deletingSession by remember { mutableStateOf<AttendanceSession?>(null) }

  val groupsWithSessions = remember(state.historySessions) {
    state.historySessions
      .map { it.groupId to it.groupName }
      .distinct()
      .sortedBy { it.second }
  }
  // If the filtered group disappeared (deleted), fall back to showing everything.
  val activeFilter = groupFilter?.takeIf { f -> groupsWithSessions.any { it.first == f } }
  val sessions = remember(state.historySessions, activeFilter) {
    if (activeFilter == null) state.historySessions else state.historySessions.filter { it.groupId == activeFilter }
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .testTag("history_screen")
  ) {
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      // Saved session notification banner if any
      if (state.lastSavedSummaryMessage != null) {
        item {
          Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = PresentGreenContainer)
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text(
                text = "✅ " + state.lastSavedSummaryMessage,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = OnPresentGreenContainer,
                modifier = Modifier.weight(1f)
              )
              TextButton(onClick = { viewModel.clearLastSavedMessage() }) {
                Text("Dismiss", color = OnPresentGreenContainer)
              }
            }
          }
        }
      }

      // History header card
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
              )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = "Attendance Records",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Text(
                text = "${sessions.size} of ${state.historySessions.size} sessions shown • newest first",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }

      // Group filter
      if (groupsWithSessions.size > 1) {
        item {
          LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
              FilterChip(
                selected = activeFilter == null,
                onClick = { groupFilter = null },
                label = { Text("All groups") }
              )
            }
            items(groupsWithSessions, key = { it.first }) { (id, name) ->
              FilterChip(
                selected = activeFilter == id,
                onClick = { groupFilter = id },
                label = { Text(name) }
              )
            }
          }
        }
      }

      // History Sessions List
      if (sessions.isEmpty()) {
        item {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 48.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "No attendance sessions logged yet.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      } else {
        items(sessions, key = { it.id }) { session ->
          HistorySessionCard(
            session = session,
            onClick = { viewModel.inspectSession(session) }
          )
        }
      }
    }

    // Inspect Session Details Dialog
    state.inspectingSession?.let { session ->
      AlertDialog(
        onDismissRequest = { viewModel.inspectSession(null) },
        title = {
          Column {
            Text(
              text = session.groupName,
              style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            Text(
              text = listOf(session.subjectName, "${session.date} at ${session.time}")
                .filter { it.isNotBlank() }
                .joinToString(" • "),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        },
        text = {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              CountBox("✅", session.presentCount, PresentGreenContainer, OnPresentGreenContainer, Modifier.weight(1f))
              CountBox("❌", session.lateCount, LateRedContainer, OnLateRedContainer, Modifier.weight(1f))
              CountBox("✈️", session.leftCount, LeftAmberContainer, OnLeftAmberContainer, Modifier.weight(1f))
              CountBox("❓", session.otherGroupCount, HomeIndigoContainer, OnHomeIndigoContainer, Modifier.weight(1f))
            }

            if (session.records.isNotEmpty()) {
              Text(
                text = "Tap a status to change it:",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
              )
              LazyColumn(
                modifier = Modifier.heightIn(max = 320.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                items(session.records) { record ->
                  HistoryRecordRow(record = record, session = session, viewModel = viewModel)
                }
              }
            }
          }
        },
        confirmButton = {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              TextButton(
                onClick = {
                  val teacherName = session.teacherName
                  val teacher = state.teachers.firstOrNull { it.name == teacherName }
                  val report = ReportUtils.buildAttendanceReport(
                    groupName = session.groupName,
                    teacherName = teacherName,
                    teacherLink = teacher?.telegramLink ?: "",
                    time = session.time,
                    total = session.totalStudents,
                    present = session.presentCount,
                    late = session.lateCount,
                    left = session.leftCount,
                    otherGroup = session.otherGroupCount,
                    entries = session.records.map { it.studentName to it.status }
                  )
                  ShareUtils.shareText(context, report, "Share Session Report")
                }
              ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Share")
              }
              TextButton(
                onClick = { deletingSession = session },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
              ) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Delete")
              }
            }
            Button(onClick = { viewModel.inspectSession(null) }) {
              Text("Close")
            }
          }
        }
      )
    }

    deletingSession?.let { session ->
      AlertDialog(
        onDismissRequest = { deletingSession = null },
        title = { Text("Delete this session?", fontWeight = FontWeight.Bold) },
        text = {
          Text("The attendance taken for ${session.groupName} on ${session.date} at ${session.time} will be removed permanently.")
        },
        confirmButton = {
          Button(
            onClick = {
              viewModel.deleteSession(session.id)
              deletingSession = null
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
          ) {
            Text("Delete")
          }
        },
        dismissButton = {
          TextButton(onClick = { deletingSession = null }) { Text("Cancel") }
        }
      )
    }
  }
}

@Composable
private fun CountBox(
  symbol: String,
  count: Int,
  container: Color,
  content: Color,
  modifier: Modifier = Modifier
) {
  Surface(shape = RoundedCornerShape(8.dp), color = container, modifier = modifier) {
    Column(
      modifier = Modifier.padding(vertical = 8.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(
        text = "$count",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = content
      )
      Text(symbol, style = MaterialTheme.typography.labelSmall, color = content)
    }
  }
}

@Composable
private fun HistorySessionCard(
  session: AttendanceSession,
  onClick: () -> Unit
) {
  val rate = if (session.totalStudents > 0) (session.presentCount * 100) / session.totalStudents else 0

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() }
      .testTag("history_item_${session.id}"),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = session.groupName,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f, fill = false)
          )
          if (session.subjectName.isNotBlank()) {
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = MaterialTheme.colorScheme.primaryContainer
            ) {
              Text(
                text = session.subjectName,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.CalendarToday,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = session.date,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Spacer(modifier = Modifier.width(10.dp))
          Icon(
            imageVector = Icons.Default.AccessTime,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = session.time,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      Row(verticalAlignment = Alignment.CenterVertically) {
        Column(horizontalAlignment = Alignment.End) {
          Text(
            text = "${session.presentCount} / ${session.totalStudents}",
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.ExtraBold,
              color = if (rate >= 85) PresentGreen else MaterialTheme.colorScheme.primary
            )
          )
          Text(
            text = "Present",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
          imageVector = Icons.Default.ChevronRight,
          contentDescription = "Inspect",
          tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }
}

@Composable
private fun HistoryRecordRow(
  record: StudentAttendanceRecord,
  session: AttendanceSession,
  viewModel: AttendanceViewModel
) {
  var statusMenuExpanded by remember { mutableStateOf(false) }

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 2.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      record.studentName,
      style = MaterialTheme.typography.bodyMedium,
      modifier = Modifier.weight(1f)
    )
    Box {
      Surface(
        modifier = Modifier.clickable { statusMenuExpanded = true },
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
      ) {
        Text(
          text = "${record.status.symbol} ${record.status.label}",
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
          color = statusColor(record.status)
        )
      }
      DropdownMenu(
        expanded = statusMenuExpanded,
        onDismissRequest = { statusMenuExpanded = false }
      ) {
        AttendanceStatus.values().forEach { status ->
          DropdownMenuItem(
            text = { Text("${status.symbol} ${status.label}") },
            onClick = {
              statusMenuExpanded = false
              // The ViewModel saves the change, recounts the session and refreshes this dialog.
              viewModel.updateSessionRecord(session.id, record.studentId, status)
            }
          )
        }
      }
    }
  }
}
