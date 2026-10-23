package com.marco.pixipet

import kotlin.math.cos
import kotlin.math.sin

const val M_NORMAL = 0
const val M_HAPPY = 1
const val M_SAD = 2
const val M_SLEEP = 3
const val M_SICK = 4
const val M_EAT = 5

val OUT = col(0x3A2F45)
val EYE = col(0x2A2033)
val BLUSH = col(0xFF7F9F)
val YELLOW = col(0xFFD84D)
val BODY = intArrayOf(col(0xFF9EC4), col(0x7FBDFF), col(0xFFA94D), col(0xA9DB5E), col(0xB48CFF))

private fun eye(s: Fb, x: Int, y: Int, mood: Int, blink: Boolean) {
    when {
        mood == M_SLEEP || blink -> s.rect(x - 1, y + 1, 3, 1, EYE)
        mood == M_HAPPY -> {
            s.set(x - 1, y + 1, EYE)
            s.set(x, y, EYE)
            s.set(x + 1, y + 1, EYE)
        }
        mood == M_SICK -> {
            s.set(x - 1, y - 1, EYE)
            s.set(x + 1, y - 1, EYE)
            s.set(x, y, EYE)
            s.set(x - 1, y + 1, EYE)
            s.set(x + 1, y + 1, EYE)
        }
        else -> {
            s.rect(x - 1, y - 1, 2, 3, EYE)
            s.set(x - 1, y - 1, WHITE)
        }
    }
}

/**
 * Crea lo sprite di una creatura (44x46, piedi su y=43, centro x=22).
 * sp = personalita' (0 dolce, 1 furbo, 2 sportivo, 3 goloso, 4 burbero).
 * aura: 0 niente, 1 angelo, 2 demone. tint != 0 sostituisce il colore.
 */
fun makePet(
    stage: Stage, sp: Int, hue: Int, mood: Int, blink: Boolean, bob: Boolean,
    aura: Int, ghost: Boolean, tint: Int = 0
): Fb {
    val s = Fb(44, 46)
    val cx = 22
    val by = 43

    if (stage == Stage.EGG) {
        val c = BODY[hue.coerceIn(0, 4)]
        s.ellipse(cx, 30, 9, 12, col(0xFFF6E4))
        s.ellipse(cx - 3, 26, 2, 2, c)
        s.ellipse(cx + 4, 32, 3, 2, c)
        s.ellipse(cx - 2, 37, 2, 1, c)
        s.rect(cx - 5, 22, 2, 3, WHITE)
        s.outline(OUT)
        return s
    }

    val big = stage.ordinal >= Stage.TEEN.ordinal
    val idx = (if (big) sp else hue).coerceIn(0, 4)
    var base = BODY[idx]
    if (stage == Stage.ELDER) base = mix(base, col(0xC9C4D4), 0.55f)
    if (tint != 0) base = tint
    if (ghost) base = col(0xE4EAF8)
    val belly = lighten(base, 0.5f)
    val dark = mix(base, OUT, 0.35f)

    var rx = when (stage) {
        Stage.BABY -> 9
        Stage.CHILD -> 11
        Stage.TEEN -> 12
        Stage.ADULT -> 14
        else -> 12
    }
    var ry = when (stage) {
        Stage.BABY -> 8
        Stage.CHILD -> 10
        Stage.TEEN -> 13
        Stage.ADULT -> 15
        else -> 13
    }
    if (big && sp == 3) rx += 3
    if (big && sp == 2) ry += 1
    if (bob) ry -= 1
    val cy = by - ry - 2
    val ex = (rx / 3).coerceAtLeast(3)
    val ey = cy - ry / 4

    // dietro al corpo
    if (aura == 1) {
        s.ellipse(cx - rx - 3, cy - 1, 4, 7, WHITE)
        s.ellipse(cx + rx + 3, cy - 1, 4, 7, WHITE)
    }
    if (aura == 2) {
        s.ellipse(cx - rx - 3, cy - 3, 4, 7, col(0x8A2346))
        s.ellipse(cx + rx + 3, cy - 3, 4, 7, col(0x8A2346))
        s.line(cx + rx, cy + ry - 2, cx + rx + 5, cy + ry - 6, col(0xD62F4F))
        s.set(cx + rx + 6, cy + ry - 7, col(0xD62F4F))
        s.set(cx + rx + 5, cy + ry - 7, col(0xD62F4F))
    }
    if (big && sp == 0) {
        s.ellipse(cx + rx + 1, cy + ry / 2, 3, 2, base)
        s.ellipse(cx + rx + 3, cy + ry / 2 - 2, 2, 2, base)
    }
    if (big && sp == 0) {
        // orecchie a punta
        s.tri(cx - rx + 3, cy - ry - 4, 8, base)
        s.tri(cx + rx - 3, cy - ry - 4, 8, base)
        s.tri(cx - rx + 3, cy - ry - 1, 4, BLUSH)
        s.tri(cx + rx - 3, cy - ry - 1, 4, BLUSH)
    }
    if (big && sp == 4) {
        s.tri(cx - rx + 4, cy - ry - 5, 7, col(0x5A3A8A))
        s.tri(cx + rx - 4, cy - ry - 5, 7, col(0x5A3A8A))
    }
    if (stage == Stage.CHILD) {
        s.ellipse(cx - rx + 3, cy - ry + 2, 3, 3, base)
        s.ellipse(cx + rx - 3, cy - ry + 2, 3, 3, base)
    }
    if (big && (sp == 1 || sp == 3 || sp == 2) && sp != 0) {
        if (sp == 3) {
            s.ellipse(cx - rx + 3, cy - ry + 2, 3, 3, base)
            s.ellipse(cx + rx - 3, cy - ry + 2, 3, 3, base)
        }
    }

    // corpo
    s.ellipse(cx, cy, rx, ry, base)
    if (stage == Stage.BABY) {
        s.rect(cx, cy - ry - 3, 1, 3, col(0x4FA84F))
        s.ellipse(cx + 2, cy - ry - 3, 2, 1, col(0x6FCF6F))
    }
    s.ellipse(cx, cy + ry / 3, (rx * 6) / 10, (ry * 5) / 10, belly)

    if (!ghost) {
        s.rect(cx - rx / 2 - 1, by - 2, 4, 2, dark)
        s.rect(cx + rx / 2 - 2, by - 2, 4, 2, dark)
    }
    s.rect(cx - rx - 1, cy, 2, 4, base)
    s.rect(cx + rx, cy, 2, 4, base)

    // dettagli per personalita'
    if (big && sp == 1) {
        s.rect(cx - 2, cy - ry - 2, 1, 3, dark)
        s.rect(cx, cy - ry - 3, 1, 4, dark)
        s.rect(cx + 2, cy - ry - 2, 1, 3, dark)
    }
    if (big && sp == 2) {
        s.rect(cx - rx + 1, cy - ry + 4, rx * 2 - 1, 3, col(0xE23B3B))
        s.rect(cx + rx - 1, cy - ry + 6, 3, 2, col(0xE23B3B))
    }
    if (big && sp == 3) {
        s.rect(cx - 4, cy + ry - 3, 9, 3, col(0xFFFFFF))
        s.rect(cx - 4, cy + ry - 3, 9, 1, col(0xE23B3B))
    }

    // viso
    if (stage == Stage.ELDER) {
        s.ellipse(cx, cy + ry / 4 + 5, rx / 2, 3, WHITE)
    }
    eye(s, cx - ex, ey, if (ghost) M_SICK else mood, blink)
    eye(s, cx + ex, ey, if (ghost) M_SICK else mood, blink)
    if (big && sp == 1) {
        s.frame(cx - ex - 3, ey - 3, 7, 7, col(0x2A2033))
        s.frame(cx + ex - 3, ey - 3, 7, 7, col(0x2A2033))
        s.rect(cx - ex + 4, ey, ex * 2 - 7, 1, col(0x2A2033))
    }
    if (big && sp == 4) {
        s.line(cx - ex - 2, ey - 4, cx - ex + 2, ey - 2, EYE)
        s.line(cx + ex + 2, ey - 4, cx + ex - 2, ey - 2, EYE)
    }
    if (stage == Stage.ELDER) {
        s.rect(cx - ex - 1, ey - 4, 4, 1, WHITE)
        s.rect(cx + ex - 2, ey - 4, 4, 1, WHITE)
    }
    if (mood == M_HAPPY || (big && sp == 0) || mood == M_EAT) {
        s.rect(cx - ex - 3, ey + 3, 3, 2, BLUSH)
        s.rect(cx + ex + 1, ey + 3, 3, 2, BLUSH)
    }
    if (mood == M_SICK) {
        s.rect(cx - ex - 3, ey + 3, 3, 2, col(0x8BCB5E))
        s.rect(cx + ex + 1, ey + 3, 3, 2, col(0x8BCB5E))
    }
    val my = cy + ry / 4 + 2
    when {
        ghost -> s.ellipse(cx, my + 1, 1, 1, EYE)
        mood == M_EAT -> {
            s.ellipse(cx, my + 1, 2, 2, EYE)
            s.rect(cx - 1, my + 2, 3, 1, col(0xFF6B81))
        }
        mood == M_SAD || mood == M_SICK -> {
            s.set(cx - 2, my + 1, EYE)
            s.set(cx - 1, my, EYE)
            s.set(cx, my, EYE)
            s.set(cx + 1, my + 1, EYE)
        }
        mood == M_SLEEP -> s.rect(cx - 1, my, 3, 1, EYE)
        big && sp == 3 -> {
            s.rect(cx - 3, my, 7, 1, EYE)
            s.rect(cx - 2, my + 1, 5, 2, EYE)
            s.rect(cx - 1, my + 2, 3, 1, col(0xFF6B81))
        }
        else -> {
            s.set(cx - 2, my, EYE)
            s.set(cx - 1, my + 1, EYE)
            s.set(cx, my + 1, EYE)
            s.set(cx + 1, my, EYE)
        }
    }
    if (mood == M_SAD) s.rect(cx - ex, ey + 3, 1, 2, col(0x6FB7FF))
    if (big && sp == 4 && mood != M_SLEEP) s.set(cx + 2, my + 1, WHITE)

    if (stage == Stage.ELDER) {
        // bastone
        s.line(cx + rx + 3, cy + 3, cx + rx + 3, by, col(0x8A5A2B))
        s.line(cx + rx + 3, cy + 3, cx + rx + 1, cy + 2, col(0x8A5A2B))
    }

    // aureole
    if (aura == 1 || ghost) {
        s.ellipse(cx, cy - ry - 5, 6, 2, YELLOW)
        s.ellipse(cx, cy - ry - 5, 3, 1, 0)
    }
    if (aura == 2) {
        s.tri(cx - 6, cy - ry - 5, 5, col(0xD62F4F))
        s.tri(cx + 6, cy - ry - 5, 5, col(0xD62F4F))
    }

    s.outline(OUT)
    return s
}

// ------------------------------------------------------------------ oggetti

fun drawFood(f: Fb, kind: Int, x: Int, y: Int) {
    when (kind) {
        0 -> { // onigiri
            f.tri(x + 5, y + 1, 8, WHITE)
            f.rect(x + 3, y + 6, 5, 3, col(0x2B3A2B))
            f.outline(OUT)
        }
        1 -> { // hamburger
            f.ellipse(x + 5, y + 2, 4, 2, col(0xE6A24B))
            f.rect(x + 1, y + 4, 9, 1, col(0x6FCF6F))
            f.rect(x + 1, y + 5, 9, 2, col(0x7A4528))
            f.ellipse(x + 5, y + 8, 4, 1, col(0xE6A24B))
        }
        2 -> { // insalata
            f.ellipse(x + 5, y + 4, 4, 3, col(0x6FCF6F))
            f.rect(x + 1, y + 6, 9, 1, col(0x4FA84F))
            f.rect(x + 2, y + 7, 7, 2, col(0xF4F4F4))
        }
        3 -> { // caramella
            f.ellipse(x + 5, y + 5, 3, 3, col(0xFF6BA8))
            f.tri(x + 1, y + 3, 3, col(0xFFC2DB))
            f.tri(x + 9, y + 3, 3, col(0xFFC2DB))
        }
        else -> { // torta
            f.rect(x + 1, y + 4, 9, 5, col(0xF7D6A6))
            f.rect(x + 1, y + 3, 9, 2, WHITE)
            f.ellipse(x + 5, y + 2, 1, 1, col(0xE23B3B))
            f.rect(x + 1, y + 6, 9, 1, col(0xC98B5E))
        }
    }
}

fun drawIcon(f: Fb, id: Int, x: Int, y: Int) {
    when (id) {
        0 -> { // mela
            f.ellipse(x + 5, y + 6, 4, 4, col(0xE23B3B))
            f.rect(x + 5, y, 1, 3, col(0x7A4528))
            f.rect(x + 6, y + 1, 3, 2, col(0x4FA84F))
            f.set(x + 3, y + 4, col(0xFF9A9A))
        }
        1 -> { // goccia / doccia
            f.tri(x + 5, y, 5, col(0x5AB8FF))
            f.ellipse(x + 5, y + 7, 3, 3, col(0x5AB8FF))
            f.set(x + 4, y + 6, WHITE)
        }
        2 -> { // scopa
            f.line(x + 8, y, x + 3, y + 6, col(0x8A5A2B))
            f.rect(x + 1, y + 6, 5, 3, col(0xE6C14B))
        }
        3 -> { // cuore
            f.ellipse(x + 3, y + 4, 2, 2, col(0xFF4D78))
            f.ellipse(x + 7, y + 4, 2, 2, col(0xFF4D78))
            f.tri(x + 5, y + 4, 6, col(0xFF4D78))
        }
        4 -> { // gamepad
            f.rect(x, y + 3, 10, 6, col(0x8A8FA8))
            f.rect(x + 1, y + 5, 3, 1, BLACK)
            f.rect(x + 2, y + 4, 1, 3, BLACK)
            f.set(x + 7, y + 5, col(0xE23B3B))
            f.set(x + 8, y + 6, col(0x4D8BFF))
        }
        5 -> { // mappa / pin
            f.ellipse(x + 5, y + 3, 3, 3, col(0xE23B3B))
            f.tri(x + 5, y + 5, 5, col(0xE23B3B))
            f.set(x + 5, y + 3, WHITE)
        }
        6 -> { // statistiche
            f.rect(x + 1, y + 5, 2, 5, col(0x6FCF6F))
            f.rect(x + 4, y + 2, 2, 8, col(0xFFD84D))
            f.rect(x + 7, y + 4, 2, 6, col(0x5AB8FF))
        }
        7 -> { // luna
            f.ellipse(x + 5, y + 5, 4, 4, YELLOW)
            f.ellipse(x + 7, y + 4, 3, 3, col(0xE9DDF7))
        }
        8 -> { // croce medica
            f.rect(x + 3, y + 1, 4, 8, col(0xE23B3B))
            f.rect(x + 1, y + 3, 8, 4, col(0xE23B3B))
        }
        9 -> { // ingranaggio
            f.ellipse(x + 5, y + 5, 4, 4, col(0x8A8FA8))
            f.ellipse(x + 5, y + 5, 1, 1, col(0xE9DDF7))
            f.rect(x + 4, y, 2, 2, col(0x8A8FA8))
            f.rect(x + 4, y + 8, 2, 2, col(0x8A8FA8))
            f.rect(x, y + 4, 2, 2, col(0x8A8FA8))
            f.rect(x + 8, y + 4, 2, 2, col(0x8A8FA8))
        }
        10 -> { // indietro
            f.line(x + 2, y + 2, x + 8, y + 8, col(0xE23B3B))
            f.line(x + 8, y + 2, x + 2, y + 8, col(0xE23B3B))
            f.line(x + 3, y + 2, x + 9, y + 8, col(0xE23B3B))
            f.line(x + 7, y + 2, x + 1, y + 8, col(0xE23B3B))
        }
        11 -> { // zZ
            f.rect(x + 1, y + 1, 4, 1, col(0x6A5ACD))
            f.line(x + 4, y + 2, x + 1, y + 5, col(0x6A5ACD))
            f.rect(x + 1, y + 5, 4, 1, col(0x6A5ACD))
            f.rect(x + 6, y + 5, 3, 1, col(0x6A5ACD))
            f.line(x + 8, y + 6, x + 6, y + 8, col(0x6A5ACD))
            f.rect(x + 6, y + 8, 3, 1, col(0x6A5ACD))
        }
        12 -> { // cacca
            f.ellipse(x + 5, y + 7, 4, 2, col(0x8A5A2B))
            f.ellipse(x + 5, y + 4, 3, 2, col(0x8A5A2B))
            f.ellipse(x + 5, y + 2, 1, 1, col(0x8A5A2B))
        }
        13 -> { // puzza
            for (k in 0..2) {
                f.line(x + 2 + k * 3, y + 8, x + 3 + k * 3, y + 5, col(0x7FBF4F))
                f.line(x + 3 + k * 3, y + 5, x + 2 + k * 3, y + 2, col(0x7FBF4F))
            }
        }
        14 -> { // moneta
            f.ellipse(x + 5, y + 5, 4, 4, YELLOW)
            f.rect(x + 5, y + 3, 1, 5, col(0xC99A1B))
        }
        15 -> { // caramella spooky
            f.ellipse(x + 5, y + 5, 3, 3, col(0x8A4DFF))
            f.tri(x + 1, y + 3, 3, col(0xFF8A2B))
            f.tri(x + 9, y + 3, 3, col(0xFF8A2B))
        }
        16 -> { // libro
            f.rect(x + 1, y + 1, 8, 8, col(0x4D8BFF))
            f.rect(x + 2, y + 2, 6, 6, WHITE)
            f.rect(x + 3, y + 4, 4, 1, col(0x4D8BFF))
            f.rect(x + 3, y + 6, 3, 1, col(0x4D8BFF))
        }
    }
}

fun drawPoop(f: Fb, x: Int, y: Int) {
    val c = col(0x8A5A2B)
    f.ellipse(x, y + 5, 5, 2, c)
    f.ellipse(x, y + 2, 3, 2, c)
    f.set(x, y - 1, c)
    f.set(x, y - 2, c)
    f.set(x - 2, y + 4, WHITE)
    f.set(x + 1, y + 4, WHITE)
    f.set(x - 2, y + 5, EYE)
    f.set(x + 1, y + 5, EYE)
}

fun drawHeart(f: Fb, x: Int, y: Int, c: Int = col(0xFF4D78)) {
    f.rect(x, y, 2, 2, c)
    f.rect(x + 3, y, 2, 2, c)
    f.rect(x, y + 1, 5, 2, c)
    f.rect(x + 1, y + 3, 3, 1, c)
    f.set(x + 2, y + 4, c)
}

fun drawCoin(f: Fb, x: Int, y: Int) {
    f.ellipse(x, y, 2, 2, YELLOW)
    f.set(x, y - 1, col(0xC99A1B))
    f.set(x, y, col(0xC99A1B))
    f.set(x, y + 1, col(0xC99A1B))
}

// ------------------------------------------------------------------- scene

fun drawRoom(f: Fb, now: Long, night: Boolean) {
    val wall = col(0xF6E6CF)
    val wall2 = col(0xEDD6B3)
    f.rect(0, 0, W, 54, wall)
    for (x in 0 until W step 8) f.rect(x, 0, 4, 54, wall2)
    f.rect(0, 52, W, 3, col(0xB98B5E))
    f.rect(0, 55, W, 25, col(0xDDAA6E))
    var y = 55
    var odd = false
    while (y < H) {
        f.rect(0, y, W, 1, col(0xC49058))
        var x = if (odd) 6 else 0
        while (x < W) {
            f.rect(x, y, 1, 6, col(0xC49058))
            x += 12
        }
        odd = !odd
        y += 6
    }
    // finestra
    f.rect(50, 8, 24, 24, col(0x9B6A3C))
    drawWindowSky(f, night, now)
    f.rect(48, 6, 28, 3, col(0x7A4F28))
    f.rect(48, 6, 5, 26, col(0xE56B8A))
    f.rect(71, 6, 5, 26, col(0xE56B8A))
    // quadro
    f.rect(8, 10, 18, 14, col(0x7A4F28))
    f.rect(10, 12, 14, 10, col(0xBDE6FF))
    f.ellipse(14, 24, 7, 4, col(0x6FCF6F))
    f.ellipse(21, 23, 6, 3, col(0x4FA84F))
    f.ellipse(20, 15, 2, 2, YELLOW)
    // tappeto
    f.ellipse(40, 69, 27, 6, col(0xE08C8C))
    f.ellipse(40, 69, 21, 4, col(0xF2B6B6))
    f.ellipse(40, 69, 10, 2, col(0xE08C8C))
}

fun drawPark(f: Fb, now: Long) {
    f.grad(0, 22, col(0x7EC8F2), col(0xB8E2F8))
    f.rect(0, 22, W, 20, col(0xCBEBFB))
    f.ellipse(66, 10, 6, 6, YELLOW)
    val cx = ((now / 250) % 110).toInt() - 20
    f.ellipse(cx, 14, 7, 3, WHITE)
    f.ellipse(cx + 6, 12, 5, 3, WHITE)
    f.ellipse(cx - 6, 13, 4, 2, WHITE)
    f.ellipse(18, 44, 34, 8, col(0x8FD07F))
    f.ellipse(66, 46, 30, 7, col(0x7CC46B))
    f.rect(0, 44, W, 36, col(0x7CC46B))
    for (i in 0 until 40) {
        val x = (i * 37) % W
        val y = 46 + (i * 17) % 34
        f.set(x, y, col(0x5FA352))
        f.set(x + 1, y - 1, col(0x5FA352))
    }
    f.ellipse(40, 76, 40, 5, col(0xE4CFA0))
    // albero
    f.rect(9, 24, 5, 24, col(0x8A5A2B))
    f.ellipse(11, 20, 11, 10, col(0x3E9A4A))
    f.ellipse(5, 25, 7, 6, col(0x4FB05B))
    f.ellipse(17, 25, 7, 6, col(0x4FB05B))
    // panchina
    f.rect(52, 50, 20, 3, col(0x9B6A3C))
    f.rect(52, 46, 20, 3, col(0xB07E48))
    f.rect(54, 53, 2, 5, col(0x6B4423))
    f.rect(68, 53, 2, 5, col(0x6B4423))
    // fiori
    val fl = intArrayOf(col(0xFF6B8A), col(0xFFD84D), WHITE, col(0xB48CFF))
    for (i in 0 until 9) f.set(4 + i * 9, 58 + (i * 5) % 14, fl[i % 4])
}

fun drawSchool(f: Fb, now: Long) {
    f.rect(0, 0, W, 56, col(0xF1E6C8))
    f.rect(0, 52, W, 3, col(0x8A6B3B))
    f.rect(0, 55, W, 25, col(0xB98B5E))
    for (y in 56 until H step 6) f.rect(0, y, W, 1, col(0xA37A4D))
    // lavagna
    f.rect(10, 8, 44, 26, col(0x7A4F28))
    f.rect(12, 10, 40, 22, col(0x2F5D4A))
    f.text("ABC 123", 17, 14, col(0xF4F4F4))
    f.text("1+1=2", 17, 22, col(0xF4F4F4))
    f.rect(12, 33, 40, 2, col(0x9B6A3C))
    // finestra
    f.rect(60, 10, 16, 18, col(0x9B6A3C))
    f.rect(62, 12, 12, 14, col(0x9AD7FF))
    f.rect(67, 12, 2, 14, col(0x9B6A3C))
    // orologio
    f.ellipse(40, 4, 3, 3, WHITE)
    f.set(40, 4, EYE)
    f.set(40, 3, EYE)
    f.set(41, 4, EYE)
    // banco
    f.rect(8, 60, 64, 4, col(0xD9A86C))
    f.rect(10, 64, 3, 12, col(0x8A5A2B))
    f.rect(67, 64, 3, 12, col(0x8A5A2B))
}

fun drawStand(f: Fb, now: Long) {
    f.grad(0, 36, col(0xFFD9A8), col(0xFFEFD6))
    f.rect(0, 36, W, 44, col(0xB9B0A0))
    for (x in 0 until W step 10) f.rect(x, 60, 5, 1, col(0xA39A8B))
    // bancarella con tenda a righe
    for (i in 0 until 8) f.rect(8 + i * 8, 6, 8, 12, if (i % 2 == 0) col(0xE23B3B) else WHITE)
    for (i in 0 until 8) f.ellipse(12 + i * 8, 18, 4, 2, if (i % 2 == 0) col(0xE23B3B) else WHITE)
    f.rect(8, 21, 3, 30, col(0x8A5A2B))
    f.rect(69, 21, 3, 30, col(0x8A5A2B))
    f.rect(8, 46, 64, 6, col(0xD9A86C))
    f.rect(8, 52, 64, 12, col(0xB07E48))
    f.text("SHOP", 32, 55, WHITE)
    // frutti sul bancone
    f.ellipse(20, 44, 3, 3, col(0xE23B3B))
    f.ellipse(27, 44, 3, 3, col(0xFFB347))
    f.ellipse(58, 44, 3, 3, col(0x8BCB5E))
}

fun drawUnder(f: Fb, now: Long) {
    f.grad(0, 56, col(0x2B1236), col(0x6A1B4D))
    f.rect(0, 56, W, 24, col(0x3A1A2E))
    for (i in 0 until 6) {
        val x = 6 + i * 14
        val h = 8 + ((now / 160 + i * 3) % 4).toInt() * 2
        f.tri(x, 56 - h, h, col(0xFF7A2B))
        f.tri(x, 56 - h + 3, h - 3, col(0xFFD84D))
    }
    // lapidi
    f.rect(8, 40, 10, 14, col(0x8A8FA8))
    f.ellipse(13, 40, 5, 3, col(0x8A8FA8))
    f.text("RIP", 7, 45, col(0x2A2340))
    f.rect(60, 44, 9, 10, col(0x8A8FA8))
    f.ellipse(64, 44, 4, 3, col(0x8A8FA8))
    // luna
    f.ellipse(66, 12, 5, 5, col(0xFFE9A8))
    f.ellipse(68, 11, 4, 4, col(0x3A1A4D))
    for (i in 0 until 8) f.set(4 + i * 10, 4 + (i * 7) % 20, col(0xFFE9A8))
}

/** Vetro della finestra con cielo di giorno (sole, nuvole) o di notte (luna e stelle che brillano). */
fun drawWindowSky(f: Fb, night: Boolean, now: Long) {
    if (night) {
        val sky = col(0x15204A)
        f.rect(52, 10, 20, 20, sky)
        f.rect(52, 24, 20, 6, col(0x1B2A5A))
        // luna crescente
        f.ellipse(67, 14, 3, 3, col(0xFFF3B0))
        f.ellipse(68, 13, 3, 3, sky)
        // stelle
        val tw = (now / 450L) % 2L == 0L
        val stars = arrayOf(
            intArrayOf(54, 12), intArrayOf(58, 16), intArrayOf(56, 25), intArrayOf(59, 28),
            intArrayOf(65, 24), intArrayOf(69, 27), intArrayOf(67, 22), intArrayOf(53, 28),
            intArrayOf(63, 12), intArrayOf(70, 11)
        )
        for (i in stars.indices) {
            val bright = (i % 2 == 0) == tw
            val c = if (bright) WHITE else col(0xFFF3B0)
            f.set(stars[i][0], stars[i][1], c)
            if (bright && i % 3 == 0) {
                f.set(stars[i][0] - 1, stars[i][1], col(0x8FA3E0))
                f.set(stars[i][0] + 1, stars[i][1], col(0x8FA3E0))
                f.set(stars[i][0], stars[i][1] - 1, col(0x8FA3E0))
                f.set(stars[i][0], stars[i][1] + 1, col(0x8FA3E0))
            }
        }
    } else {
        f.rect(52, 10, 20, 20, col(0x9AD7FF))
        f.ellipse(66, 16, 3, 3, YELLOW)
        f.ellipse(58, 24, 4, 2, WHITE)
        f.ellipse(62, 25, 3, 2, WHITE)
    }
    f.rect(61, 10, 2, 20, col(0x9B6A3C))
    f.rect(52, 19, 20, 2, col(0x9B6A3C))
}
