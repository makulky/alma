package com.alma.app.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alma.app.data.BookRepository
import com.alma.app.domain.ReadingStats
import com.alma.app.domain.computeReadingStats
import com.alma.app.ui.almaViewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class StatsViewModel(repository: BookRepository) : ViewModel() {
    val stats: StateFlow<ReadingStats?> = repository.books
        .map(::computeReadingStats)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    companion object {
        val Factory = almaViewModelFactory { container, _ -> StatsViewModel(container.repository) }
    }
}
