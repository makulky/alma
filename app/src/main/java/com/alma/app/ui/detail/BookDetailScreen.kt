package com.alma.app.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alma.app.R
import com.alma.app.domain.Book
import com.alma.app.domain.ReadingStatus
import com.alma.app.ui.components.BookCover
import com.alma.app.ui.components.BookTitleAndAuthor
import com.alma.app.ui.components.StarRatingInput
import com.alma.app.ui.formatDate
import com.alma.app.ui.labelRes
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailScreen(
    onBack: () -> Unit,
    viewModel: BookDetailViewModel = viewModel(factory = BookDetailViewModel.Factory),
) {
    val book by viewModel.book.collectAsStateWithLifecycle()
    val commentDraft by viewModel.commentDraft.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { snackbar.showSnackbar(context.getString(it)) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Filled.Delete, stringResource(R.string.detail_delete))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        book?.let { current ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .consumeWindowInsets(padding)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                BookHeader(current)
                StatusSelector(current.status, viewModel::setStatus)
                DateRows(current, viewModel::setStartedOn, viewModel::setFinishedOn)
                if (current.status == ReadingStatus.READ) {
                    RatingSection(current.ratingHalves, viewModel::setRating)
                    commentDraft?.let { draft ->
                        OutlinedTextField(
                            value = draft,
                            onValueChange = viewModel::onCommentChange,
                            label = { Text(stringResource(R.string.detail_comment)) },
                            minLines = 3,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                current.description?.let { description ->
                    Column {
                        Text(stringResource(R.string.detail_synopsis), style = MaterialTheme.typography.titleSmall)
                        Text(description, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.detail_delete_title)) },
            text = { Text(stringResource(R.string.detail_delete_message)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.delete(onDeleted = onBack)
                }) {
                    Text(stringResource(R.string.detail_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun BookHeader(book: Book) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        BookCover(book.coverUri, width = 96.dp, height = 144.dp)
        Column(Modifier.weight(1f)) {
            BookTitleAndAuthor(book.title, book.authors)
            val details = listOfNotNull(book.publisher, book.publishedYear?.toString()).joinToString(" · ")
            if (details.isNotEmpty()) {
                Text(details, style = MaterialTheme.typography.bodySmall)
            }
            book.pageCount?.let {
                Text(stringResource(R.string.detail_pages, it), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusSelector(selected: ReadingStatus, onSelect: (ReadingStatus) -> Unit) {
    val statuses = ReadingStatus.entries
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        statuses.forEachIndexed { index, status ->
            SegmentedButton(
                selected = status == selected,
                onClick = { onSelect(status) },
                shape = SegmentedButtonDefaults.itemShape(index, statuses.size),
            ) {
                Text(stringResource(status.labelRes()), maxLines = 1)
            }
        }
    }
}

@Composable
private fun DateRows(book: Book, onStarted: (LocalDate) -> Unit, onFinished: (LocalDate) -> Unit) {
    if (book.status == ReadingStatus.PENDING) return
    Column {
        DateRow(
            label = stringResource(R.string.detail_started),
            date = book.startedOn,
            latest = book.finishedOn ?: LocalDate.now(),
            earliest = null,
            onSelect = onStarted,
        )
        if (book.status == ReadingStatus.READ) {
            DateRow(
                label = stringResource(R.string.detail_finished),
                date = book.finishedOn,
                latest = LocalDate.now(),
                earliest = book.startedOn,
                onSelect = onFinished,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateRow(
    label: String,
    date: LocalDate?,
    latest: LocalDate,
    earliest: LocalDate?,
    onSelect: (LocalDate) -> Unit,
) {
    var showPicker by rememberSaveable { mutableStateOf(false) }
    ListItem(
        headlineContent = { Text(label) },
        trailingContent = { Text(date?.let(::formatDate) ?: stringResource(R.string.detail_no_date)) },
        modifier = Modifier.clickable { showPicker = true },
    )
    if (showPicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = date?.toUtcMillis(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val candidate = utcTimeMillis.toLocalDate()
                    return !candidate.isAfter(latest) && (earliest == null || !candidate.isBefore(earliest))
                }
            },
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { onSelect(it.toLocalDate()) }
                    showPicker = false
                }) {
                    Text(stringResource(R.string.accept))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

// El selector de fechas de Material trabaja con medianoche en UTC.
private fun LocalDate.toUtcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.toLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

@Composable
private fun RatingSection(ratingHalves: Int?, onChange: (Int?) -> Unit) {
    Column {
        Text(stringResource(R.string.detail_rating), style = MaterialTheme.typography.titleSmall)
        StarRatingInput(ratingHalves, onChange = onChange)
        if (ratingHalves != null) {
            TextButton(onClick = { onChange(null) }) {
                Text(stringResource(R.string.detail_remove_rating))
            }
        }
    }
}
