package com.alma.app.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alma.app.data.BookRepository
import com.alma.app.domain.CalendarEvent
import com.alma.app.domain.calendarEvents
import com.alma.app.ui.almaViewModelFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth

data class CalendarUiState(
    val month: YearMonth,
    val selectedDay: LocalDate? = null,
    val events: Map<LocalDate, List<CalendarEvent>> = emptyMap(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModel(repository: BookRepository) : ViewModel() {
    private val month = MutableStateFlow(YearMonth.now())
    private val selectedDay = MutableStateFlow<LocalDate?>(null)

    private val events = month.flatMapLatest { current ->
        repository.booksWithDatesBetween(current.atDay(1), current.atEndOfMonth())
            .map { current to calendarEvents(it, current) }
    }

    val state: StateFlow<CalendarUiState> = combine(events, selectedDay) { (current, events), day ->
        CalendarUiState(current, day, events)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CalendarUiState(YearMonth.now()))

    fun previousMonth() = changeMonth(month.value.minusMonths(1))
    fun nextMonth() = changeMonth(month.value.plusMonths(1))
    fun selectDay(day: LocalDate) { selectedDay.value = day }

    private fun changeMonth(value: YearMonth) {
        selectedDay.value = null
        month.value = value
    }

    companion object {
        val Factory = almaViewModelFactory { container, _ -> CalendarViewModel(container.repository) }
    }
}
