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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Class
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GroupItem
import com.example.model.Screen
import com.example.ui.AttendanceUiState
import com.example.ui.AttendanceViewModel
import com.example.utils.normalizeGroupKey

@Composable
fun GroupsScreen(
  state: AttendanceUiState,
  viewModel: AttendanceViewModel,
  onNavigate: (Screen) -> Unit,
  modifier: Modifier = Modifier
) {
  var showAddGroupDialog by remember { mutableStateOf(false) }
  var newGroupName by remember { mutableStateOf("") }
  var newTeacherName by remember { mutableStateOf("") }
  var newSubjectName by remember { mutableStateOf("") }

  var editingGroup by remember { mutableStateOf<GroupItem?>(null) }
  var deletingGroup by remember { mutableStateOf<GroupItem?>(null) }

  val filteredGroups = remember(state.groups, state.groupSearchQuery) {
    if (state.groupSearchQuery.isBlank()) state.groups
    else state.groups.filter {
      it.name.contains(state.groupSearchQuery, ignoreCase = true) ||
      it.teacherName.contains(state.groupSearchQuery, ignoreCase = true) ||
      it.subjectName.contains(state.groupSearchQuery, ignoreCase = true)
    }
  }

  Scaffold(
    modifier = modifier.testTag("groups_screen"),
    floatingActionButton = {
      FloatingActionButton(
        onClick = { showAddGroupDialog = true },
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = Color.White,
        modifier = Modifier.testTag("btn_fab_add_group")
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 16.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(Icons.Default.Add, contentDescription = "Add Group")
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "ADD GROUP",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
          )
        }
      }
    }
  ) { paddingValues ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .padding(paddingValues),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      // Search Bar
      item {
        OutlinedTextField(
          value = state.groupSearchQuery,
          onValueChange = { viewModel.setGroupSearchQuery(it) },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("input_search_group"),
          placeholder = { Text("Search groups by name, teacher, subject...") },
          leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
          trailingIcon = {
            if (state.groupSearchQuery.isNotEmpty()) {
              IconButton(onClick = { viewModel.setGroupSearchQuery("") }) {
                Icon(Icons.Default.Clear, contentDescription = "Clear")
              }
            }
          },
          singleLine = true,
          shape = RoundedCornerShape(12.dp)
        )
      }

      // Header summary card
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
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column {
              Text(
                text = "Class Cohorts",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Text(
                text = "${filteredGroups.size} of ${state.groups.size} groups displayed",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              OutlinedButton(
                onClick = { 
                  viewModel.setTargetGroupForImport(null)
                  onNavigate(Screen.IMPORT_STUDENTS) 
                },
                shape = RoundedCornerShape(10.dp)
              ) {
                Text("📥 Import")
              }
              Button(
                onClick = { showAddGroupDialog = true },
                modifier = Modifier.testTag("btn_add_group_top"),
                shape = RoundedCornerShape(10.dp)
              ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add")
              }
            }
          }
        }
      }

      // Groups list
      if (filteredGroups.isEmpty()) {
        item {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 48.dp),
            contentAlignment = Alignment.Center
          ) {
            Text("No matching groups found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        }
      } else {
        items(filteredGroups, key = { it.id }) { group ->
          GroupCardItem(
            group = group,
            onToggleFavorite = { viewModel.toggleGroupFavorite(group.id) },
            onCardClick = {
              viewModel.selectAttendanceGroup(group.name)
              onNavigate(Screen.GROUP_DETAILS)
            },
            onTakeAttendance = {
              viewModel.selectAttendanceGroup(group.name)
              onNavigate(Screen.ATTENDANCE)
            },
            onEdit = { editingGroup = group },
            onDelete = { deletingGroup = group }
          )
        }
      }
    }

    // Add Group Dialog
    if (showAddGroupDialog) {
      val duplicateName = newGroupName.isNotBlank() &&
        state.groups.any { normalizeGroupKey(it.name) == normalizeGroupKey(newGroupName) }
      AlertDialog(
        onDismissRequest = { showAddGroupDialog = false },
        title = { Text("Create New Group", fontWeight = FontWeight.Bold) },
        text = {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
              text = "Enter group/cohort name and details:",
              style = MaterialTheme.typography.bodyMedium
            )
            OutlinedTextField(
              value = newGroupName,
              onValueChange = { newGroupName = it },
              label = { Text("Group Name *") },
              placeholder = { Text("e.g. #SERTIFIKAT_2") },
              isError = duplicateName,
              supportingText = { if (duplicateName) Text("A group with this name already exists") },
              singleLine = true,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("input_group_name")
            )
            OutlinedTextField(
              value = newTeacherName,
              onValueChange = { newTeacherName = it },
              label = { Text("Teacher Name") },
              supportingText = { Text("A new name is added to Teachers automatically") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
              value = newSubjectName,
              onValueChange = { newSubjectName = it },
              label = { Text("Subject Name") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )
          }
        },
        confirmButton = {
          Button(
            onClick = {
              if (newGroupName.isNotBlank() && !duplicateName) {
                viewModel.addGroup(newGroupName, newTeacherName, newSubjectName)
                newGroupName = ""
                newTeacherName = ""
                newSubjectName = ""
                showAddGroupDialog = false
              }
            },
            enabled = newGroupName.isNotBlank() && !duplicateName,
            modifier = Modifier.testTag("btn_save_group")
          ) {
            Text("Create Group")
          }
        },
        dismissButton = {
          TextButton(onClick = { showAddGroupDialog = false }) {
            Text("Cancel")
          }
        }
      )
    }

    // Edit Group Dialog
    editingGroup?.let { group ->
      var editName by remember { mutableStateOf(group.name) }
      var editTeacher by remember { mutableStateOf(group.teacherName) }
      var editSubject by remember { mutableStateOf(group.subjectName) }
      val editDuplicate = editName.isNotBlank() && state.groups.any {
        it.id != group.id && normalizeGroupKey(it.name) == normalizeGroupKey(editName)
      }

      AlertDialog(
        onDismissRequest = { editingGroup = null },
        title = { Text("✏️ Edit Group", fontWeight = FontWeight.Bold) },
        text = {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
              value = editName,
              onValueChange = { editName = it },
              label = { Text("Group Name") },
              isError = editDuplicate,
              supportingText = { if (editDuplicate) Text("A group with this name already exists") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
              value = editTeacher,
              onValueChange = { editTeacher = it },
              label = { Text("Teacher Name") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
              value = editSubject,
              onValueChange = { editSubject = it },
              label = { Text("Subject") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )
            Text(
              text = "Note: Renaming the group will automatically update all enrolled students and past attendance records.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        },
        confirmButton = {
          Button(
            onClick = {
              if (editName.isNotBlank() && !editDuplicate) {
                viewModel.updateGroup(group.id, editName, editTeacher, editSubject)
                editingGroup = null
              }
            },
            enabled = editName.isNotBlank() && !editDuplicate
          ) {
            Text("Save Changes")
          }
        },
        dismissButton = {
          TextButton(onClick = { editingGroup = null }) {
            Text("Cancel")
          }
        }
      )
    }

    // Delete Group Confirmation Dialog
    deletingGroup?.let { group ->
      AlertDialog(
        onDismissRequest = { deletingGroup = null },
        title = { Text("Delete Group?", fontWeight = FontWeight.Bold) },
        text = {
          Text("Delete '${group.name}'? Its ${group.studentCount} students and all of its attendance history will be permanently removed. This cannot be undone.")
        },
        confirmButton = {
          Button(
            onClick = {
              viewModel.deleteGroup(group.id)
              deletingGroup = null
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
          ) {
            Text("Delete")
          }
        },
        dismissButton = {
          TextButton(onClick = { deletingGroup = null }) {
            Text("Cancel")
          }
        }
      )
    }
  }
}

@Composable
private fun GroupCardItem(
  group: GroupItem,
  onCardClick: () -> Unit,
  onToggleFavorite: () -> Unit,
  onTakeAttendance: () -> Unit,
  onEdit: () -> Unit,
  onDelete: () -> Unit
) {
  Card(
    modifier = Modifier.fillMaxWidth().clickable { onCardClick() },
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 10.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = group.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
          )
          Spacer(modifier = Modifier.width(8.dp))
          Icon(
            imageVector = if (group.isFavorite) Icons.Default.Star else Icons.Outlined.StarOutline,
            contentDescription = "Favorite",
            tint = if (group.isFavorite) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
              .size(24.dp)
              .padding(4.dp)
              .clickable { onToggleFavorite() }
          )
        }
        val teacherDisplay = group.teacherName.takeIf { it.isNotBlank() } ?: "No teacher assigned"
        val subjectDisplay = group.subjectName.takeIf { it.isNotBlank() } ?: "General"
        Text("👨‍🏫 $teacherDisplay • $subjectDisplay", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("👥 ${group.studentCount} students", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
      
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        Button(
          onClick = onTakeAttendance,
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
          modifier = Modifier.height(32.dp),
          shape = RoundedCornerShape(6.dp)
        ) {
          Text("Open", fontSize = 13.sp)
        }
        IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
          Text("✏️", fontSize = 14.sp)
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
          Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = "Delete",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(18.dp)
          )
        }
      }
    }
  }
}
