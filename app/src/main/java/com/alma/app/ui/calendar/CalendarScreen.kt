package com.alma.app.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alma.app.R
import com.alma.app.domain.CalendarEvent
import com.alma.app.domain.CalendarEventType
import com.alma.app.domain.monthGrid
import com.alma.app.ui.components.BookCover
import com.alma.app.ui.formatDate
import com.alma.app.ui.formatMonth
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onBookClick: (Long) -> Unit,
    viewModel: CalendarViewModel = viewModel(factory = CalendarViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val selectedEvents = state.selectedDay?.let { state.events[it] }.orEmpty()

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.nav_calendar)) }) },
    ) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize()) {
            item {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    MonthHeader(state, viewModel::previousMonth, viewModel::nextMonth)
                    MonthGrid(state, viewModel::selectDay)
                    Legend()
                }
            }
            val day = state.selectedDay
            if (day == null) {
                item { Hint(stringResource(R.string.calendar_select_day)) }
            } else if (selectedEvents.isEmpty()) {
                item { Hint(stringResource(R.string.calendar_no_events)) }
            } else {
                item {
                    Text(
                        text = formatDate(day),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                items(selectedEvents, key = { "${it.book.id}-${it.type}" }) { event ->
                    EventRow(event, onClick = { onBookClick(event.book.id) })
                }
            }
        }
    }
}

@Composable
private fun MonthHeader(state: CalendarUiState, onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.Filled.ChevronLeft, stringResource(R.string.calendar_previous))
        }
        Text(
            text = formatMonth(state.month),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onNext) {
            Icon(Icons.Filled.ChevronRight, stringResource(R.string.calendar_next))
        }
    }
}

@Composable
private fun MonthGrid(state: CalendarUiState, onSelect: (LocalDate) -> Unit) {
    val weekdays = stringArrayResource(R.array.calendar_weekdays)
    val cells = remember(state.month) { monthGrid(state.month).chunked(7) }
    Row(Modifier.fillMaxWidth()) {
        weekdays.forEach { label ->
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
        }
    }
    cells.forEach { week ->
        Row(Modifier.fillMaxWidth()) {
            week.forEach { date ->
                Box(Modifier.weight(1f).aspectRatio(1f), contentAlignment = Alignment.Center) {
                    if (date != null) {
                        DayCell(
                            date = date,
                            events = state.events[date].orEmpty(),
                            isSelected = date == state.selectedDay,
                            onClick = { onSelect(date) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(date: LocalDate, events: List<CalendarEvent>, isSelected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val hasStart = events.any { it.type == CalendarEventType.STARTED }
    val hasFinish = events.any { it.type == CalendarEventType.FINISHED }
    val startedLabel = stringResource(R.string.calendar_started)
    val finishedLabel = stringResource(R.string.calendar_finished)
    val description = listOfNotNull(
        formatDate(date),
        startedLabel.takeIf { hasStart },
        finishedLabel.takeIf { hasFinish },
    ).joinToString(", ")
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(2.dp)
            .clip(CircleShape)
            .background(if (isSelected) colors.primaryContainer else Color.Transparent)
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = if (date == LocalDate.now()) colors.primary else colors.onSurface,
        )
        Row(Modifier.height(8.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            if (hasStart) Dot(colors.primary)
            if (hasFinish) Dot(colors.tertiary)
        }
    }
}

@Composable
private fun Dot(color: Color) {
    Box(
        Modifier
            .padding(top = 2.dp)
            .size(6.dp)
            .background(color, CircleShape),
    )
}

@Composable
private fun Legend() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
    ) {
        LegendItem(MaterialTheme.colorScheme.primary, stringResource(R.string.calendar_started))
        LegendItem(MaterialTheme.colorScheme.tertiary, stringResource(R.string.calendar_finished))
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Dot(color)
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun Hint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    )
}

@Composable
private fun EventRow(event: CalendarEvent, onClick: () -> Unit) {
    val label = when (event.type) {
        CalendarEventType.STARTED -> R.string.calendar_started
        CalendarEventType.FINISHED -> R.string.calendar_finished
    }
    ListItem(
        headlineContent = { Text(event.book.title) },
        supportingContent = { Text(stringResource(label)) },
        leadingContent = { BookCover(event.book.coverUri, width = 40.dp, height = 60.dp) },
        modifier = Modifier.clickable(onClick = onClick),
    )
}
