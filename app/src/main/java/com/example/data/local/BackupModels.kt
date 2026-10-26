package com.example.data.local

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AppBackupSummary(
    val teachers: Int,
    val groups: Int,
    val students: Int,
    val sessions: Int,
    val records: Int
)

@JsonClass(generateAdapter = true)
data class BackupTeacherDto(
    val id: String,
    val name: String,
    val phone: String,
    val link: String,
    val assignedGroups: List<String>
)

@JsonClass(generateAdapter = true)
data class BackupGroupDto(
    val id: String,
    val name: String,
    val subject: String,
    val teacherId: String?,
    val teacherName: String?,
    // Added in backup v3; older backups don't have it and default to "not favourite".
    val isFavorite: Boolean = false
)

@JsonClass(generateAdapter = true)
data class BackupStudentDto(
    val id: String,
    val name: String,
    val groupId: String,
    val groupName: String,
    val teacherId: String?,
    val teacherName: String?,
    // Added in backup v3; older backups don't have these and fall back to defaults.
    val studentIdNumber: String = "",
    val gender: String = "Male",
    val attendanceRate: Int = 90
)

@JsonClass(generateAdapter = true)
data class BackupSessionDto(
    val id: String,
    val groupId: String,
    val groupName: String,
    val date: String,
    val time: String,
    val totalStudents: Int,
    val presentCount: Int,
    val lateCount: Int,
    val leftCount: Int,
    val otherGroupCount: Int
)

@JsonClass(generateAdapter = true)
data class BackupRecordDto(
    val id: Long,
    val sessionId: String,
    val date: String,
    val time: String,
    val studentId: String,
    val studentName: String,
    val groupId: String,
    val groupName: String,
    val teacherId: String?,
    val teacherName: String?,
    val status: String
)

@JsonClass(generateAdapter = true)
data class AppBackup(
    val version: Int = 3,
    val date: String,
    val summary: AppBackupSummary,
    val teachers: List<BackupTeacherDto>,
    val groups: List<BackupGroupDto>,
    val students: List<BackupStudentDto>,
    val sessions: List<BackupSessionDto>,
    val records: List<BackupRecordDto>,
    val settings: SettingsEntity?
)
