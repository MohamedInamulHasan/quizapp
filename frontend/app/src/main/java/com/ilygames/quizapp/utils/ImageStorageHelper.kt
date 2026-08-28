package com.ilygames.quizapp.utils

import android.content.Context
import android.net.Uri
import java.io.File

object ImageStorageHelper {
    /**
     * Copies a Uri stream into permanent internal storage (filesDir).
     * Returns permanent absolute file path, which survives app restarts without security/permission errors.
     */
    fun saveUriToInternalStorage(context: Context, uri: Uri, prefix: String): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
            val ext = when {
                mimeType.contains("png") -> ".png"
                mimeType.contains("gif") -> ".gif"
                mimeType.contains("webp") -> ".webp"
                else -> ".jpg"
            }
            val file = File(context.filesDir, "${prefix}_${System.currentTimeMillis()}$ext")
            file.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            inputStream.close()
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
