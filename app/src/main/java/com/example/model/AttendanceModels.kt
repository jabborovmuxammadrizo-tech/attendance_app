package com.example.model

enum class SortOrder {
  ORIGINAL,
  ASCENDING,
  DESCENDING
}

enum class AttendanceStatus(
  val label: String,
  val symbol: String,
  val code: String
) {
  PRESENT("Present", "✅", "PRESENT"),
  LATE("Late", "❌", "LATE"),
  LEFT("Left", "✈️", "LEFT"),
  OTHER_GROUP("Other Group", "❓", "OTHER_GROUP")
}

data class Teacher(
  val id: String,
  val name: String,
  val phoneNumber: String = "",
  val telegramLink: String = "",
  val groupCount: Int = 0
)

data class Student(
  val id: String,
  val name: String,
  val studentIdNumber: String,
  val groupName: String,
  val gender: String = "Male",
  val attendanceRate: Int = 90
)

data class GroupItem(
  val id: String,
  val name: String,
  val teacherName: String = "",
  val subjectName: String = "",
  val code: String = "",
  val isFavorite: Boolean = false,
  val studentCount: Int = 0
)

data class StudentAttendanceState(
  val student: Student,
  val status: AttendanceStatus = AttendanceStatus.PRESENT
)

data class AttendanceSession(
  val id: String,
  val groupId: String = "",
  val groupName: String,
  val subjectName: String,
  val teacherName: String = "",
  val date: String,
  val time: String,
  val totalStudents: Int,
  val presentCount: Int,
  val lateCount: Int,
  val leftCount: Int = 0,
  val otherGroupCount: Int = 0,
  /** Epoch millis parsed from [date] + [time]; 0 if it could not be parsed. Used for sorting/filtering. */
  val timestamp: Long = 0L,
  val records: List<StudentAttendanceRecord> = emptyList()
)

data class StudentAttendanceRecord(
  val studentId: String,
  val studentName: String,
  val status: AttendanceStatus
)

enum class Screen(val title: String) {
  HOME("Attendance Management"),
  ATTENDANCE("Take Attendance"),
  GROUPS("Groups"),
  GROUP_DETAILS("Group Details"),
  STUDENTS("Students"),
  TEACHERS("Teachers"),
  IMPORT_STUDENTS("Import Telegram Message"),
  REPORTS("Reports & Statistics"),
  HISTORY("Attendance History"),
  SETTINGS("Settings")
}
