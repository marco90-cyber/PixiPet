package com.marco.pixipet

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val PanelBg = Color(0xFF241C3D)
private val CardBg = Color(0xFF4A3D73)
private val CardSel = Color(0xFF7A66B8)
private val CardRim = Color(0xFF6B5CA0)
private val Gold = Color(0xFFFFD84D)
private val Pink = Color(0xFFFF9EC4)

private fun shellColor(i: Int): Color = Color(0xFF000000L or SHELLS[i % SHELLS.size])

private fun tone(c: Color, t: Float, toward: Color): Color = Color(
    red = c.red + (toward.red - c.red) * t,
    green = c.green + (toward.green - c.green) * t,
    blue = c.blue + (toward.blue - c.blue) * t,
    alpha = 1f
)

@Composable
fun PixiPetApp(game: PixiGame) {
    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { game.frame(System.currentTimeMillis()) }
        }
    }

    BackHandler(enabled = game.canGoBack()) { game.back() }

    val body = shellColor(game.shellIdx)
    val light = tone(body, 0.5f, Color.White)
    val dark = tone(body, 0.3f, Color.Black)
    val shell = game.showShell

    val bg = if (shell) {
        Brush.verticalGradient(listOf(light, body))
    } else {
        Brush.verticalGradient(listOf(Color(0xFF1E1833), Color(0xFF14111F)))
    }

    // Il guscio occupa tutta la finestra (anche sotto le barre di sistema).
    Box(Modifier.fillMaxSize().background(bg)) {
        if (shell) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(5.dp)
                    .border(2.dp, dark.copy(alpha = 0.55f), RoundedCornerShape(36.dp))
            )
        }
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
        ) {
            val landscape = maxWidth > maxHeight * 1.05f
            if (landscape) {
                LandscapeLayout(game, maxWidth, maxHeight, shell, light, dark)
            } else {
                PortraitLayout(game, maxWidth, maxHeight, shell, light, dark)
            }
        }
    }

    if (game.showRename) {
        var txt by remember { mutableStateOf(game.pet.name) }
        AlertDialog(
            onDismissRequest = { game.showRename = false },
            confirmButton = {
                TextButton(onClick = {
                    game.rename(txt)
                    game.showRename = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { game.showRename = false }) { Text("ANNULLA") }
            },
            title = { Text("Rinomina") },
            text = {
                OutlinedTextField(
                    value = txt,
                    onValueChange = { txt = it.take(8) },
                    singleLine = true
                )
            }
        )
    }
}

/** Verticale: schermo di gioco in alto, menu touch scorrevole sotto, pulsanti in fondo. */
@Composable
private fun PortraitLayout(
    game: PixiGame, w: Dp, h: Dp, shell: Boolean, light: Color, dark: Color
) {
    val margin = if (shell) 16.dp else 8.dp
    val btn = (w * 0.12f).coerceIn(40.dp, 56.dp)
    val header = if (shell) 26.dp else 0.dp
    val availH = h - btn - header - 220.dp - 36.dp
    val side = minOf(w - margin * 2, availH).coerceAtLeast(140.dp)

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = margin, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (shell) Header(dark)
        ScreenView(game, side, shell)
        Spacer(Modifier.height(8.dp))
        MenuPanel(game, Modifier.weight(1f).fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(btn * 0.6f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ShellButton("◀", btn, light, dark, shell) { game.press(0) }
            ShellButton("●", btn, light, dark, shell) { game.press(1) }
            ShellButton("▶", btn, light, dark, shell) { game.press(2) }
        }
    }
}

/** Tablet o telefono girato: schermo a sinistra, menu e pulsanti a destra. */
@Composable
private fun LandscapeLayout(
    game: PixiGame, w: Dp, h: Dp, shell: Boolean, light: Color, dark: Color
) {
    val btn = (h * 0.13f).coerceIn(40.dp, 56.dp)
    val side = minOf(h - 16.dp, w * 0.5f).coerceAtLeast(140.dp)

    Row(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ScreenView(game, side, shell)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
            MenuPanel(game, Modifier.weight(1f).fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(btn * 0.6f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ShellButton("◀", btn, light, dark, shell) { game.press(0) }
                ShellButton("●", btn, light, dark, shell) { game.press(1) }
                ShellButton("▶", btn, light, dark, shell) { game.press(2) }
            }
        }
    }
}

@Composable
private fun Header(dark: Color) {
    Text(
        "PIXIPET",
        color = dark,
        fontSize = 12.sp,
        letterSpacing = 5.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

// ---------------------------------------------------------------- schermo

@Composable
private fun ScreenView(game: PixiGame, side: Dp, shell: Boolean) {
    val bezel = if (shell) 8.dp else 0.dp
    Box(
        Modifier
            .size(side)
            .background(Color(0xFF15131C), RoundedCornerShape(if (shell) 18.dp else 8.dp))
            .padding(bezel)
    ) {
        Canvas(
            Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(if (shell) 10.dp else 6.dp))
                .pointerInput(Unit) {
                    detectTapGestures { o ->
                        game.tapScreen(o.x / size.width, o.y / size.height)
                    }
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { change, _ ->
                        game.dragScreen(change.position.x / size.width)
                    }
                }
        ) {
            val img = game.image
            if (img != null) {
                drawImage(
                    img,
                    dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                    filterQuality = FilterQuality.None
                )
            }
        }
    }
}

// --------------------------------------------------------------- pannello

@Composable
private fun MenuPanel(game: PixiGame, modifier: Modifier) {
    val scr = game.scr
    val cursor = game.cursor
    val rev = game.uiRev
    val homeLike = scr == Scr.HOME || scr == Scr.MENU
    val items = remember(scr, rev) { game.items() }

    Column(
        modifier
            .background(PanelBg, RoundedCornerShape(18.dp))
            .padding(10.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (game.canGoBack()) {
                Text(
                    "‹ INDIETRO",
                    color = Pink,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { game.back() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
                Spacer(Modifier.width(6.dp))
            }
            Text(
                game.title(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp
            )
            Spacer(Modifier.weight(1f))
            Text(
                "${game.pet.coins} C",
                color = Gold,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp
            )
        }

        val content = Modifier.weight(1f).fillMaxWidth()
        when {
            scr == Scr.STATS -> StatsView(game, rev, content)
            scr == Scr.NAME -> NameKeyboard(game, content)
            items.isEmpty() -> Box(content, contentAlignment = Alignment.Center) {
                Text("...", color = Color(0xFF8A82A8), fontFamily = FontFamily.Monospace, fontSize = 18.sp)
            }
            else -> ItemsGrid(game, items, homeLike, cursor, scr, content)
        }
    }
}

@Composable
private fun ItemsGrid(
    game: PixiGame, items: List<Item>, homeLike: Boolean, cursor: Int, scr: Scr, modifier: Modifier
) {
    key(scr) {
        val state = rememberLazyGridState()
        LaunchedEffect(scr, cursor, items.size) {
            val target = cursor.coerceIn(0, (items.size - 1).coerceAtLeast(0))
            if (items.isNotEmpty() && state.layoutInfo.visibleItemsInfo.none { it.index == target }) {
                state.animateScrollToItem(target)
            }
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(if (homeLike) 3 else 1),
            state = state,
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            itemsIndexed(items) { i, item ->
                ItemCard(item, i == cursor, homeLike) { game.pick(i) }
            }
            if (scr == Scr.HOME || scr == Scr.MENU) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(Modifier.padding(top = 10.dp, bottom = 14.dp)) { StatsBlock(game) }
                }
            }
        }
    }
}

@Composable
private fun ItemCard(item: Item, selected: Boolean, tile: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    val mod = Modifier
        .fillMaxWidth()
        .alpha(if (item.enabled) 1f else 0.4f)
        .background(if (selected) CardSel else CardBg, shape)
        .border(if (selected) 2.dp else 1.dp, if (selected) Gold else CardRim, shape)
        .clip(shape)
        .clickable { onClick() }

    if (tile) {
        Column(mod.padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            PixelIcon(item.icon, item.food, 34.dp)
            Spacer(Modifier.height(4.dp))
            Text(
                item.label,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
        }
    } else {
        Row(
            mod.heightIn(min = 48.dp).padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (item.icon >= 0 || item.food >= 0) {
                PixelIcon(item.icon, item.food, 28.dp)
                Spacer(Modifier.width(10.dp))
            }
            Text(
                item.label,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f)
            )
            if (item.sub.isNotEmpty()) {
                Text(
                    item.sub,
                    color = Pink,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun EggChoice(i: Int, selected: Boolean, onClick: () -> Unit) {
    val img = remember(i) {
        val s = makePet(Stage.EGG, 0, i, M_NORMAL, false, false, 0, false)
        Bitmap.createBitmap(s.px, 44, 46, Bitmap.Config.ARGB_8888).asImageBitmap()
    }
    val shape = RoundedCornerShape(10.dp)
    Canvas(
        Modifier
            .size(width = 50.dp, height = 52.dp)
            .clip(shape)
            .background(if (selected) Color(0x33FFB3D1) else Color.Transparent)
            .border(if (selected) 2.dp else 1.dp, if (selected) Gold else Color(0x33888888), shape)
            .clickable { onClick() }
    ) {
        drawImage(
            img,
            dstSize = IntSize(size.width.toInt(), size.height.toInt()),
            filterQuality = FilterQuality.None
        )
    }
}

/** Le icone sono le stesse disegnate a pixel per lo schermo di gioco. */
@Composable
private fun PixelIcon(id: Int, food: Int, dp: Dp) {
    if (id < 0 && food < 0) return
    val img = remember(id, food) {
        val f = Fb(12, 12)
        if (food >= 0) drawFood(f, food, 1, 1) else drawIcon(f, id, 1, 1)
        Bitmap.createBitmap(f.px, 12, 12, Bitmap.Config.ARGB_8888).asImageBitmap()
    }
    Canvas(Modifier.size(dp)) {
        drawImage(
            img,
            dstSize = IntSize(size.width.toInt(), size.height.toInt()),
            filterQuality = FilterQuality.None
        )
    }
}

// ----------------------------------------------------------- statistiche

@Composable
private fun StatsView(game: PixiGame, @Suppress("UNUSED_PARAMETER") rev: Int, modifier: Modifier) {
    Column(modifier.verticalScroll(rememberScrollState())) { StatsBlock(game) }
}

@Composable
private fun StatsBlock(game: PixiGame) {
    val pet = game.pet
    val now = System.currentTimeMillis()
    val trait = if (pet.immortal) "IMMORTALE" else if (pet.angel) "ANGELO" else "-"
    Column {
        StatBar("FAME", pet.hunger)
        StatBar("PULIZIA", pet.hygiene)
        StatBar("FELICITÀ", pet.happy)
        StatBar("ENERGIA", pet.energy)
        StatBar("MENTE", pet.smarts)
        StatBar("FORMA", pet.active)
        Spacer(Modifier.height(10.dp))
        InfoRow("NOME", pet.name)
        InfoRow("FASE", pet.stage(now).label)
        InfoRow("ETÀ", ageText(pet.ageMin(now)))
        InfoRow("CARATTERE", if (pet.pers >= 0) PERS[pet.pers] else "?")
        InfoRow("PESO", "${pet.weight}")
        InfoRow("MONETE", "${pet.coins}")
        InfoRow("CARAMELLE", "${pet.candy}")
        InfoRow("APPUNTAMENTI", "${pet.dates}")
        InfoRow("PARTITE", "${pet.games}")
        InfoRow("GENERAZIONE", "${pet.generation}")
        InfoRow("SPECIALE", trait)
    }
}

@Composable
private fun StatBar(label: String, value: Float) {
    val frac = (value / 100f).coerceIn(0f, 1f)
    val c = when {
        value > 50f -> Color(0xFF6FCF6F)
        value > 25f -> Color(0xFFFFD84D)
        else -> Color(0xFFE23B3B)
    }
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            modifier = Modifier.width(90.dp)
        )
        Box(
            Modifier
                .weight(1f)
                .height(14.dp)
                .background(Color(0xFF1B1530), RoundedCornerShape(7.dp))
                .border(1.dp, Color(0xFFB6A8E0), RoundedCornerShape(7.dp))
                .padding(2.dp)
        ) {
            if (frac > 0.02f) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(frac)
                        .background(c, RoundedCornerShape(5.dp))
                )
            }
        }
        Text(
            "${value.toInt()}",
            color = Color(0xFFB6A8E0),
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            modifier = Modifier.width(30.dp).padding(start = 6.dp)
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(
            label,
            color = Pink,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            modifier = Modifier.weight(1f)
        )
        Text(
            value,
            color = Color.White,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp
        )
    }
}

// ----------------------------------------------------------------- pulsanti

@Composable
private fun ShellButton(
    label: String, size: Dp, light: Color, dark: Color, shell: Boolean, onClick: () -> Unit
) {
    val face = if (shell) light else Color(0xFF3A3358)
    val rim = if (shell) dark else Color(0xFF5B4B8A)
    Box(
        Modifier
            .size(size)
            .background(face, CircleShape)
            .border(3.dp, rim, CircleShape)
            .clip(CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (shell) dark else Color.White,
            fontSize = (size.value * 0.36f).sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun NameKeyboard(game: PixiGame, modifier: Modifier) {
    val rows = listOf("ABCDEFGHI", "JKLMNOPQR", "STUVWXYZ0", "123456789")
    Column(modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for (r in rows) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                for (c in r) KeyCap(c.toString(), Modifier.weight(1f)) { game.nameKey(c) }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            KeyCap("CANCELLA", Modifier.weight(1f)) { game.nameDel() }
            KeyCap("OK", Modifier.weight(1f), true) { game.nameOk() }
        }
    }
}

@Composable
private fun KeyCap(label: String, modifier: Modifier, accent: Boolean = false, onClick: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier
            .heightIn(min = 40.dp)
            .background(if (accent) CardSel else CardBg, shape)
            .border(1.dp, if (accent) Gold else CardRim, shape)
            .clip(shape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp
        )
    }
}
