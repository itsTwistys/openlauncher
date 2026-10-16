package dev.nova.cinema

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NovaTheme {
                val model: CinemaModel = viewModel()
                CinemaApp(model)
            }
        }
    }
}

@Composable
fun CinemaApp(model: CinemaModel) {
    val state = model.state
    BackHandler(enabled = state.player != null || state.details != null || state.tab != "Home") {
        when {
            state.player != null -> model.closePlayer()
            state.details != null -> model.closeDetails()
            else -> model.selectTab("Home")
        }
    }
    Box(Modifier.fillMaxSize().background(Ink)) {
        PremiumBackground(Modifier.fillMaxSize())
        if (state.player != null) {
            PlayerScreen(state.player, model::closePlayer)
        } else {
            AnimatedContent(targetState = state.details != null,
                transitionSpec = {
                    (fadeIn(tween(360)) + slideInVertically(tween(360)) { it / 9 })
                        .togetherWith(fadeOut(tween(210)) + slideOutVertically(tween(210)) { -it / 12 })
                }, label = "cinematic-navigation") { showDetails ->
                if (showDetails && state.details != null) {
                    DetailScreen(state, model)
                } else {
                    Box(Modifier.fillMaxSize()) {
                        AnimatedContent(targetState = state.tab,
                            transitionSpec = { fadeIn(tween(280)).togetherWith(fadeOut(tween(160))) },
                            label = "tabs") { tab ->
                            when (tab) {
                                "Home" -> HomeScreen(state, model)
                                "Discover" -> DiscoverScreen(state, model)
                                "Search" -> SearchScreen(state, model)
                                "Saved" -> SavedScreen(state, model)
                                "Add-ons" -> AddonsScreen(state, model)
                                else -> HomeScreen(state, model)
                            }
                        }
                        FloatingNavigation(state.tab, model::selectTab,
                            Modifier.align(Alignment.BottomCenter).navigationBarsPadding()
                                .padding(start = 19.dp, end = 19.dp, bottom = 13.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun BrandHeader(onAddons: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 25.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(38.dp).clip(RoundedCornerShape(13.dp))
            .background(Brush.linearGradient(listOf(Amber, Violet))),
            contentAlignment = Alignment.Center) {
            Text("N", fontWeight = FontWeight.Black, fontSize = 25.sp, color = Ink)
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text("NOVA", fontSize = 19.sp, fontWeight = FontWeight.Black,
                color = WarmWhite, letterSpacing = 3.8.sp, lineHeight = 20.sp)
            Text("YOUR CINEMA, REIMAGINED", color = SoftWhite.copy(alpha = .55f),
                fontSize = 8.sp, letterSpacing = 1.3.sp)
        }
        Spacer(Modifier.weight(1f))
        Box(Modifier.size(43.dp).clip(CircleShape)
            .background(Color.White.copy(alpha = .075f))
            .border(1.dp, Color.White.copy(alpha = .08f), CircleShape)
            .clickable(onClick = onAddons), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Tune, contentDescription = "Manage add-ons", tint = WarmWhite,
                modifier = Modifier.size(21.dp))
        }
    }
}

@Composable
private fun FloatingNavigation(selected: String, onTab: (String) -> Unit, modifier: Modifier = Modifier) {
    val items = listOf(
        Triple("Home", Icons.Rounded.Home, "Home"),
        Triple("Discover", Icons.Rounded.Explore, "Explore"),
        Triple("Search", Icons.Rounded.Search, "Search"),
        Triple("Saved", Icons.Rounded.Bookmark, "Saved"),
        Triple("Add-ons", Icons.Rounded.Extension, "Add-ons"),
    )
    Row(modifier.fillMaxWidth().shadow(30.dp, RoundedCornerShape(28.dp))
        .clip(RoundedCornerShape(28.dp))
        .background(Color(0xF21B1D29))
        .border(1.dp, Color.White.copy(alpha = .13f), RoundedCornerShape(28.dp))
        .padding(horizontal = 7.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
        items.forEach { (id, icon, text) ->
            val selectedHere = selected == id
            Row(Modifier.weight(if (selectedHere) 1.5f else 1f)
                .clip(RoundedCornerShape(22.dp))
                .background(if (selectedHere) Amber.copy(alpha = .13f) else Color.Transparent)
                .clickable { onTab(id) }
                .padding(vertical = 12.dp, horizontal = 3.dp),
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = text,
                    tint = if (selectedHere) Amber else SoftWhite.copy(alpha = .7f),
                    modifier = Modifier.size(21.dp))
                AnimatedVisibility(visible = selectedHere) {
                    Row {
                        Spacer(Modifier.width(6.dp))
                        Text(text, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                            color = Amber, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
fun HomeScreen(state: CinemaState, model: CinemaModel) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 125.dp)) {
        item {
            Spacer(Modifier.statusBarsPadding())
            BrandHeader { model.selectTab("Add-ons") }
            Spacer(Modifier.height(10.dp))
            Column(Modifier.padding(horizontal = 25.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PulseDot()
                    Spacer(Modifier.width(9.dp))
                    Text("THE SCREEN IS YOURS", color = Amber, fontSize = 10.sp,
                        letterSpacing = 2.5.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(9.dp))
                Text("An entire world\nof stories.", style = MaterialTheme.typography.displayLarge,
                    color = WarmWhite, lineHeight = 47.sp)
                Spacer(Modifier.height(8.dp))
                Text("Discover something extraordinary tonight.", color = SoftWhite,
                    fontSize = 13.sp)
            }
            Spacer(Modifier.height(23.dp))
        }
        item {
            val featured = state.rows.firstOrNull()?.movies.orEmpty()
                .filter { it.backdrop.isNotBlank() || it.poster.isNotBlank() }.take(5)
            if (featured.isNotEmpty()) FeaturedCarousel(featured, model::open)
            else Skeleton(Modifier.fillMaxWidth().padding(horizontal = 18.dp).height(420.dp))
            Spacer(Modifier.height(24.dp))
        }
        item {
            BrowsePills(onTap = model::selectTab)
            Spacer(Modifier.height(26.dp))
        }
        if (state.loading) {
            items(2) {
                Column {
                    Skeleton(Modifier.padding(start = 24.dp).width(180.dp).height(22.dp))
                    Spacer(Modifier.height(18.dp))
                    LazyRow(contentPadding = PaddingValues(horizontal = 24.dp)) {
                        items(4) { Skeleton(Modifier.width(140.dp).height(205.dp)) ; Spacer(Modifier.width(12.dp)) }
                    }
                    Spacer(Modifier.height(30.dp))
                }
            }
        } else if (state.rows.isEmpty()) {
            item {
                EmptyState("The curtains are opening",
                    "Catalogs are unavailable right now. Check your connection or add-ons.",
                    Icons.Rounded.WifiOff, "Try again", model::refresh)
            }
        } else {
            items(state.rows) { row ->
                MovieStrip(row.title, row.movies, model::open)
                Spacer(Modifier.height(28.dp))
            }
        }
        item { EndCaption() }
    }
}

@Composable
private fun FeaturedCarousel(movies: List<Movie>, onOpen: (Movie) -> Unit) {
    val pager = rememberPagerState(pageCount = { movies.size })
    LaunchedEffect(pager.currentPage, movies.size) {
        delay(7500)
        if (movies.size > 1) pager.animateScrollToPage((pager.currentPage + 1) % movies.size)
    }
    Column {
        HorizontalPager(state = pager, contentPadding = PaddingValues(horizontal = 18.dp),
            pageSpacing = 12.dp, modifier = Modifier.height(416.dp)) { page ->
            val movie = movies[page]
            Box(Modifier.fillMaxSize().clip(RoundedCornerShape(30.dp))
                .background(Layer).clickable { onOpen(movie) }) {
                AsyncImage(model = movie.backdrop.ifBlank { movie.poster },
                    contentDescription = null, contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize())
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
                    colorStops = arrayOf(
                        0f to Color.Black.copy(alpha = .04f),
                        .34f to Ink.copy(alpha = .25f),
                        .72f to Ink.copy(alpha = .79f),
                        1f to Ink,
                    ))))
                Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(
                    listOf(Ink.copy(alpha = .46f), Color.Transparent))))
                Column(Modifier.align(Alignment.BottomStart)
                    .padding(start = 23.dp, end = 23.dp, bottom = 25.dp)) {
                    Tag("FEATURED  /  ${movie.type.uppercase()}", Amber, Amber.copy(alpha = .16f))
                    Spacer(Modifier.height(15.dp))
                    Text(movie.title, style = MaterialTheme.typography.displayLarge,
                        fontSize = 34.sp, lineHeight = 39.sp, color = WarmWhite, maxLines = 2,
                        overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (movie.year.isNotBlank()) Text(movie.year, color = WarmWhite, fontSize = 12.sp)
                        if (movie.rating.isNotBlank()) {
                            Spacer(Modifier.width(15.dp))
                            Icon(Icons.Rounded.Star, null, tint = Amber, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(movie.rating, color = WarmWhite, fontSize = 12.sp)
                        }
                        if (movie.genres.isNotEmpty()) {
                            Spacer(Modifier.width(15.dp))
                            Text(movie.genres.take(2).joinToString(" · "), color = SoftWhite,
                                fontSize = 12.sp, maxLines = 1)
                        }
                    }
                    Spacer(Modifier.height(19.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.clip(RoundedCornerShape(15.dp))
                            .background(Amber).padding(horizontal = 18.dp, vertical = 12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.PlayArrow, null, tint = Ink, modifier = Modifier.size(19.dp))
                                Spacer(Modifier.width(5.dp))
                                Text("Explore title", color = Ink, fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp)
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.Rounded.ArrowForward, "View details", tint = WarmWhite,
                            modifier = Modifier.size(25.dp))
                    }
                }
            }
        }
        Spacer(Modifier.height(13.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            movies.indices.forEach { i ->
                val width = if (pager.currentPage == i) 25.dp else 6.dp
                Box(Modifier.padding(horizontal = 3.dp).height(5.dp).width(width)
                    .clip(CircleShape).background(if (pager.currentPage == i) Amber
                    else Color.White.copy(alpha = .22f)))
            }
        }
    }
}

@Composable
private fun BrowsePills(onTap: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
        .padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        listOf(
            Triple("Explore all", "Discover", Icons.Rounded.AutoAwesome),
            Triple("Movies", "Discover", Icons.Rounded.Movie),
            Triple("TV series", "Discover", Icons.Rounded.LiveTv),
            Triple("My list", "Saved", Icons.Rounded.BookmarkBorder),
        ).forEach { (name, screen, icon) ->
            Row(Modifier.clip(RoundedCornerShape(15.dp))
                .background(Color.White.copy(alpha = .075f))
                .border(1.dp, Color.White.copy(alpha = .07f), RoundedCornerShape(15.dp))
                .clickable { onTap(screen) }.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = Amber, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(name, color = WarmWhite, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun MovieStrip(title: String, items: List<Movie>, onOpen: (Movie) -> Unit) {
    if (items.isEmpty()) return
    Column {
        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.headlineMedium,
                color = WarmWhite, modifier = Modifier.weight(1f), maxLines = 2)
            Icon(Icons.Rounded.ArrowForward, null, tint = Amber, modifier = Modifier.size(19.dp))
        }
        Spacer(Modifier.height(16.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(13.dp),
            contentPadding = PaddingValues(horizontal = 24.dp)) {
            items(items, key = { it.id + it.type }) { movie ->
                Poster(movie, onOpen, width = 139)
            }
        }
    }
}

@Composable
fun Poster(movie: Movie, onOpen: (Movie) -> Unit, width: Int = 142) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) .94f else 1f, tween(170), label = "poster-press")
    Column(Modifier.width(width.dp).graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(interactionSource = interaction, indication = null) { onOpen(movie) }) {
        Box(Modifier.fillMaxWidth().height((width * 1.48f).dp)
            .clip(RoundedCornerShape(19.dp)).background(Layer)) {
            if (movie.poster.isNotBlank())
                AsyncImage(model = movie.poster, contentDescription = movie.title,
                    contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            else Box(Modifier.fillMaxSize().background(Brush.linearGradient(
                listOf(Violet.copy(alpha = .35f), Amber.copy(alpha = .21f)))),
                contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Movie, null, tint = WarmWhite, modifier = Modifier.size(34.dp))
            }
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
                listOf(Color.Transparent, Ink.copy(alpha = .42f)))))
            if (movie.rating.isNotBlank()) {
                Row(Modifier.align(Alignment.TopEnd).padding(9.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Ink.copy(alpha = .78f))
                    .padding(horizontal = 7.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Star, null, tint = Amber, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(3.dp))
                    Text(movie.rating, color = WarmWhite, fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(movie.title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = WarmWhite,
            lineHeight = 17.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(3.dp))
        Text(listOf(movie.year, if (movie.type == "series") "Series" else "Film")
            .filter { it.isNotBlank() }.joinToString(" · "),
            color = SoftWhite.copy(alpha = .69f), fontSize = 11.sp, maxLines = 1)
    }
}

@Composable
private fun EndCaption() {
    Column(Modifier.fillMaxWidth().padding(top = 15.dp, bottom = 25.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text("N O V A", color = WarmWhite.copy(alpha = .37f),
            fontSize = 12.sp, letterSpacing = 5.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(5.dp))
        Text("EVERY STORY DESERVES A SCREEN", color = SoftWhite.copy(alpha = .32f),
            fontSize = 9.sp, letterSpacing = 2.sp)
    }
}

@Composable
fun EmptyState(title: String, message: String, icon: ImageVector,
    action: String? = null, onAction: (() -> Unit)? = null) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 30.dp, vertical = 45.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(78.dp).clip(RoundedCornerShape(25.dp))
            .background(Violet.copy(alpha = .11f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Violet, modifier = Modifier.size(34.dp))
        }
        Spacer(Modifier.height(19.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, color = WarmWhite)
        Spacer(Modifier.height(8.dp))
        Text(message, color = SoftWhite, fontSize = 13.sp,
            lineHeight = 20.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        if (action != null && onAction != null) {
            Spacer(Modifier.height(20.dp))
            FilledTonalButton(onClick = onAction, colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = Amber.copy(alpha = .18f), contentColor = Amber)) { Text(action) }
        }
    }
}
