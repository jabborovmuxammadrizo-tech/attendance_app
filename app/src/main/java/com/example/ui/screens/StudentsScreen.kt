package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SortOrder
import com.example.model.Student
import com.example.ui.AttendanceUiState
import com.example.ui.AttendanceViewModel
import com.example.ui.theme.PresentGreen
import com.example.ui.theme.PresentGreenContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentsScreen(
  state: AttendanceUiState,
  viewModel: AttendanceViewModel,
  modifier: Modifier = Modifier
) {
  var showAddStudentDialog by remember { mutableStateOf(false) }
  var newStudentName by remember { mutableStateOf("") }
  var newStudentID by remember { mutableStateOf("") }
  var chosenGroupForNewStudent by remember { mutableStateOf<String?>(null) }
  // Default to the group currently being filtered (if any), else the first group.
  val selectedGroupForNewStudent: String? = chosenGroupForNewStudent?.takeIf { c -> state.groups.any { it.name == c } }
    ?: state.studentSelectedGroupFilter.takeIf { f -> f != "ALL" && state.groups.any { it.name == f } }
    ?: state.groups.firstOrNull()?.name
  var groupDropdownExpanded by remember { mutableStateOf(false) }
  var sortDropdownExpanded by remember { mutableStateOf(false) }

  var editingStudent by remember { mutableStateOf<Student?>(null) }
  var movingStudent by remember { mutableStateOf<Student?>(null) }
  var addingToGroupStudent by remember { mutableStateOf<Student?>(null) }
  var deletingStudent by remember { mutableStateOf<Student?>(null) }

  // Filter and sort students
  val filteredStudents = remember(state.students, state.studentSearchQuery, state.studentSelectedGroupFilter, state.studentSortOrder) {
    val filtered = state.students.filter { student ->
      val matchesSearch = state.studentSearchQuery.isBlank() ||
        student.name.contains(state.studentSearchQuery, ignoreCase = true) ||
        student.studentIdNumber.contains(state.studentSearchQuery, ignoreCase = true)

      val matchesGroup = state.studentSelectedGroupFilter == "ALL" ||
        student.groupName.equals(state.studentSelectedGroupFilter, ignoreCase = true)

      matchesSearch && matchesGroup
    }

    when (state.studentSortOrder) {
      SortOrder.ASCENDING -> filtered.sortedBy { it.name }
      SortOrder.DESCENDING -> filtered.sortedByDescending { it.name }
      SortOrder.ORIGINAL -> filtered
    }
  }

  Scaffold(
    modifier = modifier.testTag("students_screen"),
    floatingActionButton = {
      FloatingActionButton(
        onClick = { showAddStudentDialog = true },
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = Color.White,
        modifier = Modifier.testTag("btn_fab_add_student")
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 16.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(Icons.Default.Add, contentDescription = "Add Student")
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "ADD STUDENT",
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
      // Search Bar & Sort Row
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          OutlinedTextField(
            value = state.studentSearchQuery,
            onValueChange = { viewModel.setStudentSearchQuery(it) },
            modifier = Modifier
              .weight(1f)
              .testTag("input_search_student"),
            placeholder = { Text("Search by name or ID...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            trailingIcon = {
              if (state.studentSearchQuery.isNotEmpty()) {
                IconButton(onClick = { viewModel.setStudentSearchQuery("") }) {
                  Icon(Icons.Default.Clear, contentDescription = "Clear")
                }
              }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
          )

          Box {
            OutlinedIconButton(
              onClick = { sortDropdownExpanded = true },
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.size(56.dp)
            ) {
              Icon(Icons.Default.Sort, contentDescription = "Sort")
            }
            DropdownMenu(
              expanded = sortDropdownExpanded,
              onDismissRequest = { sortDropdownExpanded = false }
            ) {
              DropdownMenuItem(
                text = { Text("Original Order") },
                onClick = {
                  viewModel.setStudentSortOrder(SortOrder.ORIGINAL)
                  sortDropdownExpanded = false
                }
              )
              DropdownMenuItem(
                text = { Text("Sort A → Z") },
                onClick = {
                  viewModel.setStudentSortOrder(SortOrder.ASCENDING)
                  sortDropdownExpanded = false
                }
              )
              DropdownMenuItem(
                text = { Text("Sort Z → A") },
                onClick = {
                  viewModel.setStudentSortOrder(SortOrder.DESCENDING)
                  sortDropdownExpanded = false
                }
              )
            }
          }
        }
      }

      // Group Filter Chips Row
      item {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          item {
            FilterChip(
              selected = state.studentSelectedGroupFilter == "ALL",
              onClick = { viewModel.setStudentGroupFilter("ALL") },
              label = { Text("All (${state.students.size})") }
            )
          }
          items(state.groups) { group ->
            FilterChip(
              selected = state.studentSelectedGroupFilter == group.name,
              onClick = { viewModel.setStudentGroupFilter(group.name) },
              label = { Text(group.name) }
            )
          }
        }
      }

      // Students List Count
      item {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Enrolled Students (${filteredStudents.size})",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
          )
          Button(
            onClick = { showAddStudentDialog = true },
            modifier = Modifier.testTag("btn_add_student_top"),
            shape = RoundedCornerShape(10.dp)
          ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Add Student")
          }
        }
      }

      // Student Items
      if (filteredStudents.isEmpty()) {
        item {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 48.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "No students match the criteria",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      } else {
        items(filteredStudents, key = { it.id }) { student ->
          StudentDirectoryCard(
            student = student,
            attendancePercent = state.studentStats[student.id]?.let { stat ->
              if (stat.totalMarks > 0) (stat.presentMarks * 100) / stat.totalMarks else null
            },
            onEdit = { editingStudent = student },
            onMove = { movingStudent = student },
            onAddToGroup = { addingToGroupStudent = student },
            onDelete = { deletingStudent = student }
          )
        }
      }
    }

    // Add Student Dialog Form
    if (showAddStudentDialog) {
      AlertDialog(
        onDismissRequest = { showAddStudentDialog = false },
        title = { Text("Add New Student", fontWeight = FontWeight.Bold) },
        text = {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
              value = newStudentName,
              onValueChange = { newStudentName = it },
              label = { Text("Full Name *") },
              placeholder = { Text("e.g. Aliyev Muhammad") },
              singleLine = true,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("input_student_name")
            )
            OutlinedTextField(
              value = newStudentID,
              onValueChange = { newStudentID = it },
              label = { Text("Student ID (Optional)") },
              placeholder = { Text("e.g. STD-0109") },
              singleLine = true,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("input_student_id")
            )

            Text(text = "Assign to Group:", style = MaterialTheme.typography.labelMedium)
            if (selectedGroupForNewStudent == null) {
              Text(
                text = "Create a group first (Groups → Add).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
              )
            }
            Box(modifier = Modifier.fillMaxWidth()) {
              OutlinedButton(
                onClick = { groupDropdownExpanded = true },
                enabled = state.groups.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
              ) {
                Text(
                  text = selectedGroupForNewStudent ?: "No groups",
                  style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
              }
              DropdownMenu(
                expanded = groupDropdownExpanded,
                onDismissRequest = { groupDropdownExpanded = false }
              ) {
                state.groups.forEach { group ->
                  DropdownMenuItem(
                    text = { Text(group.name) },
                    onClick = {
                      chosenGroupForNewStudent = group.name
                      groupDropdownExpanded = false
                    }
                  )
                }
              }
            }
          }
        },
        confirmButton = {
          Button(
            onClick = {
              val targetGroup = selectedGroupForNewStudent
              if (newStudentName.isNotBlank() && targetGroup != null) {
                viewModel.addStudent(
                  name = newStudentName,
                  studentIdNumber = newStudentID,
                  groupName = targetGroup
                )
                newStudentName = ""
                newStudentID = ""
                showAddStudentDialog = false
              }
            },
            enabled = newStudentName.isNotBlank() && selectedGroupForNewStudent != null,
            modifier = Modifier.testTag("btn_save_student")
          ) {
            Text("Add Student")
          }
        },
        dismissButton = {
          TextButton(onClick = { showAddStudentDialog = false }) {
            Text("Cancel")
          }
        }
      )
    }

    // Edit Student Dialog
    editingStudent?.let { student ->
      var editName by remember { mutableStateOf(student.name) }
      AlertDialog(
        onDismissRequest = { editingStudent = null },
        title = { Text("✏️ Edit Student Name", fontWeight = FontWeight.Bold) },
        text = {
          OutlinedTextField(
            value = editName,
            onValueChange = { editName = it },
            label = { Text("Student Full Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
        },
        confirmButton = {
          Button(onClick = {
            if (editName.isNotBlank()) {
              viewModel.updateStudent(student.id, editName)
              editingStudent = null
            }
          }) {
            Text("Save Changes")
          }
        },
        dismissButton = {
          TextButton(onClick = { editingStudent = null }) {
            Text("Cancel")
          }
        }
      )
    }

    // Move Student Dialog
    movingStudent?.let { student ->
      var targetGroup by remember {
        mutableStateOf(state.groups.firstOrNull { it.name != student.groupName }?.name ?: student.groupName)
      }
      var moveDropdownExpanded by remember { mutableStateOf(false) }

      AlertDialog(
        onDismissRequest = { movingStudent = null },
        title = { Text("↔️ Move Student to Another Group", fontWeight = FontWeight.Bold) },
        text = {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Move '${student.name}' from ${student.groupName} to:")
            Box(modifier = Modifier.fillMaxWidth()) {
              OutlinedButton(
                onClick = { moveDropdownExpanded = true },
                modifier = Modifier.fillMaxWidth()
              ) {
                Text(targetGroup)
              }
              DropdownMenu(
                expanded = moveDropdownExpanded,
                onDismissRequest = { moveDropdownExpanded = false }
              ) {
                state.groups.forEach { g ->
                  DropdownMenuItem(
                    text = { Text(g.name) },
                    onClick = {
                      targetGroup = g.name
                      moveDropdownExpanded = false
                    }
                  )
                }
              }
            }
          }
        },
        confirmButton = {
          Button(
            onClick = {
              viewModel.moveStudent(student.id, targetGroup)
              movingStudent = null
            },
            enabled = targetGroup != student.groupName
          ) {
            Text("Move Student")
          }
        },
        dismissButton = {
          TextButton(onClick = { movingStudent = null }) {
            Text("Cancel")
          }
        }
      )
    }

    // Add Student to Another Group Dialog
    addingToGroupStudent?.let { student ->
      var additionalGroup by remember {
        mutableStateOf(state.groups.firstOrNull { it.name != student.groupName }?.name ?: "")
      }
      var addDropdownExpanded by remember { mutableStateOf(false) }

      AlertDialog(
        onDismissRequest = { addingToGroupStudent = null },
        title = { Text("➕ Add to Another Group", fontWeight = FontWeight.Bold) },
        text = {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Enroll '${student.name}' in an additional group:")
            Box(modifier = Modifier.fillMaxWidth()) {
              OutlinedButton(
                onClick = { addDropdownExpanded = true },
                modifier = Modifier.fillMaxWidth()
              ) {
                Text(additionalGroup)
              }
              DropdownMenu(
                expanded = addDropdownExpanded,
                onDismissRequest = { addDropdownExpanded = false }
              ) {
                state.groups.forEach { g ->
                  DropdownMenuItem(
                    text = { Text(g.name) },
                    onClick = {
                      additionalGroup = g.name
                      addDropdownExpanded = false
                    }
                  )
                }
              }
            }
          }
        },
        confirmButton = {
          Button(
            onClick = {
              viewModel.addStudentToAnotherGroup(student.id, additionalGroup)
              addingToGroupStudent = null
            },
            enabled = additionalGroup.isNotBlank() && additionalGroup != student.groupName
          ) {
            Text("Enroll in Group")
          }
        },
        dismissButton = {
          TextButton(onClick = { addingToGroupStudent = null }) {
            Text("Cancel")
          }
        }
      )
    }

    // Delete Student Confirmation Dialog
    deletingStudent?.let { student ->
      AlertDialog(
        onDismissRequest = { deletingStudent = null },
        title = { Text("Remove Student?", fontWeight = FontWeight.Bold) },
        text = {
          Text("Remove '${student.name}' from ${student.groupName}? Past attendance sessions keep their saved names.")
        },
        confirmButton = {
          Button(
            onClick = {
              viewModel.removeStudentFromGroup(student.id)
              deletingStudent = null
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
          ) {
            Text("Remove")
          }
        },
        dismissButton = {
          TextButton(onClick = { deletingStudent = null }) {
            Text("Cancel")
          }
        }
      )
    }
  }
}

@Composable
private fun StudentDirectoryCard(
  student: Student,
  attendancePercent: Int?,
  onEdit: () -> Unit,
  onMove: () -> Unit,
  onAddToGroup: () -> Unit,
  onDelete: () -> Unit
) {
  var menuExpanded by remember { mutableStateOf(false) }

  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.weight(1f)
      ) {
        Box(
          modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
          )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
          Text(
            text = student.name,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
          )
          Spacer(modifier = Modifier.height(2.dp))
          Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
              shape = RoundedCornerShape(4.dp),
              color = MaterialTheme.colorScheme.surfaceVariant
            ) {
              Text(
                text = student.groupName,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
              )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = student.studentIdNumber,
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (attendancePercent != null) {
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "• $attendancePercent% attendance",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                color = if (attendancePercent >= 80) PresentGreen else MaterialTheme.colorScheme.error
              )
            }
          }
        }
      }

      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        // Edit ✏️
        IconButton(onClick = onEdit, modifier = Modifier.size(30.dp)) {
          Text("✏️", fontSize = 12.sp)
        }
        // Move ↔️
        IconButton(onClick = onMove, modifier = Modifier.size(30.dp)) {
          Text("↔️", fontSize = 12.sp)
        }
        // Add ➕
        IconButton(onClick = onAddToGroup, modifier = Modifier.size(30.dp)) {
          Text("➕", fontSize = 12.sp)
        }
        // Delete 🗑
        IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
          Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = "Remove",
            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
            modifier = Modifier.size(16.dp)
          )
        }
      }
    }
  }
}
