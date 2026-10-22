package com.marco.pixipet

const val W = 80
const val H = 80

fun col(v: Int): Int = v or (0xFF shl 24)

fun mix(a: Int, b: Int, t: Float): Int {
    val r = (((a shr 16) and 255) * (1 - t) + ((b shr 16) and 255) * t).toInt()
    val g = (((a shr 8) and 255) * (1 - t) + ((b shr 8) and 255) * t).toInt()
    val bl = ((a and 255) * (1 - t) + (b and 255) * t).toInt()
    return (0xFF shl 24) or (r shl 16) or (g shl 8) or bl
}

val WHITE = col(0xFFFFFF)
val BLACK = col(0x000000)

fun lighten(c: Int, t: Float) = mix(c, WHITE, t)
fun darken(c: Int, t: Float) = mix(c, BLACK, t)

/** Framebuffer a pixel colorati. 0 = trasparente. */
class Fb(val w: Int, val h: Int) {
    val px = IntArray(w * h)

    fun clear(c: Int = 0) {
        px.fill(c)
    }

    fun set(x: Int, y: Int, c: Int) {
        if (x >= 0 && y >= 0 && x < w && y < h) px[y * w + x] = c
    }

    fun get(x: Int, y: Int): Int = if (x >= 0 && y >= 0 && x < w && y < h) px[y * w + x] else 0

    fun rect(x: Int, y: Int, rw: Int, rh: Int, c: Int) {
        for (j in y until y + rh) for (i in x until x + rw) set(i, j, c)
    }

    fun frame(x: Int, y: Int, rw: Int, rh: Int, c: Int) {
        rect(x, y, rw, 1, c)
        rect(x, y + rh - 1, rw, 1, c)
        rect(x, y, 1, rh, c)
        rect(x + rw - 1, y, 1, rh, c)
    }

    fun ellipse(cx: Int, cy: Int, rx: Int, ry: Int, c: Int) {
        for (j in -ry..ry) for (i in -rx..rx) {
            val fx = i.toFloat() / (rx + 0.5f)
            val fy = j.toFloat() / (ry + 0.5f)
            if (fx * fx + fy * fy <= 1f) set(cx + i, cy + j, c)
        }
    }

    /** Triangolo con apice in alto. */
    fun tri(ax: Int, ay: Int, hgt: Int, c: Int) {
        for (r in 0 until hgt) rect(ax - r, ay + r, 2 * r + 1, 1, c)
    }

    fun line(x0: Int, y0: Int, x1: Int, y1: Int, c: Int) {
        var x = x0
        var y = y0
        val dx = kotlin.math.abs(x1 - x0)
        val dy = -kotlin.math.abs(y1 - y0)
        val sx = if (x0 < x1) 1 else -1
        val sy = if (y0 < y1) 1 else -1
        var err = dx + dy
        while (true) {
            set(x, y, c)
            if (x == x1 && y == y1) break
            val e2 = 2 * err
            if (e2 >= dy) {
                err += dy
                x += sx
            }
            if (e2 <= dx) {
                err += dx
                y += sy
            }
        }
    }

    fun grad(y0: Int, y1: Int, c1: Int, c2: Int) {
        val n = (y1 - y0).coerceAtLeast(1)
        for (y in y0 until y1) rect(0, y, w, 1, mix(c1, c2, (y - y0).toFloat() / n))
    }

    fun text(t: String, x: Int, y: Int, c: Int, scale: Int = 1) {
        var cx = x
        for (ch in t) {
            val g = Sprites.glyph(ch)
            for (r in 0 until 5) for (cc in 0 until 3) {
                if (g[r][cc] == '#') rect(cx + cc * scale, y + r * scale, scale, scale, c)
            }
            cx += 4 * scale
        }
    }

    fun tw(t: String, scale: Int = 1): Int = (t.length * 4 - 1).coerceAtLeast(0) * scale

    fun textC(t: String, y: Int, c: Int, scale: Int = 1, cx: Int = w / 2) {
        text(t, cx - tw(t, scale) / 2, y, c, scale)
    }

    fun textR(t: String, rx: Int, y: Int, c: Int, scale: Int = 1) {
        text(t, rx - tw(t, scale), y, c, scale)
    }

    /** Testo con ombra scura, leggibile su qualsiasi sfondo. */
    fun textO(t: String, x: Int, y: Int, c: Int, shadow: Int = col(0x2A2340)) {
        text(t, x + 1, y + 1, shadow)
        text(t, x, y, c)
    }

    fun blit(src: Fb, x: Int, y: Int, flip: Boolean = false) {
        for (j in 0 until src.h) for (i in 0 until src.w) {
            val c = src.px[j * src.w + (if (flip) src.w - 1 - i else i)]
            if ((c ushr 24) != 0) set(x + i, y + j, c)
        }
    }

    fun outline(c: Int) {
        val copy = px.copyOf()
        for (j in 0 until h) for (i in 0 until w) {
            if ((copy[j * w + i] ushr 24) == 0) {
                val n = (i > 0 && (copy[j * w + i - 1] ushr 24) != 0) ||
                    (i < w - 1 && (copy[j * w + i + 1] ushr 24) != 0) ||
                    (j > 0 && (copy[(j - 1) * w + i] ushr 24) != 0) ||
                    (j < h - 1 && (copy[(j + 1) * w + i] ushr 24) != 0)
                if (n) px[j * w + i] = c
            }
        }
    }

    fun shade(t: Float, target: Int) {
        for (i in px.indices) px[i] = mix(px[i], target, t)
    }

    fun shadeEllipse(cx: Int, cy: Int, rx: Int, ry: Int, t: Float, target: Int = BLACK) {
        for (j in -ry..ry) for (i in -rx..rx) {
            val fx = i.toFloat() / (rx + 0.5f)
            val fy = j.toFloat() / (ry + 0.5f)
            if (fx * fx + fy * fy <= 1f) {
                val x = cx + i
                val y = cy + j
                if (x >= 0 && y >= 0 && x < w && y < h) px[y * w + x] = mix(px[y * w + x], target, t)
            }
        }
    }
}
