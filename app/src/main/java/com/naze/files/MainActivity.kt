package com.naze.files

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.naze.files.data.archive.ArchiveRepository
import com.naze.files.data.favorites.FavoritesRepository
import com.naze.files.data.model.FileCategory
import com.naze.files.data.model.FileItem
import com.naze.files.data.operations.FileOperationsRepository
import com.naze.files.data.recent.RecentFilesRepository
import com.naze.files.data.repository.FileRepositoryImpl
import com.naze.files.data.settings.SettingsRepository
import com.naze.files.data.storage.StorageAccessManager
import com.naze.files.data.trash.TrashRepository
import com.naze.files.data.viewer.ViewerRoute
import com.naze.files.data.viewer.ViewerRouter
import com.naze.files.media.AudioPlayerController
import com.naze.files.ui.archive.ArchiveViewerScreen
import com.naze.files.ui.browser.FileBrowserScreen
import com.naze.files.ui.browser.FileBrowserViewModel
import com.naze.files.ui.browser.components.DeleteConfirmDialog
import com.naze.files.ui.browser.components.RenameDialog
import com.naze.files.ui.category.CategoryScreen
import com.naze.files.ui.favorites.FavoritesScreen
import com.naze.files.ui.favorites.FavoritesViewModel
import com.naze.files.ui.home.HomeScreen
import com.naze.files.ui.incoming.SaveIncomingScreen
import com.naze.files.ui.permission.StoragePermissionScreen
import com.naze.files.ui.recent.RecentViewModel
import com.naze.files.ui.recent.RecentScreen
import com.naze.files.ui.settings.SettingsScreen
import com.naze.files.ui.storage.StorageAnalyzerScreen
import com.naze.files.ui.theme.NazeFilesTheme
import com.naze.files.ui.theme.NazeThemeMode
import com.naze.files.ui.viewer.AudioPlayerScreen
import com.naze.files.ui.viewer.FileInfoDialog
import com.naze.files.ui.viewer.ImageViewerScreen
import com.naze.files.ui.viewer.PdfViewerScreen
import com.naze.files.ui.viewer.TextViewerScreen
import com.naze.files.ui.viewer.UnsupportedViewerScreen
import com.naze.files.ui.viewer.VideoPlayerScreen
import com.naze.files.util.ContentUriUtils
import com.naze.files.util.buildOpenWithIntent
import com.naze.files.util.buildShareIntent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private sealed class Screen {
    data object Home : Screen()
    data object Browser : Screen()
    data object Favorites : Screen()
    data object Recent : Screen()
    data object Settings : Screen()
    data object StorageAnalyzer : Screen()
    data class CategoryBrowser(val category: FileCategory) : Screen()
    data class SaveIncoming(val uris: List<Uri>) : Screen()
    data class ImageViewer(val item: FileItem) : Screen()
    data class TextViewer(val item: FileItem) : Screen()
    data class PdfViewer(val item: FileItem) : Screen()
    data class AudioViewer(val item: FileItem, val playlist: List<FileItem>, val startIndex: Int) : Screen()
    data class VideoViewer(val item: FileItem) : Screen()
    data class ArchiveViewer(val item: FileItem) : Screen()
    data class UnsupportedViewer(val item: FileItem, val reason: String) : Screen()
}

@Suppress("DEPRECATION")
private inline fun <reified T : Parcelable> Intent.getParcelableExtraCompat(name: String): T? =
    if (Build.VERSION.SDK_INT >= 33) getParcelableExtra(name, T::class.java) else getParcelableExtra(name)

@Suppress("DEPRECATION")
private inline fun <reified T : Parcelable> Intent.getParcelableArrayListExtraCompat(name: String): ArrayList<T>? =
    if (Build.VERSION.SDK_INT >= 33) getParcelableArrayListExtra(name, T::class.java) else getParcelableArrayListExtra(name)

class MainActivity : ComponentActivity() {

    private lateinit var storageAccessManager: StorageAccessManager
    private val pendingIntentState = mutableStateOf<Intent?>(null)

    private val legacyPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { /* result observed via hasFullStorageAccess() on next ON_RESUME */ }

    private val allFilesAccessLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { /* result observed via hasFullStorageAccess() on next ON_RESUME */ }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* playback works either way; this only affects notification visibility */ }

    private val openTreeLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri != null) {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
            Toast.makeText(
                this,
                "Folder access granted. SAF-scoped browsing arrives in a later phase.",
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.action in setOf(Intent.ACTION_VIEW, Intent.ACTION_SEND, Intent.ACTION_SEND_MULTIPLE)) {
            pendingIntentState.value = intent
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        storageAccessManager = StorageAccessManager(this)
        if (intent?.action in setOf(Intent.ACTION_VIEW, Intent.ACTION_SEND, Intent.ACTION_SEND_MULTIPLE)) {
            pendingIntentState.value = intent
        }

        setContent {
            var permissionGranted by remember { mutableStateOf(storageAccessManager.hasFullStorageAccess()) }
            var screen by remember { mutableStateOf<Screen>(Screen.Home) }
            var infoTarget by remember { mutableStateOf<FileItem?>(null) }
            var renameTarget by remember { mutableStateOf<FileItem?>(null) }
            var deleteTarget by remember { mutableStateOf<FileItem?>(null) }
            val scope = rememberCoroutineScope()
            val pendingIntent by pendingIntentState

            androidx.compose.runtime.DisposableEffect(Unit) {
                val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                    if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                        permissionGranted = storageAccessManager.hasFullStorageAccess()
                    }
                }
                lifecycle.addObserver(observer)
                onDispose { lifecycle.removeObserver(observer) }
            }

            val settingsRepository = remember { SettingsRepository(applicationContext) }
            val settings by settingsRepository.settings.collectAsState(initial = com.naze.files.data.settings.NazeSettings())

            NazeFilesTheme(themeMode = settings.themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (!permissionGranted) {
                        StoragePermissionScreen(
                            onGrantFullAccess = { requestFullStorageAccess() },
                            onPickFolderInstead = { openTreeLauncher.launch(null) },
                        )
                    } else {
                        val roots = remember { storageAccessManager.listStorageRoots() }
                        val primaryRoot = roots.firstOrNull { it.isPrimary } ?: roots.firstOrNull()

                        if (primaryRoot == null) {
                            StoragePermissionScreen(
                                onGrantFullAccess = { requestFullStorageAccess() },
                                onPickFolderInstead = { openTreeLauncher.launch(null) },
                            )
                        } else {
                            val fileRepository = remember { FileRepositoryImpl() }
                            val operationsRepository = remember { FileOperationsRepository() }
                            val trashRepository = remember(primaryRoot.rootFile) {
                                TrashRepository(primaryRoot.rootFile.absolutePath)
                            }
                            val favoritesRepository = remember { FavoritesRepository(applicationContext) }
                            val archiveRepository = remember { ArchiveRepository() }
                            val recentFilesRepository = remember { RecentFilesRepository(applicationContext) }
                            val audioPlayerController = remember { AudioPlayerController(applicationContext) }

                            val browserViewModel: FileBrowserViewModel = viewModel(
                                factory = FileBrowserViewModel.Factory(
                                    repository = fileRepository,
                                    operationsRepository = operationsRepository,
                                    trashRepository = trashRepository,
                                    favoritesRepository = favoritesRepository,
                                    archiveRepository = archiveRepository,
                                    settingsRepository = settingsRepository,
                                    rootPath = primaryRoot.rootFile.absolutePath,
                                    rootLabel = primaryRoot.label,
                                ),
                            )
                            val browserUiState by browserViewModel.uiState.collectAsState()

                            fun shareItem(item: FileItem) {
                                startActivity(buildShareIntent(this@MainActivity, listOf(File(item.absolutePath))))
                            }

                            fun openWithItem(item: FileItem) {
                                startActivity(buildOpenWithIntent(this@MainActivity, File(item.absolutePath), item.mimeType))
                            }

                            fun openFile(item: FileItem) {
                                scope.launch {
                                    val route = ViewerRouter.route(item)
                                    if (route !is ViewerRoute.Unsupported) {
                                        recentFilesRepository.recordOpened(item.absolutePath)
                                    }
                                    when (route) {
                                        is ViewerRoute.Image -> screen = Screen.ImageViewer(item)
                                        is ViewerRoute.TextCode -> screen = Screen.TextViewer(item)
                                        is ViewerRoute.Pdf -> screen = Screen.PdfViewer(item)
                                        is ViewerRoute.Video -> screen = Screen.VideoViewer(item)
                                        is ViewerRoute.Archive -> screen = Screen.ArchiveViewer(item)
                                        is ViewerRoute.Audio -> {
                                            if (Build.VERSION.SDK_INT >= 33) {
                                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                            }
                                            val parent = File(item.absolutePath).parent ?: primaryRoot.rootFile.absolutePath
                                            val siblings = try {
                                                fileRepository.listChildren(parent, includeHidden = false)
                                                    .filter { !it.isDirectory && it.category == FileCategory.AUDIO }
                                                    .sortedBy { it.name.lowercase() }
                                            } catch (e: Exception) {
                                                listOf(item)
                                            }.ifEmpty { listOf(item) }
                                            val startIndex = siblings.indexOfFirst { it.absolutePath == item.absolutePath }.coerceAtLeast(0)
                                            screen = Screen.AudioViewer(item, siblings, startIndex)
                                        }
                                        is ViewerRoute.Unsupported -> screen = Screen.UnsupportedViewer(item, route.reason)
                                    }
                                }
                            }

                            LaunchedEffect(pendingIntent) {
                                val incoming = pendingIntent ?: return@LaunchedEffect
                                when (incoming.action) {
                                    Intent.ACTION_VIEW -> {
                                        val uri = incoming.data
                                        if (uri != null) {
                                            val file = withContext(Dispatchers.IO) {
                                                ContentUriUtils.copyToCacheForViewing(this@MainActivity, uri)
                                            }
                                            if (file != null) {
                                                val stat = fileRepository.stat(file.absolutePath) ?: FileItem(
                                                    file.name, file.absolutePath, false, file.length(), file.lastModified(), false, null, true, true,
                                                )
                                                openFile(stat)
                                            }
                                        }
                                    }
                                    Intent.ACTION_SEND -> {
                                        val uri = incoming.getParcelableExtraCompat<Uri>(Intent.EXTRA_STREAM)
                                        if (uri != null) screen = Screen.SaveIncoming(listOf(uri))
                                    }
                                    Intent.ACTION_SEND_MULTIPLE -> {
                                        val uris = incoming.getParcelableArrayListExtraCompat<Uri>(Intent.EXTRA_STREAM)
                                        if (!uris.isNullOrEmpty()) screen = Screen.SaveIncoming(uris.toList())
                                    }
                                }
                                pendingIntentState.value = null
                            }

                            when (val currentScreen = screen) {
                                Screen.Home -> HomeScreen(
                                    rootLabel = primaryRoot.label,
                                    rootPath = primaryRoot.rootFile.absolutePath,
                                    onOpenBrowser = { screen = Screen.Browser },
                                    onOpenCategory = { category -> screen = Screen.CategoryBrowser(category) },
                                    onOpenFolder = { path -> browserViewModel.openFolder(path); screen = Screen.Browser },
                                    onOpenRecent = { screen = Screen.Recent },
                                    onOpenFavorites = { screen = Screen.Favorites },
                                    onOpenStorageAnalyzer = { screen = Screen.StorageAnalyzer },
                                    onOpenSettings = { screen = Screen.Settings },
                                )

                                Screen.Browser -> FileBrowserScreen(
                                    viewModel = browserViewModel,
                                    onOpenFile = ::openFile,
                                    onShareFiles = { items ->
                                        startActivity(buildShareIntent(this@MainActivity, items.map { File(it.absolutePath) }))
                                    },
                                    onOpenFavorites = { screen = Screen.Favorites },
                                    onNavigateBack = { screen = Screen.Home },
                                )

                                Screen.Favorites -> {
                                    androidx.activity.compose.BackHandler(enabled = true) { screen = Screen.Browser }
                                    val favoritesViewModel: FavoritesViewModel = viewModel(
                                        factory = FavoritesViewModel.Factory(favoritesRepository, fileRepository),
                                    )
                                    FavoritesScreen(
                                        viewModel = favoritesViewModel,
                                        onOpenItem = { item ->
                                            if (item.isDirectory) {
                                                screen = Screen.Browser
                                                browserViewModel.openFolder(item.absolutePath)
                                            } else {
                                                browserViewModel.openFolder(
                                                    File(item.absolutePath).parent ?: primaryRoot.rootFile.absolutePath,
                                                )
                                                openFile(item)
                                            }
                                        },
                                        onNavigateBack = { screen = Screen.Browser },
                                    )
                                }

                                Screen.Recent -> {
                                    androidx.activity.compose.BackHandler(enabled = true) { screen = Screen.Home }
                                    val recentViewModel: RecentViewModel = viewModel(
                                        factory = RecentViewModel.Factory(recentFilesRepository, fileRepository),
                                    )
                                    RecentScreen(
                                        viewModel = recentViewModel,
                                        onOpenItem = { item ->
                                            browserViewModel.openFolder(
                                                File(item.absolutePath).parent ?: primaryRoot.rootFile.absolutePath,
                                            )
                                            openFile(item)
                                        },
                                        onNavigateBack = { screen = Screen.Home },
                                    )
                                }

                                Screen.Settings -> {
                                    androidx.activity.compose.BackHandler(enabled = true) { screen = Screen.Home }
                                    SettingsScreen(
                                        settings = settings,
                                        onThemeModeChange = { mode -> scope.launch { settingsRepository.setThemeMode(mode) } },
                                        onDefaultViewModeChange = { mode -> scope.launch { settingsRepository.setDefaultViewMode(mode) } },
                                        onDefaultSortOrderChange = { order -> scope.launch { settingsRepository.setDefaultSortOrder(order) } },
                                        onFoldersFirstChange = { v -> scope.launch { settingsRepository.setFoldersFirst(v) } },
                                        onShowHiddenChange = { v -> scope.launch { settingsRepository.setShowHiddenFiles(v) } },
                                        onShowExtensionsChange = { v -> scope.launch { settingsRepository.setShowFileExtensions(v) } },
                                        onConfirmDeleteChange = { v -> scope.launch { settingsRepository.setConfirmDelete(v) } },
                                        onNavigateBack = { screen = Screen.Home },
                                    )
                                }

                                Screen.StorageAnalyzer -> {
                                    androidx.activity.compose.BackHandler(enabled = true) { screen = Screen.Home }
                                    StorageAnalyzerScreen(
                                        rootPath = primaryRoot.rootFile.absolutePath,
                                        onNavigateBack = { screen = Screen.Home },
                                    )
                                }

                                is Screen.CategoryBrowser -> {
                                    androidx.activity.compose.BackHandler(enabled = true) { screen = Screen.Home }
                                    CategoryScreen(
                                        category = currentScreen.category,
                                        rootPath = primaryRoot.rootFile.absolutePath,
                                        onOpenItem = { item ->
                                            browserViewModel.openFolder(
                                                File(item.absolutePath).parent ?: primaryRoot.rootFile.absolutePath,
                                            )
                                            openFile(item)
                                        },
                                        onNavigateBack = { screen = Screen.Home },
                                    )
                                }

                                is Screen.SaveIncoming -> {
                                    androidx.activity.compose.BackHandler(enabled = true) { screen = Screen.Home }
                                    SaveIncomingScreen(
                                        uris = currentScreen.uris,
                                        fileRepository = fileRepository,
                                        storageRootPath = primaryRoot.rootFile.absolutePath,
                                        storageRootLabel = primaryRoot.label,
                                        defaultFolder = "${primaryRoot.rootFile.absolutePath}/Download",
                                        onDone = {
                                            Toast.makeText(this@MainActivity, "Saved", Toast.LENGTH_SHORT).show()
                                            screen = Screen.Home
                                        },
                                        onCancel = { screen = Screen.Home },
                                    )
                                }

                                is Screen.ImageViewer -> {
                                    androidx.activity.compose.BackHandler(enabled = true) { screen = Screen.Browser }
                                    ImageViewerScreen(
                                        item = currentScreen.item,
                                        isFavorite = currentScreen.item.absolutePath in browserUiState.favoritePaths,
                                        onShare = { shareItem(currentScreen.item) },
                                        onDelete = { deleteTarget = currentScreen.item },
                                        onToggleFavorite = {
                                            scope.launch { favoritesRepository.toggle(currentScreen.item.absolutePath) }
                                        },
                                        onShowInfo = { infoTarget = currentScreen.item },
                                        onNavigateBack = { screen = Screen.Browser },
                                    )
                                }

                                is Screen.TextViewer -> {
                                    androidx.activity.compose.BackHandler(enabled = true) { screen = Screen.Browser }
                                    TextViewerScreen(
                                        item = currentScreen.item,
                                        onShare = { shareItem(currentScreen.item) },
                                        onRename = { renameTarget = currentScreen.item },
                                        onDelete = { deleteTarget = currentScreen.item },
                                        onShowInfo = { infoTarget = currentScreen.item },
                                        onNavigateBack = { screen = Screen.Browser },
                                    )
                                }

                                is Screen.PdfViewer -> {
                                    androidx.activity.compose.BackHandler(enabled = true) { screen = Screen.Browser }
                                    PdfViewerScreen(
                                        item = currentScreen.item,
                                        onShare = { shareItem(currentScreen.item) },
                                        onOpenWith = { openWithItem(currentScreen.item) },
                                        onDelete = { deleteTarget = currentScreen.item },
                                        onShowInfo = { infoTarget = currentScreen.item },
                                        onNavigateBack = { screen = Screen.Browser },
                                    )
                                }

                                is Screen.AudioViewer -> {
                                    androidx.activity.compose.BackHandler(enabled = true) { screen = Screen.Browser }
                                    AudioPlayerScreen(
                                        controller = audioPlayerController,
                                        playlist = currentScreen.playlist,
                                        startIndex = currentScreen.startIndex,
                                        isFavorite = currentScreen.item.absolutePath in browserUiState.favoritePaths,
                                        onShare = { shareItem(currentScreen.item) },
                                        onDelete = { deleteTarget = currentScreen.item },
                                        onToggleFavorite = {
                                            scope.launch { favoritesRepository.toggle(currentScreen.item.absolutePath) }
                                        },
                                        onShowInfo = { infoTarget = currentScreen.item },
                                        onNavigateBack = { screen = Screen.Browser },
                                    )
                                }

                                is Screen.VideoViewer -> {
                                    androidx.activity.compose.BackHandler(enabled = true) { screen = Screen.Browser }
                                    VideoPlayerScreen(
                                        item = currentScreen.item,
                                        onShare = { shareItem(currentScreen.item) },
                                        onDelete = { deleteTarget = currentScreen.item },
                                        onShowInfo = { infoTarget = currentScreen.item },
                                        onNavigateBack = { screen = Screen.Browser },
                                    )
                                }

                                is Screen.UnsupportedViewer -> {
                                    androidx.activity.compose.BackHandler(enabled = true) { screen = Screen.Browser }
                                    UnsupportedViewerScreen(
                                        item = currentScreen.item,
                                        reason = currentScreen.reason,
                                        onOpenWith = { openWithItem(currentScreen.item) },
                                        onShare = { shareItem(currentScreen.item) },
                                        onShowInfo = { infoTarget = currentScreen.item },
                                        onDelete = { deleteTarget = currentScreen.item },
                                        onNavigateBack = { screen = Screen.Browser },
                                    )
                                }

                                is Screen.ArchiveViewer -> {
                                    androidx.activity.compose.BackHandler(enabled = true) { screen = Screen.Browser }
                                    ArchiveViewerScreen(
                                        item = currentScreen.item,
                                        archiveRepository = archiveRepository,
                                        fileRepository = fileRepository,
                                        storageRootPath = primaryRoot.rootFile.absolutePath,
                                        storageRootLabel = primaryRoot.label,
                                        onShowInfo = { infoTarget = currentScreen.item },
                                        onNavigateBack = { screen = Screen.Browser },
                                    )
                                }
                            }

                            infoTarget?.let { target ->
                                FileInfoDialog(item = target, onDismiss = { infoTarget = null })
                            }
                            renameTarget?.let { target ->
                                RenameDialog(
                                    target = target,
                                    existingNames = File(target.absolutePath).parentFile
                                        ?.listFiles()?.map { it.name }?.toSet() ?: emptySet(),
                                    onConfirm = { newName ->
                                        scope.launch {
                                            val result = operationsRepository.rename(File(target.absolutePath), newName)
                                            result.exceptionOrNull()?.let {
                                                Toast.makeText(this@MainActivity, it.message ?: "Rename failed", Toast.LENGTH_SHORT).show()
                                            }
                                            renameTarget = null
                                            screen = Screen.Browser
                                            browserViewModel.refresh()
                                        }
                                    },
                                    onDismiss = { renameTarget = null },
                                )
                            }
                            deleteTarget?.let { target ->
                                DeleteConfirmDialog(
                                    targets = listOf(target),
                                    onConfirm = {
                                        scope.launch {
                                            val trashed = trashRepository.moveToTrash(File(target.absolutePath))
                                            if (trashed.isFailure) {
                                                val fallback = operationsRepository.deletePermanently(listOf(File(target.absolutePath)))
                                                fallback.exceptionOrNull()?.let {
                                                    Toast.makeText(this@MainActivity, it.message ?: "Delete failed", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                            deleteTarget = null
                                            screen = Screen.Browser
                                            browserViewModel.refresh()
                                        }
                                    },
                                    onDismiss = { deleteTarget = null },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun requestFullStorageAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            allFilesAccessLauncher.launch(storageAccessManager.allFilesAccessSettingsIntent())
        } else {
            legacyPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                ),
            )
        }
    }
}
