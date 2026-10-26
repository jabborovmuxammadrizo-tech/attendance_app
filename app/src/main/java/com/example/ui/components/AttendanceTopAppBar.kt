package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.example.model.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceTopAppBar(
  currentScreen: Screen,
  onNavigateBack: () -> Unit,
  onNavigateSettings: () -> Unit,
  modifier: Modifier = Modifier
) {
  TopAppBar(
    modifier = modifier,
    title = {
      Text(
        text = if (currentScreen == Screen.HOME) "Attendance" else currentScreen.title,
        style = if (currentScreen == Screen.HOME) {
          MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
        } else {
          MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
        },
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        color = MaterialTheme.colorScheme.onSurface
      )
    },
    navigationIcon = {
      if (currentScreen != Screen.HOME) {
        IconButton(onClick = onNavigateBack) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = MaterialTheme.colorScheme.onSurface
          )
        }
      }
    },
    actions = {
      if (currentScreen != Screen.SETTINGS) {
        IconButton(onClick = onNavigateSettings) {
          Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = "Settings",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    },
    colors = TopAppBarDefaults.topAppBarColors(
      containerColor = MaterialTheme.colorScheme.surface
    )
  )
}
