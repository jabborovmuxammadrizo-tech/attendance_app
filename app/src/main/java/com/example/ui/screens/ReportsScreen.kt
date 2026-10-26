package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.ui.AttendanceUiState
import com.example.ui.AttendanceViewModel
import com.example.ui.theme.HomeIndigoContainer
import com.example.ui.theme.LateRedContainer
import com.example.ui.theme.LeftAmberContainer
import com.example.ui.theme.LateRed
import com.example.ui.theme.OnHomeIndigoContainer
import com.example.ui.theme.OnLateRedContainer
import com.example.ui.theme.OnLeftAmberContainer
import com.example.ui.theme.OnPresentGreenContainer
import com.example.ui.theme.PresentGreen
import com.example.ui.theme.PresentGreenContainer
import com.example.utils.ShareUtils
import kotlinx.coroutines.launch

private enum class ReportPeriod(val label: String, val days: Int?) {
  ALL("All time", null),
  WEEK("Last 7 days", 7),
  MONTH("Last 30 days", 30)
}

private fun percent(part: Int, whole: Int): Int = if (whole > 0) (part * 100) / whole else 0

@Composable
fun ReportsScreen(
  state: AttendanceUiState,
  viewModel: AttendanceViewModel,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var period by remember { mutableStateOf(ReportPeriod.ALL) }

  // Everything below is computed from the sessions that were really saved.
  val sessions: List<AttendanceSession> = remember(state.historySessions, period) {
    val days = period.days
    if (days == null) {
      state.historySessions
    } else {
      val cutoff = System.currentTimeMillis() - days * 24L * 60L * 60L * 1000L
      state.historySessions.filter { it.timestamp >= cutoff }
    }
  }
  val totalMarks = sessions.sumOf { it.totalStudents }
  val totalPresent = sessions.sumOf { it.presentCount }
  val totalLate = sessions.sumOf { it.lateCount }
  val totalLeft = sessions.sumOf { it.leftCount }
  val totalOther = sessions.sumOf { it.otherGroupCount }
  val attendanceRate = percent(totalPresent, totalMarks)

  val groupRates = remember(sessions, state.groups) {
    state.groups.map { group ->
      val own = sessions.filter { it.groupId == group.id }
      Triple(group.name, own.size, if (own.isEmpty()) null else percent(own.sumOf { it.presentCount }, own.sumOf { it.totalStudents }))
    }
  }

  // Students with the lowest all-time attendance (needs a few records to mean anything).
  val needsAttention = remember(state.students, state.studentStats) {
    state.students.mapNotNull { student ->
      val stat = state.studentStats[student.id]
      if (stat != null && stat.totalMarks >= 3) {
        val rate = percent(stat.presentMarks, stat.totalMarks)
        if (rate < 80) Triple(student.name, student.groupName, rate) else null
      } else {
        null
      }
    }.sortedBy { it.third }.take(5)
  }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .testTag("reports_screen"),
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    // Period filter
    item {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ReportPeriod.values().forEach { option ->
          FilterChip(
            selected = period == option,
            onClick = { period = option },
            label = { Text(option.label) }
          )
        }
      }
    }

    // 1. Overall Attendance Summary Hero Card
    item {
      ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
      ) {
        Column(modifier = Modifier.padding(20.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Overall Attendance Rate",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = if (sessions.isEmpty()) "No sessions in this period" else "${sessions.size} sessions • $totalMarks student marks",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }

            Text(
              text = if (sessions.isEmpty()) "—" else "$attendanceRate%",
              style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
              )
            )
          }

          Spacer(modifier = Modifier.height(14.dp))

          LinearProgressIndicator(
            progress = { attendanceRate / 100f },
            modifier = Modifier
              .fillMaxWidth()
              .height(10.dp)
              .clip(RoundedCornerShape(5.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.primaryContainer,
          )

          Spacer(modifier = Modifier.height(16.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            StatBox("✅ Present", "$totalPresent", PresentGreenContainer, OnPresentGreenContainer, Modifier.weight(1f))
            StatBox("❌ Late", "$totalLate", LateRedContainer, OnLateRedContainer, Modifier.weight(1f))
            StatBox("✈️ Left", "$totalLeft", LeftAmberContainer, OnLeftAmberContainer, Modifier.weight(1f))
            StatBox("❓ Other", "$totalOther", HomeIndigoContainer, OnHomeIndigoContainer, Modifier.weight(1f))
          }
        }
      }
    }

    // 2. Class-by-Class Attendance Rates
    item {
      Text(
        text = "Attendance Rate by Group",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
      )
    }

    if (groupRates.isEmpty()) {
      item {
        Text(
          text = "No groups yet.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(horizontal = 4.dp)
        )
      }
    }

    items(groupRates) { (name, sessionCount, rate) ->
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = name,
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = if (sessionCount == 0) "No sessions" else "$sessionCount sessions",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (rate != null) {
              Spacer(modifier = Modifier.height(6.dp))
              LinearProgressIndicator(
                progress = { rate / 100f },
                modifier = Modifier
                  .fillMaxWidth(0.85f)
                  .height(6.dp)
                  .clip(RoundedCornerShape(3.dp)),
                color = if (rate >= 90) PresentGreen else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
              )
            }
          }

          Text(
            text = if (rate == null) "—" else "$rate%",
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Bold,
              color = if (rate != null && rate >= 90) PresentGreen else MaterialTheme.colorScheme.primary
            )
          )
        }
      }
    }

    // 3. Students who need attention
    if (needsAttention.isNotEmpty()) {
      item {
        Text(
          text = "Needs Attention (all time, under 80%)",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onBackground,
          modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
      }
      items(needsAttention) { (name, groupName, rate) ->
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
              Text(
                groupName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            Text(
              "$rate%",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = LateRed)
            )
          }
        }
      }
    }

    // 4. Local Export & Sharing Actions Card
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text(
            text = "Export & Share Report",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Shares the period selected above. The CSV has one row per student per session.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
          )
          Spacer(modifier = Modifier.height(14.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Button(
              onClick = {
                val reportText = buildString {
                  appendLine("📊 ATTENDANCE REPORT — ${period.label}")
                  appendLine("Sessions: ${sessions.size}")
                  appendLine("Student marks: $totalMarks")
                  appendLine("✅ Present: $totalPresent")
                  appendLine("❌ Late: $totalLate")
                  appendLine("✈️ Left: $totalLeft")
                  appendLine("❓ Other group: $totalOther")
                  appendLine("Attendance rate: $attendanceRate%")
                  appendLine()
                  groupRates.forEach { (name, count, rate) ->
                    appendLine("$name: ${if (rate == null) "no sessions" else "$rate% ($count sessions)"}")
                  }
                }
                ShareUtils.shareText(context, reportText.trim())
              },
              enabled = sessions.isNotEmpty(),
              modifier = Modifier
                .weight(1f)
                .testTag("btn_share_report"),
              shape = RoundedCornerShape(12.dp)
            ) {
              Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Share")
            }

            OutlinedButton(
              onClick = {
                scope.launch {
                  val csv = viewModel.buildCsv(sessions)
                  ShareUtils.shareFile(context, "Attendance_Report.csv", csv)
                }
              },
              enabled = sessions.isNotEmpty(),
              modifier = Modifier
                .weight(1f)
                .testTag("btn_export_csv"),
              shape = RoundedCornerShape(12.dp)
            ) {
              Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Share CSV")
            }
          }
        }
      }
    }
  }
}

@Composable
private fun StatBox(
  label: String,
  value: String,
  bgColor: Color,
  textColor: Color,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(10.dp))
      .background(bgColor)
      .padding(vertical = 10.dp, horizontal = 4.dp),
    contentAlignment = Alignment.Center
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text(
        text = value,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
        color = textColor
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = textColor.copy(alpha = 0.85f),
        maxLines = 1
      )
    }
  }
}
