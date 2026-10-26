package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // TEACHERS
    @Query("SELECT * FROM teachers ORDER BY rowid")
    fun getAllTeachers(): Flow<List<TeacherEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeacher(teacher: TeacherEntity)

    // NOTE: edits must use UPDATE statements. Re-inserting a row with REPLACE deletes the
    // old row first, which fires the foreign-key actions (cascade-deleting students /
    // clearing group teachers / deleting a session's records).
    @Query("UPDATE teachers SET name = :name, phoneNumber = :phoneNumber, telegramLink = :telegramLink WHERE id = :id")
    suspend fun updateTeacherInfo(id: String, name: String, phoneNumber: String, telegramLink: String)

    @Query("DELETE FROM teachers WHERE id = :id")
    suspend fun deleteTeacher(id: String)
    
    // GROUPS
    @Query("SELECT * FROM groups ORDER BY rowid")
    fun getAllGroups(): Flow<List<GroupEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: GroupEntity)

    @Query("UPDATE groups SET name = :name, subjectName = :subjectName, teacherId = :teacherId WHERE id = :id")
    suspend fun updateGroupInfo(id: String, name: String, subjectName: String, teacherId: String?)

    @Query("UPDATE groups SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setGroupFavorite(id: String, isFavorite: Boolean)

    @Query("DELETE FROM groups WHERE id = :id")
    suspend fun deleteGroup(id: String)

    @Query("DELETE FROM attendance_sessions WHERE groupId = :groupId")
    suspend fun deleteSessionsForGroup(groupId: String)

    /** Deletes a group together with its students (FK cascade) and its attendance history. */
    @Transaction
    suspend fun deleteGroupWithHistory(groupId: String) {
        deleteSessionsForGroup(groupId)
        deleteGroup(groupId)
    }

    @Query("UPDATE groups SET teacherId = :teacherId WHERE id = :groupId")
    suspend fun updateGroupTeacher(groupId: String, teacherId: String?)

    // STUDENTS
    @Query("SELECT * FROM students ORDER BY rowid")
    fun getAllStudents(): Flow<List<StudentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: StudentEntity)

    @Query("UPDATE students SET name = :name WHERE id = :id")
    suspend fun updateStudentName(id: String, name: String)

    @Query("UPDATE students SET groupId = :groupId WHERE id = :id")
    suspend fun updateStudentGroup(id: String, groupId: String)

    @Query("DELETE FROM students WHERE id = :id")
    suspend fun deleteStudent(id: String)

    /** Present / total marks per student, computed from all saved attendance records. */
    @Query(
        "SELECT studentId AS studentId, COUNT(*) AS totalMarks, " +
            "SUM(CASE WHEN status = 'PRESENT' THEN 1 ELSE 0 END) AS presentMarks " +
            "FROM attendance_records GROUP BY studentId"
    )
    fun getStudentStats(): Flow<List<StudentStat>>

    // SESSIONS & RECORDS
    @Query("SELECT * FROM attendance_sessions ORDER BY date DESC, time DESC")
    fun getAllSessions(): Flow<List<SessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionEntity)

    @Query("UPDATE attendance_sessions SET presentCount = :presentCount, lateCount = :lateCount, leftCount = :leftCount, otherGroupCount = :otherGroupCount WHERE id = :id")
    suspend fun updateSessionCounts(id: String, presentCount: Int, lateCount: Int, leftCount: Int, otherGroupCount: Int)

    @Query("DELETE FROM attendance_sessions WHERE id = :id")
    suspend fun deleteSession(id: String)

    @Query("SELECT * FROM attendance_records WHERE sessionId = :sessionId")
    suspend fun getRecordsForSession(sessionId: String): List<RecordEntity>
    
    @Query("SELECT * FROM attendance_records WHERE sessionId = :sessionId")
    fun getRecordsForSessionFlow(sessionId: String): Flow<List<RecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecords(records: List<RecordEntity>)
    
    @Query("UPDATE attendance_records SET status = :status WHERE sessionId = :sessionId AND studentId = :studentId")
    suspend fun updateRecordStatus(sessionId: String, studentId: String, status: String)

    @Transaction
    suspend fun insertSessionWithRecords(session: SessionEntity, records: List<RecordEntity>) {
        insertSession(session)
        insertRecords(records)
    }

    // SETTINGS
    @Query("SELECT * FROM settings WHERE id = 1")
    fun getSettings(): Flow<SettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: SettingsEntity)

    // BACKUP AND RESTORE
    @Query("SELECT * FROM teachers ORDER BY rowid")
    suspend fun getBackupTeachers(): List<TeacherEntity>

    @Query("SELECT * FROM groups ORDER BY rowid")
    suspend fun getBackupGroups(): List<GroupEntity>

    @Query("SELECT * FROM students ORDER BY rowid")
    suspend fun getBackupStudents(): List<StudentEntity>

    @Query("SELECT * FROM attendance_sessions")
    suspend fun getBackupSessions(): List<SessionEntity>

    @Query("SELECT * FROM settings WHERE id = 1")
    suspend fun getBackupSettings(): SettingsEntity?

    @Query("SELECT * FROM attendance_records")
    suspend fun getAllRecords(): List<RecordEntity>

    @Query("DELETE FROM teachers")
    suspend fun clearTeachers()

    @Query("DELETE FROM groups")
    suspend fun clearGroups()

    @Query("DELETE FROM students")
    suspend fun clearStudents()

    @Query("DELETE FROM attendance_sessions")
    suspend fun clearSessions()

    @Query("DELETE FROM attendance_records")
    suspend fun clearRecords()

    @Query("DELETE FROM settings")
    suspend fun clearSettings()

    /** Removes every teacher, group, student and session (settings are kept). */
    @Transaction
    suspend fun clearAllData() {
        clearRecords()
        clearSessions()
        clearStudents()
        clearGroups()
        clearTeachers()
    }

    @Transaction
    suspend fun restoreBackup(
        teachers: List<TeacherEntity>,
        groups: List<GroupEntity>,
        students: List<StudentEntity>,
        sessions: List<SessionEntity>,
        records: List<RecordEntity>,
        settings: SettingsEntity?
    ) {
        // Clear all current data to replace it
        clearSettings()
        clearRecords()
        clearSessions()
        clearStudents()
        clearGroups()
        clearTeachers()

        // Insert new data
        teachers.forEach { insertTeacher(it) }
        groups.forEach { insertGroup(it) }
        students.forEach { insertStudent(it) }
        sessions.forEach { insertSession(it) }
        insertRecords(records)
        settings?.let { insertSettings(it) }
    }
}
