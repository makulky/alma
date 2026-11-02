package com.alma.app.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alma.app.R
import com.alma.app.data.backup.BackupError
import com.alma.app.data.backup.BackupException
import com.alma.app.data.backup.BackupExporter
import com.alma.app.data.backup.BackupImporter
import com.alma.app.data.backup.ImportPreview
import com.alma.app.ui.almaViewModelFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.LocalDate

data class SettingsMessage(val text: Int, val count: Int? = null)

class SettingsViewModel(
    private val exporter: BackupExporter,
    private val importer: BackupImporter,
) : ViewModel() {
    val isBusy = MutableStateFlow(false)
    val preview = MutableStateFlow<ImportPreview?>(null)

    private val messageChannel = Channel<SettingsMessage>(Channel.BUFFERED)
    val messages: Flow<SettingsMessage> = messageChannel.receiveAsFlow()

    fun suggestedFileName(): String = "alma-backup-${LocalDate.now()}.zip"

    fun export(destination: Uri) = runBusy {
        val message = try {
            SettingsMessage(R.string.settings_export_done, exporter.export(destination))
        } catch (_: IOException) {
            SettingsMessage(R.string.settings_export_failed)
        }
        messageChannel.send(message)
    }

    fun prepareImport(source: Uri) = runBusy {
        try {
            preview.value = importer.prepare(source)
        } catch (e: BackupException) {
            val text = when (e.error) {
                BackupError.CORRUPT -> R.string.settings_import_corrupt
                BackupError.NEWER_VERSION -> R.string.settings_import_newer
                BackupError.INVALID_PATH -> R.string.settings_import_invalid
            }
            messageChannel.send(SettingsMessage(text))
        }
    }

    fun confirmImport() {
        val current = preview.value ?: return
        preview.value = null
        runBusy {
            val message = try {
                importer.confirm(current)
                SettingsMessage(R.string.settings_import_done)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                SettingsMessage(R.string.settings_import_failed)
            }
            messageChannel.send(message)
        }
    }

    fun cancelImport() {
        preview.value = null
        importer.cancel()
    }

    private fun runBusy(block: suspend () -> Unit) {
        viewModelScope.launch {
            isBusy.value = true
            try {
                block()
            } finally {
                isBusy.value = false
            }
        }
    }

    companion object {
        val Factory = almaViewModelFactory { container, _ ->
            SettingsViewModel(container.backupExporter, container.backupImporter)
        }
    }
}
