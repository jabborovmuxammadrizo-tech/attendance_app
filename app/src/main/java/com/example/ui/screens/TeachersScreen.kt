package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Teacher
import com.example.ui.AttendanceUiState
import com.example.ui.AttendanceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeachersScreen(
  state: AttendanceUiState,
  viewModel: AttendanceViewModel,
  modifier: Modifier = Modifier
) {
  var showAddDialog by remember { mutableStateOf(false) }
  var editingTeacher by remember { mutableStateOf<Teacher?>(null) }
  var assigningTeacher by remember { mutableStateOf<Teacher?>(null) }
  var deletingTeacher by remember { mutableStateOf<Teacher?>(null) }

  val filteredTeachers = remember(state.teachers, state.teacherSearchQuery) {
    if (state.teacherSearchQuery.isBlank()) state.teachers
    else state.teachers.filter { it.name.contains(state.teacherSearchQuery, ignoreCase = true) }
  }

  Scaffold(
    floatingActionButton = {
      FloatingActionButton(
        onClick = { showAddDialog = true },
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 16.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(Icons.Default.Add, contentDescription = "Add Teacher")
          Spacer(modifier = Modifier.width(6.dp))
          Text("ADD TEACHER", fontWeight = FontWeight.Bold)
        }
      }
    }
  ) { padding ->
    LazyColumn(
      modifier = modifier.fillMaxSize().padding(padding),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      // Search bar
      item {
        OutlinedTextField(
          value = state.teacherSearchQuery,
          onValueChange = { viewModel.setTeacherSearchQuery(it) },
          modifier = Modifier.fillMaxWidth(),
          placeholder = { Text("Search teachers by name...") },
          leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
          trailingIcon = {
            if (state.teacherSearchQuery.isNotEmpty()) {
              IconButton(onClick = { viewModel.setTeacherSearchQuery("") }) {
                Icon(Icons.Default.Clear, contentDescription = "Clear")
              }
            }
          },
          singleLine = true,
          shape = RoundedCornerShape(12.dp)
        )
      }

      if (filteredTeachers.isEmpty()) {
        item {
          Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
            Text("No teachers found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        }
      } else {
        items(filteredTeachers, key = { it.id }) { teacher ->
          val assignedGroupsCount = state.groups.count { it.teacherName.equals(teacher.name, ignoreCase = true) }
          Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
          ) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
              Text("👨‍🏫 ${teacher.name}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
              Spacer(modifier = Modifier.height(4.dp))
              Text("📞 ${if (teacher.phoneNumber.isNotBlank()) teacher.phoneNumber else "Phone not specified"}", style = MaterialTheme.typography.bodyMedium)
              Spacer(modifier = Modifier.height(2.dp))
              Text("🔗 ${if (teacher.telegramLink.isNotBlank()) teacher.telegramLink else "Link not specified"}", style = MaterialTheme.typography.bodyMedium)
              Spacer(modifier = Modifier.height(8.dp))
              Text("👥 Assigned Groups: $assignedGroupsCount", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              
              Spacer(modifier = Modifier.height(12.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                OutlinedButton(onClick = { assigningTeacher = teacher }) {
                  Text("👥 Unite with Groups")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                  OutlinedButton(onClick = { editingTeacher = teacher }) {
                    Text("✏️ Edit")
                  }
                  IconButton(onClick = { deletingTeacher = teacher }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Teacher", tint = MaterialTheme.colorScheme.error)
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  // Add Teacher Dialog
  if (showAddDialog) {
    var newName by remember { mutableStateOf("") }
    var newPhone by remember { mutableStateOf("") }
    var newLink by remember { mutableStateOf("") }
    val duplicateTeacher = newName.isNotBlank() && state.teachers.any { it.name.trim().equals(newName.trim(), ignoreCase = true) }
    AlertDialog(
      onDismissRequest = { showAddDialog = false },
      title = { Text("Add Teacher", fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = newName,
            onValueChange = { newName = it },
            label = { Text("Teacher Name *") },
            isError = duplicateTeacher,
            supportingText = { if (duplicateTeacher) Text("This teacher already exists") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = newPhone,
            onValueChange = { newPhone = it },
            label = { Text("Phone Number") },
            placeholder = { Text("+998 90 123 45 67") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = newLink,
            onValueChange = { newLink = it },
            label = { Text("Telegram Link") },
            placeholder = { Text("https://t.me/username") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (newName.isNotBlank() && !duplicateTeacher) {
              viewModel.addTeacher(newName, newPhone, newLink)
              showAddDialog = false
            }
          },
          enabled = newName.isNotBlank() && !duplicateTeacher
        ) {
          Text("Save")
        }
      },
      dismissButton = {
        TextButton(onClick = { showAddDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }

  // Edit Teacher Dialog
  editingTeacher?.let { teacher ->
    var editName by remember { mutableStateOf(teacher.name) }
    var editPhone by remember { mutableStateOf(teacher.phoneNumber) }
    var editLink by remember { mutableStateOf(teacher.telegramLink) }
    val editDuplicate = editName.isNotBlank() && state.teachers.any {
      it.id != teacher.id && it.name.trim().equals(editName.trim(), ignoreCase = true)
    }
    AlertDialog(
      onDismissRequest = { editingTeacher = null },
      title = { Text("✏️ Edit Teacher", fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = editName,
            onValueChange = { editName = it },
            label = { Text("Teacher Name *") },
            isError = editDuplicate,
            supportingText = { if (editDuplicate) Text("This teacher already exists") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = editPhone,
            onValueChange = { editPhone = it },
            label = { Text("Phone Number") },
            placeholder = { Text("+998 90 123 45 67") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = editLink,
            onValueChange = { editLink = it },
            label = { Text("Telegram Link") },
            placeholder = { Text("https://t.me/username") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
          Text(
            text = "Updating teacher info will automatically apply everywhere this teacher is assigned.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (editName.isNotBlank() && !editDuplicate) {
              viewModel.updateTeacher(teacher.id, editName, editPhone, editLink)
              editingTeacher = null
            }
          },
          enabled = editName.isNotBlank() && !editDuplicate
        ) {
          Text("Save Changes")
        }
      },
      dismissButton = {
        TextButton(onClick = { editingTeacher = null }) {
          Text("Cancel")
        }
      }
    )
  }

  // Assign to Groups Dialog
  assigningTeacher?.let { teacher ->
    val selectedGroupNames = remember { 
      mutableStateListOf<String>().apply {
        addAll(state.groups.filter { it.teacherName == teacher.name }.map { it.name })
      }
    }
    
    AlertDialog(
      onDismissRequest = { assigningTeacher = null },
      title = { 
        Column {
          Text("👨‍🏫 Teacher:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
          Text(teacher.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
          Spacer(modifier = Modifier.height(4.dp))
          Text("📞 ${if(teacher.phoneNumber.isNotBlank()) teacher.phoneNumber else "No phone"}", style = MaterialTheme.typography.bodySmall)
          Text("🔗 ${if(teacher.telegramLink.isNotBlank()) teacher.telegramLink else "No link"}", style = MaterialTheme.typography.bodySmall)
        }
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text("Select Groups:", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
          LazyColumn(
            modifier = Modifier.heightIn(max = 300.dp)
          ) {
            items(state.groups) { group ->
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 4.dp)
              ) {
                Checkbox(
                  checked = selectedGroupNames.contains(group.name),
                  onCheckedChange = { checked ->
                    if (checked) {
                      selectedGroupNames.add(group.name)
                    } else {
                      selectedGroupNames.remove(group.name)
                    }
                  }
                )
                Text(group.name, modifier = Modifier.padding(start = 8.dp))
              }
            }
          }
        }
      },
      confirmButton = {
        Button(onClick = {
          viewModel.assignTeacherToGroups(teacher.id, selectedGroupNames.toList())
          assigningTeacher = null
        }) {
          Text("Save Groups")
        }
      },
      dismissButton = {
        TextButton(onClick = { assigningTeacher = null }) {
          Text("Cancel")
        }
      }
    )
  }

  // Delete Teacher Confirmation
  deletingTeacher?.let { teacher ->
    AlertDialog(
      onDismissRequest = { deletingTeacher = null },
      title = { Text("Delete Teacher?", fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Are you sure you want to delete '${teacher.name}'?")
          Text(
            "Groups taught by this teacher will be left without a teacher.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            viewModel.deleteTeacher(teacher.id)
            deletingTeacher = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
          Text("Delete")
        }
      },
      dismissButton = {
        TextButton(onClick = { deletingTeacher = null }) {
          Text("Cancel")
        }
      }
    )
  }
}
