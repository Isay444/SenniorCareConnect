package com.isa.cuidadocompartidomayor.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import androidx.core.graphics.scale
import com.isa.cuidadocompartidomayor.BuildConfig
import java.util.Properties

object ImageUploadHelper {

    private const val TAG = "ImageUploadHelper"
    private val CLOUD_NAME = BuildConfig.CLOUDINARY_CLOUD_NAME
    private val UPLOAD_PRESET = BuildConfig.CLOUDINARY_UPLOAD_PRESET
    private val CLOUDINARY_URL = "https://api.cloudinary.com/v1_1/$CLOUD_NAME/image/upload"

    private val client = OkHttpClient()

    /**
     * Sube una imagen de perfil a Cloudinary usando la REST API (OkHttp)
     */
    suspend fun uploadProfileImage(context: Context, imageUri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            // 1. Comprimir y redimensionar imagen
            val imageBytes = compressImage(context, imageUri)
                ?: return@withContext Result.failure(Exception("No se pudo procesar la imagen"))

            // 2. Construir cuerpo multipart
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", "profile_image.jpg", 
                    imageBytes.toRequestBody("image/jpeg".toMediaTypeOrNull()))
                .addFormDataPart("upload_preset", UPLOAD_PRESET)
                .build()

            // 3. Crear request
            val request = Request.Builder()
                .url(CLOUDINARY_URL)
                .post(requestBody)
                .build()

            // 4. Ejecutar llamada
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""
                
                if (!response.isSuccessful) {
                    Log.e(TAG, "❌ Error en Cloudinary: $responseBody")
                    return@withContext Result.failure(Exception("Error en servidor: ${response.code}"))
                }

                val jsonResponse = JSONObject(responseBody)
                val secureUrl = jsonResponse.getString("secure_url")
                
                Log.d(TAG, "✅ Imagen subida con éxito: $secureUrl")
                Result.success(secureUrl)
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error subiendo imagen a Cloudinary", e)
            Result.failure(e)
        }
    }

    /**
     * Stub para eliminar imágenes (requiere firma autenticada o Admin API)
     */
    suspend fun deleteProfileImage(publicId: String): Result<Unit> = withContext(Dispatchers.IO) {
        // La eliminación requiere generar un signature (SHA-1) con el API Secret.
        // Por seguridad, el API Secret no debe estar en la app.
        Log.d(TAG, "ℹ️ Stub: deleteProfileImage llamado para $publicId")
        Result.success(Unit)
    }

    /**
     * Procesa la imagen: Corrige rotación, redimensiona a max 800px y comprime a JPEG 80%
     */
    private fun compressImage(context: Context, uri: Uri): ByteArray? {
        return try {
            // 1. Leer la rotación EXIF primero
            val rotationMatrix = getRotationMatrix(context, uri)

            // 2. Decodificar el Bitmap original
            val inputStream = context.contentResolver.openInputStream(uri)
            val rawBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (rawBitmap == null) return null

            // 3. Aplicar rotación si es necesaria
            val orientedBitmap = if (!rotationMatrix.isIdentity) {
                val rotated = Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, rotationMatrix, true)
                rawBitmap.recycle() // Liberar el original después de rotar
                rotated
            } else {
                rawBitmap
            }

            // 4. Calcular nuevas dimensiones (máximo 800px)
            val maxSize = 800
            val width = orientedBitmap.width
            val height = orientedBitmap.height

            val (newWidth, newHeight) = if (width > height) {
                if (width > maxSize) {
                    val ratio = maxSize.toFloat() / width
                    (maxSize to (height * ratio).toInt())
                } else (width to height)
            } else {
                if (height > maxSize) {
                    val ratio = maxSize.toFloat() / height
                    ((width * ratio).toInt() to maxSize)
                } else (width to height)
            }

            // 5. Redimensionar
            val resizedBitmap = orientedBitmap.scale(newWidth, newHeight)

            // 6. Comprimir a JPEG
            val outputStream = ByteArrayOutputStream()
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            val result = outputStream.toByteArray()

            // Limpieza de memoria
            if (orientedBitmap != resizedBitmap) {
                orientedBitmap.recycle()
            }
            resizedBitmap.recycle()

            result
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error procesando imagen: ${e.message}")
            null
        }
    }

    /**
     * Obtiene la matriz de rotación basada en los metadatos EXIF de la imagen
     */
    private fun getRotationMatrix(context: Context, uri: Uri): Matrix {
        val matrix = Matrix()
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val exifInterface = ExifInterface(input)
                val orientation = exifInterface.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
                when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                    ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                    ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "No se pudo leer EXIF: ${e.message}")
        }
        return matrix
    }
}
