package com.alma.app.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alma.app.data.BookRepository
import com.alma.app.domain.Book
import com.alma.app.domain.ReadingStatus
import com.alma.app.ui.almaViewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class LibrarySort { DATE, RATING }

data class LibraryUiState(
    val tab: ReadingStatus = ReadingStatus.READING,
    val filter: String = "",
    val sort: LibrarySort = LibrarySort.DATE,
    val books: List<Book> = emptyList(),
    val isLoading: Boolean = true,
)

class LibraryViewModel(repository: BookRepository) : ViewModel() {
    private val tab = MutableStateFlow(ReadingStatus.READING)
    private val filter = MutableStateFlow("")
    private val sort = MutableStateFlow(LibrarySort.DATE)

    val state: StateFlow<LibraryUiState> = combine(repository.books, tab, filter, sort) { books, tab, filter, sort ->
        val query = filter.trim()
        LibraryUiState(
            tab = tab,
            filter = filter,
            sort = sort,
            books = books
                .filter { it.status == tab }
                .filter { query.isEmpty() || it.title.contains(query, true) || it.authors.contains(query, true) }
                .sortedWith(comparator(tab, sort)),
            isLoading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

    fun selectTab(value: ReadingStatus) { tab.value = value }
    fun setFilter(value: String) { filter.value = value }
    fun setSort(value: LibrarySort) { sort.value = value }

    private fun comparator(tab: ReadingStatus, sort: LibrarySort): Comparator<Book> {
        val byDate: Comparator<Book> = when (tab) {
            ReadingStatus.PENDING -> compareByDescending { it.addedAt }
            ReadingStatus.READING -> compareByDescending<Book, java.time.LocalDate?>(nullsFirst()) { it.startedOn }
            ReadingStatus.READ -> compareByDescending<Book, java.time.LocalDate?>(nullsFirst()) { it.finishedOn }
        }
        return if (sort == LibrarySort.RATING && tab == ReadingStatus.READ) {
            compareByDescending<Book> { it.ratingHalves ?: 0 }.then(byDate)
        } else {
            byDate
        }
    }

    companion object {
        val Factory = almaViewModelFactory { container, _ -> LibraryViewModel(container.repository) }
    }
}
