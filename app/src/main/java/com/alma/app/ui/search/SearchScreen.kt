package com.alma.app.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alma.app.R
import com.alma.app.domain.ReadingStatus
import com.alma.app.ui.components.BookCover
import com.alma.app.ui.components.BookTitleAndAuthor
import com.alma.app.ui.components.CenteredMessage
import com.alma.app.ui.labelRes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onBookClick: (Long) -> Unit,
    onAddManually: (String) -> Unit,
    viewModel: SearchViewModel = viewModel(factory = SearchViewModel.Factory),
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val savedMessage = stringResource(R.string.search_saved)
    val readMessage = stringResource(R.string.search_saved_read)
    val rateAction = stringResource(R.string.search_rate_action)

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            if (event.status == ReadingStatus.READ) {
                val result = snackbar.showSnackbar(readMessage, actionLabel = rateAction)
                if (result == SnackbarResult.ActionPerformed) onBookClick(event.bookId)
            } else {
                snackbar.showSnackbar(savedMessage)
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.nav_search)) }) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.query.value = it },
                placeholder = { Text(stringResource(R.string.search_hint)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.query.value = "" }) {
                            Icon(Icons.Filled.Clear, stringResource(R.string.cancel))
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
            when (val current = state) {
                SearchUiState.Idle -> CenteredMessage(stringResource(R.string.search_idle))
                SearchUiState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator()
                }
                SearchUiState.Empty -> CenteredMessage(stringResource(R.string.search_empty)) {
                    TextButton(onClick = { onAddManually(query.trim()) }) {
                        Text(stringResource(R.string.search_add_manually))
                    }
                }
                SearchUiState.Offline -> RetryMessage(R.string.search_offline, viewModel::retry)
                SearchUiState.Rejected -> RetryMessage(R.string.search_rejected, viewModel::retry)
                SearchUiState.Failed -> RetryMessage(R.string.search_failed, viewModel::retry)
                is SearchUiState.Results -> LazyColumn(Modifier.fillMaxSize()) {
                    items(current.items, key = { it.result.sourceId }) { item ->
                        SearchRow(item, onBookClick, viewModel::save)
                    }
                    item {
                        TextButton(
                            onClick = { onAddManually(query.trim()) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                        ) {
                            Text(stringResource(R.string.search_add_manually))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RetryMessage(message: Int, onRetry: () -> Unit) {
    CenteredMessage(stringResource(message)) {
        Button(onClick = onRetry, modifier = Modifier.padding(top = 16.dp)) {
            Text(stringResource(R.string.retry))
        }
    }
}

@Composable
private fun SearchRow(
    item: SearchItem,
    onBookClick: (Long) -> Unit,
    onSave: (com.alma.app.data.remote.SearchResult, ReadingStatus) -> Unit,
) {
    val saved = item.savedBook
    val rowModifier = if (saved != null) Modifier.clickable { onBookClick(saved.id) } else Modifier
    Row(
        modifier = rowModifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BookCover(item.result.coverUri)
        Column(Modifier.weight(1f)) {
            BookTitleAndAuthor(item.result.title, item.result.authors)
            if (saved != null) {
                AssistChip(
                    onClick = { onBookClick(saved.id) },
                    label = { Text(stringResource(saved.status.labelRes())) },
                )
            } else {
                SaveMenu(onSelect = { onSave(item.result, it) })
            }
        }
    }
}

@Composable
private fun SaveMenu(onSelect: (ReadingStatus) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        FilledTonalButton(onClick = { expanded = true }) {
            Text(stringResource(R.string.save))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ReadingStatus.entries.forEach { status ->
                DropdownMenuItem(
                    text = { Text(stringResource(status.labelRes())) },
                    onClick = {
                        expanded = false
                        onSelect(status)
                    },
                )
            }
        }
    }
}
