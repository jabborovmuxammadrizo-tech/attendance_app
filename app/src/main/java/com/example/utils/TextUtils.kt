package com.example.utils

/**
 * Attendance messages print a group as "#GENERAL_7" while the app may store it as
 * "GENERAL 7". Comparing normalized keys lets an imported message find the group
 * that already exists instead of creating a duplicate.
 */
fun normalizeGroupKey(name: String): String {
  return name.trim().removePrefix("#").replace('_', ' ').trim().lowercase()
}

fun String.csvEscaped(): String = "\"" + this.replace("\"", "\"\"") + "\""
