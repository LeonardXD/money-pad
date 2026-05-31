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

fun String?.toBackendUri(): String? {
    if (this.isNullOrBlank()) return null
    
    // Check if it's a local URI/path
    if (startsWith("content://") || startsWith("file://") || startsWith("/")) {
        return this
    }
    
    // Check if it's a relative path
    val cleanPath = when {
        startsWith("backend/uploads/") -> substringAfter("backend/")
        startsWith("uploads/") -> this
        !startsWith("http") -> "uploads/$this"
        else -> null
    }
    
    if (cleanPath != null) {
        return com.example.moneypad.data.remote.RetrofitClient.baseUrl + cleanPath
    }
    
    // If it's an absolute HTTP URL, handle trycloudflare.com dynamic subdomains
    if (startsWith("http")) {
        if (contains("trycloudflare.com")) {
            val pathPart = substringAfter("trycloudflare.com/")
            val pathWithoutBackend = if (pathPart.startsWith("backend/")) {
                pathPart.substringAfter("backend/")
            } else {
                pathPart
            }
            return com.example.moneypad.data.remote.RetrofitClient.baseUrl + pathWithoutBackend
        }
        return this
    }
    
    return this
}
