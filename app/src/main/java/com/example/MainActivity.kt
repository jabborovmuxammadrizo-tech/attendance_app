package com.example

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.model.Screen
import com.example.ui.AttendanceUiState
import com.example.ui.AttendanceViewModel
import com.example.ui.components.AttendanceTopAppBar
import com.example.ui.screens.AttendanceScreen
import com.example.ui.screens.GroupDetailsScreen
import com.example.ui.screens.GroupsScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ImportStudentsScreen
import com.example.ui.screens.LockScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StudentsScreen
import com.example.ui.screens.TeachersScreen
import com.example.ui.theme.AttendanceTheme

class MainActivity : ComponentActivity() {
  private val viewModel: AttendanceViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val state by viewModel.uiState.collectAsState()

      val isDark = when (state.selectedTheme) {
        "Dark" -> true
        "Light" -> false
        else -> isSystemInDarkTheme()
      }

      // Keep the status / navigation bar icon colours in sync with the *app* theme, which
      // can differ from the phone's theme when the user picks Light or Dark in Settings.
      DisposableEffect(isDark) {
        enableEdgeToEdge(
          statusBarStyle = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT) { isDark },
          navigationBarStyle = SystemBarStyle.auto(
            AndroidColor.argb(0xe6, 0xFF, 0xFF, 0xFF),
            AndroidColor.argb(0x80, 0x1b, 0x1b, 0x1b)
          ) { isDark }
        )
        onDispose {}
      }

      AttendanceTheme(
        darkTheme = isDark,
        dynamicColor = false // Keep consistent branding
      ) {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background
        ) {
          if (state.isLocked) {
            LockScreen(onUnlock = { viewModel.unlock() })
          } else {
            AppContent(state = state, viewModel = viewModel)
          }
        }
      }
    }
  }

  override fun onStart() {
    super.onStart()
    viewModel.onAppStarted()
  }

  override fun onStop() {
    super.onStop()
    viewModel.onAppStopped()
  }
}

@Composable
private fun AppContent(state: AttendanceUiState, viewModel: AttendanceViewModel) {
  // On Home the system handles Back (closes the app); everywhere else we go up one screen.
  BackHandler(enabled = state.currentScreen != Screen.HOME) {
    viewModel.onBackPress()
  }

  Scaffold(
    topBar = {
      AttendanceTopAppBar(
        currentScreen = state.currentScreen,
        onNavigateBack = { viewModel.onBackPress() },
        onNavigateSettings = { viewModel.navigateTo(Screen.SETTINGS) }
      )
    },
    containerColor = MaterialTheme.colorScheme.background
  ) { padding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .consumeWindowInsets(padding)
        .imePadding()
    ) {
      when (state.currentScreen) {
        Screen.HOME -> HomeScreen(
          state = state,
          onNavigate = { viewModel.navigateTo(it) },
          onQuickStartGroup = { viewModel.selectAttendanceGroup(it) }
        )
        Screen.ATTENDANCE -> AttendanceScreen(state, viewModel)
        Screen.GROUPS -> GroupsScreen(state = state, viewModel = viewModel, onNavigate = { viewModel.navigateTo(it) })
        Screen.STUDENTS -> StudentsScreen(state, viewModel)
        Screen.REPORTS -> ReportsScreen(state, viewModel)
        Screen.HISTORY -> HistoryScreen(state, viewModel)
        Screen.SETTINGS -> SettingsScreen(state, viewModel)
        Screen.GROUP_DETAILS -> GroupDetailsScreen(state, viewModel)
        Screen.TEACHERS -> TeachersScreen(state, viewModel)
        Screen.IMPORT_STUDENTS -> ImportStudentsScreen(state, viewModel)
      }
    }
  }
}
