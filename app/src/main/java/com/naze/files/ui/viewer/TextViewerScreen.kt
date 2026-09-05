package com.naze.files.ui.viewer

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WrapText
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.naze.files.data.model.FileItem
import com.naze.files.util.SyntaxColors
import com.naze.files.util.SyntaxHighlighter
import com.naze.files.util.TextFileContent
import com.naze.files.util.TextFileReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TextViewerScreen(
    item: FileItem,
    onShare: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onShowInfo: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    var content by remember(item.absolutePath) { mutableStateOf<TextFileContent?>(null) }
    var loadError by remember(item.absolutePath) { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf(false) }
    var editedText by remember { mutableStateOf("") }
    var wordWrap by remember { mutableStateOf(true) }
    var fontSize by remember { mutableStateOf(13) }
    var searchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var menuExpanded by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = remember { LazyListState() }
    val horizontalScrollState = rememberScrollState()

    LaunchedEffect(item.absolutePath) {
        loadError = null
        try {
            val loaded = withContext(Dispatchers.IO) { TextFileReader.read(File(item.absolutePath)) }
            content = loaded
            editedText = loaded.text
        } catch (e: Exception) {
            loadError = e.message ?: "Unable to open this file."
        }
    }

    val lines = remember(content?.text) { content?.text?.split("\n") ?: emptyList() }
    val matchingLineIndices = remember(searchQuery, lines) {
        if (searchQuery.isBlank()) emptyList() else lines.indices.filter { lines[it].contains(searchQuery, ignoreCase = true) }
    }
    var currentMatchPointer by remember(searchQuery) { mutableStateOf(0) }

    val colors = SyntaxColors(
        keyword = MaterialTheme.colorScheme.primary,
        string = MaterialTheme.colorScheme.secondary,
        comment = MaterialTheme.colorScheme.onSurfaceVariant,
        number = MaterialTheme.colorScheme.tertiary,
    )
    val highlightEnabled = (content?.text?.length ?: 0) <= TextFileReader.HIGHLIGHT_SIZE_LIMIT

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        if (searchActive) {
                            TextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it; currentMatchPointer = 0 },
                                placeholder = { Text("Search in file") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        } else {
                            Text(item.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                if (searchActive) {
                                    searchActive = false
                                    searchQuery = ""
                                } else {
                                    onNavigateBack()
                                }
                            },
                        ) {
                            Icon(
                                imageVector = if (searchActive) Icons.Filled.Close else Icons.Filled.ArrowBack,
                                contentDescription = "Back",
                            )
                        }
                    },
                    actions = {
                        if (editing) {
                            IconButton(onClick = {
                                scope.launch {
                                    try {
                                        withContext(Dispatchers.IO) { TextFileReader.write(File(item.absolutePath), editedText) }
                                        content = TextFileContent(editedText, truncated = false)
                                        editing = false
                                        snackbarHostState.showSnackbar("Saved")
                                    } catch (e: Exception) {
                                        snackbarHostState.showSnackbar(e.message ?: "Could not save")
                                    }
                                }
                            }) {
                                Icon(imageVector = Icons.Filled.Save, contentDescription = "Save")
                            }
                        } else if (!searchActive) {
                            IconButton(onClick = { searchActive = true }) {
                                Icon(imageVector = Icons.Filled.Search, contentDescription = "Search")
                            }
                            IconButton(onClick = { wordWrap = !wordWrap }) {
                                Icon(imageVector = Icons.Filled.WrapText, contentDescription = "Word wrap")
                            }
                            IconButton(onClick = { fontSize = (fontSize - 1).coerceAtLeast(9) }) {
                                Icon(imageVector = Icons.Filled.ZoomOut, contentDescription = "Zoom out")
                            }
                            IconButton(onClick = { fontSize = (fontSize + 1).coerceAtMost(24) }) {
                                Icon(imageVector = Icons.Filled.ZoomIn, contentDescription = "Zoom in")
                            }
                            IconButton(onClick = { editing = true }) {
                                Icon(imageVector = Icons.Filled.Edit, contentDescription = "Edit")
                            }
                            IconButton(onClick = { menuExpanded = true }) {
                                Icon(imageVector = Icons.Filled.MoreVert, contentDescription = "More")
                            }
                            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                                DropdownMenuItem(
                                    text = { Text("Copy all") },
                                    leadingIcon = { Icon(Icons.Filled.ContentCopy, contentDescription = null) },
                                    onClick = {
                                        menuExpanded = false
                                        content?.let { clipboard.setText(AnnotatedString(it.text)) }
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("Share") },
                                    leadingIcon = { Icon(Icons.Filled.Share, contentDescription = null) },
                                    onClick = { menuExpanded = false; onShare() },
                                )
                                DropdownMenuItem(text = { Text("Rename") }, onClick = { menuExpanded = false; onRename() })
                                DropdownMenuItem(text = { Text("Delete") }, onClick = { menuExpanded = false; onDelete() })
                                DropdownMenuItem(text = { Text("Information") }, onClick = { menuExpanded = false; onShowInfo() })
                            }
                        }
                    },
                )
                if (searchActive && matchingLineIndices.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "${currentMatchPointer + 1} / ${matchingLineIndices.size} matches",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = {
                            currentMatchPointer = (currentMatchPointer - 1 + matchingLineIndices.size) % matchingLineIndices.size
                            scope.launch { listState.animateScrollToItem(matchingLineIndices[currentMatchPointer]) }
                        }) { Icon(Icons.Filled.ArrowUpward, contentDescription = "Previous match") }
                        IconButton(onClick = {
                            currentMatchPointer = (currentMatchPointer + 1) % matchingLineIndices.size
                            scope.launch { listState.animateScrollToItem(matchingLineIndices[currentMatchPointer]) }
                        }) { Icon(Icons.Filled.ArrowDownward, contentDescription = "Next match") }
                    }
                }
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                loadError != null -> Text(
                    text = loadError ?: "",
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    color = MaterialTheme.colorScheme.error,
                )
                content == null -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                editing -> OutlinedTextField(
                    value = editedText,
                    onValueChange = { editedText = it },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = fontSize.sp),
                )
                else -> Column(modifier = Modifier.fillMaxSize()) {
                    if (content?.truncated == true) {
                        Text(
                            text = "Showing the first part of this file - it's large enough that the rest was not loaded into memory.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(8.dp),
                        )
                    }
                    SelectionContainer {
                        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                            items(lines.size, key = { it }) { index ->
                                val lineText = lines[index]
                                val isMatch = index in matchingLineIndices
                                val annotated = remember(lineText, highlightEnabled) {
                                    if (highlightEnabled) {
                                        SyntaxHighlighter.highlight(lineText, item.extension, colors)
                                    } else {
                                        AnnotatedString(lineText)
                                    }
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .let { if (isMatch) it.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)) else it },
                                ) {
                                    Text(
                                        text = (index + 1).toString(),
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = fontSize.sp,
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier
                                            .width((24 + fontSize).dp)
                                            .padding(end = 8.dp),
                                    )
                                    Box(
                                        modifier = if (wordWrap) Modifier.weight(1f) else Modifier.horizontalScroll(horizontalScrollState),
                                    ) {
                                        Text(
                                            text = annotated,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = fontSize.sp,
                                            ),
                                            softWrap = wordWrap,
                                            overflow = if (wordWrap) TextOverflow.Clip else TextOverflow.Visible,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
