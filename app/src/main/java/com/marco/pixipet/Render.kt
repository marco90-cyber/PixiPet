package com.marco.pixipet

import androidx.compose.ui.graphics.asImageBitmap
import kotlin.math.abs
import kotlin.math.sin

private val PANEL = col(0x2A2340)
private val PANEL2 = col(0x5B4B8A)
private val PINK = col(0xFF9EC4)
private val GRAY = col(0x8A82A8)

fun PixiGame.render(now: Long) {
    val f = fb
    hits.clear()
    f.clear(PANEL)
    when (scr) {
        Scr.HOME, Scr.MENU, Scr.FOOD, Scr.PLAYLIST, Scr.MAP, Scr.SETTINGS, Scr.DEBUG, Scr.STATS, Scr.BG -> {
            val theme = if (scr == Scr.BG && cursor in ROOM_NAMES.indices) cursor else pet.bg
            drawRoom(f, now, isNight() || pet.sleeping, theme)
            drawHomeScene(now)
            drawHud(now)
            if (pet.sleeping) {
                f.shade(0.5f, col(0x0B0B2A))
                drawWindowSky(f, true, now)
                drawIcon(f, 11, petX.toInt() + 8, 14 + ((now / 500L) % 2L).toInt() * 2)
            }
        }
        Scr.PARK, Scr.SCHOOL, Scr.STAND, Scr.UNDER -> drawLocation(now)
        Scr.ANIM -> drawAnim(now)
        Scr.G_CATCH -> drawCatch(now)
        Scr.G_HILO -> drawHilo(now)
        Scr.G_SWING -> drawSwing(now)
        Scr.G_END -> drawEnd(now)
        Scr.DEAD -> drawDead(now)
    }
    if (toast.isNotEmpty()) {
        val w2 = f.tw(toast) + 8
        val x = (W - w2) / 2
        f.rect(x, 12, w2, 9, PANEL)
        f.frame(x, 12, w2, 9, PINK)
        f.textC(toast, 14, WHITE)
    }
    game.frameBitmap.setPixels(f.px, 0, W, 0, 0, W, H)
    image = game.frameBitmap.asImageBitmap()
}

// ------------------------------------------------------------------ comuni

fun ageText(min: Float): String {
    val m = min.toInt()
    return when {
        m < 60 -> "${m}M"
        m < 1440 -> "${m / 60}H${m % 60}M"
        else -> "${m / 1440}D${(m % 1440) / 60}H"
    }
}

private fun groundY(s: Scr): Int = when (s) {
    Scr.PARK -> 66
    Scr.SCHOOL -> 68
    Scr.STAND -> 45
    Scr.UNDER -> 66
    else -> 66
}

private fun PixiGame.drawScene(s: Scr, now: Long) {
    when (s) {
        Scr.PARK -> drawPark(fb, now)
        Scr.SCHOOL -> drawSchool(fb, now)
        Scr.STAND -> drawStand(fb, now)
        Scr.UNDER -> drawUnder(fb, now)
        else -> drawRoom(fb, now, isNight(), pet.bg)
    }
}

private fun PixiGame.moodNow(): Int = when {
    pet.dead -> M_SICK
    pet.sleeping -> M_SLEEP
    pet.sick -> M_SICK
    pet.hunger < 25f || pet.hygiene < 20f || pet.happy < 25f -> M_SAD
    pet.happy > 75f -> M_HAPPY
    else -> M_NORMAL
}

private fun PixiGame.drawPetActor(now: Long, x: Int, y: Int, mood: Int, flip: Boolean) {
    val st = pet.stage(now)
    val bob = (now / 450L) % 2L == 0L && !pet.sleeping
    val blink = now % 3300L < 150L
    val sp = if (pet.pers >= 0) pet.pers else 0
    val s = makePet(st, sp, pet.hue, mood, blink, bob, if (pet.angel) 1 else 0, pet.dead)
    val wob = if (st == Stage.EGG) ((now / 350L) % 3L).toInt() - 1 else 0
    fb.shadeEllipse(x, y + 1, 11, 2, 0.28f)
    fb.blit(s, x - 22 + wob, y - 43, flip)
}

private fun PixiGame.drawNpc(now: Long, kind: Int, x: Int, y: Int, flip: Boolean, seed: Int = 0) {
    val bob = (now / 450L) % 2L == 0L
    val s = when (kind) {
        1 -> makePet(Stage.ADULT, 0, 0, M_HAPPY, false, bob, 1, false)
        2 -> makePet(Stage.ADULT, 4, 0, M_HAPPY, false, bob, 2, false, col(0xE04A5F))
        else -> makePet(Stage.ADULT, seed % 5, seed % 5, M_HAPPY, false, bob, 0, false)
    }
    fb.shadeEllipse(x, y + 1, 11, 2, 0.28f)
    fb.blit(s, x - 22, y - 43, flip)
}

private fun PixiGame.drawTopBar(title: String) {
    val f = fb
    f.rect(0, 0, W, 11, PINK)
    f.text(title, 3, 3, PANEL)
    f.textR("${pet.coins}", W - 10, 3, PANEL)
    drawCoin(f, W - 5, 5)
}

// -------------------------------------------------------------------- liste

private fun PixiGame.drawListScreen(title: String) {
    val f = fb
    f.grad(0, H, col(0x3A2F5B), col(0x241C3D))
    drawTopBar(title)
    val its = items()
    if (its.isEmpty()) return
    cursor = cursor.coerceIn(0, its.size - 1)
    val rows = 6
    val first = (cursor - rows + 1).coerceAtLeast(0)
    for (r in 0 until rows) {
        val i = first + r
        if (i >= its.size) break
        val item = its[i]
        val y = 13 + r * 11
        if (i == cursor) {
            f.rect(1, y, W - 2, 10, PANEL2)
            f.frame(1, y, W - 2, 10, YELLOW)
        }
        var tx = 6
        if (item.food >= 0) {
            drawFood(f, item.food, 3, y)
            tx = 16
        } else if (item.icon >= 0) {
            drawIcon(f, item.icon, 3, y)
            tx = 16
        }
        val c = if (item.enabled) WHITE else GRAY
        f.text(item.label, tx, y + 3, c)
        if (item.sub.isNotEmpty()) f.textR(item.sub, W - 4, y + 3, if (item.enabled) PINK else GRAY)
        hits.add(Hit(0, y, W, 10, i))
    }
    if (its.size > rows) {
        if (first > 0) f.text("+", W - 5, 12, YELLOW)
        if (first + rows < its.size) f.text("+", W - 5, 76, YELLOW)
    }
}

private fun PixiGame.drawPanelList(y0: Int, rows: Int) {
    val f = fb
    val its = items()
    f.rect(0, y0, W, H - y0, PANEL)
    f.rect(0, y0, W, 1, PINK)
    if (its.isEmpty()) return
    cursor = cursor.coerceIn(0, its.size - 1)
    val first = (cursor - rows + 1).coerceAtLeast(0)
    for (r in 0 until rows) {
        val i = first + r
        if (i >= its.size) break
        val item = its[i]
        val y = y0 + 2 + r * 8
        if (i == cursor) f.rect(1, y - 1, W - 2, 8, PANEL2)
        val c = if (item.enabled) WHITE else GRAY
        if (i == cursor) f.text(">", 2, y, YELLOW)
        f.text(item.label, 8, y, c)
        if (item.sub.isNotEmpty()) f.textR(item.sub, W - 3, y, if (item.enabled) PINK else GRAY)
        hits.add(Hit(0, y - 1, W, 8, i))
    }
}

// --------------------------------------------------------------------- menu

private fun PixiGame.drawMenu() {
    val f = fb
    f.grad(0, H, col(0x3A2F5B), col(0x241C3D))
    drawTopBar(pet.name)
    val its = items()
    cursor = cursor.coerceIn(0, its.size - 1)
    for (i in its.indices) {
        val cx = 2 + (i % 4) * 19
        val cy = 14 + (i / 4) * 19
        val sel = i == cursor
        f.rect(cx, cy, 17, 17, if (sel) col(0xFFE9A8) else col(0x4A3D73))
        f.frame(cx, cy, 17, 17, if (sel) YELLOW else col(0x6B5CA0))
        drawIcon(f, its[i].icon, cx + 3, cy + 3)
        if (!its[i].enabled) {
            for (yy in cy until cy + 17) for (xx in cx until cx + 17) {
                f.set(xx, yy, mix(f.get(xx, yy), PANEL, 0.55f))
            }
        }
        hits.add(Hit(cx, cy, 17, 17, i))
    }
    f.textC(its[cursor].label, 72, WHITE)
}

// ------------------------------------------------------------------ statistiche

private fun PixiGame.drawStats(now: Long) {
    val f = fb
    f.grad(0, H, col(0x3A2F5B), col(0x241C3D))
    drawTopBar(if (statsPage == 0) "STATS 1/2" else "STATS 2/2")
    if (statsPage == 0) {
        val names = arrayOf("HUNGER", "HYGIENE", "HAPPY", "ENERGY", "SMARTS", "ACTIVE")
        val vals = floatArrayOf(pet.hunger, pet.hygiene, pet.happy, pet.energy, pet.smarts, pet.active)
        for (i in 0 until 6) {
            val y = 15 + i * 10
            f.text(names[i], 3, y, WHITE)
            f.rect(34, y - 1, 42, 7, col(0x1B1530))
            f.frame(34, y - 1, 42, 7, col(0xB6A8E0))
            val w2 = (vals[i] / 100f * 38f).toInt().coerceIn(0, 38)
            val c = when {
                vals[i] > 50f -> col(0x6FCF6F)
                vals[i] > 25f -> col(0xFFD84D)
                else -> col(0xE23B3B)
            }
            f.rect(36, y + 1, w2, 3, c)
        }
        f.textC("< > MORE   OK BACK", 76, GRAY)
    } else {
        val st = pet.stage(now)
        val trait = if (pet.immortal) "IMMORTAL" else if (pet.angel) "ANGEL" else "-"
        val rows = listOf(
            "NAME" to pet.name,
            "STAGE" to st.label,
            "AGE" to ageText(pet.ageMin(now)),
            "TYPE" to (if (pet.pers >= 0) PERS[pet.pers] else "?"),
            "WEIGHT" to "${pet.weight}",
            "COINS" to "${pet.coins}",
            "CANDY" to "${pet.candy}",
            "DATES" to "${pet.dates}",
            "GAMES" to "${pet.games}",
            "TRAIT" to trait
        )
        for (i in rows.indices) {
            val y = 14 + i * 6
            f.text(rows[i].first, 3, y, PINK)
            f.textR(rows[i].second, W - 3, y, WHITE)
        }
    }
}

// ------------------------------------------------------------------- casa

private fun PixiGame.drawHomeScene(now: Long) {
    val f = fb
    val st = pet.stage(now)
    val spots = listOf(12 to 60, 62 to 62, 24 to 70, 50 to 72)
    for (i in 0 until pet.poops.coerceAtMost(4)) drawPoop(f, spots[i].first, spots[i].second)

    if (away()) {
        f.rect(6, 20, 68, 32, col(0x9B6A3C))
        f.rect(8, 22, 64, 28, col(0xE9D3A4))
        val what = when (pet.awayKind) {
            1 -> "DAI GENITORI"
            2 -> "IN VACANZA"
            else -> "AL LAVORO"
        }
        f.textC(what, 26, col(0x6B4423))
        val m = ((pet.awayUntil - now) / 60000L).coerceAtLeast(0L)
        val left = if (m >= 60L) "${m / 60}H${m % 60}M" else "${m}M"
        f.textC("TORNA TRA", 34, col(0x6B4423))
        f.textC(left, 41, col(0x8A3B2B))
        return
    }

    val px = petX.toInt()
    drawPetActor(now, px, 66, moodNow(), facingLeft)

    if (pet.hygiene < 25f && !pet.sleeping && st != Stage.EGG) {
        for (i in 0 until 3) {
            val x0 = px - 10 + i * 9
            for (k in 0 until 9) {
                val xx = x0 + (if (((now / 200L) + k) % 4L < 2L) 0 else 1)
                f.set(xx, 22 - k, col(0x7FBF4F))
            }
        }
    }
}

private fun PixiGame.drawHud(now: Long) {
    val f = fb
    f.shadeRect(0, 0, W, 10, 0.5f, PANEL)
    drawCoin(f, 5, 5)
    f.text("${pet.coins}", 10, 2, WHITE)
    f.text(pet.name, W / 2 - f.tw(pet.name) / 2 + 3, 2, WHITE)
    if (pet.dead || away()) return
    if ((now / 500L) % 2L == 0L) return
    var x = W - 11
    if (pet.hunger < 30f) {
        drawIcon(f, 0, x, 0)
        x -= 11
    }
    if (pet.hygiene < 30f) {
        drawIcon(f, 13, x, 0)
        x -= 11
    }
    if (pet.sick) {
        drawIcon(f, 8, x, 0)
        x -= 11
    }
    if (pet.energy < 25f && !pet.sleeping) {
        drawIcon(f, 11, x, 0)
        x -= 11
    }
    if (pet.poops >= 2) drawIcon(f, 12, x, 0)
}

// ---------------------------------------------------------------- luoghi

private fun PixiGame.drawLocation(now: Long) {
    val f = fb
    drawScene(scr, now)
    val gy = groundY(scr)
    if (scr == Scr.UNDER) {
        drawNpc(now, 1, 10, gy, false)
        drawNpc(now, 2, 70, gy, true)
    }
    drawPetActor(now, 40, gy, moodNow(), false)
    val label = when (scr) {
        Scr.PARK -> "PARCO"
        Scr.SCHOOL -> "SCUOLA"
        Scr.STAND -> "BANCARELLA"
        else -> "ALDILA"
    }
    f.textO(label, 3, 2, WHITE)
    f.textR("${pet.coins}", W - 10, 2, WHITE)
    drawCoin(f, W - 5, 4)
}

// ------------------------------------------------------------ animazioni

private fun PixiGame.drawAnim(now: Long) {
    val a = anim ?: return
    val f = fb
    val p = ((now - a.t0).toFloat() / a.dur).coerceIn(0f, 1f)
    if (a.kind == A_BATH) drawBathroom(f, now) else drawScene(a.back, now)
    val gy = groundY(a.back)
    val x0 = 40

    if (homeLike(a.back) && a.kind != A_BATH) {
        val spots = listOf(12 to 60, 62 to 62, 24 to 70, 50 to 72)
        val left = if (a.kind == A_CLEAN) Math.ceil((pet.poops * (1f - p)).toDouble()).toInt() else pet.poops
        for (i in 0 until left.coerceIn(0, 4)) drawPoop(f, spots[i].first, spots[i].second)
    }

    val t = now / 100L
    when (a.kind) {
        A_FEED -> {
            val food = FOODS[a.arg.coerceIn(0, FOODS.size - 1)]
            val mood = if (p > 0.35f && (now / 250L) % 2L == 0L) M_EAT else M_NORMAL
            drawPetActor(now, x0, gy, mood, false)
            val prog = (p / 0.4f).coerceIn(0f, 1f)
            val fx = (8 + (x0 - 24 - 8) * prog).toInt()
            if (p < 0.85f) drawFood(f, food.kind, fx, gy - 22 + (if (p > 0.4f) ((now / 200L) % 2L).toInt() else 0))
        }
        A_BATH -> {
            drawPetActor(now, x0, gy, M_HAPPY, false)
            drawTubFront(f, now)
            for (i in 0 until 8) {
                val bxx = x0 - 18 + (i * 7) % 36
                val byy = gy - ((t * 2 + i * 11) % 40).toInt()
                f.ellipse(bxx, byy, 2, 2, col(0xBFE8FF))
                f.set(bxx - 1, byy - 1, WHITE)
            }
            for (i in 0 until 6) f.rect(x0 - 14 + i * 5, 0, 1, 10 + ((t + i * 3) % 6).toInt(), col(0x5AB8FF))
        }
        A_REFUSE -> {
            val shake = (sin(now / 60.0) * 3).toInt()
            drawPetActor(now, x0 + shake, gy, M_SAD, shake > 0)
            f.rect(x0 - 10, 12, 22, 11, WHITE)
            f.frame(x0 - 10, 12, 22, 11, col(0x333333))
            f.textC("NO!", 15, col(0xE23B3B), 1, x0 + 1)
        }
        A_CLEAN -> {
            drawPetActor(now, x0, gy, M_HAPPY, false)
            val bx2 = 14 + (((now / 140L) % 6L).toInt()) * 8
            f.line(bx2 + 6, 44, bx2, 58, col(0x8A5A2B))
            f.rect(bx2 - 3, 58, 8, 4, col(0xE6C14B))
        }
        A_PET -> {
            drawPetActor(now, x0, gy, M_HAPPY, false)
            val hx = x0 + (sin(now / 150.0) * 6).toInt()
            f.ellipse(hx, gy - 52, 5, 3, col(0xF5C9A0))
            f.outline(OUT)
            for (i in 0 until 3) drawHeart(f, x0 - 14 + i * 10, gy - 52 - ((t + i * 9) % 18).toInt())
        }
        A_MED -> {
            drawPetActor(now, x0, gy, if (p > 0.6f) M_NORMAL else M_SICK, false)
            drawIcon(f, 8, x0 + 14, gy - 40 + (if ((now / 200L) % 2L == 0L) 0 else 3))
            for (i in 0 until 5) f.set(x0 - 14 + (i * 7) % 30, gy - 8 - ((t + i * 5) % 26).toInt(), col(0x9BF0B0))
        }
        A_STUDY -> {
            drawPetActor(now, x0, gy, M_NORMAL, false)
            drawIcon(f, 16, x0 + 16, gy - 14)
            val words = arrayOf("ABC", "1+1", "A+B", "2X2")
            val w = words[((now / 700L) % 4L).toInt()]
            f.rect(x0 - 12, gy - 54, 24, 9, WHITE)
            f.frame(x0 - 12, gy - 54, 24, 9, OUT)
            f.textC(w, gy - 52, EYE, 1, x0)
        }
        A_WORK -> {
            drawPetActor(now, x0, gy, M_HAPPY, false)
            for (i in 0 until 3) drawCoin(f, 20 + i * 20, 40 - ((t + i * 7) % 20).toInt())
            f.textO("LAVORO!", 3, 2, WHITE)
        }
        A_BALL -> {
            drawPetActor(now, x0, gy, M_HAPPY, false)
            val ph = ((now / 500L) % 2L).toInt()
            val bxx = if (ph == 0) 12 + ((now % 500L) * 56 / 500L).toInt() else 68 - ((now % 500L) * 56 / 500L).toInt()
            val byy = gy - 20 - (sin((now % 500L) / 500.0 * Math.PI) * 14).toInt()
            f.ellipse(bxx, byy, 3, 3, WHITE)
            f.set(bxx, byy, col(0xE23B3B))
            f.outline(OUT)
        }
        A_DATE -> {
            drawPetActor(now, 26, gy, M_HAPPY, false)
            val nx = (100 - 38 * (p / 0.3f).coerceIn(0f, 1f)).toInt()
            drawNpc(now, 0, nx, gy, true, (a.t0 % 5L).toInt())
            if (p > 0.4f) {
                for (i in 0 until 2) {
                    val hy = gy - 46 - ((t + i * 9) % 14).toInt()
                    if (dateOk) drawHeart(f, 40 + i * 6, hy) else drawHeart(f, 40 + i * 6, hy, GRAY)
                }
            }
        }
        A_TALK -> {
            drawPetActor(now, 26, gy, M_HAPPY, false)
            drawNpc(now, if (a.arg == 0) 1 else 2, 62, gy, true)
            for (i in 0 until 2) drawHeart(f, 40 + i * 6, gy - 46 - ((t + i * 9) % 14).toInt(), if (a.arg == 0) col(0xFF4D78) else col(0x8A2346))
        }
        A_ANGEL -> {
            drawPetActor(now, x0, gy, M_HAPPY, false)
            for (i in 0 until 14) {
                val ang = i * 0.45 + now / 300.0
                val rr = 18 + ((t + i) % 8).toInt()
                f.rect(x0 + (Math.cos(ang) * rr).toInt(), gy - 22 + (sin(ang) * rr).toInt(), 2, 2, YELLOW)
            }
            if (p > 0.85f) f.shade((p - 0.85f) * 6f, WHITE)
        }
    }
    f.rect(8, 76, 64, 2, PANEL)
    f.rect(8, 76, (64 * p).toInt(), 2, PINK)
}

// ----------------------------------------------------------------- minigiochi

private fun PixiGame.drawCatch(now: Long) {
    val f = fb
    f.grad(0, 60, col(0x6A4DC8), col(0xF3A0C8))
    f.rect(0, 60, W, 20, col(0x3A2A5A))
    f.ellipse(66, 10, 5, 5, col(0xFFE9A8))
    f.ellipse(68, 9, 4, 4, col(0x7A58D0))
    for (d in drops) {
        val x = d.x.toInt()
        val y = d.y.toInt()
        when (d.type) {
            0 -> {
                f.ellipse(x, y, 2, 2, col(0xFF6BA8))
                f.set(x - 3, y, col(0xFFC2DB))
                f.set(x + 3, y, col(0xFFC2DB))
            }
            1 -> {
                f.ellipse(x, y, 2, 2, col(0x8A4DFF))
                f.set(x - 3, y, col(0xFF8A2B))
                f.set(x + 3, y, col(0xFF8A2B))
                f.set(x, y - 3, YELLOW)
            }
            else -> {
                f.ellipse(x, y, 2, 2, col(0x8BCB5E))
                f.set(x - 1, y - 1, EYE)
                f.set(x + 1, y - 1, EYE)
                f.set(x, y + 1, EYE)
            }
        }
    }
    val bxi = bx.toInt()
    f.rect(bxi - 8, 68, 16, 8, col(0xB07E48))
    f.rect(bxi - 9, 67, 18, 2, col(0x8A5A2B))
    f.rect(bxi - 6, 71, 12, 1, col(0x8A5A2B))
    f.textO("PUNTI ${cScore}", 3, 2, WHITE)
    for (i in 0 until cLives) drawHeart(f, W - 8 - i * 7, 2)
    val left = ((cEnd - now) / 1000L).coerceAtLeast(0L)
    f.textO("${left}S", 3, 10, WHITE)
}

private fun PixiGame.drawHilo(now: Long) {
    val f = fb
    f.grad(0, H, col(0x4B3A8A), col(0x241C3D))
    drawTopBar("TURNO ${sRound}/5")
    f.textC("SX=BASSO  DX=ALTO", 14, GRAY)
    when (sPhase) {
        0 -> {
            val n = sNum.toString()
            f.textC(n, 28, WHITE, 5)
            f.text("<BASSO", 4, 66, col(0x9BF0B0))
            f.textR("ALTO>", W - 4, 66, col(0xFFC2DB))
        }
        1, 2 -> {
            val n = sNext.toString()
            f.textC(n, 28, if (sPhase == 1) col(0x9BF0B0) else col(0xFF8A8A), 5)
            f.textC(if (sPhase == 1) "GIUSTO!" else "ERRATO", 62, WHITE)
        }
        else -> {
            f.textC("PUNTI", 22, PINK)
            f.textC("$sCorrect/5", 34, WHITE, 3)
        }
    }
}

private fun PixiGame.drawSwing(now: Long) {
    val f = fb
    f.grad(0, 56, col(0x7EC8F2), col(0xCBEBFB))
    f.rect(0, 56, W, 24, col(0x7CC46B))
    f.rect(0, 74, W, 6, col(0x5FA352))
    drawPetActor(now, 14, 74, M_HAPPY, false)
    f.rect(40, 62, 13, 2, YELLOW)
    f.text("v", 45, 56, YELLOW)
    if (now < swingUntil) {
        f.line(28, 44, 40, 60, col(0x8A5A2B))
        f.line(29, 44, 41, 60, col(0x8A5A2B))
    } else {
        f.line(30, 62, 30, 46, col(0x8A5A2B))
    }
    when (wPhase) {
        0 -> {
            f.ellipse(ballX.toInt(), 58, 2, 2, WHITE)
            f.outline(OUT)
        }
        1 -> {
            val t = (now - wT0) / 1000f
            f.ellipse(52 + (t * 40f).toInt(), 58 - (t * 40f).toInt(), 2, 2, WHITE)
            f.textO("COLPITO!", 34, 30, WHITE)
        }
        2 -> f.textO("MANCATO", 34, 30, WHITE)
        else -> f.textC(if (wHits >= 3) "VINTO" else "PERSO", 28, WHITE, 3)
    }
    f.textO("TURNO ${wRound}/5  COLPI $wHits", 3, 2, WHITE)
    f.textC("PREMI OK / TOCCA", 76, WHITE)
}

private fun PixiGame.drawEnd(now: Long) {
    val f = fb
    f.grad(0, H, col(0x4B3A8A), col(0x241C3D))
    drawTopBar("RISULTATO")
    for (i in endLines.indices) f.textC(endLines[i], 24 + i * 12, WHITE, 1)
    if ((now / 500L) % 2L == 0L) f.textC("OK", 62, YELLOW)
}

private fun PixiGame.drawDead(now: Long) {
    val f = fb
    f.grad(0, H, col(0x2B2850), col(0x0F0C1E))
    for (i in 0 until 14) f.set((i * 23) % W, (i * 13) % 40, col(0xFFE9A8))
    // lapide
    f.rect(26, 46, 28, 28, col(0x9097B0))
    f.ellipse(40, 46, 14, 8, col(0x9097B0))
    f.rect(24, 72, 32, 4, col(0x5C6385))
    f.textC("RIP", 48, col(0x2A2340))
    f.textC(pet.name, 56, col(0x2A2340))
    f.textC(pet.cause, 63, col(0x2A2340))
    val st = pet.stage(now)
    val sp = if (pet.pers >= 0) pet.pers else 0
    val ghost = makePet(st, sp, pet.hue, M_SICK, false, false, 1, true)
    f.blit(ghost, 18, 2 + ((sin(now / 400.0) * 2).toInt()))
    if ((now / 500L) % 2L == 0L) f.textC("OK: NUOVO UOVO", 3, YELLOW)
}
