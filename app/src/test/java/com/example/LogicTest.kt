package com.example

import com.example.model.AttendanceStatus
import com.example.ui.screens.parseTelegramMessage
import com.example.utils.DateUtils
import com.example.utils.ReportUtils
import com.example.utils.csvEscaped
import com.example.utils.normalizeGroupKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LogicTest {

  @Test
  fun telegramTagMatchesStoredGroupName() {
    assertEquals(normalizeGroupKey("GENERAL 7"), normalizeGroupKey("#GENERAL_7"))
    assertEquals(normalizeGroupKey("  #pre-intermediate "), normalizeGroupKey("PRE-INTERMEDIATE"))
  }

  @Test
  fun dateOrderingIsChronologicalNotAlphabetical() {
    // As plain text "03 October" sorts BEFORE "21 September"; as dates it is later.
    val october = DateUtils.toMillis("03 October 2026", "09:00")
    val september = DateUtils.toMillis("21 September 2026", "09:00")
    assertTrue(october > september)
    assertTrue(DateUtils.toMillis("21 September 2026", "10:30") > DateUtils.toMillis("21 September 2026", "09:00"))
    assertEquals(0L, DateUtils.toMillis("not a date", "xx"))
  }

  @Test
  fun reportRoundTripsThroughTheImporter() {
    val report = ReportUtils.buildAttendanceReport(
      groupName = "GENERAL 7",
      teacherName = "",
      teacherLink = "",
      time = "08:05",
      total = 2,
      present = 1,
      late = 1,
      left = 0,
      otherGroup = 0,
      entries = listOf(
        "Aliyev Muhammad" to AttendanceStatus.PRESENT,
        "Karimov Aziz" to AttendanceStatus.LATE
      )
    )
    val parsed = parseTelegramMessage(report)
    assertEquals("#GENERAL_7", parsed.groupName)
    assertEquals(listOf("Aliyev Muhammad", "Karimov Aziz"), parsed.students)
    // The report prints "Not specified" for a missing teacher; that must not become a teacher.
    assertEquals("", parsed.teacherName)
  }

  @Test
  fun parserReadsTeacherAndStudents() {
    val message = """
      👥  GURUXI:
      #GENERAL_7

      👨‍🏫 O'qituvchi:
      Sobirjonov Mirkamol

      📅 Davomat vaqti: 08:05
      👥 Jami: 2

      ✅:1 ❌:1 ✈️:0 ❓:0

      1.Aliyev Muhammad ✅
      2) Karimov Aziz ❌
    """.trimIndent()
    val parsed = parseTelegramMessage(message)
    assertEquals("#GENERAL_7", parsed.groupName)
    assertEquals("Sobirjonov Mirkamol", parsed.teacherName)
    assertEquals(listOf("Aliyev Muhammad", "Karimov Aziz"), parsed.students)
  }

  @Test
  fun csvFieldsAreQuoted() {
    assertEquals("\"a,b\"", "a,b".csvEscaped())
    assertEquals("\"say \"\"hi\"\"\"", "say \"hi\"".csvEscaped())
  }
}
