package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Class
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GroupItem
import com.example.model.Screen
import com.example.ui.AttendanceUiState

@Composable
fun HomeScreen(
  state: AttendanceUiState,
  onNavigate: (Screen) -> Unit,
  onQuickStartGroup: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .testTag("home_screen"),
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // 1. Teacher Welcome Banner & Status Summary
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Column(
            modifier = Modifier
              .weight(1f, fill = false)
              .padding(end = 8.dp)
          ) {
            Text(
              text = "Welcome, Teacher",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "${state.groups.size} Classes • ${state.students.size} Students Enrolled",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
          }

          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(12.dp))
              .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
              .padding(horizontal = 10.dp, vertical = 6.dp)
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "Offline Mode",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                softWrap = false
              )
            }
          }
        }
      }
    }

    // 2. THE PROMINENT PRIMARY ATTENDANCE ACTION (More Compact)
    item {
      ElevatedCard(
        modifier = Modifier
          .fillMaxWidth()
          .testTag("btn_take_attendance")
          .clickable { onNavigate(Screen.ATTENDANCE) },
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
      ) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .background(
              Brush.horizontalGradient(
                colors = listOf(
                  Color(0xFF1D4ED8), // Deep Rich Blue
                  Color(0xFF2563EB), // Vibrant Blue
                  Color(0xFF3B82F6)  // Light Blue
                )
              )
            )
            .padding(16.dp) // Reduced from 24.dp
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.weight(1f)
            ) {
              Box(
                modifier = Modifier
                  .size(40.dp) // Reduced from 56.dp
                  .clip(CircleShape)
                  .background(Color.White.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.Assignment,
                  contentDescription = "Take Attendance",
                  tint = Color.White,
                  modifier = Modifier.size(24.dp) // Reduced from 32.dp
                )
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text(
                  text = "📋 TAKE ATTENDANCE",
                  style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                  ),
                  color = Color.White
                )
                Text(
                  text = "Start fast 1-tap roll call",
                  style = MaterialTheme.typography.bodySmall,
                  color = Color.White.copy(alpha = 0.9f)
                )
              }
            }

            Box(
              modifier = Modifier
                .size(36.dp) // Reduced from 44.dp
                .clip(CircleShape)
                .background(Color.White),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Start",
                tint = Color(0xFF1D4ED8),
                modifier = Modifier.size(20.dp) // Reduced from 24.dp
              )
            }
          }
        }
      }
    }

    // 3. Quick Recent / Favorite Groups Horizontal Section
    item {
      Column {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "⭐ Quick Start Classes",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
          )
          Text(
            text = "Tap to Launch",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        Spacer(modifier = Modifier.height(8.dp))
        if (state.groups.isEmpty()) {
          Text(
            text = "No groups yet — open GROUPS below to create your first class.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp)
          )
        }
        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          val pinned = state.groups.sortedByDescending { it.isFavorite }
          items(pinned) { group ->
            QuickGroupChip(
              group = group,
              onClick = {
                onQuickStartGroup(group.name)
                onNavigate(Screen.ATTENDANCE)
              }
            )
          }
        }
      }
    }

    // 4. MAIN SECTIONS GRID: GROUPS, STUDENTS, REPORTS, TEACHERS, HISTORY, SETTINGS
    item {
      Text(
        text = "Management Modules",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
      )
    }

    item {
      val modules = listOf(
        ModuleCardSpec(
          title = "👥 GROUPS",
          subtitle = "${state.groups.size} Active Classes",
          icon = Icons.Default.Groups,
          iconTint = MaterialTheme.colorScheme.primary,
          testTag = "btn_groups",
          onClick = { onNavigate(Screen.GROUPS) }
        ),
        ModuleCardSpec(
          title = "🎓 STUDENTS",
          subtitle = "${state.students.size} Enrolled",
          icon = Icons.Default.School,
          iconTint = Color(0xFF0D9488), // Teal
          testTag = "btn_students",
          onClick = { onNavigate(Screen.STUDENTS) }
        ),
        ModuleCardSpec(
          title = "📊 REPORTS",
          subtitle = "Statistics & Export",
          icon = Icons.Default.Assessment,
          iconTint = Color(0xFFF59E0B), // Amber
          testTag = "btn_reports",
          onClick = { onNavigate(Screen.REPORTS) }
        ),
        ModuleCardSpec(
          title = "👨‍🏫 TEACHERS",
          subtitle = "${state.teachers.size} Staff",
          icon = Icons.Default.Person,
          iconTint = Color(0xFFEF4444), // Red
          testTag = "btn_teachers",
          onClick = { onNavigate(Screen.TEACHERS) }
        ),
        ModuleCardSpec(
          title = "🗂 HISTORY",
          subtitle = "${state.historySessions.size} Saved Logs",
          icon = Icons.Default.History,
          iconTint = Color(0xFF6366F1), // Indigo
          testTag = "btn_history",
          onClick = { onNavigate(Screen.HISTORY) }
        ),
        ModuleCardSpec(
          title = "⚙️ SETTINGS",
          subtitle = "Backup & Prefs",
          icon = Icons.Default.Settings,
          iconTint = Color(0xFF64748B), // Slate
          testTag = "btn_settings",
          onClick = { onNavigate(Screen.SETTINGS) }
        )
      )

      // Adaptive column count: stays 2-up on a normal phone, but opens up to 3 or 4
      // columns on wider screens (tablets, foldables, split-screen / multi-window on
      // Huawei and other OEM devices) instead of leaving half the row empty.
      BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val columns = when {
          maxWidth >= 840.dp -> 4
          maxWidth >= 600.dp -> 3
          else -> 2
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          modules.chunked(columns).forEach { rowModules ->
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              rowModules.forEach { module ->
                MainSectionCard(
                  title = module.title,
                  subtitle = module.subtitle,
                  icon = module.icon,
                  containerColor = MaterialTheme.colorScheme.surface,
                  iconTint = module.iconTint,
                  modifier = Modifier
                    .weight(1f)
                    .testTag(module.testTag),
                  onClick = module.onClick
                )
              }
              // Pad out a short last row so its cards stay the same width as full rows.
              repeat(columns - rowModules.size) {
                Spacer(modifier = Modifier.weight(1f))
              }
            }
          }
        }
      }
    }
  }
}

private data class ModuleCardSpec(
  val title: String,
  val subtitle: String,
  val icon: ImageVector,
  val iconTint: Color,
  val testTag: String,
  val onClick: () -> Unit
)

@Composable
private fun QuickGroupChip(
  group: GroupItem,
  onClick: () -> Unit
) {
  Surface(
    shape = RoundedCornerShape(14.dp),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 2.dp,
    shadowElevation = 1.dp,
    modifier = Modifier
      .clip(RoundedCornerShape(14.dp))
      .clickable { onClick() }
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      if (group.isFavorite) {
        Icon(
          imageVector = Icons.Default.Star,
          contentDescription = "Favorite",
          tint = Color(0xFFF59E0B),
          modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
      } else {
        Icon(
          imageVector = Icons.Default.Class,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
      }
      Column {
        Text(
          text = group.name,
          style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurface
        )
        Text(
          text = "${group.studentCount} Students",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }
}

@Composable
private fun MainSectionCard(
  title: String,
  subtitle: String,
  icon: ImageVector,
  containerColor: Color,
  iconTint: Color,
  modifier: Modifier = Modifier,
  onClick: () -> Unit
) {
  ElevatedCard(
    modifier = modifier
      .clickable { onClick() },
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.elevatedCardColors(containerColor = containerColor),
    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Box(
        modifier = Modifier
          .size(32.dp)
          .clip(CircleShape)
          .background(iconTint.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = iconTint,
          modifier = Modifier.size(18.dp)
        )
      }

      Column {
        Text(
          text = title,
          style = MaterialTheme.typography.titleSmall.copy(
            fontWeight = FontWeight.Bold
          ),
          color = MaterialTheme.colorScheme.onSurface
        )
        Text(
          text = subtitle,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }
}
