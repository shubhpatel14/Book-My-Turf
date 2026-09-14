@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.bookmyturf.app.ui.screens

import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import android.view.Surface
import android.view.TextureView
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.bookmyturf.app.R
import com.bookmyturf.app.data.Sport
import com.bookmyturf.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private data class Spotlight(
    val title: String,
    val kicker: String,
    val detail: String,
    val image: Int,
    val accent: Color,
    val sport: Sport,
    val video: Int? = null
)

private val spotlights = listOf(
    Spotlight("VADODARA CRICKET CHAMPIONS LEAGUE", "FEATURED TOURNAMENT", "6-a-side • Gotri • Registrations soon", R.drawable.vccl_thumbnail, Color(0xFFFFA24C), Sport.CRICKET, R.raw.vccl),
    Spotlight("YOUR FIRST GAME. 20% OFF.", "TODAY'S OFFER", "Use FIRSTGAME • Demo offer", R.drawable.lineup_offer, PulseLavender, Sport.FOOTBALL),
    Spotlight("FRIDAY NIGHT FLOODLIGHT FIVES", "SQUAD SPOTLIGHT", "Football • 5-a-side • Under the lights", R.drawable.lineup_football, PulseLavender, Sport.FOOTBALL),
    Spotlight("RALLY HARD. DOUBLES NIGHT.", "WEEKEND WATCHLIST", "Badminton • Doubles • Indoor courts", R.drawable.lineup_badminton, Color(0xFF71E5DF), Sport.BADMINTON)
)

private fun centerCropVideo(view: TextureView, videoWidth: Int, videoHeight: Int) {
    if (view.width == 0 || view.height == 0 || videoWidth == 0 || videoHeight == 0) return
    val viewRatio = view.width.toFloat() / view.height
    val videoRatio = videoWidth.toFloat() / videoHeight
    val scaleX = if (videoRatio > viewRatio) videoRatio / viewRatio else 1f
    val scaleY = if (videoRatio < viewRatio) viewRatio / videoRatio else 1f
    view.setTransform(Matrix().apply {
        setScale(scaleX, scaleY, view.width / 2f, view.height / 2f)
    })
}

@Composable
private fun SpotlightVideo(rawResource: Int, shouldPlay: Boolean) {
    val context = LocalContext.current
    val latestShouldPlay = rememberUpdatedState(shouldPlay)
    var prepared by remember { mutableStateOf(false) }
    var firstFrameRendered by remember { mutableStateOf(false) }
    var textureView by remember { mutableStateOf<TextureView?>(null) }
    val player = remember(rawResource) { MediaPlayer() }

    DisposableEffect(player, rawResource) {
        player.setDataSource(context, Uri.parse("android.resource://${context.packageName}/$rawResource"))
        player.isLooping = true
        player.setVolume(0f, 0f)
        player.setOnPreparedListener {
            prepared = true
            textureView?.let { view -> centerCropVideo(view, player.videoWidth, player.videoHeight) }
            if (latestShouldPlay.value) {
                player.seekTo(0)
                player.start()
            }
        }
        player.setOnInfoListener { _, what, _ ->
            if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                firstFrameRendered = true
            }
            false
        }
        player.prepareAsync()
        onDispose {
            player.setOnPreparedListener(null)
            player.setOnInfoListener(null)
            player.release()
        }
    }

    LaunchedEffect(shouldPlay, prepared) {
        if (prepared) {
            if (shouldPlay) {
                player.seekTo(0)
                player.start()
            } else if (player.isPlaying) {
                player.pause()
            }
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize().alpha(if (firstFrameRendered) 1f else 0f),
        factory = { viewContext ->
            TextureView(viewContext).also { view ->
                textureView = view
                view.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
                        player.setSurface(Surface(surfaceTexture))
                        if (prepared) centerCropVideo(view, player.videoWidth, player.videoHeight)
                        if (prepared && latestShouldPlay.value) player.start()
                    }

                    override fun onSurfaceTextureSizeChanged(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
                        if (prepared) centerCropVideo(view, player.videoWidth, player.videoHeight)
                    }

                    override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
                        player.setSurface(null)
                        return true
                    }

                    override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) = Unit
                }
            }
        }
    )
}

@Composable
private fun DelayedSpotlightVideo(video: Int, thumbnail: Int, shouldPlay: Boolean) {
    var loadVideo by remember(video) { mutableStateOf(false) }
    LaunchedEffect(shouldPlay) {
        loadVideo = false
        if (shouldPlay) {
            delay(300)
            loadVideo = true
        }
    }
    Box(Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(thumbnail),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        if (loadVideo) SpotlightVideo(video, shouldPlay = true)
    }
}

@Composable
fun HomeHighlights(onSport: (Sport) -> Unit, isVisible: Boolean) {
    val pager = rememberPagerState(initialPage = spotlights.size * 500, pageCount = { spotlights.size * 1_000 })
    val dragging by pager.interactionSource.collectIsDraggedAsState()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle, dragging, isVisible) {
        if (isVisible && !dragging) lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (isActive) {
                delay(5_000)
                if (!pager.isScrollInProgress) {
                    if (pager.currentPage == pager.pageCount - 1) pager.scrollToPage(spotlights.size * 500 - 1)
                    pager.animateScrollToPage(
                        pager.currentPage + 1,
                        animationSpec = tween(350, easing = FastOutSlowInEasing)
                    )
                }
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("In the spotlight", Modifier.padding(horizontal = 16.dp), fontFamily = ManropeUi, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.headlineSmall)
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val cardWidth = maxWidth * .66f
            val sidePreview = (maxWidth - cardWidth) / 2
            HorizontalPager(
                state = pager,
                pageSize = PageSize.Fixed(cardWidth),
                pageSpacing = 14.dp,
                contentPadding = PaddingValues(horizontal = sidePreview),
                beyondViewportPageCount = 1,
                key = { it }
            ) { page ->
                val item = spotlights[page % spotlights.size]
                Card(
                    onClick = { onSport(item.sport) },
                    modifier = Modifier.fillMaxWidth().aspectRatio(.74f),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Charcoal)
                ) {
                    Box(Modifier.fillMaxSize()) {
                        if (item.video != null) {
                            DelayedSpotlightVideo(
                                video = item.video,
                                thumbnail = item.image,
                                shouldPlay = isVisible && page == pager.currentPage
                            )
                        } else {
                            Image(painterResource(item.image), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        }
                        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Ink.copy(.04f), Ink.copy(.52f)))))
                        Surface(Modifier.align(Alignment.TopStart).padding(12.dp), color = Ink.copy(.75f), shape = RoundedCornerShape(12.dp)) {
                            Text(item.kicker, Modifier.padding(horizontal = 11.dp, vertical = 7.dp), color = item.accent, style = MaterialTheme.typography.labelSmall)
                        }
                        Surface(Modifier.align(Alignment.TopEnd).padding(12.dp), color = Ink.copy(.72f), shape = RoundedCornerShape(12.dp)) {
                            Text("DEMO", Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = Color.White.copy(.78f), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
        val active = spotlights[pager.currentPage % spotlights.size]
        Crossfade(active, animationSpec = tween(220), label = "spotlightCopy") { item ->
            Column(Modifier.fillMaxWidth().padding(horizontal = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(item.title, fontFamily = ManropeUi, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, maxLines = 2)
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(item.detail, color = TextMuted, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                    Spacer(Modifier.width(6.dp)); Icon(Icons.Default.ArrowForward, null, tint = item.accent, modifier = Modifier.size(16.dp))
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            spotlights.indices.forEach { index ->
                Box(
                    Modifier.padding(horizontal = 3.dp).width(if (index == pager.currentPage % spotlights.size) 24.dp else 6.dp).height(6.dp)
                        .background(if (index == pager.currentPage % spotlights.size) PulseLavender else Line, RoundedCornerShape(4.dp))
                )
            }
        }
    }
}
