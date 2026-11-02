package com.alma.app.data

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

private const val LOCAL_COVER_PREFIX = "local:"
private val COVER_NAME = Regex("[A-Za-z0-9_-]+\\.(jpg|jpeg|png|webp)")

/** Las portadas propias se guardan como "local:<archivo>", sin ruta, para que sobrevivan a una copia. */
fun localCoverRef(fileName: String): String = LOCAL_COVER_PREFIX + fileName

fun localCoverName(coverUri: String?): String? = coverUri?.takeIf { it.startsWith(LOCAL_COVER_PREFIX) }?.removePrefix(LOCAL_COVER_PREFIX)

/** Evita que un nombre de archivo pueda salir de la carpeta de portadas. */
fun isValidCoverName(name: String): Boolean = COVER_NAME.matches(name)

class CoverStore(private val context: Context) {
    val directory: File = File(context.filesDir, "covers")

    fun fileFor(coverUri: String?): File? {
        val name = localCoverName(coverUri)?.takeIf(::isValidCoverName) ?: return null
        return File(directory, name)
    }

    /**
     * Guarda una imagen elegida por el usuario. Se reduce de tamaño para que un puñado de fotos no
     * agote el límite de 25 MB de la copia automática de Android. Devuelve null si no se puede leer.
     */
    suspend fun saveFromPicker(uri: Uri): String? {
        val request = ImageRequest.Builder(context)
            .data(uri)
            .size(MAX_COVER_SIZE_PX)
            .allowHardware(false)
            .build()
        val image = (context.imageLoader.execute(request) as? SuccessResult)?.image ?: return null
        return withContext(Dispatchers.IO) {
            directory.mkdirs()
            val name = "${UUID.randomUUID()}.jpg"
            File(directory, name).outputStream().use { out ->
                image.toBitmap().compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            }
            localCoverRef(name)
        }
    }

    suspend fun delete(coverUri: String?) {
        val file = fileFor(coverUri) ?: return
        withContext(Dispatchers.IO) { file.delete() }
    }

    private companion object {
        const val MAX_COVER_SIZE_PX = 800
        const val JPEG_QUALITY = 85
    }
}
