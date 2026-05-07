package com.gridy.rohmahapp.utils

import android.content.Context
import android.net.Uri
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

object ProfilePhotoUpload {

    const val MAX_BYTES: Long = 2L * 1024L * 1024L

    /**
     * Membaca [uri] dari [ContentResolver] (aman untuk content://) dan membangun part multipart `photo`.
     */
    fun buildPhotoPart(context: Context, uri: Uri): MultipartBody.Part {
        val cr = context.contentResolver
        val mime = cr.getType(uri)
        if (mime == null || !mime.startsWith("image/")) {
            throw IllegalArgumentException("Pilih file gambar yang valid.")
        }
        val bytes = cr.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IOException("Tidak dapat membaca berkas.")
        if (bytes.isEmpty()) {
            throw IllegalArgumentException("Berkas kosong.")
        }
        if (bytes.size > MAX_BYTES) {
            throw IllegalArgumentException("Ukuran gambar maksimal 2 MB.")
        }
        val ext = when (mime) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> "jpg"
        }
        val body = bytes.toRequestBody(mime.toMediaTypeOrNull())
        return MultipartBody.Part.createFormData("photo", "profile.$ext", body)
    }
}
