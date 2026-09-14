package com.bookmyturf.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import com.bookmyturf.app.R
import com.bookmyturf.app.data.Sport
import com.bookmyturf.app.ui.theme.*

@Composable
fun HomeSportTile(sport: Sport, selected: Boolean, onClick: () -> Unit) {
    val image = when (sport) {
        Sport.FOOTBALL -> R.drawable.sport_football
        Sport.CRICKET -> R.drawable.sport_cricket
        Sport.BADMINTON -> R.drawable.sport_badminton
        Sport.BASKETBALL -> R.drawable.sport_basketball
        Sport.TENNIS -> R.drawable.sport_tennis
        Sport.VOLLEYBALL -> R.drawable.sport_volleyball
    }
    Column(
        modifier = Modifier.width(118.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
            color = if (selected) PulsePurple.copy(.2f) else Steel,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) PulseLavender else Line)
        ) {
            Image(
                painter = painterResource(image),
                contentDescription = sport.label,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }
        Text(
            sport.label,
            modifier = Modifier.padding(top = 7.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (selected) PulseLavender else TextPrimary
        )
    }
}
