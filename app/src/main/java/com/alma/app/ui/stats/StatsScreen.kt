package com.alma.app.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alma.app.R
import com.alma.app.domain.Book
import com.alma.app.domain.ReadingStats
import com.alma.app.domain.formatRating
import com.alma.app.ui.components.BookRow
import com.alma.app.ui.components.CenteredMessage
import com.alma.app.ui.formatMonthName
import java.util.Locale

private const val UNDATED_KEY = "undated"
private val HistoryLabelWidth = 88.dp
private val MonthIndent = 16.dp

private val ExpandedKeysSaver = Saver<Set<String>, ArrayList<String>>(
    save = { ArrayList(it) },
    restore = { it.toSet() },
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    onBookClick: (Long) -> Unit,
    viewModel: StatsViewModel = viewModel(factory = StatsViewModel.Factory),
) {
    val stats by viewModel.stats.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.nav_stats)) }) },
    ) { padding ->
        val current = stats
        when {
            current == null -> Unit
            current.readCount == 0 -> CenteredMessage(stringResource(R.string.stats_empty), Modifier.padding(padding))
            else -> StatsContent(current, onBookClick, Modifier.padding(padding))
        }
    }
}

@Composable
private fun StatsContent(stats: ReadingStats, onBookClick: (Long) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(stringResource(R.string.stats_read_total), stats.readCount.toString(), Modifier.weight(1f))
            StatCard(
                label = stringResource(R.string.stats_average),
                value = stats.averageStars?.let { String.format(Locale.forLanguageTag("es"), "%.2f ★", it) }
                    ?: stringResource(R.string.stats_no_rating),
                modifier = Modifier.weight(1f),
            )
        }
        ReadingHistorySection(stats, onBookClick)
        if (stats.averageStars != null) {
            BarSection(
                title = stringResource(R.string.stats_distribution),
                bars = stats.ratingDistribution.reversed().map { (halves, count) -> "${formatRating(halves)} ★" to count },
            )
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(value, style = MaterialTheme.typography.headlineSmall)
            Text(label, style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** Años desplegables en meses, y meses desplegables en los libros leídos. */
@Composable
private fun ReadingHistorySection(stats: ReadingStats, onBookClick: (Long) -> Unit) {
    var expanded by rememberSaveable(stateSaver = ExpandedKeysSaver) { mutableStateOf(emptySet<String>()) }
    fun toggle(key: String) {
        expanded = if (key in expanded) expanded - key else expanded + key
    }

    val maxPerYear = (stats.history.map { it.count } + stats.undatedBooks.size).max().coerceAtLeast(1)
    Column {
        Text(stringResource(R.string.stats_per_year), style = MaterialTheme.typography.titleSmall)
        stats.history.forEach { year ->
            val yearKey = year.year.toString()
            ExpandableBarRow(year.year.toString(), year.count, maxPerYear, yearKey in expanded, { toggle(yearKey) })
            if (yearKey in expanded) {
                val maxPerMonth = year.months.maxOf { it.books.size }
                year.months.forEach { month ->
                    val monthKey = month.month.toString()
                    ExpandableBarRow(
                        label = formatMonthName(month.month),
                        count = month.books.size,
                        max = maxPerMonth,
                        isExpanded = monthKey in expanded,
                        onToggle = { toggle(monthKey) },
                        modifier = Modifier.padding(start = MonthIndent),
                    )
                    if (monthKey in expanded) HistoryBooks(month.books, onBookClick)
                }
            }
        }
        if (stats.undatedBooks.isNotEmpty()) {
            ExpandableBarRow(
                label = stringResource(R.string.stats_undated),
                count = stats.undatedBooks.size,
                max = maxPerYear,
                isExpanded = UNDATED_KEY in expanded,
                onToggle = { toggle(UNDATED_KEY) },
            )
            if (UNDATED_KEY in expanded) HistoryBooks(stats.undatedBooks, onBookClick)
        }
    }
}

@Composable
private fun HistoryBooks(books: List<Book>, onBookClick: (Long) -> Unit) {
    books.forEach { book ->
        BookRow(book, onClick = { onBookClick(book.id) }, modifier = Modifier.padding(start = MonthIndent))
    }
}

@Composable
private fun ExpandableBarRow(
    label: String,
    count: Int,
    max: Int,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = stringResource(if (isExpanded) R.string.stats_expanded else R.string.stats_collapsed)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClick = onToggle)
            .semantics(mergeDescendants = true) { stateDescription = state },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = if (isExpanded) Icons.Filled.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
        )
        BarContent(label, count, max, HistoryLabelWidth)
    }
}

@Composable
private fun BarSection(title: String, bars: List<Pair<String, Int>>) {
    val max = bars.maxOf { it.second }.coerceAtLeast(1)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        bars.forEach { (label, count) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BarContent(label, count, max, 48.dp)
            }
        }
    }
}

@Composable
private fun RowScope.BarContent(label: String, count: Int, max: Int, labelWidth: Dp) {
    Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(labelWidth))
    Box(Modifier.weight(1f)) {
        if (count > 0) {
            Box(
                Modifier
                    .fillMaxWidth(count.toFloat() / max)
                    .height(16.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp)),
            )
        }
    }
    Text(
        text = count.toString(),
        style = MaterialTheme.typography.bodySmall,
        textAlign = TextAlign.End,
        modifier = Modifier.width(32.dp),
    )
}
