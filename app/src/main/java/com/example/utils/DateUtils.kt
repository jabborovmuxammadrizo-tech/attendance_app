package com.example.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Sessions store their date as text ("21 October 2026") and time as "HH:mm".
 * Sorting those as plain strings puts "9 ..." / "21 August" in the wrong order,
 * so everything that needs chronological order goes through [toMillis].
 */
object DateUtils {
  private const val DATE_PATTERN = "dd MMMM yyyy"
  private const val TIME_PATTERN = "HH:mm"

  fun today(): String = SimpleDateFormat(DATE_PATTERN, Locale.ENGLISH).format(Date())

  fun nowTime(): String = SimpleDateFormat(TIME_PATTERN, Locale.ENGLISH).format(Date())

  fun toMillis(date: String, time: String): Long {
    return try {
      val format = SimpleDateFormat("$DATE_PATTERN $TIME_PATTERN", Locale.ENGLISH)
      format.isLenient = true
      format.parse("${date.trim()} ${time.trim()}")?.time ?: 0L
    } catch (e: Exception) {
      0L
    }
  }
}
