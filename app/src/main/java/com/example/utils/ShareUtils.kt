package com.example.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

object ShareUtils {
  fun copyToClipboard(context: Context, text: String, label: String = "Attendance report") {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(context, "Report copied", Toast.LENGTH_SHORT).show()
  }

  fun shareText(context: Context, text: String, title: String = "Share via") {
    val intent = Intent(Intent.ACTION_SEND).apply {
      type = "text/plain"
      putExtra(Intent.EXTRA_TEXT, text)
    }
    try {
      context.startActivity(Intent.createChooser(intent, title))
    } catch (e: Exception) {
      Toast.makeText(context, "No app available to share with", Toast.LENGTH_SHORT).show()
    }
  }

  /** Opens a web / t.me link in whatever app handles it (Telegram, browser, ...). */
  fun openLink(context: Context, link: String) {
    val trimmed = link.trim()
    if (trimmed.isEmpty()) return
    val url = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) trimmed else "https://$trimmed"
    try {
      context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: Exception) {
      Toast.makeText(context, "Can't open this link", Toast.LENGTH_SHORT).show()
    }
  }

  fun shareFile(context: Context, fileName: String, content: String, mimeType: String = "text/csv") {
    try {
      val file = File(context.cacheDir, fileName)
      // UTF-8 BOM so Excel shows Uzbek / Cyrillic names correctly when opening a CSV.
      val bom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
      file.outputStream().use { output ->
        output.write(bom)
        output.write(content.toByteArray(Charsets.UTF_8))
      }

      val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

      val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        clipData = ClipData.newRawUri(fileName, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }
      context.startActivity(Intent.createChooser(intent, "Share File"))
    } catch (e: Exception) {
      e.printStackTrace()
      Toast.makeText(context, "Could not share file: ${e.message}", Toast.LENGTH_SHORT).show()
    }
  }
}
