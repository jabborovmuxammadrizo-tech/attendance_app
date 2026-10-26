package com.example.utils

import com.example.model.AttendanceStatus

/** Builds the Telegram-style attendance message (same format the Import screen parses). */
object ReportUtils {
  fun buildAttendanceReport(
    groupName: String,
    teacherName: String,
    teacherLink: String,
    time: String,
    total: Int,
    present: Int,
    late: Int,
    left: Int,
    otherGroup: Int,
    entries: List<Pair<String, AttendanceStatus>>
  ): String {
    val tag = groupName.replace(" ", "_")
    return buildString {
      appendLine("👥  GURUXI:")
      if (tag.startsWith("#")) appendLine(tag) else appendLine("#$tag")
      appendLine()
      appendLine("👨‍🏫 O'qituvchi:")
      appendLine(teacherName.ifBlank { "Not specified" })
      appendLine()
      appendLine("📅 Davomat vaqti: $time")
      appendLine("👥 Jami: $total")
      appendLine()
      appendLine("✅:$present ❌:$late ✈️:$left ❓:$otherGroup")
      appendLine()
      entries.forEachIndexed { index, (name, status) ->
        appendLine("${index + 1}.$name ${status.symbol}")
      }
      appendLine()
      appendLine("📌 Izohlar:")
      appendLine("✅ - darsda qatnashmoqda")
      appendLine("❌ - kechikyapti")
      appendLine("✈️ - ketgan, biz bilan bog'laning")
      appendLine("❓ - boshqa guruhda o‘qiyapti")
      appendLine()
      appendLine("👉 ${teacherLink.ifBlank { "Link not specified" }}")
    }
  }
}
