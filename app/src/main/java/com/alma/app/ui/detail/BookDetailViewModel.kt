package com.alma.app.ui.detail

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alma.app.R
import com.alma.app.data.BookRepository
import com.alma.app.domain.Book
import com.alma.app.domain.ReadingStatus
import com.alma.app.ui.almaViewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

private const val COMMENT_SAVE_DELAY_MS = 500L

class BookDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: BookRepository,
    private val applicationScope: CoroutineScope,
) : ViewModel() {
    private val bookId: Long = checkNotNull(savedStateHandle["bookId"])

    val book: StateFlow<Book?> = repository.book(bookId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Borrador del comentario; nulo hasta que se carga el libro. */
    val commentDraft = MutableStateFlow<String?>(null)
    private var commentSaveJob: Job? = null
    private var commentPending = false

    private val messageChannel = Channel<Int>(Channel.BUFFERED)
    val messages: Flow<Int> = messageChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            commentDraft.value = repository.book(bookId).first()?.comment.orEmpty()
        }
    }

    fun setStatus(status: ReadingStatus) {
        viewModelScope.launch {
            val wasRead = book.value?.status == ReadingStatus.READ
            repository.setStatus(bookId, status)
            if (status == ReadingStatus.READ && !wasRead) send(R.string.detail_finished_prompt)
        }
    }

    fun setRating(halves: Int?) {
        viewModelScope.launch { repository.setRating(bookId, halves) }
    }

    fun setStartedOn(date: LocalDate) = saveDates(date, book.value?.finishedOn)

    fun setFinishedOn(date: LocalDate) = saveDates(book.value?.startedOn, date)

    private fun saveDates(startedOn: LocalDate?, finishedOn: LocalDate?) {
        viewModelScope.launch {
            if (!repository.setDates(bookId, startedOn, finishedOn)) send(R.string.detail_invalid_dates)
        }
    }

    /** El comentario se guarda solo, poco después de dejar de escribir. */
    fun onCommentChange(text: String) {
        commentDraft.value = text
        commentPending = true
        commentSaveJob?.cancel()
        commentSaveJob = viewModelScope.launch {
            delay(COMMENT_SAVE_DELAY_MS)
            commentPending = false
            repository.setComment(bookId, text)
        }
    }

    fun delete(onDeleted: () -> Unit) {
        commentPending = false
        commentSaveJob?.cancel()
        viewModelScope.launch {
            repository.delete(bookId)
            onDeleted()
        }
    }

    override fun onCleared() {
        // Si se sale antes de que venza la espera, el comentario se guarda igualmente.
        val text = commentDraft.value
        if (commentPending && text != null) {
            applicationScope.launch { repository.setComment(bookId, text) }
        }
    }

    private suspend fun send(@StringRes message: Int) = messageChannel.send(message)

    companion object {
        val Factory = almaViewModelFactory { container, handle ->
            BookDetailViewModel(handle, container.repository, container.applicationScope)
        }
    }
}
