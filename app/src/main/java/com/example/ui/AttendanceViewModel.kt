package com.example.ui

import android.app.Application
import android.content.Context
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppBackup
import com.example.data.local.AppBackupSummary
import com.example.data.local.AppDatabase
import com.example.data.local.BackupGroupDto
import com.example.data.local.BackupRecordDto
import com.example.data.local.BackupSessionDto
import com.example.data.local.BackupStudentDto
import com.example.data.local.BackupTeacherDto
import com.example.data.local.GroupEntity
import com.example.data.local.RecordEntity
import com.example.data.local.SessionEntity
import com.example.data.local.SettingsEntity
import com.example.data.local.StudentEntity
import com.example.data.local.StudentStat
import com.example.data.local.TeacherEntity
import com.example.model.AttendanceSession
import com.example.model.AttendanceStatus
import com.example.model.GroupItem
import com.example.model.Screen
import com.example.model.SortOrder
import com.example.model.Student
import com.example.model.StudentAttendanceRecord
import com.example.model.StudentAttendanceState
import com.example.model.Teacher
import com.example.utils.DateUtils
import com.example.utils.csvEscaped
import com.example.utils.normalizeGroupKey
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AttendanceUiState(
  val currentScreen: Screen = Screen.HOME,
  val groups: List<GroupItem> = emptyList(),
  val students: List<Student> = emptyList(),
  val teachers: List<Teacher> = emptyList(),
  val historySessions: List<AttendanceSession> = emptyList(),
  /** studentId -> real attendance numbers computed from saved records. */
  val studentStats: Map<String, StudentStat> = emptyMap(),

  // Active Attendance State
  val selectedGroup: String = "",
  val activeDate: String = "",
  val activeTime: String = "",
  val activeAttendanceList: List<StudentAttendanceState> = emptyList(),
  val attendanceSearchQuery: String = "",
  val showAttendanceSummaryDialog: Boolean = false,
  val lastSavedSummaryMessage: String? = null,

  // Students / Groups / Teachers screens: filter & search
  val studentSearchQuery: String = "",
  val studentSelectedGroupFilter: String = "ALL",
  val targetGroupForImport: String? = null,
  val groupSearchQuery: String = "",
  val teacherSearchQuery: String = "",
  val studentSortOrder: SortOrder = SortOrder.ORIGINAL,

  // Settings (persisted in SharedPreferences, except the report link which lives in the DB)
  val selectedTheme: String = "System", // Light, Dark, System
  val defaultAllPresent: Boolean = true,
  val autoSaveEnabled: Boolean = true,
  val appLockEnabled: Boolean = false,
  val isLocked: Boolean = false,
  val reportTelegramLink: String = "https://t.me/High_Educator_1",

  // Selected History Session for Inspection
  val inspectingSession: AttendanceSession? = null,

  // Import Preview State
  val pendingImportJson: String? = null,
  val pendingImportPreview: AppBackup? = null
)

class AttendanceViewModel(application: Application) : AndroidViewModel(application) {
  private val dao = AppDatabase.getDatabase(application).appDao()
  private val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
  private val moshi: Moshi by lazy { Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build() }

  var pendingBackupString: String? = null
  var pendingBackupPreview: AppBackup? = null

  private val _uiState = MutableStateFlow(
    AttendanceUiState(
      selectedTheme = prefs.getString(KEY_THEME, "System") ?: "System",
      defaultAllPresent = prefs.getBoolean(KEY_DEFAULT_PRESENT, true),
      autoSaveEnabled = prefs.getBoolean(KEY_AUTOSAVE, true),
      appLockEnabled = prefs.getBoolean(KEY_APP_LOCK, false),
      isLocked = prefs.getBoolean(KEY_APP_LOCK, false)
    )
  )
  val uiState: StateFlow<AttendanceUiState> = _uiState.asStateFlow()

  private val backStack = mutableListOf(Screen.HOME)
  private var lastStopTime = 0L

  init {
    refreshDateTime()
    viewModelScope.launch {
      // Demo data is inserted once, on the very first launch only. Previously it was
      // re-inserted every time the groups table happened to be empty.
      if (!prefs.getBoolean(KEY_SEEDED, false)) {
        if (dao.getBackupGroups().isEmpty() && dao.getBackupTeachers().isEmpty()) {
          insertInitialData()
        }
        prefs.edit().putBoolean(KEY_SEEDED, true).apply()
      }
      observeDatabase()
      observeStudentStats()
    }
  }

  private suspend fun insertInitialData() {
    val t1 = TeacherEntity("tch_1", "Sobirjonov Mirkamol", "+998901234567", "https://t.me/mirkamol")
    val t2 = TeacherEntity("tch_2", "Abdulloyev Aziz", "", "")
    val t3 = TeacherEntity("tch_3", "Karimov Sardor", "", "")
    listOf(t1, t2, t3).forEach { dao.insertTeacher(it) }

    val g1 = GroupEntity("grp_1", "GENERAL 5", "English", "tch_1", false)
    val g2 = GroupEntity("grp_2", "GENERAL 6", "English", "tch_2", false)
    val g3 = GroupEntity("grp_3", "GENERAL 7", "English", "tch_1", true)
    val g4 = GroupEntity("grp_4", "ELEMENTARY BOYS", "English", null, false)
    val g5 = GroupEntity("grp_5", "PRE-INTERMEDIATE", "Grammar", null, true)
    listOf(g1, g2, g3, g4, g5).forEach { dao.insertGroup(it) }

    val sList = listOf(
      StudentEntity("std_1", "Aliyev Muhammad", "STD-0101", "grp_3", "Male", 92),
      StudentEntity("std_2", "Karimov Aziz", "STD-0102", "grp_3", "Male", 88),
      StudentEntity("std_3", "Sobirov Bek", "STD-0103", "grp_3", "Male", 95),
      StudentEntity("std_4", "Abdullayev Hasan", "STD-0104", "grp_3", "Male", 85),
      StudentEntity("std_5", "Ismoilov Muhammad", "STD-0105", "grp_3", "Male", 90),
      StudentEntity("std_6", "Nazarov Timur", "STD-0106", "grp_3", "Male", 94),
      StudentEntity("std_7", "Rustamov Jasur", "STD-0107", "grp_3", "Male", 80),
      StudentEntity("std_8", "Yoqubov Farhod", "STD-0108", "grp_3", "Male", 86),
      StudentEntity("std_9", "Toshmatov Otabek", "STD-0201", "grp_1", "Male", 91),
      StudentEntity("std_10", "Qodirov Anvar", "STD-0202", "grp_1", "Male", 89),
      StudentEntity("std_11", "Jumayev Dilshod", "STD-0301", "grp_5", "Male", 96),
      StudentEntity("std_12", "Rahmonov Botir", "STD-0302", "grp_5", "Male", 84)
    )
    sList.forEach { dao.insertStudent(it) }

    dao.insertSettings(SettingsEntity(1, "https://t.me/High_Educator_1"))
  }

  private fun observeDatabase() {
    viewModelScope.launch {
      combine(
        dao.getAllTeachers(),
        dao.getAllGroups(),
        dao.getAllStudents(),
        dao.getAllSessions(),
        dao.getSettings()
      ) { teachers, groups, students, sessions, settings ->
        val groupCountMap = groups.groupBy { it.teacherId }.mapValues { it.value.size }
        val mappedTeachers = teachers.map { t ->
          Teacher(t.id, t.name, t.phoneNumber, t.telegramLink, groupCountMap[t.id] ?: 0)
        }
        val studentCountMap = students.groupBy { it.groupId }.mapValues { it.value.size }
        val teacherNameMap = teachers.associateBy({ it.id }, { it.name })
        val groupNameMap = groups.associateBy({ it.id }, { it.name })

        val mappedGroups = groups.map { g ->
          GroupItem(
            id = g.id,
            name = g.name,
            teacherName = teacherNameMap[g.teacherId] ?: "",
            subjectName = g.subjectName,
            isFavorite = g.isFavorite,
            studentCount = studentCountMap[g.id] ?: 0
          )
        }
        val groupById = mappedGroups.associateBy { it.id }

        val mappedStudents = students.map { s ->
          Student(s.id, s.name, s.studentIdNumber, groupNameMap[s.groupId] ?: "Unknown", s.gender, s.attendanceRate)
        }

        val mappedSessions = sessions.map { s ->
          val group = groupById[s.groupId]
          AttendanceSession(
            id = s.id,
            groupId = s.groupId,
            groupName = group?.name ?: "Deleted group",
            subjectName = group?.subjectName ?: "",
            teacherName = group?.teacherName ?: "",
            date = s.date,
            time = s.time,
            totalStudents = s.totalStudents,
            presentCount = s.presentCount,
            lateCount = s.lateCount,
            leftCount = s.leftCount,
            otherGroupCount = s.otherGroupCount,
            timestamp = DateUtils.toMillis(s.date, s.time)
          )
        }.sortedWith(compareByDescending<AttendanceSession> { it.timestamp }.thenByDescending { it.id })

        _uiState.update { current ->
          val selected = when {
            mappedGroups.any { it.name == current.selectedGroup } -> current.selectedGroup
            else -> (mappedGroups.firstOrNull { it.isFavorite } ?: mappedGroups.firstOrNull())?.name ?: ""
          }
          val groupChanged = selected != current.selectedGroup
          val keepStatuses = if (groupChanged) {
            emptyMap()
          } else {
            current.activeAttendanceList.associate { it.student.id to it.status }
          }
          current.copy(
            teachers = mappedTeachers,
            groups = mappedGroups,
            students = mappedStudents,
            historySessions = mappedSessions,
            reportTelegramLink = settings?.reportTelegramLink ?: "https://t.me/High_Educator_1",
            selectedGroup = selected,
            // Keep marks the teacher already made, but pick up added / removed / renamed students.
            activeAttendanceList = buildActiveList(
              groupName = selected,
              groups = mappedGroups,
              students = mappedStudents,
              existing = keepStatuses,
              useDraft = groupChanged || current.activeAttendanceList.isEmpty(),
              defaultStatus = defaultStatus(current)
            )
          )
        }
      }.collect {}
    }
  }

  private fun observeStudentStats() {
    viewModelScope.launch {
      dao.getStudentStats().collect { stats ->
        _uiState.update { it.copy(studentStats = stats.associateBy { s -> s.studentId }) }
      }
    }
  }

  // ---------------------------------------------------------------------------------------
  // Date / time, navigation, app lock
  // ---------------------------------------------------------------------------------------

  fun refreshDateTime() {
    _uiState.update { it.copy(activeDate = DateUtils.today(), activeTime = DateUtils.nowTime()) }
  }

  private fun goTo(screen: Screen) {
    val existing = backStack.indexOf(screen)
    if (existing >= 0) {
      while (backStack.size > existing + 1) backStack.removeAt(backStack.size - 1)
    } else {
      backStack.add(screen)
    }
    _uiState.update {
      it.copy(
        currentScreen = screen,
        inspectingSession = null,
        lastSavedSummaryMessage = if (screen == Screen.HISTORY) it.lastSavedSummaryMessage else null
      )
    }
  }

  fun navigateTo(screen: Screen) = goTo(screen)

  fun onBackPress(): Boolean {
    if (backStack.size > 1) {
      backStack.removeAt(backStack.size - 1)
      goTo(backStack.last())
      return true
    }
    return false
  }

  fun unlock() {
    _uiState.update { it.copy(isLocked = false) }
  }

  fun onAppStopped() {
    lastStopTime = SystemClock.elapsedRealtime()
  }

  fun onAppStarted() {
    if (_uiState.value.appLockEnabled && lastStopTime > 0L &&
      SystemClock.elapsedRealtime() - lastStopTime > LOCK_AFTER_MS
    ) {
      _uiState.update { it.copy(isLocked = true) }
    }
  }

  // ---------------------------------------------------------------------------------------
  // Taking attendance
  // ---------------------------------------------------------------------------------------

  private fun defaultStatus(state: AttendanceUiState): AttendanceStatus {
    return if (state.defaultAllPresent) AttendanceStatus.PRESENT else AttendanceStatus.LATE
  }

  private fun buildActiveList(
    groupName: String,
    groups: List<GroupItem>,
    students: List<Student>,
    existing: Map<String, AttendanceStatus>,
    useDraft: Boolean,
    defaultStatus: AttendanceStatus
  ): List<StudentAttendanceState> {
    if (groupName.isBlank()) return emptyList()
    val group = groups.firstOrNull { it.name == groupName }
    val draft = if (useDraft && group != null) loadDraft(group.id) else emptyMap()
    return students.filter { it.groupName == groupName }.map { student ->
      StudentAttendanceState(student, existing[student.id] ?: draft[student.id] ?: defaultStatus)
    }
  }

  private fun saveDraft() {
    val state = _uiState.value
    if (!state.autoSaveEnabled) return
    val group = state.groups.firstOrNull { it.name == state.selectedGroup } ?: return
    val body = state.activeAttendanceList.joinToString(";") { "${it.student.id}=${it.status.name}" }
    prefs.edit().putString(draftKey(group.id), DateUtils.today() + "|" + body).apply()
  }

  private fun loadDraft(groupId: String): Map<String, AttendanceStatus> {
    val raw = prefs.getString(draftKey(groupId), null) ?: return emptyMap()
    val parts = raw.split("|", limit = 2)
    // A draft is only valid for the day it was made.
    if (parts.size < 2 || parts[0] != DateUtils.today()) return emptyMap()
    val result = HashMap<String, AttendanceStatus>()
    parts[1].split(";").forEach { pair ->
      val kv = pair.split("=", limit = 2)
      if (kv.size == 2) {
        val status = try {
          AttendanceStatus.valueOf(kv[1])
        } catch (e: Exception) {
          null
        }
        if (status != null) result[kv[0]] = status
      }
    }
    return result
  }

  private fun clearDraft(groupId: String) {
    prefs.edit().remove(draftKey(groupId)).apply()
  }

  fun selectAttendanceGroup(groupName: String) {
    _uiState.update { s ->
      s.copy(
        selectedGroup = groupName,
        attendanceSearchQuery = "",
        activeAttendanceList = buildActiveList(
          groupName = groupName,
          groups = s.groups,
          students = s.students,
          existing = emptyMap(),
          useDraft = true,
          defaultStatus = defaultStatus(s)
        )
      )
    }
  }

  fun setStudentStatus(studentId: String, status: AttendanceStatus) {
    _uiState.update { state ->
      state.copy(
        activeAttendanceList = state.activeAttendanceList.map { item ->
          if (item.student.id == studentId) item.copy(status = status) else item
        }
      )
    }
    saveDraft()
  }

  fun markAllStudents(status: AttendanceStatus) {
    _uiState.update { state ->
      state.copy(activeAttendanceList = state.activeAttendanceList.map { it.copy(status = status) })
    }
    saveDraft()
  }

  fun setAttendanceSearchQuery(query: String) {
    _uiState.update { it.copy(attendanceSearchQuery = query) }
  }

  fun showSummaryDialog(show: Boolean) {
    _uiState.update { it.copy(showAttendanceSummaryDialog = show) }
  }

  fun finishAttendance() {
    refreshDateTime()
    val state = _uiState.value
    val list = state.activeAttendanceList
    val group = state.groups.firstOrNull { it.name == state.selectedGroup } ?: return
    if (list.isEmpty()) return

    val present = list.count { it.status == AttendanceStatus.PRESENT }
    val late = list.count { it.status == AttendanceStatus.LATE }
    val left = list.count { it.status == AttendanceStatus.LEFT }
    val other = list.count { it.status == AttendanceStatus.OTHER_GROUP }

    val sessionId = "ses_" + UUID.randomUUID().toString().take(8)
    val newSession = SessionEntity(
      id = sessionId,
      groupId = group.id,
      date = state.activeDate,
      time = state.activeTime,
      totalStudents = list.size,
      presentCount = present,
      lateCount = late,
      leftCount = left,
      otherGroupCount = other
    )
    val records = list.map {
      RecordEntity(
        sessionId = sessionId,
        studentId = it.student.id,
        studentName = it.student.name,
        status = it.status.name
      )
    }

    viewModelScope.launch { dao.insertSessionWithRecords(newSession, records) }
    clearDraft(group.id)

    // The attendance screen is replaced by History, so "back" returns to where you came from.
    if (backStack.lastOrNull() == Screen.ATTENDANCE) backStack.removeAt(backStack.size - 1)
    goTo(Screen.HISTORY)
    _uiState.update { s ->
      s.copy(
        showAttendanceSummaryDialog = false,
        lastSavedSummaryMessage = "Attendance saved for ${group.name} ($present/${list.size} present)",
        activeAttendanceList = buildActiveList(
          groupName = group.name,
          groups = s.groups,
          students = s.students,
          existing = emptyMap(),
          useDraft = false,
          defaultStatus = defaultStatus(s)
        )
      )
    }
  }

  fun clearLastSavedMessage() {
    _uiState.update { it.copy(lastSavedSummaryMessage = null) }
  }

  // ---------------------------------------------------------------------------------------
  // Teachers
  // ---------------------------------------------------------------------------------------

  /** Finds a teacher by (case-insensitive) name, creating one if needed. Blank name -> null. */
  private suspend fun resolveTeacherId(name: String): String? {
    val clean = name.trim()
    if (clean.isEmpty()) return null
    val existing = dao.getBackupTeachers().firstOrNull { it.name.equals(clean, ignoreCase = true) }
    if (existing != null) return existing.id
    val created = TeacherEntity("tch_" + UUID.randomUUID().toString().take(6), clean, "", "")
    dao.insertTeacher(created)
    return created.id
  }

  fun addTeacher(name: String, phoneNumber: String = "", telegramLink: String = "") {
    val clean = name.trim()
    if (clean.isEmpty()) return
    viewModelScope.launch {
      if (dao.getBackupTeachers().any { it.name.equals(clean, ignoreCase = true) }) return@launch
      dao.insertTeacher(
        TeacherEntity(
          id = "tch_" + UUID.randomUUID().toString().take(6),
          name = clean,
          phoneNumber = phoneNumber.trim(),
          telegramLink = telegramLink.trim()
        )
      )
    }
  }

  fun updateTeacher(teacherId: String, newName: String, phoneNumber: String, telegramLink: String) {
    val clean = newName.trim()
    if (clean.isEmpty()) return
    viewModelScope.launch {
      dao.updateTeacherInfo(teacherId, clean, phoneNumber.trim(), telegramLink.trim())
    }
  }

  fun assignTeacherToGroups(teacherId: String, groupNames: List<String>) {
    viewModelScope.launch {
      dao.getBackupGroups().forEach { g ->
        if (groupNames.contains(g.name)) {
          dao.updateGroupTeacher(g.id, teacherId)
        } else if (g.teacherId == teacherId) {
          dao.updateGroupTeacher(g.id, null)
        }
      }
    }
  }

  /** Deleting a teacher always un-assigns their groups (database FK = SET NULL). */
  fun deleteTeacher(teacherId: String) {
    viewModelScope.launch { dao.deleteTeacher(teacherId) }
  }

  // ---------------------------------------------------------------------------------------
  // Groups
  // ---------------------------------------------------------------------------------------

  fun addGroup(name: String, teacherName: String = "", subjectName: String = "") {
    val clean = name.trim()
    if (clean.isEmpty()) return
    viewModelScope.launch {
      if (dao.getBackupGroups().any { normalizeGroupKey(it.name) == normalizeGroupKey(clean) }) return@launch
      val teacherId = resolveTeacherId(teacherName)
      dao.insertGroup(
        GroupEntity(
          id = "grp_" + UUID.randomUUID().toString().take(6),
          name = clean,
          subjectName = subjectName.trim(),
          teacherId = teacherId,
          isFavorite = false
        )
      )
    }
  }

  fun updateGroup(groupId: String, newName: String, newTeacher: String, newSubject: String) {
    val clean = newName.trim()
    if (clean.isEmpty()) return
    viewModelScope.launch {
      val groups = dao.getBackupGroups()
      val old = groups.firstOrNull { it.id == groupId } ?: return@launch
      if (groups.any { it.id != groupId && normalizeGroupKey(it.name) == normalizeGroupKey(clean) }) return@launch
      val teacherId = resolveTeacherId(newTeacher)
      dao.updateGroupInfo(groupId, clean, newSubject.trim(), teacherId)
      // Keep screens that remember the group by name pointing at the renamed group.
      _uiState.update { s ->
        s.copy(
          selectedGroup = if (s.selectedGroup == old.name) clean else s.selectedGroup,
          studentSelectedGroupFilter = if (s.studentSelectedGroupFilter == old.name) clean else s.studentSelectedGroupFilter
        )
      }
    }
  }

  fun toggleGroupFavorite(groupId: String) {
    val group = _uiState.value.groups.firstOrNull { it.id == groupId } ?: return
    viewModelScope.launch { dao.setGroupFavorite(groupId, !group.isFavorite) }
  }

  /** Deletes the group, its students and its attendance history. */
  fun deleteGroup(groupId: String) {
    viewModelScope.launch {
      dao.deleteGroupWithHistory(groupId)
      clearDraft(groupId)
      _uiState.update { s ->
        val deleted = s.groups.firstOrNull { it.id == groupId }?.name
        s.copy(
          studentSelectedGroupFilter = if (deleted != null && s.studentSelectedGroupFilter == deleted) "ALL" else s.studentSelectedGroupFilter
        )
      }
    }
  }

  fun setGroupSearchQuery(query: String) {
    _uiState.update { it.copy(groupSearchQuery = query) }
  }

  fun setTeacherSearchQuery(query: String) {
    _uiState.update { it.copy(teacherSearchQuery = query) }
  }

  // ---------------------------------------------------------------------------------------
  // Students
  // ---------------------------------------------------------------------------------------

  private fun newStudentNumber(): String = "STD-" + UUID.randomUUID().toString().take(4).uppercase()

  fun setStudentSearchQuery(query: String) {
    _uiState.update { it.copy(studentSearchQuery = query) }
  }

  fun setTargetGroupForImport(groupId: String?) {
    _uiState.update { it.copy(targetGroupForImport = groupId) }
  }

  fun setStudentGroupFilter(group: String) {
    _uiState.update { it.copy(studentSelectedGroupFilter = group) }
  }

  fun setStudentSortOrder(order: SortOrder) {
    _uiState.update { it.copy(studentSortOrder = order) }
  }

  fun addStudent(name: String, studentIdNumber: String, groupName: String, gender: String = "Male") {
    val clean = name.trim()
    if (clean.isEmpty()) return
    viewModelScope.launch {
      val group = dao.getBackupGroups().firstOrNull { it.name == groupName } ?: return@launch
      val alreadyThere = dao.getBackupStudents().any {
        it.groupId == group.id && it.name.trim().equals(clean, ignoreCase = true)
      }
      if (alreadyThere) return@launch
      dao.insertStudent(
        StudentEntity(
          id = "std_" + UUID.randomUUID().toString().take(6),
          name = clean,
          studentIdNumber = if (studentIdNumber.isBlank()) newStudentNumber() else studentIdNumber.trim(),
          groupId = group.id,
          gender = gender,
          attendanceRate = 100
        )
      )
    }
  }

  fun updateStudent(studentId: String, newName: String) {
    val clean = newName.trim()
    if (clean.isEmpty()) return
    viewModelScope.launch { dao.updateStudentName(studentId, clean) }
  }

  fun moveStudent(studentId: String, newGroupName: String) {
    if (newGroupName.isBlank()) return
    viewModelScope.launch {
      val group = dao.getBackupGroups().firstOrNull { it.name == newGroupName } ?: return@launch
      dao.updateStudentGroup(studentId, group.id)
    }
  }

  /** Enrols a copy of the student in a second group (the original stays where it is). */
  fun addStudentToAnotherGroup(studentId: String, newGroupName: String) {
    if (newGroupName.isBlank()) return
    viewModelScope.launch {
      val student = dao.getBackupStudents().firstOrNull { it.id == studentId } ?: return@launch
      val group = dao.getBackupGroups().firstOrNull { it.name == newGroupName } ?: return@launch
      val alreadyThere = dao.getBackupStudents().any {
        it.groupId == group.id && it.name.trim().equals(student.name.trim(), ignoreCase = true)
      }
      if (alreadyThere) return@launch
      dao.insertStudent(
        StudentEntity(
          id = "std_" + UUID.randomUUID().toString().take(6),
          name = student.name,
          studentIdNumber = student.studentIdNumber,
          groupId = group.id,
          gender = student.gender,
          attendanceRate = student.attendanceRate
        )
      )
    }
  }

  fun removeStudentFromGroup(studentId: String) {
    viewModelScope.launch { dao.deleteStudent(studentId) }
  }

  /** Imports parsed names into a group (created if needed). [onDone] receives how many were added. */
  fun importStudents(
    resolvedGroupName: String,
    teacherName: String,
    subjectName: String,
    studentNames: List<String>,
    onDone: (Int) -> Unit = {}
  ) {
    viewModelScope.launch {
      val teacherId = resolveTeacherId(teacherName)
      val found = dao.getBackupGroups().firstOrNull {
        normalizeGroupKey(it.name) == normalizeGroupKey(resolvedGroupName)
      }
      val group = found ?: GroupEntity(
        id = "grp_" + UUID.randomUUID().toString().take(6),
        name = resolvedGroupName.trim(),
        subjectName = subjectName.trim(),
        teacherId = teacherId,
        isFavorite = false
      ).also { dao.insertGroup(it) }

      if (found != null) {
        // Only fill gaps - never overwrite a teacher / subject the user already set.
        val newTeacher = found.teacherId ?: teacherId
        val newSubject = found.subjectName.ifBlank { subjectName.trim() }
        if (newTeacher != found.teacherId || newSubject != found.subjectName) {
          dao.updateGroupInfo(found.id, found.name, newSubject, newTeacher)
        }
      }

      val existingNames = dao.getBackupStudents()
        .filter { it.groupId == group.id }
        .map { it.name.trim().lowercase() }
        .toMutableSet()
      var added = 0
      studentNames.forEach { raw ->
        val clean = raw.trim()
        if (clean.isNotEmpty() && existingNames.add(clean.lowercase())) {
          dao.insertStudent(
            StudentEntity(
              id = "std_" + UUID.randomUUID().toString().take(6),
              name = clean,
              studentIdNumber = newStudentNumber(),
              groupId = group.id,
              gender = "Male",
              attendanceRate = 100
            )
          )
          added++
        }
      }
      onDone(added)
    }
  }

  // ---------------------------------------------------------------------------------------
  // History
  // ---------------------------------------------------------------------------------------

  private fun parseStatus(value: String): AttendanceStatus {
    return try {
      AttendanceStatus.valueOf(value)
    } catch (e: Exception) {
      AttendanceStatus.PRESENT
    }
  }

  fun inspectSession(session: AttendanceSession?) {
    if (session == null) {
      _uiState.update { it.copy(inspectingSession = null) }
      return
    }
    viewModelScope.launch {
      val records = dao.getRecordsForSession(session.id).map {
        StudentAttendanceRecord(it.studentId, it.studentName, parseStatus(it.status))
      }
      _uiState.update { it.copy(inspectingSession = session.copy(records = records)) }
    }
  }

  fun updateSessionRecord(sessionId: String, studentId: String, newStatus: AttendanceStatus) {
    viewModelScope.launch {
      dao.updateRecordStatus(sessionId, studentId, newStatus.name)
      val records = dao.getRecordsForSession(sessionId).map {
        StudentAttendanceRecord(it.studentId, it.studentName, parseStatus(it.status))
      }
      val present = records.count { it.status == AttendanceStatus.PRESENT }
      val late = records.count { it.status == AttendanceStatus.LATE }
      val left = records.count { it.status == AttendanceStatus.LEFT }
      val other = records.count { it.status == AttendanceStatus.OTHER_GROUP }
      // UPDATE only the counters - re-inserting the session row would cascade-delete its records.
      dao.updateSessionCounts(sessionId, present, late, left, other)
      _uiState.update { s ->
        val current = s.inspectingSession
        if (current != null && current.id == sessionId) {
          s.copy(
            inspectingSession = current.copy(
              records = records,
              presentCount = present,
              lateCount = late,
              leftCount = left,
              otherGroupCount = other
            )
          )
        } else {
          s
        }
      }
    }
  }

  fun deleteSession(sessionId: String) {
    viewModelScope.launch {
      dao.deleteSession(sessionId)
      _uiState.update { it.copy(inspectingSession = null) }
    }
  }

  // ---------------------------------------------------------------------------------------
  // Reports
  // ---------------------------------------------------------------------------------------

  /** One CSV row per student mark for the given sessions (oldest first). */
  suspend fun buildCsv(sessions: List<AttendanceSession>): String {
    val byId = sessions.associateBy { it.id }
    val records = dao.getAllRecords().filter { byId.containsKey(it.sessionId) }.groupBy { it.sessionId }
    val sb = StringBuilder("Date,Time,Group,Subject,Teacher,Student,Status\n")
    sessions.sortedBy { it.timestamp }.forEach { s ->
      (records[s.id] ?: emptyList()).forEach { r ->
        val status = parseStatus(r.status)
        sb.append(
          listOf(s.date, s.time, s.groupName, s.subjectName, s.teacherName, r.studentName, status.label)
            .joinToString(",") { it.csvEscaped() }
        ).append('\n')
      }
    }
    return sb.toString()
  }

  // ---------------------------------------------------------------------------------------
  // Settings
  // ---------------------------------------------------------------------------------------

  fun setTheme(theme: String) {
    prefs.edit().putString(KEY_THEME, theme).apply()
    _uiState.update { it.copy(selectedTheme = theme) }
  }

  fun toggleDefaultAllPresent(enabled: Boolean) {
    prefs.edit().putBoolean(KEY_DEFAULT_PRESENT, enabled).apply()
    _uiState.update { it.copy(defaultAllPresent = enabled) }
  }

  fun toggleAutoSave(enabled: Boolean) {
    prefs.edit().putBoolean(KEY_AUTOSAVE, enabled).apply()
    _uiState.update { it.copy(autoSaveEnabled = enabled) }
  }

  fun toggleAppLock(enabled: Boolean) {
    prefs.edit().putBoolean(KEY_APP_LOCK, enabled).apply()
    _uiState.update { it.copy(appLockEnabled = enabled) }
  }

  fun updateReportLink(link: String) {
    viewModelScope.launch { dao.insertSettings(SettingsEntity(1, link.trim())) }
  }

  /** Wipes teachers, groups, students and history. Settings are kept. */
  fun clearAllData() {
    viewModelScope.launch {
      dao.clearAllData()
      val editor = prefs.edit()
      prefs.all.keys.filter { it.startsWith(DRAFT_PREFIX) }.forEach { editor.remove(it) }
      editor.apply()
      _uiState.update {
        it.copy(
          studentSelectedGroupFilter = "ALL",
          inspectingSession = null,
          lastSavedSummaryMessage = null
        )
      }
    }
  }

  // ---------------------------------------------------------------------------------------
  // Backup / restore
  // ---------------------------------------------------------------------------------------

  fun setImportPreviewData(json: String?, preview: AppBackup?) {
    _uiState.update { it.copy(pendingImportJson = json, pendingImportPreview = preview) }
  }

  suspend fun getBackupDataAndJson(): Pair<AppBackup, String>? {
    return try {
      val teachers = dao.getBackupTeachers()
      val groups = dao.getBackupGroups()
      val students = dao.getBackupStudents()
      val sessions = dao.getBackupSessions()
      val records = dao.getAllRecords()
      val settings = dao.getBackupSettings()

      val teacherMap = teachers.associateBy { it.id }
      val groupMap = groups.associateBy { it.id }
      val sessionMap = sessions.associateBy { it.id }

      val backupTeachers = teachers.map { t ->
        BackupTeacherDto(
          id = t.id,
          name = t.name,
          phone = t.phoneNumber,
          link = t.telegramLink,
          assignedGroups = groups.filter { it.teacherId == t.id }.map { it.name }
        )
      }

      val backupGroups = groups.map { g ->
        BackupGroupDto(
          id = g.id,
          name = g.name,
          subject = g.subjectName,
          teacherId = g.teacherId,
          teacherName = teacherMap[g.teacherId]?.name,
          isFavorite = g.isFavorite
        )
      }

      val backupStudents = students.map { s ->
        val g = groupMap[s.groupId]
        BackupStudentDto(
          id = s.id,
          name = s.name,
          groupId = s.groupId,
          groupName = g?.name ?: "",
          teacherId = g?.teacherId,
          teacherName = teacherMap[g?.teacherId]?.name,
          studentIdNumber = s.studentIdNumber,
          gender = s.gender,
          attendanceRate = s.attendanceRate
        )
      }

      val backupSessions = sessions.map { s ->
        BackupSessionDto(
          id = s.id,
          groupId = s.groupId,
          groupName = groupMap[s.groupId]?.name ?: "",
          date = s.date,
          time = s.time,
          totalStudents = s.totalStudents,
          presentCount = s.presentCount,
          lateCount = s.lateCount,
          leftCount = s.leftCount,
          otherGroupCount = s.otherGroupCount
        )
      }

      val backupRecords = records.map { r ->
        val s = sessionMap[r.sessionId]
        val g = groupMap[s?.groupId]
        BackupRecordDto(
          id = r.id,
          sessionId = r.sessionId,
          date = s?.date ?: "",
          time = s?.time ?: "",
          studentId = r.studentId,
          studentName = r.studentName,
          groupId = g?.id ?: "",
          groupName = g?.name ?: "",
          teacherId = g?.teacherId,
          teacherName = teacherMap[g?.teacherId]?.name,
          status = r.status
        )
      }

      val backup = AppBackup(
        version = 3,
        date = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date()),
        summary = AppBackupSummary(
          teachers = teachers.size,
          groups = groups.size,
          students = students.size,
          sessions = sessions.size,
          records = records.size
        ),
        teachers = backupTeachers,
        groups = backupGroups,
        students = backupStudents,
        sessions = backupSessions,
        records = backupRecords,
        settings = settings
      )
      val json = moshi.adapter(AppBackup::class.java).indent("  ").toJson(backup)
      Pair(backup, json)
    } catch (e: Exception) {
      e.printStackTrace()
      null
    }
  }

  fun getBackupPreview(jsonString: String): AppBackup? {
    return try {
      moshi.adapter(AppBackup::class.java).fromJson(jsonString)
    } catch (e: Exception) {
      e.printStackTrace()
      null
    }
  }

  suspend fun restoreFromBackupJson(jsonString: String): Boolean {
    return try {
      val backup = moshi.adapter(AppBackup::class.java).fromJson(jsonString) ?: return false

      val teachers = backup.teachers.map { TeacherEntity(it.id, it.name, it.phone, it.link) }
      val teacherIds = teachers.map { it.id }.toSet()
      val groups = backup.groups.map {
        GroupEntity(it.id, it.name, it.subject, it.teacherId?.takeIf { id -> id in teacherIds }, it.isFavorite)
      }
      val groupIds = groups.map { it.id }.toSet()
      // Drop rows that point at something missing so one bad row can't fail the whole restore.
      val students = backup.students.filter { it.groupId in groupIds }.map {
        StudentEntity(
          id = it.id,
          name = it.name,
          studentIdNumber = it.studentIdNumber.ifBlank { "STD-" + it.id.takeLast(4).uppercase() },
          groupId = it.groupId,
          gender = it.gender,
          attendanceRate = it.attendanceRate
        )
      }
      val sessions = backup.sessions.filter { it.groupId in groupIds }.map {
        SessionEntity(it.id, it.groupId, it.date, it.time, it.totalStudents, it.presentCount, it.lateCount, it.leftCount, it.otherGroupCount)
      }
      val sessionIds = sessions.map { it.id }.toSet()
      val records = backup.records.filter { it.sessionId in sessionIds }.map {
        RecordEntity(it.id, it.sessionId, it.studentId, it.studentName, it.status)
      }

      dao.restoreBackup(
        teachers = teachers,
        groups = groups,
        students = students,
        sessions = sessions,
        records = records,
        settings = backup.settings
      )
      true
    } catch (e: Exception) {
      e.printStackTrace()
      false
    }
  }

  private fun draftKey(groupId: String) = DRAFT_PREFIX + groupId

  companion object {
    private const val PREFS_NAME = "attendance_prefs"
    private const val KEY_THEME = "theme"
    private const val KEY_DEFAULT_PRESENT = "default_all_present"
    private const val KEY_AUTOSAVE = "auto_save"
    private const val KEY_APP_LOCK = "app_lock"
    private const val KEY_SEEDED = "demo_data_seeded"
    private const val DRAFT_PREFIX = "draft_"
    private const val LOCK_AFTER_MS = 60_000L
  }
}
