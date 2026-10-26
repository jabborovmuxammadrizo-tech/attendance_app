package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.TypeConverter
import com.example.model.AttendanceStatus
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@Entity(tableName = "teachers")
data class TeacherEntity(
    @PrimaryKey val id: String,
    val name: String,
    val phoneNumber: String,
    val telegramLink: String
)

@JsonClass(generateAdapter = true)
@Entity(
    tableName = "groups",
    foreignKeys = [
        ForeignKey(
            entity = TeacherEntity::class,
            parentColumns = ["id"],
            childColumns = ["teacherId"],
            onDelete = ForeignKey.SET_NULL,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [Index("teacherId")]
)
data class GroupEntity(
    @PrimaryKey val id: String,
    val name: String, // Group name
    val subjectName: String,
    val teacherId: String?,
    val isFavorite: Boolean = false
)

@JsonClass(generateAdapter = true)
@Entity(
    tableName = "students",
    foreignKeys = [
        ForeignKey(
            entity = GroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [Index("groupId")]
)
data class StudentEntity(
    @PrimaryKey val id: String,
    val name: String,
    val studentIdNumber: String,
    val groupId: String,
    val gender: String = "Male",
    val attendanceRate: Int = 90
)

@JsonClass(generateAdapter = true)
@Entity(tableName = "attendance_sessions")
data class SessionEntity(
    @PrimaryKey val id: String,
    val groupId: String,
    val date: String,
    val time: String,
    val totalStudents: Int,
    val presentCount: Int,
    val lateCount: Int,
    val leftCount: Int,
    val otherGroupCount: Int
)

@JsonClass(generateAdapter = true)
@Entity(
    tableName = "attendance_records",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId")]
)
data class RecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String,
    val studentId: String,
    val studentName: String,
    val status: String // Store the enum name
)

@JsonClass(generateAdapter = true)
@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val id: Int = 1,
    val reportTelegramLink: String
)

/** Result row of the per-student attendance aggregate query (not a table). */
data class StudentStat(
    val studentId: String,
    val totalMarks: Int,
    val presentMarks: Int
)
