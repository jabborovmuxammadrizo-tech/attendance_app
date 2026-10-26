package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.Screen
import com.example.ui.AttendanceUiState
import com.example.ui.AttendanceViewModel
import com.example.utils.normalizeGroupKey

data class ParsedData(
  val groupName: String = "",
  val teacherName: String = "",
  val subjectName: String = "",
  val students: List<String> = emptyList()
)

fun parseTelegramMessage(text: String): ParsedData {
  val lines = text.lines()
  var groupName = ""
  var teacherName = ""
  var subjectName = ""
  val students = mutableListOf<String>()
  
  var parseState = 0 // 0: looking for metadata, 1: looking for students
  
  for (i in lines.indices) {
    val line = lines[i].trim()
    if (line.isEmpty()) continue
    
    // Group detection
    if (line.contains("GURUXI:", ignoreCase = true) || line.contains("Group:", ignoreCase = true)) {
      if (i + 1 < lines.size) {
        groupName = lines[i+1].trim()
      }
    } else if (line.startsWith("#") && groupName.isEmpty()) {
      groupName = line
    }
    
    // Teacher detection
    if (line.contains("O'qituvchi:", ignoreCase = true) || line.contains("Teacher:", ignoreCase = true)) {
      if (i + 1 < lines.size) {
        val nextLine = lines[i+1].trim()
        if (!nextLine.matches(Regex("^\\d+.*"))) {
          teacherName = nextLine
        }
      }
    }
    
    // Subject detection
    if (line.contains("Fan:", ignoreCase = true) || line.contains("Subject:", ignoreCase = true)) {
      if (i + 1 < lines.size) {
        val nextLine = lines[i+1].trim()
        if (!nextLine.matches(Regex("^\\d+.*"))) {
          subjectName = nextLine
        }
      }
    }
    
    // Student detection: Starts with digit, optional punctuation, then name, then optional emojis
    if (line.firstOrNull()?.isDigit() == true) {
      // Remove leading digits and punctuation
      var name = line.replaceFirst(Regex("^\\d+[\\.\\)\\-]?\\s*"), "")
      // Remove trailing attendance emojis and whitespaces
      name = name.replace(Regex("[✅❌✈️❓]+"), "").trim()
      if (name.isNotEmpty() && name != "Izohlar:" && !name.contains("darsda qatnashmoqda")) {
        students.add(name)
      }
    }
  }
  
  // Reports created by this app print "Not specified" when a group has no teacher.
  if (teacherName.equals("Not specified", ignoreCase = true)) teacherName = ""

  return ParsedData(groupName, teacherName, subjectName, students.distinctBy { it.lowercase() })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportStudentsScreen(
  state: AttendanceUiState,
  viewModel: AttendanceViewModel,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  var pasteText by remember { mutableStateOf("") }
  var manualGroupName by remember { mutableStateOf("") }
  var parsedData by remember { mutableStateOf<ParsedData?>(null) }
  val targetGroup = state.groups.find { it.id == state.targetGroupForImport }
  
  if (parsedData == null) {
    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
      Text("Paste Telegram Message", style = MaterialTheme.typography.titleLarge)
      Spacer(modifier = Modifier.height(8.dp))
      OutlinedTextField(
        value = pasteText,
        onValueChange = { pasteText = it },
        modifier = Modifier.fillMaxWidth().weight(1f),
        placeholder = { Text("Paste the whole attendance message here...") },
        shape = RoundedCornerShape(12.dp)
      )
      Spacer(modifier = Modifier.height(16.dp))
      Button(
        onClick = { parsedData = parseTelegramMessage(pasteText) },
        modifier = Modifier.fillMaxWidth(),
        enabled = pasteText.isNotBlank()
      ) {
        Text("Parse Message")
      }
    }
  } else {
    val data = parsedData!!
    // Importing from a group's own page uses that group. Otherwise match the message's group
    // against existing ones ("#GENERAL_7" == "GENERAL 7") before creating a new group.
    val matchedGroup = state.groups.firstOrNull { normalizeGroupKey(it.name) == normalizeGroupKey(data.groupName) }
    val resolvedGroupName = when {
      targetGroup != null -> targetGroup.name
      matchedGroup != null -> matchedGroup.name
      data.groupName.isNotBlank() -> data.groupName
      else -> manualGroupName.trim()
    }
    val existingStudentsInGroup = state.students
      .filter { normalizeGroupKey(it.groupName) == normalizeGroupKey(resolvedGroupName) }
      .map { it.name.trim().lowercase() }
    
    val newStudents = data.students.filter { it.trim().lowercase() !in existingStudentsInGroup }
    val duplicateCount = data.students.size - newStudents.size
    
    var selectedStudents by remember { mutableStateOf(newStudents.toSet()) }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
      Text("IMPORT PREVIEW", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
      Spacer(modifier = Modifier.height(8.dp))
      
      Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text("👥 Group: ${if (resolvedGroupName.isBlank()) "Unknown" else resolvedGroupName}", fontWeight = FontWeight.Bold)
          if (targetGroup == null && matchedGroup == null && resolvedGroupName.isNotBlank()) {
            Text("New group — it will be created", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
          }
          if (data.teacherName.isNotBlank()) Text("👨\u200D🏫 Teacher: ${data.teacherName}")
          if (data.subjectName.isNotBlank()) Text("📚 Subject: ${data.subjectName}")
          HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
          Text("👥 Students found: ${data.students.size}")
          Text("New students: ${newStudents.size}", color = MaterialTheme.colorScheme.primary)
          Text("Already in group: $duplicateCount", color = MaterialTheme.colorScheme.error)
        }
      }
      
      if (targetGroup == null && data.groupName.isBlank()) {
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
          value = manualGroupName,
          onValueChange = { manualGroupName = it },
          label = { Text("Group name (not found in the message)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
      }

      Spacer(modifier = Modifier.height(8.dp))
      
      LazyColumn(modifier = Modifier.weight(1f)) {
        item {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("Select Students to Import:", fontWeight = FontWeight.Bold)
            TextButton(onClick = { 
              if (selectedStudents.size == newStudents.size) selectedStudents = emptySet()
              else selectedStudents = newStudents.toSet()
            }) {
              Text(if (selectedStudents.size == newStudents.size) "Deselect All" else "Select All")
            }
          }
        }
        items(newStudents) { studentName ->
          Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Checkbox(
              checked = selectedStudents.contains(studentName),
              onCheckedChange = { checked ->
                if (checked) selectedStudents = selectedStudents + studentName
                else selectedStudents = selectedStudents - studentName
              }
            )
            Text(studentName)
          }
        }
      }
      
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
          onClick = { parsedData = null },
          modifier = Modifier.weight(1f)
        ) {
          Text("Cancel")
        }
        Button(
          onClick = {
            if (resolvedGroupName.isNotBlank()) {
              val importedGroup = resolvedGroupName
              viewModel.importStudents(
                resolvedGroupName = importedGroup,
                teacherName = data.teacherName,
                subjectName = data.subjectName,
                studentNames = selectedStudents.toList(),
                onDone = { added ->
                  Toast.makeText(context, "Imported $added students into $importedGroup", Toast.LENGTH_LONG).show()
                }
              )
              // Go back to where the import was started (Groups or the group's page).
              viewModel.onBackPress()
            }
          },
          modifier = Modifier.weight(1f),
          enabled = selectedStudents.isNotEmpty() && resolvedGroupName.isNotBlank()
        ) {
          Text("Import Selected")
        }
      }
    }
  }
}
