package dev.nova.cinema

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Ink = Color(0xFF141014)
val Layer = Color(0xFF241D24)
val WarmWhite = Color(0xFFF8F5F2)
val SoftWhite = Color(0xFFD1C7CB)
val Amber = Color(0xFFFFC86F)
val Violet = Color(0xFFB5A1FF)
val Hairline = Color(0xFF564B52)

private val colors = darkColorScheme(
    primary = WarmWhite, secondary = Violet, background = Ink,
    surface = Layer, onPrimary = Ink, onBackground = WarmWhite,
    onSurface = WarmWhite, outline = Hairline
)

@Composable
fun UniTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, typography = Typography(
        displayLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 43.sp,
            lineHeight = 47.sp, fontWeight = FontWeight.Black, letterSpacing = (-1.7).sp),
        headlineLarge = TextStyle(fontSize = 32.sp, lineHeight = 37.sp,
            fontWeight = FontWeight.Bold, letterSpacing = (-1).sp),
        headlineMedium = TextStyle(fontSize = 23.sp, lineHeight = 29.sp,
            fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
        titleLarge = TextStyle(fontSize = 19.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold),
        bodyLarge = TextStyle(fontSize = 15.sp, lineHeight = 23.sp),
        bodyMedium = TextStyle(fontSize = 13.sp, lineHeight = 19.sp),
        labelLarge = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold)
    ), content = content)
}

@Composable
fun PremiumBackground(modifier: Modifier = Modifier) {
    Box(modifier.background(Ink)) {
        Box(Modifier.fillMaxSize().background(Brush.radialGradient(
            colors = listOf(Color(0xFF8A514A).copy(alpha = .24f), Color.Transparent),
            radius = 1050f
        )))
        Box(Modifier.fillMaxWidth().height(550.dp).align(Alignment.TopEnd)
            .background(Brush.verticalGradient(
                listOf(Violet.copy(alpha = .075f), Color.Transparent))))
    }
}

@Composable
fun SectionHeading(title: String, eyebrow: String = "", modifier: Modifier = Modifier) {
    Column(modifier) {
        if (eyebrow.isNotBlank()) {
            Text(eyebrow.uppercase(), fontSize = 10.sp, letterSpacing = 2.8.sp,
                color = Amber, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(7.dp))
        }
        Text(title, style = MaterialTheme.typography.headlineMedium, color = WarmWhite)
    }
}

@Composable
fun Tag(text: String, tint: Color = WarmWhite, fill: Color = Color.White.copy(alpha = .1f),
    horizontal: Dp = 11.dp) {
    Box(Modifier.clip(RoundedCornerShape(50)).background(fill)
        .padding(horizontal = horizontal, vertical = 7.dp)) {
        Text(text, fontSize = 11.sp, color = tint, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun PulseDot() {
    val transition = rememberInfiniteTransition(label = "pulse")
    val alpha = transition.animateFloat(.35f, 1f,
        infiniteRepeatable(tween(1600), RepeatMode.Reverse), label = "alpha")
    Box(Modifier.size(7.dp).graphicsLayer { this.alpha = alpha.value }
        .clip(CircleShape).background(Amber))
}

@Composable
fun Skeleton(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "loading")
    val glow = transition.animateFloat(0.15f, 0.45f,
        infiniteRepeatable(tween(1100), RepeatMode.Reverse), label = "light")
    Box(modifier.clip(RoundedCornerShape(20.dp)).background(Brush.linearGradient(
        listOf(Color.White.copy(alpha = .05f), Color.White.copy(alpha = glow.value),
            Color.White.copy(alpha = .05f)))))
}
