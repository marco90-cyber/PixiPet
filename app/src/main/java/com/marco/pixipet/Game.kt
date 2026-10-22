package com.marco.pixipet

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.SharedPreferences
import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Base64
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import org.json.JSONObject
import java.util.Calendar
import kotlin.math.abs
import kotlin.random.Random

enum class Scr {
    HOME, MENU, FOOD, PLAYLIST, MAP, STATS, SETTINGS, PARK, SCHOOL, STAND, UNDER,
    ANIM, G_CATCH, G_HILO, G_SWING, G_END, DEAD, DEBUG
}

const val A_FEED = 1
const val A_BATH = 2
const val A_CLEAN = 3
const val A_PET = 4
const val A_MED = 5
const val A_STUDY = 6
const val A_WORK = 7
const val A_BALL = 8
const val A_DATE = 9
const val A_TALK = 10
const val A_ANGEL = 11

val SHELLS = longArrayOf(0xFFB3D1L, 0x9FE3C8L, 0x9CCBFFL, 0xFFE38AL, 0xC7B3FFL, 0xFFB98AL, 0xF2F2F2L)

fun homeLike(s: Scr): Boolean = when (s) {
    Scr.HOME, Scr.MENU, Scr.FOOD, Scr.PLAYLIST, Scr.MAP, Scr.SETTINGS, Scr.DEBUG, Scr.STATS -> true
    else -> false
}

class Item(
    val label: String,
    val sub: String = "",
    val enabled: Boolean = true,
    val icon: Int = -1,
    val food: Int = -1,
    val act: () -> Unit
)

class Hit(val x: Int, val y: Int, val w: Int, val h: Int, val idx: Int)
class Drop(var x: Float, var y: Float, val type: Int, val vy: Float)
class Anim(val kind: Int, val t0: Long, val dur: Long, val arg: Int, val back: Scr)

class PixiGame(val ctx: Context) {
    val prefs: SharedPreferences = ctx.getSharedPreferences("pixipet2", Context.MODE_PRIVATE)
    val rnd = Random(System.nanoTime())
    val fb = Fb(W, H)
    val hits = ArrayList<Hit>()

    var image by mutableStateOf<ImageBitmap?>(null)
    var showRename by mutableStateOf(false)
    var shellIdx by mutableStateOf(prefs.getInt("shell", 0))
    var showShell by mutableStateOf(prefs.getBoolean("showShell", true))
    var sound = prefs.getBoolean("sound", true)
    var dbgAnyHour = false

    private val tone: ToneGenerator? = try {
        ToneGenerator(AudioManager.STREAM_MUSIC, 35)
    } catch (e: Throwable) {
        null
    }

    var pet: Pet = loadPet()
    var scr by mutableStateOf(Scr.HOME)
    var cursor by mutableIntStateOf(0)
    var uiRev by mutableIntStateOf(0)
    var anim: Anim? = null
    var toast = ""
    var toastUntil = 0L
    var petX = 40f
    var facingLeft = false
    var nextMove = 0L
    var statsPage = 0
    var confirmReset = false
    var wasCalling = false
    var lastStage: Stage = pet.stage(System.currentTimeMillis())
    var dateOk = false

    // fine partita
    var endLines: List<String> = emptyList()
    var endBack = Scr.HOME

    // minigioco: caramelle
    val drops = ArrayList<Drop>()
    var bx = 40f
    var cScore = 0
    var cCandy = 0
    var cLives = 3
    var cEnd = 0L
    var cSpawn = 0L
    var cLast = 0L

    // minigioco: alto/basso
    var sRound = 1
    var sCorrect = 0
    var sNum = 5
    var sNext = 5
    var sPhase = 0
    var sT0 = 0L

    // minigioco: battuta
    var wRound = 1
    var wHits = 0
    var ballX = 78f
    var wPhase = 0
    var wT0 = 0L
    var swingUntil = 0L
    var wLast = 0L

    init {
        save()
    }

    // ---------------------------------------------------------------- utilita'

    fun now(): Long = System.currentTimeMillis()

    fun hour(): Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

    fun isNight(): Boolean = hour() >= 21 || hour() < 7

    fun away(): Boolean = pet.awayKind != 0

    private fun newPetObj(n: Long, gen: Int): Pet {
        val p = Pet(n)
        p.name = NAMES.random(rnd)
        p.hue = rnd.nextInt(5)
        p.generation = gen
        return p
    }

    private fun loadPet(): Pet {
        val s = prefs.getString("pet", null)
        if (s != null) {
            try {
                return Pet.fromJson(JSONObject(s))
            } catch (e: Throwable) {
            }
        }
        return newPetObj(System.currentTimeMillis(), 1)
    }

    fun save() {
        prefs.edit().putString("pet", pet.toJson().toString()).apply()
    }

    private fun commit() {
        pet.clamp()
        save()
        uiRev++
    }

    fun say(t: String) {
        toast = t
        toastUntil = now() + 1700L
    }

    fun beep() {
        if (!sound) return
        try {
            tone?.startTone(ToneGenerator.TONE_PROP_BEEP, 40)
        } catch (e: Throwable) {
        }
    }

    fun open(s: Scr) {
        scr = s
        cursor = 0
        confirmReset = false
    }

    // ------------------------------------------------------------------ liste

    fun items(): List<Item> {
        val st = pet.stage(now())
        val egg = st == Stage.EGG
        val gone = away()
        val asleep = pet.sleeping
        val free = !egg && !asleep && !gone
        val child = st.atLeast(Stage.CHILD)
        val teen = st.atLeast(Stage.TEEN)

        return when (scr) {
            Scr.HOME, Scr.MENU -> listOf(
                Item("FEED", enabled = free, icon = 0) { open(Scr.FOOD) },
                Item("BATH", enabled = free, icon = 1) { startAnim(A_BATH, 2800L, 0, Scr.HOME) },
                Item("CLEAN", enabled = free, icon = 2) {
                    if (pet.poops > 0) startAnim(A_CLEAN, 2400L, 0, Scr.HOME) else say("ALL CLEAN")
                },
                Item("PET", enabled = free, icon = 3) { startAnim(A_PET, 2400L, 0, Scr.HOME) },
                Item("PLAY", enabled = free && child, icon = 4) { open(Scr.PLAYLIST) },
                Item("MAP", enabled = !egg && !asleep, icon = 5) { open(Scr.MAP) },
                Item("STATS", icon = 6) { open(Scr.STATS) },
                Item(if (asleep) "WAKE" else "SLEEP", enabled = !egg && !gone, icon = 11) { toggleSleep() },
                Item("MEDS", enabled = free && pet.sick, icon = 8) {
                    if (pet.coins >= 10) pet.coins -= 10
                    startAnim(A_MED, 2600L, 0, Scr.HOME)
                },
                Item("SETUP", icon = 9) { open(Scr.SETTINGS) }
            )

            Scr.FOOD -> FOODS.mapIndexed { i, f ->
                Item(f.name, "${f.price}C", pet.coins >= f.price, food = f.kind) {
                    pet.coins -= f.price
                    startAnim(A_FEED, 2400L, i, Scr.HOME)
                }
            } + Item("BACK", icon = 10) { open(Scr.MENU) }

            Scr.PLAYLIST -> listOf(
                Item("TRICK TREAT", "E-10", pet.energy >= 15f, icon = 15) { startCatch(now()) },
                Item("HI-LO", "E-8", pet.energy >= 15f, icon = 16) { startHilo(now()) },
                Item("SWING", "E-12", pet.energy >= 15f, icon = 4) { startSwing(now()) },
                Item("BACK", icon = 10) { open(Scr.MENU) }
            )

            Scr.MAP -> listOf(
                Item("HOME") { scr = Scr.HOME; cursor = 0 },
                Item("PARK", "", !gone && child) { open(Scr.PARK) },
                Item("SCHOOL", "", !gone && child) { open(Scr.SCHOOL) },
                Item("STAND", "WORK", !gone && teen) { open(Scr.STAND) },
                if (gone) {
                    Item("CALL BACK") {
                        finishAway()
                        scr = Scr.HOME
                    }
                } else {
                    Item(
                        "PARENTS", "3-18H",
                        st.ordinal in Stage.CHILD.ordinal..Stage.TEEN.ordinal &&
                            (dbgAnyHour || hour() in 3..17)
                    ) {
                        pet.awayKind = 1
                        pet.awayUntil = now() + 6L * 3600000L
                        scr = Scr.HOME
                        commit()
                        say("BYE BYE")
                    }
                },
                Item("UNDERWORLD", "10 CANDY", !gone && teen && pet.candy >= 10) {
                    pet.candy -= 10
                    commit()
                    open(Scr.UNDER)
                },
                Item("VACATION", "200C", !gone && !egg && pet.coins >= 200) {
                    pet.coins -= 200
                    pet.awayKind = 2
                    pet.awayUntil = now() + 3600000L
                    scr = Scr.HOME
                    commit()
                    say("BON VOYAGE")
                },
                Item("BACK", icon = 10) { open(Scr.MENU) }
            )

            Scr.SETTINGS -> listOf(
                Item(if (sound) "SOUND ON" else "SOUND OFF") {
                    sound = !sound
                    prefs.edit().putBoolean("sound", sound).apply()
                },
                Item(if (prefs.getBoolean("notify", true)) "NOTIFY ON" else "NOTIFY OFF") {
                    prefs.edit().putBoolean("notify", !prefs.getBoolean("notify", true)).apply()
                },
                Item("SHELL COLOR") {
                    shellIdx = (shellIdx + 1) % SHELLS.size
                    prefs.edit().putInt("shell", shellIdx).apply()
                },
                Item(if (showShell) "SHELL ON" else "SHELL OFF") {
                    showShell = !showShell
                    prefs.edit().putBoolean("showShell", showShell).apply()
                },
                Item("RENAME") { showRename = true },
                Item("EXPORT SAVE") { exportSave() },
                Item("IMPORT SAVE") { importSave() },
                Item(if (confirmReset) "SURE? OK=NEW" else "NEW EGG") {
                    if (confirmReset) newPet() else confirmReset = true
                },
                Item("DEBUG") { open(Scr.DEBUG) },
                Item("BACK", icon = 10) { open(Scr.MENU) }
            )

            Scr.DEBUG -> listOf(
                Item("STAGE NEXT") { dbgNextStage() },
                Item("AGE +1H") { dbgShift(60, false) },
                Item("AGE +1D") { dbgShift(1440, false) },
                Item("SKIP +1H") { dbgShift(60, true) },
                Item("SKIP +1D") { dbgShift(1440, true) },
                Item("COINS +100") {
                    pet.coins += 100
                    dbg("+100 COINS")
                },
                Item("CANDY +10") {
                    pet.candy += 10
                    dbg("+10 CANDY")
                },
                Item("ALL FULL") { dbgFill() },
                Item("ALL LOW") { dbgLow() },
                Item(if (pet.sick) "CURE SICK" else "MAKE SICK") {
                    pet.sick = !pet.sick
                    pet.sickMin = 0f
                    dbg(if (pet.sick) "SICK" else "CURED")
                },
                Item("POOP +1") {
                    pet.poops = (pet.poops + 1).coerceAtMost(4)
                    dbg("POOP")
                },
                Item("TYPE NEXT") {
                    pet.pers = (pet.pers + 1) % 5
                    dbg(PERS[pet.pers])
                },
                Item(if (pet.angel) "ANGEL OFF" else "ANGEL ON") {
                    pet.angel = !pet.angel
                    pet.immortal = pet.angel
                    dbg(if (pet.angel) "ANGEL" else "MORTAL")
                },
                Item(if (dbgAnyHour) "ANY HOUR ON" else "ANY HOUR OFF") {
                    dbgAnyHour = !dbgAnyHour
                    say(if (dbgAnyHour) "PARENTS 24H" else "NORMAL HOURS")
                },
                Item("KILL") { dbgKill() },
                Item("REVIVE") { dbgRevive() },
                Item("NOTIFY TEST") {
                    Notifier.test(ctx, pet.name)
                    say("SENT")
                },
                Item("BACK", icon = 10) { open(Scr.SETTINGS) }
            )

            Scr.PARK -> listOf(
                Item("PLAY BALL", "E-10", pet.energy >= 15f) { startAnim(A_BALL, 3000L, 0, Scr.PARK) },
                Item("MEET", "DATE", teen) {
                    val chance = 0.25f + pet.happy / 200f + (if (pet.pers == 0) 0.15f else 0f)
                    dateOk = rnd.nextFloat() < chance
                    startAnim(A_DATE, 3600L, 0, Scr.PARK)
                },
                Item("BACK", icon = 10) { open(Scr.MAP) }
            )

            Scr.SCHOOL -> listOf(
                Item("STUDY", "E-8", pet.energy >= 15f) { startAnim(A_STUDY, 5000L, 0, Scr.SCHOOL) },
                Item("BACK", icon = 10) { open(Scr.MAP) }
            )

            Scr.STAND -> listOf(
                Item("WORK", "E-12", pet.energy >= 20f) { startAnim(A_WORK, 5000L, 0, Scr.STAND) },
                Item("BACK", icon = 10) { open(Scr.MAP) }
            )

            Scr.UNDER -> listOf(
                Item("TALK ANGEL") { startAnim(A_TALK, 2600L, 0, Scr.UNDER) },
                Item("TALK DEMON") { startAnim(A_TALK, 2600L, 1, Scr.UNDER) },
                Item("BE ANGEL", "30 CANDY", !pet.angel && teen && pet.candy >= 30) {
                    pet.candy -= 30
                    startAnim(A_ANGEL, 3200L, 0, Scr.UNDER)
                },
                Item("LEAVE", icon = 10) { open(Scr.MAP) }
            )

            Scr.G_HILO -> listOf(
                Item("LOW") { hiloInput(0) },
                Item("HIGH") { hiloInput(2) }
            )

            Scr.G_SWING -> listOf(Item("SWING!") { swingHit() })

            Scr.G_CATCH -> listOf(
                Item("< LEFT") { bx = (bx - 12f).coerceIn(6f, 74f) },
                Item("RIGHT >") { bx = (bx + 12f).coerceIn(6f, 74f) }
            )

            Scr.G_END -> listOf(
                Item("OK") {
                    scr = endBack
                    cursor = 0
                }
            )

            Scr.DEAD -> listOf(Item("NEW EGG") { newPet() })

            else -> emptyList()
        }
    }

    fun activate() {
        val its = items()
        if (its.isEmpty()) return
        cursor = cursor.coerceIn(0, its.size - 1)
        val item = its[cursor]
        if (item.enabled) item.act() else say("NOT NOW")
    }

    private fun toggleSleep() {
        if (pet.sleeping) {
            pet.sleeping = false
        } else if (pet.energy >= 90f) {
            say("NOT TIRED")
            return
        } else {
            pet.sleeping = true
        }
        scr = Scr.HOME
        commit()
    }

    // ------------------------------------------------------------------ input

    fun press(b: Int) {
        beep()
        if (pet.dead || scr == Scr.DEAD) {
            if (b == 1) newPet()
            return
        }
        when (scr) {
            Scr.ANIM -> {}
            Scr.STATS -> if (b == 1) open(Scr.MENU) else statsPage = (statsPage + 1) % 2
            Scr.G_CATCH -> {
                if (b == 0) bx -= 9f
                if (b == 2) bx += 9f
                bx = bx.coerceIn(6f, 74f)
            }
            Scr.G_HILO -> hiloInput(b)
            Scr.G_SWING -> if (b == 1) swingHit()
            Scr.G_END -> if (b == 1) {
                scr = endBack
                cursor = 0
            }
            else -> {
                val n = items().size
                if (n == 0) return
                when (b) {
                    0 -> cursor = (cursor - 1 + n) % n
                    2 -> cursor = (cursor + 1) % n
                    else -> activate()
                }
            }
        }
    }

    fun tapScreen(fx: Float, fy: Float) {
        val x = (fx * W).toInt().coerceIn(0, W - 1)
        val y = (fy * H).toInt().coerceIn(0, H - 1)
        if (pet.dead || scr == Scr.DEAD) {
            press(1)
            return
        }
        when (scr) {
            Scr.ANIM -> {}
            Scr.STATS -> {}
            Scr.HOME, Scr.MENU, Scr.FOOD, Scr.PLAYLIST, Scr.MAP, Scr.SETTINGS, Scr.DEBUG -> {
                val st = pet.stage(now())
                if (!pet.sleeping && !away() && st != Stage.EGG) {
                    beep()
                    startAnim(A_PET, 2400L, 0, scr)
                }
            }
            Scr.G_CATCH -> bx = x.toFloat().coerceIn(6f, 74f)
            Scr.G_HILO -> hiloInput(if (x < W / 2) 0 else 2)
            Scr.G_SWING -> swingHit()
            Scr.G_END -> press(1)
            else -> {
                for (h in hits) {
                    if (x >= h.x && x < h.x + h.w && y >= h.y && y < h.y + h.h) {
                        cursor = h.idx
                        beep()
                        activate()
                        return
                    }
                }
            }
        }
    }

    fun dragScreen(fx: Float) {
        if (scr == Scr.G_CATCH) bx = (fx * W).coerceIn(6f, 74f)
    }

    // ------------------------------------------------------------ rinomina/salva

    fun rename(t: String) {
        val clean = t.trim().uppercase().filter { it.isLetterOrDigit() }.take(8)
        if (clean.isNotEmpty()) pet.name = clean
        commit()
    }

    private fun exportSave() {
        try {
            val s = Base64.encodeToString(pet.toJson().toString().toByteArray(), Base64.NO_WRAP)
            val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("PixiPet", s))
            say("COPIED")
        } catch (e: Throwable) {
            say("ERROR")
        }
    }

    private fun importSave() {
        try {
            val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val t = cm.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
            val j = JSONObject(String(Base64.decode(t.trim(), Base64.DEFAULT)))
            pet = Pet.fromJson(j)
            lastStage = pet.stage(now())
            scr = Scr.HOME
            commit()
            say("LOADED")
        } catch (e: Throwable) {
            say("BAD CODE")
        }
    }

    fun newPet() {
        val g = if (pet.dead) pet.generation + 1 else 1
        pet = newPetObj(now(), g)
        scr = Scr.HOME
        cursor = 0
        anim = null
        lastStage = Stage.EGG
        petX = 40f
        commit()
    }

    // ---------------------------------------------------------------- animazioni

    fun startAnim(kind: Int, dur: Long, arg: Int, back: Scr) {
        anim = Anim(kind, now(), dur, arg, back)
        scr = Scr.ANIM
    }

    private fun finishAnim() {
        val a = anim ?: return
        when (a.kind) {
            A_FEED -> {
                val f = FOODS[a.arg.coerceIn(0, FOODS.size - 1)]
                pet.hunger += f.hunger
                pet.happy += f.happy
                pet.weight += f.weight
                pet.cFood++
            }
            A_BATH -> {
                pet.hygiene = 100f
                pet.happy -= 3f
            }
            A_CLEAN -> {
                pet.poops = 0
                pet.hygiene += 10f
            }
            A_PET -> {
                pet.happy += 10f
                pet.cPet++
            }
            A_MED -> {
                pet.sick = false
                pet.sickMin = 0f
                pet.happy -= 5f
            }
            A_STUDY -> {
                pet.smarts += 12f
                pet.energy -= 8f
                pet.hunger -= 5f
                pet.cStudy++
            }
            A_WORK -> {
                val gain = 12 + rnd.nextInt(10) + (pet.smarts / 10f).toInt()
                pet.coins += gain
                pet.energy -= 12f
                pet.hygiene -= 6f
                say("+$gain COINS")
            }
            A_BALL -> {
                pet.active += 12f
                pet.happy += 8f
                pet.energy -= 10f
                pet.hunger -= 6f
                pet.cPlay++
            }
            A_DATE -> {
                if (dateOk) {
                    pet.dates++
                    pet.happy += 25f
                    pet.coins += 10
                    say(if (pet.dates == 3) "HEARTBREAKER!" else "DATE OK!")
                } else {
                    pet.happy -= 10f
                    say("REJECTED")
                }
            }
            A_TALK -> {
                if (a.arg == 0) {
                    pet.happy += 20f
                } else {
                    pet.coins += 15
                    pet.happy += 5f
                    pet.smarts += 3f
                }
            }
            A_ANGEL -> {
                pet.angel = true
                pet.immortal = true
                say("IMMORTAL!")
            }
        }
        anim = null
        scr = a.back
        cursor = 0
        commit()
    }

    private fun finishAway() {
        when (pet.finishAway()) {
            1 -> say("BACK HOME")
            2 -> say("WELCOME BACK")
        }
        commit()
    }

    // ------------------------------------------------------------------ debug

    private val stageStarts = floatArrayOf(
        0f, Cfg.EGG_MIN, Cfg.BABY_MIN, Cfg.CHILD_MIN, Cfg.TEEN_MIN, Cfg.ADULT_MIN
    )

    private fun dbg(msg: String) {
        commit()
        say(msg)
    }

    private fun dbgNextStage() {
        if (pet.dead) {
            say("DEAD")
            return
        }
        val next = (pet.stage(now()).ordinal + 1) % Stage.values().size
        pet.bornAt = now() - (stageStarts[next] * 60000f).toLong() - 1000L
        if (next < Stage.TEEN.ordinal) pet.pers = -1
        dbg("STAGE " + Stage.values()[next].label)
    }

    /** simulate=false: cambia solo l'eta'. simulate=true: fa passare davvero il tempo (le barre calano). */
    private fun dbgShift(min: Int, simulate: Boolean) {
        val ms = min * 60000L
        pet.bornAt -= ms
        if (simulate) {
            pet.lastTick -= ms
            if (pet.awayKind != 0) pet.awayUntil -= ms
        }
        dbg(if (simulate) "TIME SKIPPED" else "AGE UP")
    }

    private fun dbgFill() {
        pet.hunger = 100f
        pet.hygiene = 100f
        pet.happy = 100f
        pet.energy = 100f
        pet.smarts = 100f
        pet.active = 100f
        pet.sick = false
        pet.sickMin = 0f
        pet.starveMin = 0f
        pet.poops = 0
        pet.sleeping = false
        dbg("ALL FULL")
    }

    private fun dbgLow() {
        pet.hunger = 15f
        pet.hygiene = 15f
        pet.happy = 15f
        pet.energy = 15f
        dbg("ALL LOW")
    }

    private fun dbgKill() {
        pet.dead = true
        pet.cause = "DEBUG"
        pet.diedAt = now()
        commit()
    }

    private fun dbgRevive() {
        pet.dead = false
        pet.cause = ""
        pet.sick = false
        pet.sickMin = 0f
        pet.starveMin = 0f
        pet.hunger = 60f
        pet.hygiene = 60f
        pet.happy = 60f
        pet.energy = 60f
        pet.lastTick = now()
        if (pet.ageMin(now()) >= Cfg.DEATH_MIN - 1f) {
            pet.bornAt = now() - (Cfg.ADULT_MIN * 60000f).toLong()
        }
        if (scr == Scr.DEAD) scr = Scr.HOME
        dbg("REVIVED")
    }

    // ----------------------------------------------------- ciclo di vita app

    fun onResume() {
        pet = loadPet()
        lastStage = pet.stage(now())
        Notifier.check(ctx, pet, now(), false)
    }

    fun onPause() {
        save()
        Notifier.onLeave(ctx, pet, now())
        Notifier.schedule(ctx, Notifier.nextDelayMs(pet, now()))
    }

    // ------------------------------------------------------------- minigioco 1

    private fun startCatch(n: Long) {
        scr = Scr.G_CATCH
        drops.clear()
        bx = 40f
        cScore = 0
        cCandy = 0
        cLives = 3
        cEnd = n + 30000L
        cSpawn = n + 500L
        cLast = n
    }

    private fun updateCatch(n: Long) {
        val dt = (n - cLast).coerceIn(0L, 100L) / 1000f
        cLast = n
        if (n >= cSpawn) {
            val r = rnd.nextInt(100)
            val type = when {
                r < 60 -> 0
                r < 75 -> 1
                else -> 2
            }
            drops.add(Drop(6f + rnd.nextInt(68), -4f, type, 24f + rnd.nextInt(16)))
            cSpawn = n + 450L + rnd.nextInt(400)
        }
        val iter = drops.iterator()
        while (iter.hasNext()) {
            val d = iter.next()
            d.y += d.vy * dt
            if (d.y >= 64f && d.y <= 74f && abs(d.x - bx) <= 9f) {
                when (d.type) {
                    0 -> cScore += 1
                    1 -> {
                        cCandy += 1
                        cScore += 2
                    }
                    else -> cLives -= 1
                }
                iter.remove()
                beep()
            } else if (d.y > 82f) {
                iter.remove()
            }
        }
        if (n >= cEnd || cLives <= 0) {
            pet.coins += cScore
            pet.candy += cCandy
            pet.happy += minOf(20f, cScore * 1.5f)
            pet.energy -= 10f
            pet.hunger -= 5f
            pet.games++
            endLines = listOf("COINS +$cScore", "CANDY +$cCandy")
            finishGame()
        }
    }

    private fun finishGame() {
        endBack = Scr.HOME
        scr = Scr.G_END
        commit()
    }

    // ------------------------------------------------------------- minigioco 2

    private fun pickNext(): Int {
        var n: Int
        do {
            n = 1 + rnd.nextInt(9)
        } while (n == sNum)
        return n
    }

    private fun startHilo(n: Long) {
        scr = Scr.G_HILO
        sRound = 1
        sCorrect = 0
        sNum = 1 + rnd.nextInt(9)
        sNext = pickNext()
        sPhase = 0
        sT0 = n
    }

    private fun hiloInput(b: Int) {
        if (sPhase != 0 || b == 1) return
        val high = b == 2
        val ok = (sNext > sNum) == high
        if (ok) sCorrect++
        sPhase = if (ok) 1 else 2
        sT0 = now()
    }

    private fun updateHilo(n: Long) {
        when (sPhase) {
            1, 2 -> if (n - sT0 > 1100L) {
                if (sRound >= 5) {
                    sPhase = 3
                    sT0 = n
                } else {
                    sRound++
                    sNum = sNext
                    sNext = pickNext()
                    sPhase = 0
                }
            }
            3 -> if (n - sT0 > 1500L) {
                pet.coins += sCorrect * 4
                pet.smarts += 6f * sCorrect
                pet.happy += 3f * sCorrect
                pet.energy -= 8f
                pet.games++
                endLines = listOf("RIGHT $sCorrect/5", "COINS +${sCorrect * 4}")
                finishGame()
            }
        }
    }

    // ------------------------------------------------------------- minigioco 3

    private fun startSwing(n: Long) {
        scr = Scr.G_SWING
        wRound = 1
        wHits = 0
        ballX = 78f
        wPhase = 0
        swingUntil = 0L
        wLast = n
    }

    private fun swingHit() {
        if (wPhase != 0) return
        val n = now()
        swingUntil = n + 250L
        if (ballX in 40f..52f) {
            wHits++
            wPhase = 1
        } else {
            wPhase = 2
        }
        wT0 = n
    }

    private fun updateSwing(n: Long) {
        val dt = (n - wLast).coerceIn(0L, 100L) / 1000f
        wLast = n
        when (wPhase) {
            0 -> {
                ballX -= (20f + 4f * wRound) * dt
                if (ballX < 30f) {
                    wPhase = 2
                    wT0 = n
                }
            }
            1, 2 -> if (n - wT0 > 900L) {
                if (wRound >= 5) {
                    wPhase = 3
                    wT0 = n
                } else {
                    wRound++
                    ballX = 78f
                    wPhase = 0
                }
            }
            3 -> if (n - wT0 > 1500L) {
                pet.coins += wHits * 5
                pet.active += 6f * wHits + 4f
                pet.happy += 4f * wHits
                pet.energy -= 12f
                pet.hunger -= 6f
                pet.cPlay++
                pet.games++
                endLines = listOf("HITS $wHits/5", "COINS +${wHits * 5}")
                finishGame()
            }
        }
    }

    // --------------------------------------------------------------- ciclo

    private fun onStage(st: Stage, n: Long) {
        if (st != Stage.EGG) {
            say(if (lastStage == Stage.EGG) "HATCHED!" else "GREW UP!")
            beep()
        }
        lastStage = st
        commit()
    }

    fun update(n: Long) {
        var changed = false
        var guard = 0
        while (n - pet.lastTick >= 60000L && guard < 300000) {
            pet.step(rnd)
            guard++
            changed = true
        }
        if (changed) commit()

        if (pet.awayKind != 0 && n >= pet.awayUntil) finishAway()

        val st = pet.stage(n)
        if (st != lastStage) onStage(st, n)
        if (pet.pers < 0 && st.atLeast(Stage.TEEN)) {
            pet.assignPers()
            say(PERS[pet.pers])
            commit()
        }
        if (pet.dead && scr != Scr.DEAD) {
            anim = null
            scr = Scr.DEAD
        }
        if (toast.isNotEmpty() && n > toastUntil) toast = ""

        val a = anim
        if (a != null && n - a.t0 >= a.dur) finishAnim()

        val calling = pet.calling && !pet.sleeping && !pet.dead && st != Stage.EGG && !away()
        if (calling && !wasCalling) beep()
        wasCalling = calling

        when (scr) {
            Scr.G_CATCH -> updateCatch(n)
            Scr.G_HILO -> updateHilo(n)
            Scr.G_SWING -> updateSwing(n)
            else -> {}
        }

        if (homeLike(scr) && anim == null && !pet.sleeping && !pet.dead && !away() &&
            st != Stage.EGG && n > nextMove
        ) {
            if (rnd.nextInt(100) < 65) {
                val dir = if (facingLeft) -1f else 1f
                var nx = petX + dir * 2f
                if (nx < 22f || nx > 58f) {
                    facingLeft = !facingLeft
                    nx = petX - dir * 2f
                }
                petX = nx
            } else if (rnd.nextInt(100) < 30) {
                facingLeft = !facingLeft
            }
            nextMove = n + 500L + rnd.nextInt(800)
        }
    }

    fun title(): String = when (scr) {
        Scr.HOME, Scr.MENU -> "MENU"
        Scr.FOOD -> "FEED"
        Scr.PLAYLIST -> "PLAY"
        Scr.MAP -> "MAP"
        Scr.SETTINGS -> "SETUP"
        Scr.DEBUG -> "DEBUG"
        Scr.STATS -> "STATS"
        Scr.PARK -> "PARK"
        Scr.SCHOOL -> "SCHOOL"
        Scr.STAND -> "STAND"
        Scr.UNDER -> "UNDERWORLD"
        Scr.G_CATCH -> "TRICK TREAT"
        Scr.G_HILO -> "HI-LO"
        Scr.G_SWING -> "SWING"
        Scr.G_END -> "RESULT"
        Scr.DEAD -> "R.I.P."
        Scr.ANIM -> "..."
    }

    fun canGoBack(): Boolean = when (scr) {
        Scr.MENU, Scr.FOOD, Scr.PLAYLIST, Scr.MAP, Scr.SETTINGS, Scr.STATS, Scr.DEBUG,
        Scr.PARK, Scr.SCHOOL, Scr.STAND, Scr.UNDER -> true
        else -> false
    }

    fun back() {
        when (scr) {
            Scr.MENU, Scr.FOOD, Scr.PLAYLIST, Scr.MAP, Scr.SETTINGS, Scr.STATS -> open(Scr.HOME)
            Scr.DEBUG -> open(Scr.SETTINGS)
            Scr.PARK, Scr.SCHOOL, Scr.STAND, Scr.UNDER -> open(Scr.MAP)
            else -> {}
        }
    }

    /** Tocco su una voce del pannello. */
    fun pick(i: Int) {
        cursor = i
        beep()
        activate()
    }

    fun frame(n: Long) {
        update(n)
        render(n)
    }
}
