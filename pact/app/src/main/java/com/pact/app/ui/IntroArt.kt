package com.pact.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.pact.app.ui.theme.CardBorder
import com.pact.app.ui.theme.Ink
import com.pact.app.ui.theme.Mint
import com.pact.app.ui.theme.PactGradient
import com.pact.app.ui.theme.Periwinkle
import com.pact.app.ui.theme.Surface1
import com.pact.app.ui.theme.Surface2
import com.pact.app.ui.theme.Violet

/**
 * Decorative illustrations for the intro pages, drawn entirely in Compose —
 * no image assets, crisp at any density, tiny APK cost.
 */

@Composable
private fun Glow(size: Int, color: Color, content: @Composable () -> Unit) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(size.dp)) {
        Box(
            modifier = Modifier
                .size(size.dp)
                .background(
                    Brush.radialGradient(listOf(color.copy(alpha = 0.28f), color.copy(alpha = 0f)))
                )
        )
        content()
    }
}

private val TILE_COLORS = listOf(
    Color(0xFFFF6B81), Color(0xFFFFB65C), Color(0xFF5EEAD4),
    Color(0xFFA5B8FF), Color(0xFFF472B6), Color(0xFF7DD3FC),
    Color(0xFF8E7CFF), Color(0xFF60D394), Color(0xFFFACC15),
)

/** Page 1: a phone full of tempting app tiles with a big lock badge. */
@Composable
fun ArtLockedPhone() {
    Glow(260, Violet) {
        Box {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .size(width = 132.dp, height = 200.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Surface1)
                    .border(2.dp, CardBorder, RoundedCornerShape(28.dp))
                    .padding(top = 18.dp),
            ) {
                repeat(3) { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        repeat(3) { col ->
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(TILE_COLORS[row * 3 + col].copy(alpha = 0.85f))
                            )
                        }
                    }
                }
                Spacer(Modifier.height(2.dp))
                Box(
                    Modifier
                        .size(width = 44.dp, height = 5.dp)
                        .clip(CircleShape)
                        .background(Surface2)
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 26.dp, y = 18.dp)
                    .size(74.dp)
                    .clip(CircleShape)
                    .background(PactGradient)
                    .border(4.dp, Ink, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = Ink,
                    modifier = Modifier.size(34.dp),
                )
            }
        }
    }
}

/** Page 2: two people joined by a key — the human unlock. */
@Composable
fun ArtTwoPeople() {
    Glow(260, Periwinkle) {
        Box(contentAlignment = Alignment.Center) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PersonBubble(tint = Periwinkle)
                Canvas(Modifier.size(width = 84.dp, height = 24.dp)) {
                    drawLine(
                        color = Color(0xFF3A4568),
                        start = Offset(0f, size.height / 2),
                        end = Offset(size.width, size.height / 2),
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(6.dp.toPx(), 8.dp.toPx())
                        ),
                    )
                }
                PersonBubble(tint = Mint)
            }
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(PactGradient)
                    .border(4.dp, Ink, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Key,
                    contentDescription = null,
                    tint = Ink,
                    modifier = Modifier.size(26.dp),
                )
            }
        }
    }
}

@Composable
private fun PersonBubble(tint: Color) {
    Box(
        modifier = Modifier
            .size(84.dp)
            .clip(CircleShape)
            .background(Surface1)
            .border(3.dp, tint.copy(alpha = 0.7f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Rounded.Person,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(44.dp),
        )
    }
}

/** Page 3: a shield in orbit — private and offline. */
@Composable
fun ArtOfflineShield() {
    Glow(260, Mint) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(210.dp)) {
                val ringStroke = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 10.dp.toPx())),
                )
                drawCircle(color = Color(0xFF3A4568), style = ringStroke)
                drawCircle(color = Color(0xFF3A4568), radius = size.minDimension / 2.9f, style = ringStroke)
                // orbiting dots
                drawCircle(Periwinkle, radius = 5.dp.toPx(), center = Offset(size.width * 0.5f, 0f + 2.dp.toPx()))
                drawCircle(Mint, radius = 5.dp.toPx(), center = Offset(size.width * 0.86f, size.height * 0.78f))
                drawCircle(Violet, radius = 4.dp.toPx(), center = Offset(size.width * 0.16f, size.height * 0.7f))
            }
            Box(
                modifier = Modifier
                    .size(108.dp)
                    .clip(RoundedCornerShape(34.dp))
                    .background(PactGradient),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Shield,
                    contentDescription = null,
                    tint = Color(0xFFF4F6FF),
                    modifier = Modifier.size(54.dp),
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = (-16).dp, y = (-16).dp)
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Surface1)
                    .border(2.dp, Mint.copy(alpha = 0.7f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.CloudOff,
                    contentDescription = null,
                    tint = Mint,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}
