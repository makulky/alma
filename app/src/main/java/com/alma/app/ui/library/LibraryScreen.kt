package com.alma.app.ui.library

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alma.app.R
import com.alma.app.domain.ReadingStatus
import com.alma.app.ui.components.BookRow
import com.alma.app.ui.components.CenteredMessage

private val Tabs = listOf(
    ReadingStatus.READING to R.string.tab_reading,
    ReadingStatus.PENDING to R.string.tab_pending,
    ReadingStatus.READ to R.string.tab_read,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onBookClick: (Long) -> Unit,
    onAddClick: () -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: LibraryViewModel = viewModel(factory = LibraryViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Image(
                        painter = painterResource(R.drawable.logo_alma),
                        contentDescription = stringResource(R.string.app_name),
                        modifier = Modifier.height(36.dp),
                    )
                },
                actions = {
                    if (state.tab == ReadingStatus.READ) {
                        SortMenu(onSelect = viewModel::setSort)
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Filled.Settings, stringResource(R.string.library_settings))
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(Icons.Filled.Add, stringResource(R.string.library_add))
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            PrimaryTabRow(selectedTabIndex = Tabs.indexOfFirst { it.first == state.tab }) {
                Tabs.forEach { (status, label) ->
                    Tab(
                        selected = state.tab == status,
                        onClick = { viewModel.selectTab(status) },
                        text = { Text(stringResource(label)) },
                    )
                }
            }
            OutlinedTextField(
                value = state.filter,
                onValueChange = viewModel::setFilter,
                placeholder = { Text(stringResource(R.string.library_filter)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (state.filter.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setFilter("") }) {
                            Icon(Icons.Filled.Clear, stringResource(R.string.cancel))
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
            when {
                state.isLoading -> Unit
                state.books.isEmpty() -> CenteredMessage(stringResource(emptyMessage(state)))
                else -> LazyColumn(Modifier.fillMaxSize()) {
                    items(state.books, key = { it.id }) { book ->
                        BookRow(book, onClick = { onBookClick(book.id) })
                    }
                }
            }
        }
    }
}

private fun emptyMessage(state: LibraryUiState): Int = when {
    state.filter.isNotBlank() -> R.string.library_empty_filter
    state.tab == ReadingStatus.READING -> R.string.library_empty_reading
    state.tab == ReadingStatus.PENDING -> R.string.library_empty_pending
    else -> R.string.library_empty_read
}

@Composable
private fun SortMenu(onSelect: (LibrarySort) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.AutoMirrored.Filled.Sort, stringResource(R.string.library_sort))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.library_sort_date)) },
                onClick = {
                    onSelect(LibrarySort.DATE)
                    expanded = false
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.library_sort_rating)) },
                onClick = {
                    onSelect(LibrarySort.RATING)
                    expanded = false
                },
            )
        }
    }
}
