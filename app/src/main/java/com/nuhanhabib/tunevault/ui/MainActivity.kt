package com.nuhanhabib.tunevault.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuhanhabib.tunevault.data.db.TrackStatus
import com.nuhanhabib.tunevault.ui.screens.AddScreen
import com.nuhanhabib.tunevault.ui.screens.DownloadsScreen
import com.nuhanhabib.tunevault.ui.screens.LibraryScreen
import com.nuhanhabib.tunevault.ui.screens.MiniPlayer
import com.nuhanhabib.tunevault.ui.screens.PlayerScreen
import com.nuhanhabib.tunevault.ui.screens.PlaylistScreen
import com.nuhanhabib.tunevault.ui.screens.SettingsScreen
import com.nuhanhabib.tunevault.ui.theme.TuneVaultTheme

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()
    private val sharedText = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) handleShare(intent)
        setContent {
            TuneVaultTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    App(vm, sharedText.value, onShareConsumed = { sharedText.value = null })
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShare(intent)
    }

    private fun handleShare(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND) {
            sharedText.value = intent.getStringExtra(Intent.EXTRA_TEXT)
        }
    }
}

private enum class Tab(val label: String, val icon: ImageVector) {
    Library("Library", Icons.Default.LibraryMusic),
    Add("Add", Icons.Default.Add),
    Downloads("Downloads", Icons.Default.Download),
    Settings("Settings", Icons.Default.Settings),
}

@Composable
private fun App(vm: MainViewModel, sharedText: String?, onShareConsumed: () -> Unit) {
    var tab by rememberSaveable { mutableStateOf(Tab.Library) }
    var openPlaylist by rememberSaveable { mutableStateOf<String?>(null) }
    var playerOpen by rememberSaveable { mutableStateOf(false) }
    var addInput by rememberSaveable { mutableStateOf("") }
    val snackbar = remember { SnackbarHostState() }
    val queue by vm.queue.collectAsStateWithLifecycle()

    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    LaunchedEffect(Unit) { vm.messages.collect { snackbar.showSnackbar(it) } }
    LaunchedEffect(sharedText) {
        if (sharedText != null) {
            tab = Tab.Add
            openPlaylist = null
            playerOpen = false
            addInput = sharedText
            vm.submit(sharedText)
            onShareConsumed()
        }
    }

    BackHandler(enabled = playerOpen || openPlaylist != null || tab != Tab.Library) {
        when {
            playerOpen -> playerOpen = false
            openPlaylist != null -> openPlaylist = null
            else -> tab = Tab.Library
        }
    }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                Column {
                    MiniPlayer(vm.player, onExpand = { playerOpen = true })
                    NavigationBar {
                        Tab.entries.forEach { t ->
                            NavigationBarItem(
                                selected = tab == t,
                                onClick = {
                                    tab = t
                                    if (t != Tab.Library) openPlaylist = null
                                },
                                icon = {
                                    val pending = queue.count { it.status != TrackStatus.FAILED }
                                    if (t == Tab.Downloads && pending > 0) {
                                        BadgedBox(badge = { Badge { Text("$pending") } }) { Icon(t.icon, t.label) }
                                    } else {
                                        Icon(t.icon, t.label)
                                    }
                                },
                                label = { Text(t.label) },
                            )
                        }
                    }
                }
            },
        ) { padding ->
            Box(Modifier.padding(padding)) {
                when (tab) {
                    Tab.Library -> {
                        val id = openPlaylist
                        if (id != null) PlaylistScreen(vm, id, onBack = { openPlaylist = null })
                        else LibraryScreen(vm, onOpenPlaylist = { openPlaylist = it })
                    }
                    Tab.Add -> AddScreen(vm, addInput, onInputChange = { addInput = it })
                    Tab.Downloads -> DownloadsScreen(vm)
                    Tab.Settings -> SettingsScreen(vm)
                }
            }
        }

        AnimatedVisibility(
            visible = playerOpen,
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
        ) {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                Box(Modifier.fillMaxSize().padding(top = 24.dp)) {
                    PlayerScreen(vm.player, onCollapse = { playerOpen = false })
                }
            }
        }
    }
}
