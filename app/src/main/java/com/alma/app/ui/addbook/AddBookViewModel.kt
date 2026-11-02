package com.alma.app.ui.addbook

import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alma.app.R
import com.alma.app.data.BookRepository
import com.alma.app.data.CoverStore
import com.alma.app.data.ManualBook
import com.alma.app.domain.ReadingStatus
import com.alma.app.ui.almaViewModelFactory
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddBookForm(
    val title: String = "",
    val authors: String = "",
    val isbn: String = "",
    val publisher: String = "",
    val year: String = "",
    val pages: String = "",
    val description: String = "",
    val coverPick: Uri? = null,
    val status: ReadingStatus = ReadingStatus.PENDING,
    val isSaving: Boolean = false,
    @param:StringRes val error: Int? = null,
) {
    val canSave: Boolean get() = title.isNotBlank() && authors.isNotBlank() && !isSaving
}

class AddBookViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: BookRepository,
    private val covers: CoverStore,
) : ViewModel() {
    val form = MutableStateFlow(AddBookForm(title = savedStateHandle.get<String>("title").orEmpty()))

    private val savedChannel = Channel<Long>(Channel.BUFFERED)
    val saved: Flow<Long> = savedChannel.receiveAsFlow()

    fun edit(change: AddBookForm.() -> AddBookForm) = form.update { it.change().copy(error = null) }

    fun save() {
        val current = form.value
        if (!current.canSave) return
        val isbn = current.isbn.filter(Char::isDigit).ifEmpty { null }
        if (isbn != null && isbn.length != ISBN_LENGTH) {
            form.update { it.copy(error = R.string.add_invalid_isbn) }
            return
        }
        form.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            if (isbn != null && repository.findIdByIsbn(isbn) != null) {
                form.update { it.copy(isSaving = false, error = R.string.add_duplicate) }
                return@launch
            }
            val coverUri = current.coverPick?.let { pick ->
                covers.saveFromPicker(pick) ?: run {
                    form.update { it.copy(isSaving = false, error = R.string.add_cover_failed) }
                    return@launch
                }
            }
            val id = repository.addManual(
                ManualBook(
                    title = current.title.trim(),
                    authors = current.authors.trim(),
                    coverUri = coverUri,
                    description = current.description.trim().ifEmpty { null },
                    isbn13 = isbn,
                    publisher = current.publisher.trim().ifEmpty { null },
                    pageCount = current.pages.toIntOrNull()?.takeIf { it > 0 },
                    publishedYear = current.year.toIntOrNull(),
                ),
                current.status,
            )
            savedChannel.send(id)
        }
    }

    companion object {
        private const val ISBN_LENGTH = 13

        val Factory = almaViewModelFactory { container, handle ->
            AddBookViewModel(handle, container.repository, container.covers)
        }
    }
}
