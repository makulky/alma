package com.alma.app.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alma.app.data.BookRepository
import com.alma.app.data.SearchOutcome
import com.alma.app.data.remote.SearchResult
import com.alma.app.domain.Book
import com.alma.app.domain.ReadingStatus
import com.alma.app.ui.almaViewModelFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val SEARCH_DEBOUNCE_MS = 400L

data class SearchItem(val result: SearchResult, val savedBook: Book?)

sealed interface SearchUiState {
    data object Idle : SearchUiState
    data object Loading : SearchUiState
    data object Empty : SearchUiState
    data object Offline : SearchUiState
    data object Rejected : SearchUiState
    data object Failed : SearchUiState
    data class Results(val items: List<SearchItem>) : SearchUiState
}

data class SavedEvent(val bookId: Long, val status: ReadingStatus)

private sealed interface SearchPhase {
    data object Idle : SearchPhase
    data object Loading : SearchPhase
    data class Done(val outcome: SearchOutcome) : SearchPhase
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class SearchViewModel(private val repository: BookRepository) : ViewModel() {
    val query = MutableStateFlow("")
    private val retries = MutableStateFlow(0)

    private val savedEvents = Channel<SavedEvent>(Channel.BUFFERED)
    val events: Flow<SavedEvent> = savedEvents.receiveAsFlow()

    private val phases: Flow<SearchPhase> = combine(
        query.debounce(SEARCH_DEBOUNCE_MS).map { it.trim() }.distinctUntilChanged(),
        retries,
    ) { text, _ -> text }.flatMapLatest { text ->
        if (text.isEmpty()) {
            flowOf<SearchPhase>(SearchPhase.Idle)
        } else {
            flow<SearchPhase> {
                emit(SearchPhase.Loading)
                emit(SearchPhase.Done(repository.search(text)))
            }
        }
    }

    val state: StateFlow<SearchUiState> = combine(phases, repository.books) { phase, books ->
        when (phase) {
            SearchPhase.Idle -> SearchUiState.Idle
            SearchPhase.Loading -> SearchUiState.Loading
            is SearchPhase.Done -> when (val outcome = phase.outcome) {
                SearchOutcome.Offline -> SearchUiState.Offline
                SearchOutcome.Rejected -> SearchUiState.Rejected
                SearchOutcome.Failed -> SearchUiState.Failed
                is SearchOutcome.Success ->
                    if (outcome.results.isEmpty()) {
                        SearchUiState.Empty
                    } else {
                        SearchUiState.Results(outcome.results.map { SearchItem(it, books.matching(it)) })
                    }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUiState.Idle)

    fun retry() { retries.value++ }

    fun save(result: SearchResult, status: ReadingStatus) {
        viewModelScope.launch {
            val id = repository.saveFromSearch(result, status)
            savedEvents.send(SavedEvent(id, status))
        }
    }

    private fun List<Book>.matching(result: SearchResult): Book? =
        firstOrNull { it.sourceId == result.sourceId }
            ?: firstOrNull { result.isbn13 != null && it.isbn13 == result.isbn13 }

    companion object {
        val Factory = almaViewModelFactory { container, _ -> SearchViewModel(container.repository) }
    }
}
