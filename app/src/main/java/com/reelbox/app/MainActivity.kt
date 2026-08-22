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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
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
private val Black     = Color(0xFF0A0A0A)
private val OffWhite  = Color(0xFFF0EDE8)
private val Dim       = Color(0xFF555555)
private val SidebarBg = Color(0xFF111111)
private val GlassBg   = Color(0x33FFFFFF)
private val GlassBorder = Color(0x22FFFFFF)

data class ThemeConfig(
    val name: String,
    val primary: Color,
    val secondary: Color
)

val Themes = listOf(
    ThemeConfig("NEON SHUTTER", Color(0xFFE8FF47), Color(0xFF47FFD6)),
    ThemeConfig("MIDNIGHT AURORA", Color(0xFFBD47FF), Color(0xFF477BFF)),
    ThemeConfig("LAVA GLASS", Color(0xFFFF4747), Color(0xFFFF47B6))
)

val LocalTheme = compositionLocalOf { Themes[0] }

// ── Glass Modifier ─────────────────────────────────────────────────────────────
fun Modifier.glass(
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(16.dp),
    shadowElevation: Dp = 0.dp
) = this
    .shadow(shadowElevation, shape)
    .background(GlassBg, shape)
    .border(0.5.dp, GlassBorder, shape)
    .clip(shape)

// ── Screens ────────────────────────────────────────────────────────────────────
enum class Screen { SPLASH, PLAYER, END, ABOUT, PRIVACY }

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

// ── Share video ────────────────────────────────────────────────────────────────
fun shareVideo(context: Context, uri: Uri) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "video/*"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(shareIntent, "Share video via"))
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
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("reelbox", Context.MODE_PRIVATE) }
    
    var screen by remember { mutableStateOf(Screen.SPLASH) }
    var watchedCount by remember { mutableIntStateOf(0) }
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    
    // Theme state
    var selectedThemeIndex by remember { mutableIntStateOf(prefs.getInt("theme_index", 0)) }
    val currentTheme = Themes[selectedThemeIndex.coerceIn(Themes.indices)]

    // Global Folder state (loaded during splash)
    var allVideos by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var folderName by remember { mutableStateOf("") }
    var isInitialized by remember { mutableStateOf(false) }

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
        isInitialized = true
    }

    CompositionLocalProvider(LocalTheme provides currentTheme) {
        Surface(modifier = Modifier.fillMaxSize(), color = Black) {
            AnimatedContent(
                targetState = screen,
                transitionSpec = {
                    fadeIn(tween(500)) togetherWith fadeOut(tween(500))
                },
                label = "screen_transition"
            ) { targetScreen ->
                when (targetScreen) {
                    Screen.SPLASH -> SplashScreen(
                        isInitialized = isInitialized,
                        onDone = { screen = Screen.PLAYER }
                    )
                    Screen.PLAYER -> PlayerScreen(
                        initialVideos = allVideos,
                        initialFolderName = folderName,
                        onNavigate = { screen = it },
                        onThemeChange = { 
                            selectedThemeIndex = it
                            prefs.edit().putInt("theme_index", it).apply()
                        },
                        onEnd = { watched, elapsed ->
                            watchedCount = watched
                            elapsedSeconds = elapsed
                            screen = Screen.END
                        }
                    )
                    Screen.END -> EndScreen(
                        watchedCount = watchedCount,
                        elapsedSeconds = elapsedSeconds,
                        onAgain = { screen = Screen.PLAYER }
                    )
                    Screen.ABOUT -> AboutPage(onBack = { screen = Screen.PLAYER })
                    Screen.PRIVACY -> PrivacyPage(onBack = { screen = Screen.PLAYER })
                }
            }
        }
    }
}

// ── Splash screen ──────────────────────────────────────────────────────────────
@Composable
fun SplashScreen(isInitialized: Boolean, onDone: () -> Unit) {
    val theme = LocalTheme.current
    val scale = remember { Animatable(0.5f) }
    val rotation = remember { Animatable(-90f) }
    val alpha = remember { Animatable(0f) }
    var taglineVisible by remember { mutableStateOf(false) }
    var isAnimationDone by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        launch {
            // Mechanical scale-up with overshoot
            scale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
        launch {
            // Rotation from -90 to 0 (Shutter opening)
            rotation.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 1000, easing = EaseOutBack)
            )
        }
        launch {
            alpha.animateTo(1f, tween(600))
        }

        delay(800)
        taglineVisible = true
        
        // Mark animation as finished after holding the logo briefly
        delay(1500)
        isAnimationDone = true
    }

    // Reactive transition: release only when BOTH are ready
    LaunchedEffect(isInitialized, isAnimationDone) {
        if (isInitialized && isAnimationDone) {
            onDone()
        }
    }

    val taglineAlpha by animateFloatAsState(
        targetValue = if (taglineVisible) 1f else 0f,
        animationSpec = tween(800),
        label = "tagline_alpha"
    )

    Box(
        modifier = Modifier.fillMaxSize().background(Black),
        contentAlignment = Alignment.Center
    ) {
        // Background glow with dual tone
        Box(
            modifier = Modifier
                .size(300.dp)
                .graphicsLayer(alpha = alpha.value * 0.2f)
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(theme.primary, theme.secondary, Color.Transparent),
                            radius = size.width / 2f
                        )
                    )
                }
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = com.reelbox.app.R.drawable.ic_reelbox_logo),
                contentDescription = null,
                modifier = Modifier
                    .size(180.dp)
                    .graphicsLayer(
                        scaleX = scale.value,
                        scaleY = scale.value,
                        rotationZ = rotation.value,
                        alpha = alpha.value
                    ),
                colorFilter = ColorFilter.tint(OffWhite)
            )
            
            Spacer(Modifier.height(32.dp))

            Text(
                text = "YOUR VIDEOS · YOUR RULES · NO ALGORITHMS",
                modifier = Modifier.alpha(taglineAlpha).graphicsLayer(translationY = (10 * (1 - taglineAlpha))),
                fontSize = 10.sp,
                letterSpacing = 3.sp,
                color = theme.primary.copy(alpha = 0.6f),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ── Player screen ──────────────────────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    initialVideos: List<Uri>,
    initialFolderName: String,
    onNavigate: (Screen) -> Unit,
    onThemeChange: (Int) -> Unit,
    onEnd: (Int, Int) -> Unit
) {
    val theme = LocalTheme.current
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("reelbox", Context.MODE_PRIVATE) }
    val scope = rememberCoroutineScope()

    // Folder / video state
    var allVideos by remember { mutableStateOf(initialVideos) }
    var folderName by remember { mutableStateOf(initialFolderName) }

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

    // Rebuild playlist when videos change (new folder picked or first load)
    LaunchedEffect(allVideos) {
        if (allVideos.isNotEmpty()) {
            dynamicPlaylist.clear()
            dynamicPlaylist.addAll(weightedShuffle(allVideos, prefs))
            remainingSeconds = 300
            timerKey++
            // Reset pager to first page when folder changes
            // Only scroll if we have pages
            delay(100) // Small delay to let playlist sync
        }
    }

    val pagerState = rememberPagerState(pageCount = { dynamicPlaylist.size })

    // Session countdown — restarts when timerKey changes
    LaunchedEffect(timerKey) {
        while (remainingSeconds > 0) {
            delay(1000L)
            remainingSeconds--
        }
        showExtendOverlay = true
    }

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
                onNavigate = onNavigate,
                onThemeChange = onThemeChange,
                onEndSession = {
                    val elapsed = ((System.currentTimeMillis() - startTime) / 1000).toInt()
                    onEnd(sessionWatchedUris.size, elapsed)
                }
            )
        }
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Black)) {
            // Background Layer (Video + HUD) - Blurs when overlay is shown
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .blur(if (showExtendOverlay) 10.dp else 0.dp)
            ) {
                if (dynamicPlaylist.isEmpty()) {
                    // Empty state
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        // Background soft glow
                        Box(Modifier.size(200.dp).graphicsLayer(alpha = 0.1f).drawBehind { drawCircle(theme.primary) })
                        
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .glass(RoundedCornerShape(32.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("📂", fontSize = 42.sp)
                            }
                            Spacer(Modifier.height(32.dp))
                            Text(
                                "READY FOR CONTENT",
                                color = OffWhite,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 2.sp,
                                fontSize = 14.sp
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Swipe right to pick a video folder",
                                color = OffWhite.copy(alpha = 0.4f),
                                textAlign = TextAlign.Center,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                letterSpacing = 1.sp
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
                        key(dynamicPlaylist[page]) {
                            VideoPage(
                                uri = dynamicPlaylist[page],
                                isActive = pagerState.currentPage == page,
                                isSystemPaused = showExtendOverlay
                            )
                        }
                    }

                    // Bottom progress bar (thin neon line)
                    val roundSize = allVideos.size.coerceAtLeast(1)
                    val progress = ((pagerState.currentPage % roundSize) + 1).toFloat() / roundSize
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .align(Alignment.BottomCenter)
                            .background(Color.White.copy(alpha = 0.05f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress)
                                .fillMaxHeight()
                                .background(theme.primary)
                                .shadow(elevation = 8.dp, shape = RoundedCornerShape(2.dp), ambientColor = theme.primary, spotColor = theme.primary)
                        )
                    }
                }

                // HUD always on top
                HUD(
                    remainingSeconds = remainingSeconds,
                    onShare = {
                        dynamicPlaylist.getOrNull(pagerState.currentPage)?.let { uri ->
                            shareVideo(context, uri)
                        }
                    }
                )
            }

            // Extend overlay (Sharp, on top of blurred background)
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
fun VideoPage(uri: Uri, isActive: Boolean, isSystemPaused: Boolean = false) {
    val theme = LocalTheme.current
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
        // If it's already active (like the first reel), start playing
        if (isActive && !userPaused && !isSystemPaused) player.play()
        onDispose { player.release() }
    }

    // Play/pause based on active page and user intent
    LaunchedEffect(isActive, userPaused, isSystemPaused) {
        if (isActive && !userPaused && !isSystemPaused) {
            if (!player.isPlaying) player.play()
        } else {
            player.pause()
        }
        if (!isActive) { player.seekTo(0); userPaused = false }
    }

    // Lifecycle observer — pause on background, respect userPaused on resume
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> player.pause()
                Lifecycle.Event.ON_RESUME -> if (isActive && !userPaused && !isSystemPaused) player.play()
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
            enter = scaleIn(tween(200, easing = EaseOutBack)) + fadeIn(),
            exit = scaleOut(tween(300)) + fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .glass(shape = RoundedCornerShape(100.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (userPaused) {
                    // Custom Play Triangle
                    Canvas(modifier = Modifier.size(24.dp)) {
                        val path = androidx.compose.ui.graphics.Path().apply {
                            moveTo(0f, 0f)
                            lineTo(size.width, size.height / 2f)
                            lineTo(0f, size.height)
                            close()
                        }
                        drawPath(path, color = theme.primary)
                    }
                } else {
                    // Custom Pause Bars
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(Modifier.size(6.dp, 24.dp).background(theme.primary, RoundedCornerShape(2.dp)))
                        Box(Modifier.size(6.dp, 24.dp).background(theme.primary, RoundedCornerShape(2.dp)))
                    }
                }
            }
        }
    }
}

// ── HUD ────────────────────────────────────────────────────────────────────────
@Composable
fun HUD(remainingSeconds: Int, onShare: () -> Unit) {
    val theme = LocalTheme.current
    val timerColor = when {
        remainingSeconds < 60  -> Color(0xFFFF4747)
        remainingSeconds < 120 -> theme.primary
        else                   -> OffWhite
    }
    val m = remainingSeconds / 60
    val s = remainingSeconds % 60
    val timerText = "$m:${s.toString().padStart(2, '0')}"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .systemBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // Left: brand
        Column(modifier = Modifier.align(Alignment.TopStart)) {
            Text(
                "REELBOX",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                letterSpacing = 4.sp,
                color = OffWhite,
                modifier = Modifier.graphicsLayer(alpha = 0.9f)
            )
            Box(
                Modifier
                    .width(40.dp)
                    .height(2.dp)
                    .background(theme.primary)
                    .shadow(elevation = 6.dp, shape = RoundedCornerShape(1.dp), ambientColor = theme.primary, spotColor = theme.primary)
            )
        }

        // Right: timer pill + share button
        Row(
            modifier = Modifier.align(Alignment.TopEnd),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Share Button
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .glass(shape = RoundedCornerShape(100.dp), shadowElevation = 8.dp)
                    .clickable { onShare() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_share),
                    contentDescription = "Share",
                    tint = OffWhite,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Timer pill
            Box(
                modifier = Modifier
                    .glass(shape = RoundedCornerShape(100.dp), shadowElevation = 8.dp)
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Pulse dot
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                    val alpha by infiniteTransition.animateFloat(
                        initialValue = 0.4f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "alpha"
                    )
                    Box(
                        Modifier
                            .size(6.dp)
                            .graphicsLayer(alpha = alpha)
                            .background(timerColor, RoundedCornerShape(100.dp))
                    )
                    Text(
                        text = timerText,
                        color = OffWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
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
    onNavigate: (Screen) -> Unit,
    onThemeChange: (Int) -> Unit,
    onEndSession: () -> Unit
) {
    val theme = LocalTheme.current
    
    ModalDrawerSheet(
        modifier = Modifier.width(300.dp),
        drawerContainerColor = Color.Transparent,
        drawerContentColor = OffWhite
    ) {
        Box(modifier = Modifier.fillMaxSize().background(SidebarBg.copy(alpha = 0.85f)).glass(RoundedCornerShape(0.dp))) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(vertical = 48.dp, horizontal = 24.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    // Modernized logo
                    Text(
                        text = "REEL",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 42.sp,
                        color = OffWhite,
                        lineHeight = 40.sp,
                        letterSpacing = 2.sp
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("B", fontWeight = FontWeight.ExtraBold, fontSize = 42.sp, color = theme.primary, lineHeight = 40.sp, letterSpacing = 2.sp)
                        Text("OX", fontWeight = FontWeight.ExtraBold, fontSize = 42.sp, color = OffWhite, lineHeight = 40.sp, letterSpacing = 2.sp)
                        Box(Modifier.padding(start = 8.dp, bottom = 8.dp).size(6.dp).background(theme.secondary, RoundedCornerShape(100.dp)))
                    }
                    Text(
                        text = "YOUR VIDEOS · YOUR RULES",
                        fontSize = 8.sp,
                        letterSpacing = 4.sp,
                        color = theme.primary.copy(alpha = 0.5f),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(48.dp))
                    
                    // Folder section
                    SidebarSectionTitle("CONTENT SOURCE")
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glass(RoundedCornerShape(12.dp))
                            .clickable(onClick = onPickFolder)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                if (folderName.isEmpty()) {
                                    Text("Pick a folder", color = OffWhite.copy(alpha = 0.5f), fontSize = 13.sp)
                                } else {
                                    Text(folderName, color = OffWhite, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                    Text("$videoCount VIDEOS", color = theme.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }
                            }
                            Text("📂", fontSize = 20.sp)
                        }
                    }

                    Spacer(Modifier.height(32.dp))
                    
                    // Theme section
                    SidebarSectionTitle("THEME PRESETS")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Themes.forEachIndexed { index, config ->
                            val isSelected = theme.name == config.name
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) Color.White.copy(alpha = 0.1f) else Color.Transparent)
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) config.primary else Color.White.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { onThemeChange(index) },
                                contentAlignment = Alignment.Center
                            ) {
                                // Dual tone preview
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .drawBehind {
                                            drawRect(
                                                brush = Brush.linearGradient(
                                                    colors = listOf(config.primary, config.secondary)
                                                )
                                            )
                                        }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(32.dp))

                    // Navigation Links
                    SidebarSectionTitle("REELBOX")
                    SidebarLink("About Project", onClick = { onNavigate(Screen.ABOUT) })
                    Spacer(Modifier.height(8.dp))
                    SidebarLink("Privacy Policy", onClick = { onNavigate(Screen.PRIVACY) })
                }

                // End session button
                Button(
                    onClick = onEndSession,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = theme.primary.copy(alpha = 0.1f), contentColor = theme.primary),
                    border = BorderStroke(1.dp, theme.primary.copy(alpha = 0.2f))
                ) {
                    Text("END SESSION", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, letterSpacing = 2.sp)
                }
            }
        }
    }
}

@Composable
fun SidebarSectionTitle(text: String) {
    Text(
        text = text,
        fontSize = 10.sp,
        letterSpacing = 2.sp,
        color = OffWhite.copy(alpha = 0.4f),
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

@Composable
fun SidebarLink(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glass(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Text(text = text, color = OffWhite, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

// ── Extend session overlay ─────────────────────────────────────────────────────
@Composable
fun ExtendSessionOverlay(onExtend: () -> Unit, onEnd: () -> Unit) {
    val theme = LocalTheme.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Black.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .padding(32.dp)
                .glass(RoundedCornerShape(24.dp), shadowElevation = 16.dp)
                .background(Black.copy(alpha = 0.4f))
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(40.dp)
            ) {
                // Animated neon dot with dual tone
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val glow by infiniteTransition.animateFloat(
                    initialValue = 4f,
                    targetValue = 12f,
                    animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse),
                    label = "glow"
                )
                Box(
                    Modifier
                        .size(12.dp)
                        .shadow(elevation = glow.dp, shape = RoundedCornerShape(100.dp), ambientColor = theme.primary, spotColor = theme.secondary)
                        .background(theme.primary, RoundedCornerShape(100.dp))
                )
                
                Spacer(Modifier.height(24.dp))
                Text(
                    "TIME'S UP",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 44.sp,
                    lineHeight = 44.sp,
                    letterSpacing = 2.sp,
                    color = OffWhite,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "You watched intentionally.\nKeep going?",
                    fontSize = 13.sp,
                    color = OffWhite.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(Modifier.height(48.dp))
                Button(
                    onClick = onExtend,
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = theme.primary, contentColor = Black),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("+ 5 MORE MINUTES", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, letterSpacing = 1.sp)
                }
                Spacer(Modifier.height(16.dp))
                TextButton(
                    onClick = onEnd,
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Text("END SESSION", color = OffWhite.copy(alpha = 0.5f), fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 2.sp)
                }
            }
        }
    }
}

// ── End screen ─────────────────────────────────────────────────────────────────
@Composable
fun EndScreen(watchedCount: Int, elapsedSeconds: Int, onAgain: () -> Unit) {
    val theme = LocalTheme.current
    val mins = maxOf(1, elapsedSeconds / 60)
    Box(modifier = Modifier.fillMaxSize().background(Black)) {
        // Subtle background glow
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(400.dp)
                .graphicsLayer(alpha = 0.15f)
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(theme.primary, theme.secondary, Color.Transparent),
                            center = Offset(size.width / 2, 0f),
                            radius = size.width
                        )
                    )
                }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "SESSION\nCOMPLETE",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 52.sp,
                lineHeight = 52.sp,
                letterSpacing = 2.sp,
                color = OffWhite,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "INTENTIONAL WATCHING DONE.",
                fontSize = 11.sp,
                color = theme.primary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp,
                fontFamily = FontFamily.Monospace
            )
            
            Spacer(Modifier.height(64.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                StatBlock(value = "$watchedCount", label = "VIDEOS", modifier = Modifier.weight(1f))
                StatBlock(value = "${mins}m", label = "MINUTES", modifier = Modifier.weight(1f))
            }
            
            Spacer(Modifier.height(64.dp))
            
            Button(
                onClick = onAgain,
                modifier = Modifier.fillMaxWidth().height(60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OffWhite, contentColor = Black),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("WATCH AGAIN", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, letterSpacing = 2.sp)
            }
        }
    }
}

@Composable
fun StatBlock(value: String, label: String, modifier: Modifier = Modifier) {
    val theme = LocalTheme.current
    Box(
        modifier = modifier
            .glass(RoundedCornerShape(20.dp))
            .padding(vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 48.sp, color = theme.primary, lineHeight = 48.sp)
            Spacer(Modifier.height(4.dp))
            Text(label, fontSize = 10.sp, letterSpacing = 2.sp, color = OffWhite.copy(alpha = 0.6f), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        }
    }
}

// ── About Page ─────────────────────────────────────────────────────────────────
@Composable
fun AboutPage(onBack: () -> Unit) {
    val theme = LocalTheme.current
    PageTemplate(title = "ABOUT PROJECT", onBack = onBack) {
        Column {
            Text(
                "REELBOX",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 32.sp,
                color = OffWhite,
                letterSpacing = 4.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Created by Kunal Verma",
                color = theme.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(32.dp))
            Text(
                "ReelBox was built for imperfect humans who want the modern 'reel' experience without the soul-sucking algorithms. It's TikTok, but for people who actually want to decide when to stop watching.",
                color = OffWhite.copy(alpha = 0.7f),
                lineHeight = 24.sp,
                fontSize = 15.sp
            )
            Spacer(Modifier.height(24.dp))
            Text(
                "The system is simple: your videos, your rules, and a mandatory 5-minute timer to keep you present.",
                color = OffWhite.copy(alpha = 0.7f),
                lineHeight = 24.sp,
                fontSize = 15.sp
            )
        }
    }
}

// ── Privacy Page ───────────────────────────────────────────────────────────────
@Composable
fun PrivacyPage(onBack: () -> Unit) {
    val theme = LocalTheme.current
    PageTemplate(title = "PRIVACY POLICY", onBack = onBack) {
        Column {
            Text(
                "ZERO TRACKING. PERIOD.",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp,
                color = theme.primary,
                letterSpacing = 2.sp
            )
            Spacer(Modifier.height(24.dp))
            PrivacyItem("Local First", "ReelBox only looks at the folders you specifically pick. No data ever leaves your device.")
            PrivacyItem("No Analytics", "I don't care how many videos you watch or how long you spend here. No trackers, no cookies, no BS.")
            PrivacyItem("SAF Storage", "Uses Android's Storage Access Framework to ensure we only have permission for what you grant.")
        }
    }
}

@Composable
fun PrivacyItem(title: String, desc: String) {
    Column(Modifier.padding(bottom = 24.dp)) {
        Text(title, fontWeight = FontWeight.Bold, color = OffWhite, fontSize = 16.sp)
        Spacer(Modifier.height(4.dp))
        Text(desc, color = OffWhite.copy(alpha = 0.6f), fontSize = 14.sp, lineHeight = 22.sp)
    }
}

@Composable
fun PageTemplate(title: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    val theme = LocalTheme.current
    Box(modifier = Modifier.fillMaxSize().background(Black).systemBarsPadding()) {
        Column(modifier = Modifier.fillMaxSize().padding(32.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    letterSpacing = 3.sp,
                    color = theme.primary.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .glass(RoundedCornerShape(100.dp))
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✕", color = OffWhite, fontSize = 18.sp)
                }
            }
            Spacer(Modifier.height(48.dp))
            content()
        }
    }
}
