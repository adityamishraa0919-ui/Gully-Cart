package com.example.ui.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object ImageStorageHelper {

    /**
     * Reads an image URI (e.g. from PhotoPicker), persists it locally to app internal storage
     * so it NEVER expires or disappears on app restart, and encodes it to a Base64 data URI
     * for seamless cross-user real-time Firestore synchronization.
     */
    fun saveImagePermanently(context: Context, uri: Uri): String {
        return try {
            val contentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(uri) ?: return uri.toString()

            // 1. Decode bounds first to prevent OOM
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()

            val maxDimension = 960
            var sampleSize = 1
            var w = options.outWidth
            var h = options.outHeight
            while (w > maxDimension || h > maxDimension) {
                sampleSize *= 2
                w /= 2
                h /= 2
            }

            // 2. Decode actual bitmap with sample size
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            val secondStream = contentResolver.openInputStream(uri) ?: return uri.toString()
            val bitmap = BitmapFactory.decodeStream(secondStream, null, decodeOptions)
            secondStream.close()

            if (bitmap == null) return uri.toString()

            // 3. Save copy to internal app files directory (never deleted by OS)
            val imagesDir = File(context.filesDir, "gc_images").apply { if (!exists()) mkdirs() }
            val permanentFile = File(imagesDir, "prod_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.jpg")
            FileOutputStream(permanentFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }

            // 4. Also generate Base64 data URI so all users can see it instantly via Firestore
            val byteOut = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 78, byteOut)
            val bytes = byteOut.toByteArray()

            if (bytes.size < 400_000) {
                val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                "data:image/jpeg;base64,$base64"
            } else {
                "file://${permanentFile.absolutePath}"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            uri.toString()
        }
    }
}
