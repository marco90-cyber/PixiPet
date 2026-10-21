package com.marco.pixipet

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Lcd = Color(0xFFB3B8A7)
private val Ink = Color(0xFF1B1E19)
private val IconOff = Color(0xFF737869)
private val Frame = Color(0xFF8E9484)
private val Shell = Color(0xFF000000)
private val Key = Color(0xFF2B2B2B)

private const val COLS = 32
private const val ROWS = 28

// ------------------------------------------------------------------ schermata

@Composable
fun PixiPetApp(game: PixiGame) {
    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { game.update(System.currentTimeMillis()) }
        }
    }

    val rev = game.rev
    val pet = game.pet
    val statusPad = Modifier.windowInsetsPadding(WindowInsets.statusBars)
    val navPad = Modifier.windowInsetsPadding(WindowInsets.navigationBars)

    Column(Modifier.fillMaxSize().background(Shell)) {
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Lcd)
                .then(statusPad)
        ) {
            // barre di stato
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 6.dp)
                    .border(1.dp, Ink.copy(alpha = 0.6f))
            ) {
                StatCell("HUNGER", pet.hunger, Modifier.weight(1f), rev)
                StatCell("HYGIENE", pet.hygiene, Modifier.weight(1f), rev)
                StatCell("SMARTS", pet.smarts, Modifier.weight(1f), rev)
                StatCell("ACTIVE", pet.active, Modifier.weight(1f), rev)
                StatCell("ENERGY", pet.energy, Modifier.weight(1f), rev)
                StatCell("HAPPY", pet.happy, Modifier.weight(1f), rev)
            }

            // schermo LCD
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .border(1.dp, Frame, RoundedCornerShape(10.dp))
                    .padding(6.dp)
            ) {
                LcdScreen(game, Modifier.fillMaxSize())
            }

            // icone del menu
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until 7) {
                    Box(
                        Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clickable { game.pickIcon(i) },
                        contentAlignment = Alignment.Center
                    ) {
                        MenuIcon(i, i == game.sel && game.mode == Mode.HOME)
                    }
                }
            }
        }

        // pulsanti
        Row(
            Modifier
                .fillMaxWidth()
                .background(Shell)
                .then(navPad)
                .padding(horizontal = 50.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            KeyButton("◀", Modifier.width(64.dp)) { game.onLeft() }
            Box(Modifier.width(14.dp))
            KeyButton("SELECT", Modifier.weight(1f)) { game.onSelect() }
            Box(Modifier.width(14.dp))
            KeyButton("▶", Modifier.width(64.dp)) { game.onRight() }
        }
    }
}

@Composable
private fun StatCell(label: String, value: Float, modifier: Modifier, @Suppress("UNUSED_PARAMETER") rev: Int) {
    Column(
        modifier
            .padding(horizontal = 1.dp, vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            label,
            color = Ink,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
        Box(
            Modifier
                .padding(top = 3.dp, start = 4.dp, end = 4.dp)
                .fillMaxWidth()
                .height(7.dp)
                .border(1.dp, Ink, RoundedCornerShape(4.dp))
                .padding(1.5.dp)
        ) {
            val frac = (value / 100f).coerceIn(0f, 1f)
            if (frac > 0.01f) {
                Box(
                    Modifier
                        .fillMaxWidth(frac)
                        .fillMaxSize()
                        .background(Ink, RoundedCornerShape(3.dp))
                )
            }
        }
    }
}

@Composable
private fun KeyButton(text: String, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .height(44.dp)
            .background(Key, RoundedCornerShape(10.dp))
            .border(1.dp, Color(0xFF3F3F3F), RoundedCornerShape(10.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

// --------------------------------------------------------------------- icone

@Composable
private fun MenuIcon(index: Int, selected: Boolean) {
    val color = if (selected) Ink else IconOff
    Canvas(Modifier.size(30.dp)) {
        val u = size.width / 24f
        fun line(x1: Float, y1: Float, x2: Float, y2: Float, w: Float) =
            drawLine(color, Offset(x1 * u, y1 * u), Offset(x2 * u, y2 * u), strokeWidth = w * u, cap = StrokeCap.Round)

        when (index) {
            0 -> { // forchetta e coltello
                line(5f, 3f, 5f, 9f, 1.6f)
                line(8f, 3f, 8f, 9f, 1.6f)
                line(11f, 3f, 11f, 9f, 1.6f)
                line(5f, 9f, 11f, 9f, 1.8f)
                line(8f, 9f, 8f, 21f, 2.2f)
                drawRoundRect(color, Offset(15.2f * u, 3f * u), Size(3.2f * u, 10f * u), CornerRadius(1.5f * u))
                line(16.8f, 12f, 16.8f, 21f, 2.2f)
            }
            1 -> { // bimbo
                drawCircle(color, 5f * u, Offset(12f * u, 7.5f * u))
                drawRoundRect(color, Offset(6.5f * u, 13.5f * u), Size(11f * u, 8f * u), CornerRadius(3f * u))
            }
            2 -> { // libro
                drawRoundRect(color, Offset(5f * u, 3f * u), Size(14f * u, 18f * u), CornerRadius(1.5f * u))
                for (y in listOf(8f, 12f, 16f)) {
                    drawLine(Lcd, Offset(8f * u, y * u), Offset(16f * u, y * u), strokeWidth = 1.2f * u)
                }
            }
            3 -> { // mazza
                line(6f, 20f, 17f, 5f, 3.6f)
                drawCircle(color, 2.6f * u, Offset(19f * u, 18.5f * u))
            }
            4 -> { // lampadina
                drawCircle(color, 6f * u, Offset(12f * u, 9f * u))
                drawRoundRect(color, Offset(9.5f * u, 15f * u), Size(5f * u, 3.5f * u), CornerRadius(1f * u))
                line(10.5f, 20.5f, 13.5f, 20.5f, 1.6f)
            }
            5 -> { // kit medico
                drawRoundRect(color, Offset(3f * u, 7f * u), Size(18f * u, 13f * u), CornerRadius(2.5f * u))
                drawRoundRect(
                    color, Offset(8.5f * u, 3.5f * u), Size(7f * u, 4f * u), CornerRadius(1f * u),
                    style = Stroke(width = 1.5f * u)
                )
                drawLine(Lcd, Offset(7f * u, 13.5f * u), Offset(17f * u, 13.5f * u), strokeWidth = 2.6f * u)
                drawLine(Lcd, Offset(12f * u, 9f * u), Offset(12f * u, 18f * u), strokeWidth = 2.6f * u)
            }
            else -> { // info
                drawCircle(color, 9f * u, Offset(12f * u, 12f * u))
                drawCircle(Lcd, 1.5f * u, Offset(12f * u, 7.8f * u))
                drawLine(Lcd, Offset(12f * u, 11f * u), Offset(12f * u, 17.5f * u), strokeWidth = 2.6f * u)
            }
        }
    }
}

// ----------------------------------------------------------------- LCD pixel

@Composable
private fun LcdScreen(game: PixiGame, modifier: Modifier) {
    Canvas(modifier) {
        @Suppress("UNUSED_VARIABLE")
        val tick = game.frame
        val cell = minOf(size.width / COLS, size.height / ROWS)
        val ox = (size.width - cell * COLS) / 2f
        val oy = (size.height - cell * ROWS) / 2f
        translate(ox, oy) {
            drawScreen(game, System.currentTimeMillis(), cell)
        }
    }
}

private fun DrawScope.px(c: Int, r: Int, cell: Float, color: Color) {
    if (c < 0 || c >= COLS || r < 0 || r >= ROWS) return
    drawRect(color, Offset(c * cell, r * cell), Size(cell * 0.9f, cell * 0.9f))
}

private fun DrawScope.sprite(s: List<String>, x: Int, y: Int, cell: Float, color: Color = Ink) {
    for (r in s.indices) {
        val row = s[r]
        for (c in row.indices) {
            if (row[c] == '#') px(x + c, y + r, cell, color)
        }
    }
}

private fun textW(t: String, scale: Int = 1) = (t.length * 4 - 1) * scale

private fun DrawScope.text(t: String, x: Int, y: Int, cell: Float, scale: Int = 1, color: Color = Ink) {
    var cx = x
    for (ch in t) {
        val g = Sprites.glyph(ch)
        for (r in 0 until 5) {
            for (c in 0 until 3) {
                if (g[r][c] == '#') {
                    for (dy in 0 until scale) for (dx in 0 until scale) {
                        px(cx + c * scale + dx, y + r * scale + dy, cell, color)
                    }
                }
            }
        }
        cx += 4 * scale
    }
}

private fun DrawScope.textC(t: String, y: Int, cell: Float, scale: Int = 1) {
    text(t, (COLS - textW(t, scale)) / 2, y, cell, scale)
}

private fun petSprite(stage: Stage): List<String> = when (stage) {
    Stage.EGG -> Sprites.egg
    Stage.BABY -> Sprites.baby
    Stage.CHILD -> Sprites.child
    Stage.TEEN -> Sprites.teen
    Stage.ADULT -> Sprites.adult
    Stage.ELDER -> Sprites.elder
    Stage.DEAD -> Sprites.ghost
}

private fun ageText(min: Float): String {
    val m = min.toInt()
    return when {
        m < 60 -> "${m}M"
        m < 1440 -> "${m / 60}H${m % 60}"
        else -> "${m / 1440}D${(m % 1440) / 60}H"
    }
}

private fun DrawScope.drawScreen(g: PixiGame, now: Long, cell: Float) {
    @Suppress("UNUSED_VARIABLE")
    val r = g.rev
    val pet = g.pet

    // pixel "spenti" molto tenui, come su un vero LCD
    for (rr in 0 until ROWS) for (cc in 0 until COLS) {
        drawRect(Ink.copy(alpha = 0.045f), Offset(cc * cell, rr * cell), Size(cell * 0.9f, cell * 0.9f))
    }

    when (g.mode) {
        Mode.HOME -> drawHome(g, now, cell)
        Mode.FEED -> drawFeed(g, cell)
        Mode.PLAY -> drawPlay(g, now, cell)
        Mode.SMART -> drawSmart(g, cell)
        Mode.INFO -> drawInfo(g, now, cell)
    }

    if (pet.sleeping && g.mode == Mode.HOME) {
        drawRect(Color(0x55000000), Offset(0f, 0f), Size(cell * COLS, cell * ROWS))
    }
}

private fun DrawScope.drawHome(g: PixiGame, now: Long, cell: Float) {
    val pet = g.pet
    val stage = pet.stage(now)
    val bob = if ((now / 450L) % 2L == 0L) 0 else 1
    val blink = (now / 500L) % 2L == 0L

    // icone di stato
    if (g.toast.isNotEmpty()) {
        textC(g.toast, 1, cell)
    } else if (pet.dead) {
        textC("BYE", 1, cell)
    } else {
        if (pet.sleeping) sprite(Sprites.zzz, 1, 1, cell)
        if (pet.sick) sprite(Sprites.cross, 23, 1, cell)
        if (pet.calling && !pet.sleeping && blink) sprite(Sprites.heart, 12, 1, cell)
    }

    // cacchine
    val spots = listOf(0 to 18, 24 to 18, 8 to 20, 16 to 20)
    for (i in 0 until pet.poops.coerceAtMost(4)) {
        sprite(Sprites.poop, spots[i].first, spots[i].second, cell)
    }

    val body = petSprite(stage)
    val shown = if (g.facingLeft) Sprites.flip(body) else body

    when {
        pet.dead -> {
            sprite(body, 8, 3 + bob, cell)
            textC(pet.cause, 18, cell)
            if (blink) textC("SEL:NEW", 23, cell)
        }
        stage == Stage.EGG -> {
            val dx = if ((now / 400L) % 2L == 0L) 0 else 1
            sprite(Sprites.egg, 8 + dx, 9, cell)
        }
        g.anim == Anim.MEAL || g.anim == Anim.SNACK -> {
            val t = ((now - g.animT0).toFloat() / Config.ANIM_MS).coerceIn(0f, 1f)
            val food = if (g.anim == Anim.MEAL) Sprites.meal else Sprites.snack
            val left = (8 * (1f - t)).toInt().coerceIn(0, 8)
            sprite(food.take(left.coerceAtLeast(1)), 5, 14 + (8 - left.coerceAtLeast(1)) / 2, cell)
            sprite(Sprites.flip(body), 12, 9 + bob, cell)
        }
        g.anim == Anim.CLEAN -> {
            val t = (now - g.animT0).toFloat() / Config.ANIM_MS
            sprite(body, 8, 9 + bob, cell)
            for (i in 0 until 3) {
                val yy = 22 - (((t * 20f) + i * 6f) % 20f).toInt()
                val xx = 4 + i * 10 + (if ((now / 300L + i) % 2L == 0L) 0 else 1)
                sprite(Sprites.ring, xx, yy, cell)
            }
        }
        g.anim == Anim.MED -> {
            sprite(body, 8 + (if (blink) 0 else 1), 9, cell)
            if (blink) sprite(Sprites.cross, 4, 12, cell)
        }
        else -> {
            val y = if (pet.sleeping) 10 else 9 + bob
            sprite(shown, g.petX, y, cell)
        }
    }
}

private fun DrawScope.drawFeed(g: PixiGame, cell: Float) {
    textC("FEED", 1, cell)
    sprite(Sprites.meal, 3, 10, cell)
    sprite(Sprites.snack, 12, 10, cell)
    text("X", 23, 11, cell)
    val ux = when (g.sub) {
        0 -> 3
        1 -> 12
        else -> 22
    }
    val uw = if (g.sub == 2) 5 else 8
    for (i in 0 until uw) px(ux + i, 20, cell, Ink)
    val label = when (g.sub) {
        0 -> "MEAL"
        1 -> "SNACK"
        else -> "BACK"
    }
    textC(label, 23, cell)
}

private fun DrawScope.drawPlay(g: PixiGame, now: Long, cell: Float) {
    val pet = g.pet
    val stage = pet.stage(now)
    val body = petSprite(if (stage == Stage.EGG) Stage.BABY else stage)

    textC("R${g.pRound} H${g.pHits}", 1, cell)
    sprite(body, 0, 9, cell)

    // zona di battuta
    for (x in 13..20) px(x, 25, cell, Ink)

    // mazza
    val swinging = now < g.swingUntil
    if (swinging) {
        for (i in 0 until 5) px(15 + i, 17, cell, Ink)
    } else {
        for (i in 0 until 6) px(16, 12 + i, cell, Ink)
    }

    // pallina
    when (g.pPhase) {
        0 -> {
            val bx = g.ballX.toInt()
            for (dx in 0..1) for (dy in 0..1) px(bx + dx, 16 + dy, cell, Ink)
        }
        1 -> {
            val t = ((now - g.pPhaseT0) / 1000f)
            val bx = 20 + (t * 22f).toInt()
            val by = 16 - (t * 18f).toInt()
            for (dx in 0..1) for (dy in 0..1) px(bx + dx, by + dy, cell, Ink)
            textC("HIT", 22, cell)
        }
        2 -> textC("MISS", 22, cell)
        else -> textC(if (g.pHits >= 3) "WIN" else "LOSE", 12, cell, 2)
    }
}

private fun DrawScope.drawSmart(g: PixiGame, cell: Float) {
    when (g.sPhase) {
        0 -> {
            textC("ROUND ${g.sRound}", 1, cell)
            val n = g.sNum.toString()
            text(n, (COLS - textW(n, 3)) / 2, 7, cell, 3)
            text("<LO", 1, 23, cell)
            text("HI>", COLS - 1 - textW("HI>"), 23, cell)
        }
        1, 2 -> {
            textC(if (g.sPhase == 1) "OK" else "NO", 1, cell)
            val n = g.sNext.toString()
            text(n, (COLS - textW(n, 3)) / 2, 7, cell, 3)
        }
        else -> {
            textC("SCORE", 6, cell)
            val s = "${g.sCorrect}/5"
            text(s, (COLS - textW(s, 3)) / 2, 13, cell, 3)
        }
    }
}

private fun DrawScope.drawInfo(g: PixiGame, now: Long, cell: Float) {
    val pet = g.pet
    when (g.sub) {
        0 -> {
            textC(pet.stage(now).label, 4, cell)
            textC("AGE ${ageText(pet.ageMin(now))}", 12, cell)
            textC("GEN ${pet.generation}", 20, cell)
        }
        1 -> {
            textC("WT ${pet.weight}", 4, cell)
            textC("CARE ${pet.careMistakes}", 12, cell)
            textC(if (pet.sick) "SICK" else "HEALTHY", 20, cell)
        }
        else -> {
            textC("PIXIPET", 4, cell)
            textC("V1.0", 12, cell)
            textC("<  >", 20, cell)
        }
    }
    for (i in 0 until 3) {
        val x = 13 + i * 3
        if (i == g.sub) {
            px(x, 26, cell, Ink)
            px(x + 1, 26, cell, Ink)
        } else {
            px(x, 26, cell, Ink.copy(alpha = 0.35f))
        }
    }
}
