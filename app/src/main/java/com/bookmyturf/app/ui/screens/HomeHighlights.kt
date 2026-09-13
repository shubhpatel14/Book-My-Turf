@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.bookmyturf.app.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
    val sport: Sport
)

private val spotlights = listOf(
    Spotlight("YOUR FIRST GAME. 20% OFF.", "TODAY'S OFFER", "Use FIRSTGAME • Demo offer", R.drawable.lineup_offer, PulseLavender, Sport.FOOTBALL),
    Spotlight("VADODARA CRICKET CHAMPIONS LEAGUE", "FEATURED TOURNAMENT", "6-a-side • Gotri • Registrations soon", R.drawable.lineup_cricket, Color(0xFFFFA24C), Sport.CRICKET),
    Spotlight("FRIDAY NIGHT FLOODLIGHT FIVES", "SQUAD SPOTLIGHT", "Football • 5-a-side • Under the lights", R.drawable.lineup_football, PulseLavender, Sport.FOOTBALL),
    Spotlight("RALLY HARD. DOUBLES NIGHT.", "WEEKEND WATCHLIST", "Badminton • Doubles • Indoor courts", R.drawable.lineup_badminton, Color(0xFF71E5DF), Sport.BADMINTON)
)

@Composable
fun HomeHighlights(onSport: (Sport) -> Unit, isVisible: Boolean) {
    val pager = rememberPagerState(initialPage = spotlights.size * 500, pageCount = { spotlights.size * 1_000 })
    val dragging by pager.interactionSource.collectIsDraggedAsState()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle, dragging, isVisible) {
        if (isVisible && !dragging) lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (isActive) {
                delay(2_650)
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

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("In the spotlight", fontFamily = ManropeUi, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.headlineSmall)
        HorizontalPager(
            state = pager,
            pageSpacing = 12.dp,
            contentPadding = PaddingValues(horizontal = 44.dp),
            key = { it }
        ) { page ->
            val item = spotlights[page % spotlights.size]
            Card(
                onClick = { onSport(item.sport) },
                modifier = Modifier.fillMaxWidth().height(270.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Charcoal)
            ) {
                Box(Modifier.fillMaxSize()) {
                    Image(painterResource(item.image), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Ink.copy(.04f), Ink.copy(.45f)))))
                    Surface(Modifier.align(Alignment.TopStart).padding(12.dp), color = Ink.copy(.75f), shape = RoundedCornerShape(12.dp)) {
                        Text(item.kicker, Modifier.padding(horizontal = 10.dp, vertical = 7.dp), color = item.accent, style = MaterialTheme.typography.labelSmall)
                    }
                    Surface(Modifier.align(Alignment.TopEnd).padding(12.dp), color = Ink.copy(.72f), shape = RoundedCornerShape(12.dp)) {
                        Text("DEMO", Modifier.padding(horizontal = 9.dp, vertical = 6.dp), color = Color.White.copy(.78f), style = MaterialTheme.typography.labelSmall)
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
