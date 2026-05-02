package com.example.moneypad.utils

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object ImageUtils {
    /**
     * Copies an image from a system URI to the app's internal storage and returns the local file path.
     */
    fun saveImageToInternalStorage(context: Context, uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val extension = getFileExtension(context, uri)
            val fileName = "img_${UUID.randomUUID()}.$extension"
            val file = File(context.filesDir, fileName)
            
            val outputStream = FileOutputStream(file)
            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getFileExtension(context: Context, uri: Uri): String {
        return context.contentResolver.getType(uri)?.substringAfterLast("/") ?: "jpg"
    }
}
