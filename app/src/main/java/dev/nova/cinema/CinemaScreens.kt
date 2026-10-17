package dev.nova.cinema

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay

@Composable
fun DiscoverScreen(state: CinemaState, model: CinemaModel) {
    var selectedType by rememberSaveable { mutableStateOf("movie") }
    val rows = state.rows.filter { row -> row.movies.firstOrNull()?.type == selectedType }
    LazyColumn(Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 126.dp),
        verticalArrangement = Arrangement.spacedBy(23.dp)) {
        item {
            Spacer(Modifier.statusBarsPadding())
            TopHeading("DISCOVER", "Find your next obsession.",
                "Curated stories from your connected add-ons.")
            Spacer(Modifier.height(19.dp))
            Row(Modifier.padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(18.dp)).background(Color.White.copy(alpha = .07f))
                .padding(5.dp)) {
                listOf("movie" to "Movies", "series" to "TV shows").forEach { (type, label) ->
                    val chosen = type == selectedType
                    Box(Modifier.weight(1f).clip(RoundedCornerShape(14.dp))
                        .background(if (chosen) WarmWhite else Color.Transparent)
                        .clickable { selectedType = type }
                        .padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                        Text(label, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                            color = if (chosen) Ink else SoftWhite)
                    }
                }
            }
        }
        if (state.loading) item { Skeleton(Modifier.fillMaxWidth().padding(24.dp).height(200.dp)) }
        else if (rows.isEmpty()) item {
            EmptyState("Nothing on this shelf yet",
                "Try refreshing, or add another Stremio catalog.",
                Icons.Rounded.Explore, "Reload catalogs", model::refresh)
        }
        else items(rows) { row -> MovieStrip(row.title, row.movies, model::open) }
    }
}

@Composable
fun SearchScreen(state: CinemaState, model: CinemaModel) {
    LaunchedEffect(state.query) {
        val term = state.query.trim()
        if (term.length >= 2) {
            delay(430)
            model.search(term)
        }
    }
    Column(Modifier.fillMaxSize()) {
        Spacer(Modifier.statusBarsPadding())
        TopHeading("SEARCH", "What are we watching?", "One search. Every connected catalog.")
        Spacer(Modifier.height(22.dp))
        OutlinedTextField(
            value = state.query, onValueChange = model::updateQuery,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 23.dp),
            placeholder = { Text("Search titles, films, series...", color = SoftWhite.copy(alpha = .55f)) },
            leadingIcon = { Icon(Icons.Rounded.Search, null, tint = Amber) },
            trailingIcon = {
                if (state.query.isNotEmpty()) IconButton(onClick = { model.updateQuery("") }) {
                    Icon(Icons.Rounded.Close, "Clear search", tint = SoftWhite)
                }
            },
            singleLine = true, shape = RoundedCornerShape(19.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Amber.copy(alpha = .65f),
                unfocusedBorderColor = Color.White.copy(alpha = .12f),
                focusedContainerColor = Layer, unfocusedContainerColor = Layer,
                focusedTextColor = WarmWhite, unfocusedTextColor = WarmWhite),
        )
        Spacer(Modifier.height(19.dp))
        if (state.query.trim().length < 2) {
            EmptyState("The search starts here", "Search for a film, a series, or a story you love.",
                Icons.Rounded.AutoAwesome)
        } else if (state.searching) {
            Row(Modifier.padding(start = 25.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(17.dp), color = Amber, strokeWidth = 2.dp)
                Spacer(Modifier.width(11.dp))
                Text("Exploring your add-ons…", fontSize = 13.sp, color = SoftWhite)
            }
        } else if (state.searchResults.isEmpty()) {
            EmptyState("No matching stories", "Try a different title or install more catalog add-ons.",
                Icons.Rounded.SearchOff)
        } else {
            Text("${state.searchResults.size} results",
                Modifier.padding(start = 25.dp, bottom = 14.dp),
                fontSize = 12.sp, color = SoftWhite, letterSpacing = .3.sp)
            MediaGrid(state.searchResults, model::open)
        }
    }
}

@Composable
fun SavedScreen(state: CinemaState, model: CinemaModel) {
    Column(Modifier.fillMaxSize()) {
        Spacer(Modifier.statusBarsPadding())
        TopHeading("MY COLLECTION", "Keep the good ones.",
            "Your personal watchlist, saved on this device.")
        Spacer(Modifier.height(18.dp))
        if (state.saved.isEmpty()) {
            EmptyState("Your collection awaits",
                "Save films and shows from their detail page. They will appear here.",
                Icons.Rounded.BookmarkBorder, "Discover titles", { model.selectTab("Discover") })
        } else MediaGrid(state.saved, model::open)
    }
}

@Composable
private fun MediaGrid(items: List<Movie>, onTap: (Movie) -> Unit) {
    LazyVerticalGrid(columns = GridCells.Adaptive(minSize = 146.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 21.dp, end = 21.dp, bottom = 125.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(23.dp)) {
        items(items, key = { it.type + it.id }) { movie ->
            Box(contentAlignment = Alignment.TopCenter) {
                Poster(movie, onTap, width = 146)
            }
        }
    }
}

@Composable
fun AddonsScreen(state: CinemaState, model: CinemaModel) {
    var url by rememberSaveable { mutableStateOf("") }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 135.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Spacer(Modifier.statusBarsPadding())
            TopHeading("CONNECTED WORLD", "Your add-ons. Your rules.",
                "Bring catalogs and viewing options into one beautiful place.")
        }
        item {
            Column(Modifier.padding(horizontal = 23.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(25.dp))
                .background(Brush.linearGradient(listOf(Violet.copy(alpha = .16f),
                    Amber.copy(alpha = .075f), Layer)))
                .border(1.dp, Color.White.copy(alpha = .11f), RoundedCornerShape(25.dp))
                .padding(19.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Extension, null, tint = Amber, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(9.dp))
                    Text("INSTALL AN ADD-ON", fontSize = 11.sp, color = WarmWhite,
                        fontWeight = FontWeight.Black, letterSpacing = 1.6.sp)
                }
                Spacer(Modifier.height(9.dp))
                Text("Paste a Stremio manifest link. Both stremio:// and https:// addresses are supported.",
                    color = SoftWhite, fontSize = 12.sp, lineHeight = 19.sp)
                Spacer(Modifier.height(15.dp))
                OutlinedTextField(value = url, onValueChange = { url = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("https://addon.example/manifest.json", fontSize = 12.sp,
                        color = SoftWhite.copy(alpha = .4f)) },
                    singleLine = true, shape = RoundedCornerShape(15.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Amber, unfocusedBorderColor = Hairline,
                        focusedTextColor = WarmWhite, unfocusedTextColor = WarmWhite))
                Spacer(Modifier.height(12.dp))
                Button(onClick = { if (url.isNotBlank()) { model.install(url); url = "" } },
                    enabled = url.isNotBlank() && !state.changingAddon,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Amber,
                        contentColor = Ink)) {
                    if (state.changingAddon) CircularProgressIndicator(
                        Modifier.size(16.dp), color = Ink, strokeWidth = 2.dp)
                    else {
                        Icon(Icons.Rounded.Add, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(7.dp))
                        Text("Connect add-on", fontWeight = FontWeight.Bold)
                    }
                }
                if (state.message.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Text(state.message, fontSize = 12.sp,
                        color = if (state.message.contains("installed") ||
                            state.message.contains("removed")) Amber else Color(0xFFFF9E9E))
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 25.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text("Connected add-ons", style = MaterialTheme.typography.titleLarge,
                    color = WarmWhite)
                Spacer(Modifier.weight(1f))
                Tag("${state.addons.size} active", Amber, Amber.copy(alpha = .12f))
            }
        }
        items(state.addons, key = { it.id }) { addon ->
            AddonCard(addon, state.changingAddon, { model.remove(addon.id) })
        }
        item {
            Column(Modifier.fillMaxWidth().padding(horizontal = 26.dp, vertical = 10.dp)) {
                Icon(Icons.Rounded.Shield, contentDescription = null,
                    tint = SoftWhite.copy(alpha = .75f), modifier = Modifier.size(18.dp))
                Spacer(Modifier.height(8.dp))
                Text("Only install add-ons from sources you trust. Add-ons are independent services and may have their own terms. UNI does not supply video content.",
                    color = SoftWhite.copy(alpha = .73f), fontSize = 12.sp, lineHeight = 19.sp)
            }
        }
    }
}

@Composable
private fun AddonCard(addon: Addon, busy: Boolean, onRemove: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 23.dp)
        .clip(RoundedCornerShape(20.dp)).background(Layer)
        .border(1.dp, Color.White.copy(alpha = .08f), RoundedCornerShape(20.dp))
        .padding(15.dp),
        verticalAlignment = Alignment.Top) {
        Box(Modifier.size(45.dp).clip(RoundedCornerShape(14.dp))
            .background(Violet.copy(alpha = .14f)), contentAlignment = Alignment.Center) {
            Icon(if (addon.id == "com.linvo.cinemeta") Icons.Rounded.Movie
                 else Icons.Rounded.Extension, null, tint = Violet, modifier = Modifier.size(23.dp))
        }
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(addon.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = WarmWhite)
            Spacer(Modifier.height(4.dp))
            Text(addon.description.ifBlank { "Connected Stremio add-on" },
                fontSize = 11.sp, color = SoftWhite, lineHeight = 16.sp, maxLines = 2,
                overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(11.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if ("catalog" in addon.resources)
                    Tag("Catalog", Violet, Violet.copy(alpha = .12f), 9.dp)
                if ("stream" in addon.resources)
                    Tag("Streams", Amber, Amber.copy(alpha = .12f), 9.dp)
                if ("subtitles" in addon.resources)
                    Tag("Subtitles", SoftWhite, Color.White.copy(alpha = .07f), 9.dp)
            }
        }
        if (addon.id != "com.linvo.cinemeta") {
            IconButton(onClick = onRemove, enabled = !busy,
                modifier = Modifier.size(36.dp)) {
                Icon(Icons.Rounded.DeleteOutline, "Remove add-on", tint = SoftWhite,
                    modifier = Modifier.size(18.dp))
            }
        } else Icon(Icons.Rounded.Verified, "Built-in", tint = Amber,
            modifier = Modifier.padding(top = 9.dp).size(18.dp))
    }
}

@Composable
private fun TopHeading(eyebrow: String, title: String, description: String) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 25.dp, vertical = 18.dp)) {
        Text(eyebrow, fontSize = 10.sp, letterSpacing = 2.4.sp, color = Amber,
            fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(9.dp))
        Text(title, style = MaterialTheme.typography.headlineLarge, color = WarmWhite)
        Spacer(Modifier.height(8.dp))
        Text(description, fontSize = 13.sp, color = SoftWhite, lineHeight = 19.sp)
    }
}

@Composable
fun DetailScreen(state: CinemaState, model: CinemaModel) {
    val movie = state.details ?: return
    val context = LocalContext.current
    val saved = state.saved.any { it.id == movie.id && it.type == movie.type }
    val seasons = movie.episodes.filter { it.season > 0 }.map { it.season }.distinct()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 48.dp)) {
        item {
            Spacer(Modifier.statusBarsPadding())
            Box(Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 9.dp)
                .height(416.dp).clip(RoundedCornerShape(31.dp))) {
                AsyncImage(model = movie.backdrop.ifBlank { movie.poster },
                    contentDescription = null, contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize())
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
                    listOf(Ink.copy(alpha = .36f), Color.Transparent,
                        Ink.copy(alpha = .48f), Ink))))
                Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(
                    listOf(Ink.copy(alpha = .4f), Color.Transparent))))
                Row(Modifier.align(Alignment.TopStart).fillMaxWidth()
                    .padding(horizontal = 15.dp, vertical = 15.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    CircleIcon(Icons.Rounded.ArrowBack, "Go back", model::closeDetails)
                    Spacer(Modifier.weight(1f))
                    Tag("UNI  /  ${movie.type.uppercase()}", WarmWhite, Ink.copy(alpha = .70f))
                }
                Column(Modifier.align(Alignment.BottomStart).padding(horizontal = 25.dp, vertical = 13.dp)) {
                    Text("NOW SHOWING", fontSize = 10.sp, letterSpacing = 2.6.sp,
                        fontWeight = FontWeight.Bold, color = Amber)
                    Spacer(Modifier.height(11.dp))
                    Text(movie.title, fontSize = 37.sp, lineHeight = 42.sp,
                        fontWeight = FontWeight.Black, letterSpacing = (-1.2).sp, color = WarmWhite,
                        maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        item {
            Column(Modifier.fillMaxWidth().padding(horizontal = 25.dp)) {
                Spacer(Modifier.height(13.dp))
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                    if (movie.rating.isNotBlank()) {
                        Icon(Icons.Rounded.Star, null, tint = Amber, modifier = Modifier.size(15.dp))
                        Text(movie.rating, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Amber)
                    }
                    if (movie.year.isNotBlank()) Text(movie.year, fontSize = 13.sp, color = SoftWhite)
                    if (movie.runtime.isNotBlank()) Text(movie.runtime, fontSize = 13.sp, color = SoftWhite)
                    Tag(if (movie.type == "series") "SERIES" else "MOVIE",
                        WarmWhite, Color.White.copy(alpha = .09f))
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    movie.genres.take(3).forEach {
                        Tag(it, SoftWhite, Color.White.copy(alpha = .07f), 10.dp)
                    }
                }
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = {
                        if (movie.type == "series") {
                            movie.episodes.firstOrNull { it.season == state.season }?.let(model::selectEpisode)
                        } else model.loadStreams(null)
                    }, modifier = Modifier.weight(1f).height(53.dp),
                        shape = RoundedCornerShape(17.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = WarmWhite, contentColor = Ink)) {
                        Icon(Icons.Rounded.PlayArrow, null)
                        Spacer(Modifier.width(7.dp))
                        Text(if (movie.type == "series") "Find episodes" else "Find streams",
                            fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    FilledTonalButton(onClick = { model.toggleSaved(movie) },
                        modifier = Modifier.height(53.dp),
                        shape = RoundedCornerShape(17.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (saved) Violet.copy(alpha = .29f) else Layer,
                            contentColor = if (saved) Violet else WarmWhite)) {
                        Icon(if (saved) Icons.Rounded.BookmarkAdded else Icons.Rounded.BookmarkAdd,
                            contentDescription = if (saved) "Remove from list" else "Save to list",
                            modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (saved) "Saved" else "My list", fontSize = 13.sp)
                    }
                }
                Spacer(Modifier.height(23.dp))
                Text("THE STORY", fontSize = 10.sp, letterSpacing = 2.4.sp,
                    fontWeight = FontWeight.Bold, color = Amber)
                Spacer(Modifier.height(10.dp))
                Text(movie.description.ifBlank { "Discover more about this story through your connected metadata add-ons." },
                    fontSize = 14.sp, lineHeight = 23.sp, color = SoftWhite)
            }
            Spacer(Modifier.height(29.dp))
        }
        if (movie.type == "series") {
            item {
                Column(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(horizontal = 25.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("Episodes", style = MaterialTheme.typography.headlineMedium,
                            color = WarmWhite)
                        Spacer(Modifier.weight(1f))
                        Text("${movie.episodes.size} total", fontSize = 12.sp, color = SoftWhite)
                    }
                    Spacer(Modifier.height(14.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 25.dp)) {
                        items(seasons) { season ->
                            val active = season == state.season
                            Box(Modifier.clip(RoundedCornerShape(13.dp))
                                .background(if (active) WarmWhite else Layer)
                                .clickable { model.selectSeason(season) }
                                .padding(horizontal = 17.dp, vertical = 11.dp)) {
                                Text("Season $season", fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                    color = if (active) Ink else SoftWhite)
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                }
            }
            val episodes = movie.episodes.filter { it.season == state.season }
            items(episodes, key = { it.id }) { episode ->
                EpisodeCard(episode, state.episode?.id == episode.id) { model.selectEpisode(episode) }
            }
            item { Spacer(Modifier.height(19.dp)) }
        }
        item {
            Column(Modifier.fillMaxWidth().padding(horizontal = 25.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Watch options", style = MaterialTheme.typography.headlineMedium, color = WarmWhite)
                    Spacer(Modifier.weight(1f))
                    if (state.streamsLoading) CircularProgressIndicator(
                        Modifier.size(17.dp), color = Amber, strokeWidth = 2.dp)
                    else Tag("${state.streams.size} found", Amber, Amber.copy(alpha = .12f))
                }
                Spacer(Modifier.height(9.dp))
                Text("Sources provided by the Stremio add-ons you connect.",
                    fontSize = 12.sp, lineHeight = 18.sp, color = SoftWhite)
                Spacer(Modifier.height(17.dp))
                if (state.detailLoading || state.streamsLoading) {
                    repeat(2) {
                        Skeleton(Modifier.fillMaxWidth().height(77.dp))
                        Spacer(Modifier.height(9.dp))
                    }
                } else if (movie.type == "series" && state.episode == null) {
                    SourceNotice("Select an episode above to view available streams.")
                } else if (state.streams.isEmpty()) {
                    SourceNotice("No streams were returned. Install a compatible Stremio stream add-on under Add-ons.")
                } else {
                    state.streams.forEach { stream ->
                        SourceCard(stream, onClick = {
                            if (stream.direct) model.play(stream)
                            else launchExternalStream(stream, context)
                        })
                        Spacer(Modifier.height(9.dp))
                    }
                }
            }
        }
        item {
            Spacer(Modifier.height(18.dp))
            Text("Only play content you are authorized to access.",
                Modifier.fillMaxWidth().padding(horizontal = 25.dp),
                fontSize = 11.sp, color = SoftWhite.copy(alpha = .65f))
        }
    }
}

@Composable
private fun EpisodeCard(episode: Episode, selected: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 25.dp, vertical = 5.dp)
        .clip(RoundedCornerShape(18.dp))
        .background(if (selected) Amber.copy(alpha = .1f) else Layer)
        .border(1.dp, if (selected) Amber.copy(alpha = .45f) else Hairline.copy(alpha = .5f),
            RoundedCornerShape(18.dp))
        .clickable(onClick = onClick).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(width = 96.dp, height = 68.dp)
            .clip(RoundedCornerShape(12.dp)).background(Violet.copy(alpha = .2f)),
            contentAlignment = Alignment.Center) {
            if (episode.thumbnail.isNotBlank()) AsyncImage(model = episode.thumbnail,
                contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize())
            Icon(Icons.Rounded.PlayCircle, null, tint = WarmWhite,
                modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text("EPISODE ${episode.number}", fontSize = 10.sp, fontWeight = FontWeight.Bold,
                color = if (selected) Amber else SoftWhite, letterSpacing = 1.2.sp)
            Spacer(Modifier.height(5.dp))
            Text(episode.title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                color = WarmWhite, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(5.dp))
        Icon(Icons.Rounded.ChevronRight, null, tint = if (selected) Amber else SoftWhite,
            modifier = Modifier.size(21.dp))
    }
}

@Composable
private fun SourceNotice(text: String) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp))
        .background(Layer).padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.Info, null, tint = Violet, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Text(text, fontSize = 12.sp, color = SoftWhite, lineHeight = 19.sp)
    }
}

@Composable
private fun SourceCard(stream: Stream, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp))
        .background(Layer).border(1.dp, Color.White.copy(alpha = .07f),
            RoundedCornerShape(17.dp))
        .clickable(onClick = onClick).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(41.dp).clip(RoundedCornerShape(13.dp))
            .background(Amber.copy(alpha = .12f)), contentAlignment = Alignment.Center) {
            Icon(if (stream.direct) Icons.Rounded.PlayArrow else Icons.Rounded.OpenInNew,
                null, tint = Amber, modifier = Modifier.size(21.dp))
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(stream.label, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                color = WarmWhite, maxLines = 1)
            Spacer(Modifier.height(3.dp))
            Text(stream.description.replace("\n", "  ·  ").take(115), fontSize = 11.sp,
                color = SoftWhite, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(3.dp))
            Text(stream.addonName, fontSize = 10.sp, color = Violet)
        }
        Icon(Icons.Rounded.ChevronRight, null, tint = SoftWhite, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun CircleIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String,
    onClick: () -> Unit) {
    Box(Modifier.size(43.dp).clip(CircleShape).background(Ink.copy(alpha = .73f))
        .clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, description, tint = WarmWhite, modifier = Modifier.size(22.dp))
    }
}

private fun launchExternalStream(stream: Stream, context: android.content.Context) {
    val url = when {
        stream.external.startsWith("http") -> stream.external
        stream.ytId.isNotBlank() -> "https://www.youtube.com/watch?v=${stream.ytId}"
        stream.infoHash.isNotBlank() -> "magnet:?xt=urn:btih:${stream.infoHash}"
        else -> stream.url
    }
    if (url.isBlank()) return
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: Exception) {
        Toast.makeText(context, "No compatible app is installed for this source.", Toast.LENGTH_LONG).show()
    }
}
