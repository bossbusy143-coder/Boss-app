package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import java.io.ByteArrayOutputStream

object ImageUtils {
    private const val TAG = "ImageUtils"

    /**
     * Reads an image from the given Uri, scales it to maxDimension,
     * and compresses it into a Base64 data URL string (e.g. data:image/jpeg;base64,...).
     */
    fun uriToBase64(
        context: Context,
        uri: Uri,
        maxDimension: Int = 400,
        quality: Int = 80
    ): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            try {
                inputStream.close()
            } catch (_: Exception) {}

            if (originalBitmap == null) return null

            val width = originalBitmap.width
            val height = originalBitmap.height
            val maxSide = maxOf(width, height)

            val scaledBitmap = if (maxSide > maxDimension) {
                val scale = maxDimension.toFloat() / maxSide
                val targetWidth = (width * scale).toInt().coerceAtLeast(1)
                val targetHeight = (height * scale).toInt().coerceAtLeast(1)
                Bitmap.createScaledBitmap(originalBitmap, targetWidth, targetHeight, true)
            } else {
                originalBitmap
            }

            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            val bytes = outputStream.toByteArray()
            val base64String = Base64.encodeToString(bytes, Base64.NO_WRAP)
            "data:image/jpeg;base64,$base64String"
        } catch (e: Exception) {
            Log.e(TAG, "Error encoding image from URI: ${e.message}", e)
            null
        }
    }

    /**
     * Prepares an image model for Coil:
     * If source is a Base64 data URL (starts with "data:image"), decodes to ByteArray for instant loading.
     * Otherwise returns the original String (e.g. HTTP/HTTPS URL or file URI).
     */
    fun toCoilModel(source: String?): Any? {
        if (source.isNullOrBlank()) return null
        if (source.startsWith("data:image")) {
            val commaIndex = source.indexOf(",")
            if (commaIndex != -1) {
                val base64Data = source.substring(commaIndex + 1)
                return try {
                    Base64.decode(base64Data, Base64.DEFAULT)
                } catch (e: Exception) {
                    source
                }
            }
        }
        return source
    }
}

fun String?.toCoilModel(): Any? = ImageUtils.toCoilModel(this)
