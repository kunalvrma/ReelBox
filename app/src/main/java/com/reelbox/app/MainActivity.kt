package com.reelbox.app

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.*

// ── Color palette ──────────────────────────────────────────────────────────────
private val Black    = Color(0xFF0A0A0A)
private val OffWhite = Color(0xFFF0EDE8)
private val Accent   = Color(0xFFE8FF47)
private val Muted    = Color(0xFF2A2A2A)
private val Dim      = Color(0xFF555555)
private val SidebarBg = Color(0xFF111111)

// ── Screens ────────────────────────────────────────────────────────────────────
enum class Screen { SPLASH, PLAYER, END }

// ── Weighted shuffle ───────────────────────────────────────────────────────────
fun weightedShuffle(uris: List<Uri>, prefs: SharedPreferences): List<Uri> {
    val byCount = uris.groupBy { prefs.getInt("wc_${it.toString().hashCode()}", 0) }
    return byCount.entries.sortedBy { it.key }.flatMap { it.value.shuffled() }
}

// ── Scan folder for videos ─────────────────────────────────────────────────────
fun scanFolder(context: Context, uri: Uri): Pair<List<Uri>, String> {
    val docFile = DocumentFile.fromTreeUri(context, uri) ?: return Pair(emptyList(), "")
    val videoMimes = setOf("video/mp4", "video/3gpp", "video/webm", "video/mkv", "video/avi", "video/x-matroska")
    val uris = mutableListOf<Uri>()
    fun scan(dir: DocumentFile) {
        for (file in dir.listFiles()) {
            if (file.isDirectory) scan(file)
            else if (file.type in videoMimes || file.name?.let { n ->
                    listOf(".mp4", ".mkv", ".webm", ".3gp", ".avi", ".mov").any { n.endsWith(it, ignoreCase = true) }
                } == true) {
                uris.add(file.uri)
            }
        }
    }
    scan(docFile)
    return Pair(uris, docFile.name ?: "Selected folder")
}

// ── Activity ───────────────────────────────────────────────────────────────────
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { ReelBoxApp() }
    }
}

// ── Root app ───────────────────────────────────────────────────────────────────
@Composable
fun ReelBoxApp() {
    var screen by remember { mutableStateOf(Screen.SPLASH) }
    var watchedCount by remember { mutableIntStateOf(0) }
    var elapsedSeconds by remember { mutableIntStateOf(0) }

    Surface(modifier = Modifier.fillMaxSize(), color = Black) {
        when (screen) {
            Screen.SPLASH -> SplashScreen(onDone = { screen = Screen.PLAYER })
            Screen.PLAYER -> PlayerScreen(
                onEnd = { watched, elapsed ->
                    watchedCount = watched
                    elapsedSeconds = elapsed
                    screen = Screen.END
                }
            )
            Screen.END -> EndScreen(
                watchedCount = watchedCount,
                elapsedSeconds = elapsedSeconds,
                onAgain = { screen = Screen.PLAYER } // skip splash on replay
            )
        }
    }
}

// ── Splash screen ──────────────────────────────────────────────────────────────
@Composable
fun SplashScreen(onDone: () -> Unit) {
    var logoVisible by remember { mutableStateOf(false) }
    var taglineVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        logoVisible = true
        delay(400)
        taglineVisible = true
        delay(900)
        onDone()
    }

    val logoAlpha by animateFloatAsState(
        targetValue = if (logoVisible) 1f else 0f,
        animationSpec = tween(600, easing = EaseOutCubic),
        label = "logo_alpha"
    )
    val logoOffsetY by animateFloatAsState(
        targetValue = if (logoVisible) 0f else 24f,
        animationSpec = tween(600, easing = EaseOutCubic),
        label = "logo_offset"
    )
    val taglineAlpha by animateFloatAsState(
        targetValue = if (taglineVisible) 1f else 0f,
        animationSpec = tween(500),
        label = "tagline_alpha"
    )

    Box(
        modifier = Modifier.fillMaxSize().background(Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 28.dp)
                .offset(y = logoOffsetY.dp)
                .alpha(logoAlpha),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "REEL",
                fontWeight = FontWeight.Black,
                fontSize = 72.sp,
                color = OffWhite,
                lineHeight = 68.sp,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center
            )
            Row(horizontalArrangement = Arrangement.Center) {
                Text("B", fontWeight = FontWeight.Black, fontSize = 72.sp, color = Accent, lineHeight = 68.sp, letterSpacing = 2.sp)
                Text("OX", fontWeight = FontWeight.Black, fontSize = 72.sp, color = OffWhite, lineHeight = 68.sp, letterSpacing = 2.sp)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "YOUR VIDEOS · YOUR RULES · NO ALGORITHMS",
                modifier = Modifier.alpha(taglineAlpha),
                fontSize = 9.sp,
                letterSpacing = 2.sp,
                color = Dim,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ── Player screen ──────────────────────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(onEnd: (Int, Int) -> Unit) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("reelbox", Context.MODE_PRIVATE)
    val scope = rememberCoroutineScope()

    // Folder / video state
    var allVideos by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var folderName by remember { mutableStateOf("") }
    var isInitialLoad by remember { mutableStateOf(true) }

    // Dynamic growing playlist (infinite non-repeating queue)
    val dynamicPlaylist = remember { mutableStateListOf<Uri>() }

    // Session watched URIs (unique, for stats)
    var sessionWatchedUris by remember { mutableStateOf(setOf<String>()) }

    // Timer state — always 5 min, restartable via timerKey
    var timerKey by remember { mutableIntStateOf(0) }
    var remainingSeconds by remember { mutableIntStateOf(300) }
    var showExtendOverlay by remember { mutableStateOf(false) }
    val startTime = remember { System.currentTimeMillis() }

    // Drawer
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    // Folder picker launcher
    val folderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            prefs.edit().putString("folder_uri", it.toString()).apply()
            scope.launch(Dispatchers.IO) {
                val (uris, name) = scanFolder(context, it)
                allVideos = uris
                folderName = name
            }
        }
    }

    // Load saved folder on launch
    LaunchedEffect(Unit) {
        prefs.getString("folder_uri", null)?.let { saved ->
            try {
                val uri = Uri.parse(saved)
                val (uris, name) = withContext(Dispatchers.IO) {
                    scanFolder(context, uri)
                }
                if (uris.isNotEmpty()) { allVideos = uris; folderName = name }
            } catch (_: Exception) {}
        }
        isInitialLoad = false
    }

    // Auto-open drawer if no folder found after initial load attempt
    LaunchedEffect(allVideos, isInitialLoad) {
        if (!isInitialLoad && allVideos.isEmpty()) {
            drawerState.open()
        }
    }

    // Rebuild playlist when videos change (new folder picked)
    LaunchedEffect(allVideos) {
        if (allVideos.isNotEmpty()) {
            dynamicPlaylist.clear()
            dynamicPlaylist.addAll(weightedShuffle(allVideos, prefs))
            remainingSeconds = 300
            timerKey++
        }
    }

    // Session countdown — restarts when timerKey changes
    LaunchedEffect(timerKey) {
        while (remainingSeconds > 0) {
            delay(1000L)
            remainingSeconds--
        }
        showExtendOverlay = true
    }

    val pagerState = rememberPagerState(pageCount = { maxOf(1, dynamicPlaylist.size) })

    // Track watched + append new round near end
    LaunchedEffect(pagerState.currentPage) {
        val uri = dynamicPlaylist.getOrNull(pagerState.currentPage) ?: return@LaunchedEffect
        sessionWatchedUris = sessionWatchedUris + uri.toString()
        // Increment global watch count
        val key = "wc_${uri.toString().hashCode()}"
        prefs.edit().putInt(key, prefs.getInt(key, 0) + 1).apply()
        // Append new round when approaching end
        if (pagerState.currentPage >= dynamicPlaylist.size - 3 && allVideos.isNotEmpty()) {
            dynamicPlaylist.addAll(weightedShuffle(allVideos, prefs))
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            SidebarContent(
                folderName = folderName,
                videoCount = allVideos.size,
                onPickFolder = { folderLauncher.launch(null) },
                onEndSession = {
                    val elapsed = ((System.currentTimeMillis() - startTime) / 1000).toInt()
                    onEnd(sessionWatchedUris.size, elapsed)
                }
            )
        }
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Black)) {
            if (dynamicPlaylist.isEmpty()) {
                // Empty state
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📂", fontSize = 48.sp)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Swipe right to open the sidebar\nand pick a video folder",
                            color = Dim,
                            textAlign = TextAlign.Center,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            lineHeight = 20.sp
                        )
                    }
                }
            } else {
                // Video pager
                VerticalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    beyondViewportPageCount = 1
                ) { page ->
                    VideoPage(
                        uri = dynamicPlaylist[page],
                        isActive = pagerState.currentPage == page
                    )
                }

                // Bottom progress bar (within current round)
                val roundSize = allVideos.size.coerceAtLeast(1)
                LinearProgressIndicator(
                    progress = { ((pagerState.currentPage % roundSize) + 1).toFloat() / roundSize },
                    modifier = Modifier.fillMaxWidth().height(2.dp).align(Alignment.BottomCenter),
                    color = Accent,
                    trackColor = Muted
                )
            }

            // HUD always on top
            HUD(remainingSeconds = remainingSeconds)

            // Extend overlay
            if (showExtendOverlay) {
                ExtendSessionOverlay(
                    onExtend = {
                        showExtendOverlay = false
                        remainingSeconds = 300
                        timerKey++
                    },
                    onEnd = {
                        val elapsed = ((System.currentTimeMillis() - startTime) / 1000).toInt()
                        onEnd(sessionWatchedUris.size, elapsed)
                    }
                )
            }
        }
    }
}

// ── Video page ─────────────────────────────────────────────────────────────────
@Composable
fun VideoPage(uri: Uri, isActive: Boolean) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var userPaused by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var showPauseIcon by remember { mutableStateOf(false) }

    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = ExoPlayer.REPEAT_MODE_ONE
        }
    }

    DisposableEffect(uri) {
        player.setMediaItem(MediaItem.fromUri(uri))
        player.prepare()
        onDispose { player.release() }
    }

    // Play/pause based on active page and user intent
    LaunchedEffect(isActive, userPaused) {
        if (isActive && !userPaused) player.play()
        else player.pause()
        if (!isActive) { player.seekTo(0); userPaused = false }
    }

    // Lifecycle observer — pause on background, respect userPaused on resume
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> player.pause()
                Lifecycle.Event.ON_RESUME -> if (isActive && !userPaused) player.play()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = {
                PlayerView(context).apply {
                    this.player = player
                    useController = false
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Invisible tap layer for pause/resume
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        userPaused = !userPaused
                        showPauseIcon = true
                        scope.launch {
                            delay(700)
                            showPauseIcon = false
                        }
                    })
                }
        )

        // Pause indicator
        AnimatedVisibility(
            visible = userPaused || showPauseIcon,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(300)),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Surface(
                color = Black.copy(alpha = 0.55f),
                shape = RoundedCornerShape(50.dp)
            ) {
                Text(
                    text = if (userPaused) "▶" else "⏸",
                    fontSize = 36.sp,
                    modifier = Modifier.padding(20.dp)
                )
            }
        }
    }
}

// ── HUD ────────────────────────────────────────────────────────────────────────
@Composable
fun HUD(remainingSeconds: Int) {
    val timerColor = when {
        remainingSeconds < 60  -> Color(0xFFFF4747)
        remainingSeconds < 120 -> Accent
        else                   -> OffWhite.copy(alpha = 0.7f)
    }
    val m = remainingSeconds / 60
    val s = remainingSeconds % 60
    val timerText = "$m:${s.toString().padStart(2, '0')}"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .systemBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Left: brand
        Text(
            "REELBOX",
            modifier = Modifier.align(Alignment.TopStart),
            fontWeight = FontWeight.Black,
            fontSize = 16.sp,
            letterSpacing = 2.sp,
            color = OffWhite
        )

        // Right: timer
        Column(
            modifier = Modifier.align(Alignment.TopEnd),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                color = Black.copy(alpha = 0.6f),
                shape = RoundedCornerShape(100.dp),
                border = BorderStroke(0.5.dp, timerColor.copy(alpha = 0.4f))
            ) {
                Text(
                    text = timerText,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                    color = timerColor,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

// ── Sidebar ────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SidebarContent(
    folderName: String,
    videoCount: Int,
    onPickFolder: () -> Unit,
    onEndSession: () -> Unit
) {
    ModalDrawerSheet(
        modifier = Modifier.width(280.dp),
        drawerContainerColor = SidebarBg,
        drawerContentColor = OffWhite
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                // Static logo
                Text(
                    text = "REEL",
                    fontWeight = FontWeight.Black,
                    fontSize = 42.sp,
                    color = OffWhite,
                    lineHeight = 40.sp,
                    letterSpacing = 2.sp
                )
                Row {
                    Text("B", fontWeight = FontWeight.Black, fontSize = 42.sp, color = Accent, lineHeight = 40.sp, letterSpacing = 2.sp)
                    Text("OX", fontWeight = FontWeight.Black, fontSize = 42.sp, color = OffWhite, lineHeight = 40.sp, letterSpacing = 2.sp)
                }
                Text(
                    text = "YOUR VIDEOS · YOUR RULES",
                    fontSize = 8.sp,
                    letterSpacing = 2.sp,
                    color = Dim,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(Modifier.height(32.dp))
                HorizontalDivider(color = Muted)
                Spacer(Modifier.height(24.dp))

                // Folder section
                Text(
                    text = "VIDEO FOLDER",
                    fontSize = 9.sp,
                    letterSpacing = 2.sp,
                    color = Dim,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(Modifier.height(10.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onPickFolder),
                    color = Color.Transparent,
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(
                        1.dp,
                        if (folderName.isNotEmpty()) Accent.copy(alpha = 0.4f) else Muted
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            if (folderName.isEmpty()) {
                                Text("Tap to pick a folder", color = Dim, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                            } else {
                                Text(folderName, color = OffWhite, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium)
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    "$videoCount video${if (videoCount != 1) "s" else ""}",
                                    color = Accent,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                        Text("📂", fontSize = 20.sp)
                    }
                }
            }

            // End session button at bottom
            OutlinedButton(
                onClick = onEndSession,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, Muted),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Dim)
            ) {
                Text("END SESSION", fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 2.sp)
            }
        }
    }
}

// ── Extend session overlay ─────────────────────────────────────────────────────
@Composable
fun ExtendSessionOverlay(onExtend: () -> Unit, onEnd: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Black.copy(alpha = 0.92f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(40.dp)
        ) {
            Text("◼", fontSize = 32.sp, color = Accent)
            Spacer(Modifier.height(16.dp))
            Text(
                "TIME'S\nUP",
                fontWeight = FontWeight.Black,
                fontSize = 52.sp,
                lineHeight = 50.sp,
                letterSpacing = 2.sp,
                color = OffWhite,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "You watched intentionally.\nKeep going?",
                fontSize = 12.sp,
                color = Dim,
                textAlign = TextAlign.Center,
                letterSpacing = 1.sp,
                lineHeight = 20.sp,
                fontFamily = FontFamily.Monospace
            )
            Spacer(Modifier.height(40.dp))
            Button(
                onClick = onExtend,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Black),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text("+ 5 MORE MINUTES", fontWeight = FontWeight.Bold, fontSize = 14.sp, letterSpacing = 2.sp)
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = onEnd,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, OffWhite.copy(alpha = 0.3f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = OffWhite.copy(alpha = 0.6f))
            ) {
                Text("END SESSION", fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 2.sp)
            }
        }
    }
}

// ── End screen ─────────────────────────────────────────────────────────────────
@Composable
fun EndScreen(watchedCount: Int, elapsedSeconds: Int, onAgain: () -> Unit) {
    val mins = maxOf(1, elapsedSeconds / 60)
    Column(
        modifier = Modifier.fillMaxSize().systemBarsPadding().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("◼", fontSize = 36.sp, color = Accent)
        Spacer(Modifier.height(16.dp))
        Text(
            "SESSION\nDONE",
            fontWeight = FontWeight.Black,
            fontSize = 52.sp,
            lineHeight = 50.sp,
            letterSpacing = 2.sp,
            color = OffWhite,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "You watched intentionally.\nThat's the point.",
            fontSize = 12.sp,
            color = Dim,
            textAlign = TextAlign.Center,
            letterSpacing = 1.sp,
            lineHeight = 20.sp,
            fontFamily = FontFamily.Monospace
        )
        Spacer(Modifier.height(40.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(48.dp)) {
            StatBlock(value = "$watchedCount", label = "VIDEOS")
            StatBlock(value = "${mins}m", label = "WATCHED")
        }
        Spacer(Modifier.height(48.dp))
        OutlinedButton(
            onClick = onAgain,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(6.dp),
            border = BorderStroke(1.dp, OffWhite.copy(alpha = 0.4f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = OffWhite)
        ) {
            Text("WATCH AGAIN", fontWeight = FontWeight.Bold, fontSize = 14.sp, letterSpacing = 2.sp)
        }
    }
}

@Composable
fun StatBlock(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Black, fontSize = 44.sp, color = Accent, lineHeight = 44.sp)
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 9.sp, letterSpacing = 2.sp, color = Dim, fontFamily = FontFamily.Monospace)
    }
}
