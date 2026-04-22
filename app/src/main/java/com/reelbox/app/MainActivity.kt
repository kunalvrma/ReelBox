package com.reelbox.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.documentfile.provider.DocumentFile
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

// ── Color palette ──────────────────────────────────────────────────────────────
private val Black      = Color(0xFF0A0A0A)
private val OffWhite   = Color(0xFFF0EDE8)
private val Accent     = Color(0xFFE8FF47)
private val Muted      = Color(0xFF2A2A2A)
private val Dim        = Color(0xFF555555)

// ── Screens ────────────────────────────────────────────────────────────────────
enum class Screen { LANDING, PLAYER, END }

// ── Session lengths ────────────────────────────────────────────────────────────
data class SessionOption(val label: String, val seconds: Int)
val SESSION_OPTIONS = listOf(
    SessionOption("5 min", 300),
    SessionOption("10 min", 600),
    SessionOption("15 min", 900),
    SessionOption("30 min", 1800),
    SessionOption("∞", 0)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ReelBoxApp()
        }
    }
}

// ── Root app ───────────────────────────────────────────────────────────────────
@Composable
fun ReelBoxApp() {
    var screen by remember { mutableStateOf(Screen.LANDING) }
    var playlist by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var sessionSeconds by remember { mutableIntStateOf(600) }
    var watchedCount by remember { mutableIntStateOf(0) }
    var elapsedSeconds by remember { mutableIntStateOf(0) }

    Surface(modifier = Modifier.fillMaxSize(), color = Black) {
        when (screen) {
            Screen.LANDING -> LandingScreen(
                onStart = { uris, secs ->
                    playlist = uris.shuffled()
                    sessionSeconds = secs
                    watchedCount = 0
                    elapsedSeconds = 0
                    screen = Screen.PLAYER
                }
            )
            Screen.PLAYER -> PlayerScreen(
                playlist = playlist,
                sessionSeconds = sessionSeconds,
                onEnd = { watched, elapsed ->
                    watchedCount = watched
                    elapsedSeconds = elapsed
                    screen = Screen.END
                }
            )
            Screen.END -> EndScreen(
                watchedCount = watchedCount,
                elapsedSeconds = elapsedSeconds,
                onAgain = { screen = Screen.LANDING }
            )
        }
    }
}

// ── Landing ────────────────────────────────────────────────────────────────────
@Composable
fun LandingScreen(onStart: (List<Uri>, Int) -> Unit) {
    val context = LocalContext.current
    var videoUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var selectedSession by remember { mutableIntStateOf(600) }
    var folderName by remember { mutableStateOf("") }

    // Persist last folder
    val prefs = context.getSharedPreferences("reelbox", Context.MODE_PRIVATE)

    // Folder picker — uses SAF (Storage Access Framework) for persistent URI permission
    val folderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            // Persist permission across app restarts
            context.contentResolver.takePersistableUriPermission(
                it, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            prefs.edit().putString("folder_uri", it.toString()).apply()
            val (uris, name) = scanFolder(context, it)
            videoUris = uris
            folderName = name
        }
    }

    // Restore last folder on launch
    LaunchedEffect(Unit) {
        prefs.getString("folder_uri", null)?.let { saved ->
            try {
                val uri = Uri.parse(saved)
                val (uris, name) = scanFolder(context, uri)
                if (uris.isNotEmpty()) {
                    videoUris = uris
                    folderName = name
                }
            } catch (_: Exception) {}
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Center
    ) {
        // Logo
        Text(
            text = "REEL",
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Black,
            fontSize = 72.sp,
            color = OffWhite,
            lineHeight = 68.sp,
            letterSpacing = 2.sp
        )
        Row {
            Text(
                text = "B",
                fontWeight = FontWeight.Black,
                fontSize = 72.sp,
                color = Accent,
                lineHeight = 68.sp,
                letterSpacing = 2.sp
            )
            Text(
                text = "OX",
                fontWeight = FontWeight.Black,
                fontSize = 72.sp,
                color = OffWhite,
                lineHeight = 68.sp,
                letterSpacing = 2.sp
            )
        }

        Spacer(Modifier.height(4.dp))
        Text(
            text = "YOUR VIDEOS · YOUR RULES · NO ALGORITHMS",
            fontSize = 9.sp,
            letterSpacing = 2.sp,
            color = Dim,
            fontFamily = FontFamily.Monospace
        )

        Spacer(Modifier.height(40.dp))

        // Folder picker
        SectionLabel("VIDEO FOLDER")
        Spacer(Modifier.height(8.dp))
        FolderPickerCard(
            folderName = folderName,
            videoCount = videoUris.size,
            onClick = { folderLauncher.launch(null) }
        )

        Spacer(Modifier.height(24.dp))

        // Session length
        SectionLabel("SESSION LENGTH")
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SESSION_OPTIONS.forEach { opt ->
                ChipButton(
                    label = opt.label,
                    active = selectedSession == opt.seconds,
                    onClick = { selectedSession = opt.seconds }
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        // Start button
        Button(
            onClick = { onStart(videoUris, selectedSession) },
            enabled = videoUris.isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Accent,
                disabledContainerColor = Muted,
                contentColor = Black
            ),
            shape = RoundedCornerShape(6.dp)
        ) {
            Text(
                text = "START SESSION →",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                letterSpacing = 2.sp,
                color = if (videoUris.isNotEmpty()) Black else Dim
            )
        }
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 9.sp,
        letterSpacing = 2.sp,
        color = Dim,
        fontFamily = FontFamily.Monospace
    )
}

@Composable
fun FolderPickerCard(folderName: String, videoCount: Int, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = Color.Transparent,
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.dp, if (folderName.isNotEmpty()) Accent.copy(alpha = .4f) else Muted)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                if (folderName.isEmpty()) {
                    Text("Tap to pick a folder", color = Dim, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                } else {
                    Text(folderName, color = OffWhite, fontSize = 13.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "$videoCount video${if (videoCount != 1) "s" else ""} found",
                        color = Accent,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
            Text("📂", fontSize = 22.sp)
        }
    }
}

@Composable
fun ChipButton(label: String, active: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        color = if (active) Accent else Color.Transparent,
        shape = RoundedCornerShape(100.dp),
        border = BorderStroke(1.dp, if (active) Accent else Muted)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            color = if (active) Black else Dim,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
    }
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

// ── Player ─────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlayerScreen(
    playlist: List<Uri>,
    sessionSeconds: Int,
    onEnd: (Int, Int) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val pagerState = rememberPagerState(pageCount = { Int.MAX_VALUE })
    var remainingSeconds by remember { mutableIntStateOf(sessionSeconds) }
    val startTime = remember { System.currentTimeMillis() }

    // Track max watched (unique indices visited)
    var watchedIndices by remember { mutableStateOf(setOf<Int>()) }
    LaunchedEffect(pagerState.currentPage) {
        watchedIndices = watchedIndices + (pagerState.currentPage % playlist.size)
    }

    // Session countdown
    LaunchedEffect(sessionSeconds) {
        if (sessionSeconds > 0) {
            while (remainingSeconds > 0) {
                delay(1000)
                remainingSeconds--
            }
            val elapsed = ((System.currentTimeMillis() - startTime) / 1000).toInt()
            onEnd(watchedIndices.size, elapsed)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Black)) {
        // Vertical pager — one video per page
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1
        ) { page ->
            VideoPage(uri = playlist[page % playlist.size], isActive = pagerState.currentPage == page)
        }

        // HUD overlay
        HUD(
            current = (pagerState.currentPage % playlist.size) + 1,
            total = playlist.size,
            remainingSeconds = remainingSeconds,
            isUnlimited = sessionSeconds == 0,
            onStop = {
                val elapsed = ((System.currentTimeMillis() - startTime) / 1000).toInt()
                onEnd(watchedIndices.size, elapsed)
            }
        )

        // Bottom progress bar
        LinearProgressIndicator(
            progress = { (pagerState.currentPage + 1).toFloat() / playlist.size },
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .align(Alignment.BottomCenter),
            color = Accent,
            trackColor = Muted
        )
    }
}

@Composable
fun VideoPage(uri: Uri, isActive: Boolean) {
    val context = LocalContext.current
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

    LaunchedEffect(isActive) {
        if (isActive) player.play() else { player.pause(); player.seekTo(0) }
    }

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
}

@Composable
fun HUD(
    current: Int,
    total: Int,
    remainingSeconds: Int,
    isUnlimited: Boolean,
    onStop: () -> Unit
) {
    val timerColor = when {
        !isUnlimited && remainingSeconds < 60 -> Color(0xFFFF4747)
        !isUnlimited && remainingSeconds < 120 -> Accent
        else -> OffWhite.copy(alpha = .7f)
    }

    val timerText = if (isUnlimited) "∞" else {
        val m = remainingSeconds / 60
        val s = remainingSeconds % 60
        "$m:${s.toString().padStart(2, '0')}"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .systemBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Left: title + counter
        Column(modifier = Modifier.align(Alignment.TopStart)) {
            Text(
                "REELBOX",
                fontWeight = FontWeight.Black,
                fontSize = 16.sp,
                letterSpacing = 2.sp,
                color = OffWhite
            )
            Text(
                "$current / $total",
                fontSize = 10.sp,
                letterSpacing = 1.sp,
                color = OffWhite.copy(alpha = .4f),
                fontFamily = FontFamily.Monospace
            )
        }

        // Right: timer + stop
        Column(
            modifier = Modifier.align(Alignment.TopEnd),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                color = Black.copy(alpha = .6f),
                shape = RoundedCornerShape(100.dp),
                border = BorderStroke(.5.dp, timerColor.copy(alpha = .4f))
            ) {
                Text(
                    text = timerText,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                    color = timerColor,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Surface(
                modifier = Modifier.clickable(onClick = onStop),
                color = Black.copy(alpha = .6f),
                shape = RoundedCornerShape(100.dp),
                border = BorderStroke(.5.dp, Muted)
            ) {
                Text(
                    text = "✕ Stop",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                    color = OffWhite.copy(alpha = .7f),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

// ── End Screen ─────────────────────────────────────────────────────────────────
@Composable
fun EndScreen(watchedCount: Int, elapsedSeconds: Int, onAgain: () -> Unit) {
    val mins = maxOf(1, elapsedSeconds / 60)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(32.dp),
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
            border = BorderStroke(1.dp, OffWhite.copy(alpha = .4f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = OffWhite)
        ) {
            Text(
                "WATCH AGAIN",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                letterSpacing = 2.sp
            )
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
