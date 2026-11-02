package com.alma.app

import android.content.Context
import com.alma.app.data.BookRepository
import com.alma.app.data.CoverStore
import com.alma.app.data.backup.BackupExporter
import com.alma.app.data.backup.BackupImporter
import com.alma.app.data.local.AlmaDatabase
import com.alma.app.data.remote.OpenLibraryApi
import com.alma.app.data.remote.OpenLibraryUserAgent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/** Crea y comparte las dependencias de la app. */
class AppContainer(context: Context) {
    /** Para trabajo que debe terminar aunque se cierre la pantalla, como guardar un comentario. */
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val database = AlmaDatabase.create(context)

    private val json = Json { ignoreUnknownKeys = true }

    // Open Library a veces tarda bastante en responder.
    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(OpenLibraryUserAgent)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val api: OpenLibraryApi = Retrofit.Builder()
        .baseUrl(OpenLibraryApi.BASE_URL)
        .client(httpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(OpenLibraryApi::class.java)

    val covers = CoverStore(context)

    val repository = BookRepository(
        dao = database.bookDao(),
        api = api,
        covers = covers,
    )

    val backupExporter = BackupExporter(context, database.bookDao(), covers)
    val backupImporter = BackupImporter(context, database.bookDao(), covers)
}
