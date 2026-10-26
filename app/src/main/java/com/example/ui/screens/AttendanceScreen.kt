package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AttendanceStatus
import com.example.model.Screen
import com.example.model.StudentAttendanceState
import com.example.ui.AttendanceUiState
import com.example.ui.AttendanceViewModel
import com.example.ui.theme.HomeIndigo
import com.example.ui.theme.LateRed
import com.example.ui.theme.LateRedContainer
import com.example.ui.theme.LeftAmber
import com.example.ui.theme.PresentGreen
import com.example.ui.theme.PresentGreenContainer
import com.example.utils.ReportUtils
import com.example.utils.ShareUtils

@Composable
fun AttendanceScreen(
  state: AttendanceUiState,
  viewModel: AttendanceViewModel,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current

  // Always show the real current date / time when the screen is opened.
  LaunchedEffect(Unit) { viewModel.refreshDateTime() }

  if (state.groups.isEmpty()) {
    Column(
      modifier = modifier
        .fillMaxSize()
        .padding(32.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Text(
        "No groups yet",
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
      )
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        "Create a group and add students to start taking attendance.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Spacer(modifier = Modifier.height(16.dp))
      Button(onClick = { viewModel.navigateTo(Screen.GROUPS) }) { Text("Go to Groups") }
    }
    return
  }

  val group = state.groups.firstOrNull { it.name == state.selectedGroup }
  val displayedStudents = state.activeAttendanceList.filter {
    state.attendanceSearchQuery.isBlank() || it.student.name.contains(state.attendanceSearchQuery, ignoreCase = true)
  }

  val totalCount = state.activeAttendanceList.size
  val presentCount = state.activeAttendanceList.count { it.status == AttendanceStatus.PRESENT }
  val lateCount = state.activeAttendanceList.count { it.status == AttendanceStatus.LATE }
  val leftCount = state.activeAttendanceList.count { it.status == AttendanceStatus.LEFT }
  val otherGroupCount = state.activeAttendanceList.count { it.status == AttendanceStatus.OTHER_GROUP }

  var groupMenuOpen by remember { mutableStateOf(false) }

  Box(modifier = modifier
    .fillMaxSize()
    .background(MaterialTheme.colorScheme.background)) {
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(bottom = 90.dp, top = 12.dp, start = 16.dp, end = 16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      // 1. Header: group picker + date/time
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Box {
              Row(
                modifier = Modifier
                  .clip(RoundedCornerShape(10.dp))
                  .clickable { groupMenuOpen = true },
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = state.selectedGroup,
                  style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                  maxLines = 1,
                  modifier = Modifier.weight(1f, fill = false)
                )
                Icon(Icons.Default.ArrowDropDown, contentDescription = "Change group")
              }
              DropdownMenu(expanded = groupMenuOpen, onDismissRequest = { groupMenuOpen = false }) {
                state.groups.sortedByDescending { it.isFavorite }.forEach { g ->
                  DropdownMenuItem(
                    text = { Text((if (g.isFavorite) "⭐ " else "") + g.name + "  (" + g.studentCount + ")") },
                    onClick = {
                      groupMenuOpen = false
                      viewModel.selectAttendanceGroup(g.name)
                    }
                  )
                }
              }
            }
            val info = listOf(group?.subjectName.orEmpty(), group?.teacherName.orEmpty())
              .filter { it.isNotBlank() }
              .joinToString(" • ")
            if (info.isNotEmpty()) {
              Text(
                text = info,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
          Spacer(modifier = Modifier.width(8.dp))
          Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer) {
            Column(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Text(
                text = state.activeDate,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
              )
              Text(
                text = state.activeTime,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
              )
            }
          }
        }
      }

      // 2. Search Bar
      item {
        OutlinedTextField(
          value = state.attendanceSearchQuery,
          onValueChange = { viewModel.setAttendanceSearchQuery(it) },
          modifier = Modifier.fillMaxWidth(),
          placeholder = { Text("Search student...") },
          leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
          trailingIcon = {
            if (state.attendanceSearchQuery.isNotEmpty()) {
              IconButton(onClick = { viewModel.setAttendanceSearchQuery("") }) {
                Icon(Icons.Default.Clear, contentDescription = "Clear")
              }
            }
          },
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedBorderColor = Color.Transparent
          )
        )
      }

      // 3. Mark All Row + legend
      item {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Mark all as:",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = PresentGreenContainer,
                modifier = Modifier.clickable { viewModel.markAllStudents(AttendanceStatus.PRESENT) }
              ) {
                Text(
                  "✅ Present",
                  style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                  color = PresentGreen,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                )
              }
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = LateRedContainer,
                modifier = Modifier.clickable { viewModel.markAllStudents(AttendanceStatus.LATE) }
              ) {
                Text(
                  "❌ Late",
                  style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                  color = LateRed,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                )
              }
            }
          }
          Text(
            text = "✅ Present   ❌ Late   ✈️ Left   ❓ Other group",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      // 4. Student List
      if (displayedStudents.isEmpty()) {
        item {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(32.dp),
            contentAlignment = Alignment.Center
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(
                text = if (totalCount == 0) "This group has no students yet" else "No students found",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              if (totalCount == 0) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(onClick = {
                  viewModel.setStudentGroupFilter(state.selectedGroup)
                  viewModel.navigateTo(Screen.STUDENTS)
                }) { Text("Add students") }
              }
            }
          }
        }
      } else {
        itemsIndexed(displayedStudents, key = { _, it -> it.student.id }) { _, item ->
          // Number by position in the whole roll call, not in the filtered list.
          val number = state.activeAttendanceList.indexOfFirst { it.student.id == item.student.id } + 1
          StudentAttendanceRow(
            number = number,
            studentState = item,
            onStatusChange = { newStatus -> viewModel.setStudentStatus(item.student.id, newStatus) }
          )
        }
      }
    }

    // 5. Sticky Bottom Action Bar
    Surface(
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .fillMaxWidth(),
      shadowElevation = 8.dp,
      color = MaterialTheme.colorScheme.surface
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "$totalCount students",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
          )
          Text(
            text = "✅ $presentCount  ❌ $lateCount  ✈️ $leftCount  ❓ $otherGroupCount",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        Button(
          onClick = { viewModel.showSummaryDialog(true) },
          enabled = totalCount > 0,
          modifier = Modifier.height(40.dp),
          contentPadding = PaddingValues(horizontal = 20.dp, vertical = 0.dp),
          shape = RoundedCornerShape(10.dp),
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
          Text("Review & Save", style = MaterialTheme.typography.labelLarge)
        }
      }
    }

    // 6. Preview Dialog
    if (state.showAttendanceSummaryDialog) {
      val reportText = remember(
        state.activeAttendanceList,
        state.selectedGroup,
        state.activeTime,
        state.groups,
        state.teachers
      ) {
        val teacher = group?.teacherName?.takeIf { it.isNotBlank() }?.let { tName ->
          state.teachers.firstOrNull { it.name == tName }
        }
        ReportUtils.buildAttendanceReport(
          groupName = state.selectedGroup,
          teacherName = teacher?.name ?: "",
          teacherLink = teacher?.telegramLink ?: "",
          time = state.activeTime,
          total = totalCount,
          present = presentCount,
          late = lateCount,
          left = leftCount,
          otherGroup = otherGroupCount,
          entries = state.activeAttendanceList.map { it.student.name to it.status }
        )
      }

      androidx.compose.ui.window.Dialog(
        onDismissRequest = { viewModel.showSummaryDialog(false) },
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
      ) {
        Surface(
          modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
          shape = RoundedCornerShape(16.dp),
          color = MaterialTheme.colorScheme.surface
        ) {
          Column(modifier = Modifier.fillMaxSize()) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text("Attendance Report", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
              IconButton(onClick = { viewModel.showSummaryDialog(false) }) {
                Icon(Icons.Default.Close, "Close")
              }
            }
            Box(
              modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                .padding(12.dp)
            ) {
              LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                  Text(text = reportText, style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace)
                }
              }
            }
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                  onClick = { viewModel.showSummaryDialog(false) },
                  modifier = Modifier.weight(1f),
                  shape = RoundedCornerShape(8.dp),
                  contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                  Text("Edit")
                }
                Button(
                  onClick = { viewModel.finishAttendance() },
                  modifier = Modifier.weight(1f),
                  shape = RoundedCornerShape(8.dp),
                  contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                  Text("Save")
                }
              }
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                  onClick = { ShareUtils.copyToClipboard(context, reportText) },
                  modifier = Modifier.weight(1f),
                  shape = RoundedCornerShape(8.dp),
                  contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                  Text("Copy")
                }
                OutlinedButton(
                  onClick = { ShareUtils.shareText(context, reportText, "Share") },
                  modifier = Modifier.weight(1f),
                  shape = RoundedCornerShape(8.dp),
                  contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                  Text("Share")
                }
              }
              if (state.reportTelegramLink.isNotBlank()) {
                OutlinedButton(
                  onClick = {
                    // Copy first, then open the report chat so the teacher can just paste.
                    ShareUtils.copyToClipboard(context, reportText)
                    ShareUtils.openLink(context, state.reportTelegramLink)
                  },
                  modifier = Modifier.fillMaxWidth(),
                  shape = RoundedCornerShape(8.dp),
                  contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                  Text("Copy & open report chat")
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun StudentAttendanceRow(
  number: Int,
  studentState: StudentAttendanceState,
  onStatusChange: (AttendanceStatus) -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 4.dp, vertical = 2.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Text(
      text = "$number. ${studentState.student.name}",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurface,
      maxLines = 2,
      modifier = Modifier.weight(1f)
    )
    Spacer(modifier = Modifier.width(6.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
      CompactAttendanceButton(
        symbol = AttendanceStatus.PRESENT.symbol,
        isSelected = studentState.status == AttendanceStatus.PRESENT,
        selectedBgColor = PresentGreen,
        onClick = { onStatusChange(AttendanceStatus.PRESENT) }
      )
      CompactAttendanceButton(
        symbol = AttendanceStatus.LATE.symbol,
        isSelected = studentState.status == AttendanceStatus.LATE,
        selectedBgColor = LateRed,
        onClick = { onStatusChange(AttendanceStatus.LATE) }
      )
      CompactAttendanceButton(
        symbol = AttendanceStatus.LEFT.symbol,
        isSelected = studentState.status == AttendanceStatus.LEFT,
        selectedBgColor = LeftAmber,
        onClick = { onStatusChange(AttendanceStatus.LEFT) }
      )
      CompactAttendanceButton(
        symbol = AttendanceStatus.OTHER_GROUP.symbol,
        isSelected = studentState.status == AttendanceStatus.OTHER_GROUP,
        selectedBgColor = HomeIndigo,
        onClick = { onStatusChange(AttendanceStatus.OTHER_GROUP) }
      )
    }
  }
}

@Composable
fun CompactAttendanceButton(
  symbol: String,
  isSelected: Boolean,
  selectedBgColor: Color,
  onClick: () -> Unit
) {
  Surface(
    modifier = Modifier
      .size(36.dp)
      .clip(RoundedCornerShape(8.dp))
      .clickable { onClick() }
      .border(
        width = if (isSelected) 1.5.dp else 1.dp,
        color = if (isSelected) selectedBgColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
        shape = RoundedCornerShape(8.dp)
      ),
    color = if (isSelected) selectedBgColor.copy(alpha = 0.3f) else Color.Transparent,
    shape = RoundedCornerShape(8.dp)
  ) {
    Box(contentAlignment = Alignment.Center) {
      Text(text = symbol, fontSize = 16.sp)
    }
  }
}
