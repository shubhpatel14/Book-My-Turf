package com.bookmyturf.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import com.bookmyturf.app.data.Sport
import com.bookmyturf.app.ui.components.SportIcon
import com.bookmyturf.app.ui.theme.*
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun HomeSportTile(sport: Sport, selected: Boolean, onClick: () -> Unit) {
    val loop = rememberInfiniteTransition(label = "${sport.name}Loop")
    val phase = loop.animateFloat(0f, 1f, infiniteRepeatable(tween(2_400, easing = LinearEasing)), label = "${sport.name}Phase")
    val glow = loop.animateFloat(.08f, .2f, infiniteRepeatable(tween(1_200), RepeatMode.Reverse), label = "${sport.name}Glow")
    val accent = when (sport) {
        Sport.FOOTBALL -> Color(0xFF9BF2A1)
        Sport.CRICKET -> Color(0xFFFFB46B)
        Sport.BADMINTON -> Color(0xFF72E5E1)
        Sport.BASKETBALL -> Color(0xFFFF9255)
        Sport.TENNIS -> Color(0xFFD9F879)
        Sport.VOLLEYBALL -> Color(0xFF99B4FF)
    }
    val surface by animateColorAsState(if (selected) PulsePurple.copy(.28f) else Steel, tween(220), label = "sportSurface")
    Surface(
        onClick = onClick,
        modifier = Modifier.width(112.dp),
        color = surface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) PulseLavender else Line)
    ) {
        Column(Modifier.padding(vertical = 13.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                Surface(modifier = Modifier.fillMaxSize(), color = accent.copy(alpha = glow.value), shape = RoundedCornerShape(22.dp)) {}
                SportIcon(
                    sport,
                    Modifier.size(34.dp).graphicsLayer {
                        val wave = sin(phase.value * PI.toFloat() * 2f)
                        when (sport) {
                            Sport.FOOTBALL -> { rotationZ = phase.value * 360f; translationX = wave * 8f }
                            Sport.CRICKET -> rotationZ = wave * 13f
                            Sport.BADMINTON -> translationY = wave * 7f
                            Sport.BASKETBALL -> { rotationZ = phase.value * 300f; translationY = -kotlin.math.abs(wave) * 8f }
                            Sport.TENNIS -> { scaleX = 1f + wave * .08f; scaleY = scaleX }
                            Sport.VOLLEYBALL -> rotationZ = phase.value * -360f
                        }
                    },
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(sport.label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = if (selected) Color.White else TextPrimary)
            Text(if (selected) "SELECTED" else "VIEW TURFS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium, color = if (selected) PulseLavender else TextMuted)
        }
    }
}
