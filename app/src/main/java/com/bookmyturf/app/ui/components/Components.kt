package com.bookmyturf.app.ui.components

import android.graphics.Matrix
import android.media.MediaPlayer
import android.net.Uri
import android.view.Surface
import android.view.TextureView
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.bookmyturf.app.data.Sport
import com.bookmyturf.app.data.Turf
import com.bookmyturf.app.R
import com.bookmyturf.app.ui.theme.*
import kotlin.math.min

private val turfVerticalOverlay = Brush.verticalGradient(
    listOf(Color.Transparent, Ink.copy(.08f), Ink.copy(.76f))
)
private val turfHorizontalOverlay = Brush.horizontalGradient(
    listOf(Ink.copy(.18f), Color.Transparent, Ink.copy(.08f))
)

@Composable
fun BrandMark(modifier: Modifier = Modifier, compact: Boolean = false, animatedCrest: Boolean = true) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "bookmyturf",
            color = WarmWhite,
            fontFamily = ManropeUi,
            fontSize = if (compact) 27.sp else 43.sp,
            lineHeight = if (compact) 30.sp else 46.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-2.1).sp
        )
        if (!compact) {
            Spacer(Modifier.height(4.dp))
            Text("BOOK  •  PLAY  •  DOMINATE", color = PulseLavender, style = MaterialTheme.typography.labelMedium, letterSpacing = 1.5.sp)
        }
    }
}

@Composable
fun AnimatedBrandCrest(modifier: Modifier = Modifier) {
    val playerHolder = remember { arrayOfNulls<MediaPlayer>(1) }
    DisposableEffect(Unit) {
        onDispose {
            playerHolder[0]?.release()
            playerHolder[0] = null
        }
    }
    AndroidView(
        modifier = modifier.clip(RoundedCornerShape(4.dp)),
        factory = { context ->
            TextureView(context).apply videoView@ {
                contentDescription = "Animated BookMyTurf crest"
                isOpaque = true
                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    private fun centerCrop(view: TextureView) {
                        if (view.width == 0 || view.height == 0) return
                        view.setTransform(Matrix().apply {
                            setScale(1.78f, 1f, view.width / 2f, view.height / 2f)
                        })
                    }

                    override fun onSurfaceTextureAvailable(surfaceTexture: android.graphics.SurfaceTexture, width: Int, height: Int) {
                        centerCrop(this@videoView)
                        val outputSurface = Surface(surfaceTexture)
                        playerHolder[0] = MediaPlayer().apply {
                            setDataSource(context, Uri.parse("android.resource://${context.packageName}/${R.raw.bmt_logo_animation}"))
                            setSurface(outputSurface)
                            setVolume(0f, 0f)
                            isLooping = true
                            setOnPreparedListener { it.start() }
                            setOnVideoSizeChangedListener { _, _, _ -> centerCrop(this@videoView) }
                            prepareAsync()
                        }
                        outputSurface.release()
                    }

                    override fun onSurfaceTextureSizeChanged(surfaceTexture: android.graphics.SurfaceTexture, width: Int, height: Int) {
                        centerCrop(this@videoView)
                    }

                    override fun onSurfaceTextureDestroyed(surfaceTexture: android.graphics.SurfaceTexture): Boolean {
                        playerHolder[0]?.release()
                        playerHolder[0] = null
                        return true
                    }

                    override fun onSurfaceTextureUpdated(surfaceTexture: android.graphics.SurfaceTexture) = Unit
                }
            }
        }
    )
}

@Composable
fun SectionTitle(title: String, action: String? = null, onAction: () -> Unit = {}) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(if (LocalAdminStyle.current) title.uppercase() else title, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
        if (action != null) TextButton(onClick = onAction) { Text(action, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium) }
    }
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, icon: ImageVector? = null) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val buttonScale by androidx.compose.animation.core.animateFloatAsState(if (pressed) .975f else 1f, tween(100), label = "buttonPress")
    Button(
        onClick = onClick, enabled = enabled,
        modifier = modifier.fillMaxWidth().height(54.dp).scale(buttonScale),
        interactionSource = interaction,
        shape = RoundedCornerShape(if (LocalAdminStyle.current) 8.dp else 16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = if (LocalAdminStyle.current) Lime else PulsePurple, contentColor = if (LocalAdminStyle.current) Ink else Color.White, disabledContainerColor = Line, disabledContentColor = TextMuted)
    ) {
        if (icon != null) { Icon(icon, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)) }
        Text(if (LocalAdminStyle.current) text.uppercase() else text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun SportIcon(sport: Sport, modifier: Modifier = Modifier) {
    val icon = when (sport) {
        Sport.FOOTBALL -> Icons.Default.SportsSoccer
        Sport.CRICKET -> Icons.Default.SportsCricket
        Sport.BADMINTON -> Icons.Default.SportsTennis
        Sport.BASKETBALL -> Icons.Default.SportsBasketball
        Sport.TENNIS -> Icons.Default.SportsTennis
        Sport.VOLLEYBALL -> Icons.Default.SportsVolleyball
    }
    Icon(icon, sport.label, modifier)
}

@Composable
fun TurfVisual(seed: Int, modifier: Modifier = Modifier, label: String? = null) {
    val photo = when (seed % 6) {
        1, 4 -> R.drawable.venue_cricket
        5 -> R.drawable.venue_badminton
        else -> R.drawable.venue_football
    }
    Box(modifier) {
        Image(painterResource(photo), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(turfVerticalOverlay))
        Box(Modifier.fillMaxSize().background(turfHorizontalOverlay))
        if (label != null) Surface(Modifier.align(Alignment.BottomStart).padding(12.dp), color = Ink.copy(.76f), shape = RoundedCornerShape(4.dp)) {
            Text(label.uppercase(), Modifier.padding(horizontal = 9.dp, vertical = 5.dp), style = MaterialTheme.typography.labelMedium, color = Lime)
        }
    }
}

@Composable
fun TurfCard(turf: Turf, onOpen: () -> Unit, onFavorite: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Charcoal), border = BorderStroke(1.dp, Line)) {
        Box {
            VenuePhoto(turf, Modifier.fillMaxWidth().height(176.dp))
            Surface(Modifier.align(Alignment.TopStart).padding(10.dp), color = Ink.copy(.84f), shape = RoundedCornerShape(5.dp), border = BorderStroke(1.dp, Lime.copy(.36f))) {
                Row(Modifier.padding(horizontal = 9.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(6.dp).background(Lime, CircleShape)); Spacer(Modifier.width(6.dp))
                    Text("${3 + turf.visualSeed.mod(5)} SLOTS TODAY", color = Lime, style = MaterialTheme.typography.labelMedium)
                }
            }
            val heartScale by androidx.compose.animation.core.animateFloatAsState(if (turf.isFavorite) 1.18f else 1f, label = "heart")
            IconButton(onClick = onFavorite, Modifier.align(Alignment.TopEnd).padding(8.dp).background(Ink.copy(.78f), CircleShape)) {
                Icon(if (turf.isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder, "Favorite", Modifier.scale(heartScale), tint = if (turf.isFavorite) Lime else Color.White)
            }
        }
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(turf.name, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, null, Modifier.size(14.dp), tint = TextMuted)
                        Text(" ${turf.area}, ${turf.city}  •  ${turf.distanceKm} km", color = TextMuted, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("₹${turf.pricePerHour}", color = Lime, style = MaterialTheme.typography.titleLarge)
                    Text("PER HOUR", color = TextMuted, style = MaterialTheme.typography.labelMedium)
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Star, null, Modifier.size(17.dp), tint = Orange)
                Text(" ${turf.rating}", fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                OutlinedButton(onClick = onOpen, shape = RoundedCornerShape(6.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = Lime), border = BorderStroke(1.dp, Lime)) {
                    Text("VIEW SLOTS", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
fun Tag(text: String, selected: Boolean = false, onClick: (() -> Unit)? = null) {
    val bg by animateColorAsState(if (selected) MaterialTheme.colorScheme.primary else Steel, label = "tag")
    val fg = if (selected) Ink else TextPrimary
    Surface(
        color = bg, contentColor = fg, shape = RoundedCornerShape(if (LocalAdminStyle.current) 6.dp else 16.dp),
        border = if (selected) null else BorderStroke(1.dp, Line),
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    ) { Text(text.uppercase(), Modifier.padding(horizontal = 12.dp, vertical = 9.dp), style = MaterialTheme.typography.labelMedium) }
}

@Composable
fun StatusBadge(text: String, color: Color = Lime) {
    Surface(color = color.copy(alpha = .13f), shape = RoundedCornerShape(4.dp), border = BorderStroke(1.dp, color.copy(.45f))) {
        Text(text.uppercase(), Modifier.padding(horizontal = 8.dp, vertical = 5.dp), color = color, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun StaggeredReveal(index: Int, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    val progress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(240, delayMillis = index.coerceAtMost(6) * 34),
        label = "homeReveal"
    )
    LaunchedEffect(Unit) { visible = true }
    val motion = if (progress < .999f) Modifier.graphicsLayer {
        alpha = progress
        translationY = (1f - progress) * 22f
        scaleX = .985f + (.015f * progress)
        scaleY = .985f + (.015f * progress)
    } else Modifier
    Box(modifier.then(motion)) { content() }
}

fun formatInr(value: Double): String = if (value % 1.0 == 0.0) "₹%,.0f".format(value) else "₹%,.2f".format(value)
