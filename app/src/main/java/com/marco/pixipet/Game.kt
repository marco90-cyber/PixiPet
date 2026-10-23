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
    HOME, MENU, FOOD, PLAYLIST, MAP, STATS, BG, SETTINGS, PARK, SCHOOL, STAND, UNDER,
    ANIM, G_CATCH, G_HILO, G_SWING, G_BOUNCE, G_MEM, G_END, DEAD, DEBUG, INFO, NAME, EGG
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
const val A_REFUSE = 12

val SHELLS = longArrayOf(0xFFB3D1L, 0x9FE3C8L, 0x9CCBFFL, 0xFFE38AL, 0xC7B3FFL, 0xFFB98AL, 0xF2F2F2L)

fun homeLike(s: Scr): Boolean = when (s) {
    Scr.HOME, Scr.MENU, Scr.FOOD, Scr.PLAYLIST, Scr.MAP, Scr.SETTINGS, Scr.DEBUG, Scr.STATS, Scr.BG, Scr.INFO -> true
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
    var nameBuf by mutableStateOf("")
    var eggSel = 0
    var bgOwned = prefs.getInt("bgOwned", 1)
    var bgSel = -1
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

    // minigioco: rimbalzo
    var rx = 40f
    var ry = 24f
    var rvx = 20f
    var rvy = 34f
    var rHits = 0
    var rLives = 3
    var rEnd = 0L
    var rLast = 0L
    var rServe = 0L

    // minigioco: memoria
    val memSeq = ArrayList<Int>()
    var memPhase = 0
    var memT0 = 0L
    var memIn = 0
    var memLit = -1
    var memLitT = 0L

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
        if (!pet.named) scr = Scr.NAME else if (!pet.eggPicked) scr = Scr.EGG
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
        p.named = false
        p.eggPicked = false
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
        bgSel = -1
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
                Item("MANGIA", enabled = free, icon = 0) { open(Scr.FOOD) },
                Item("BAGNO", enabled = free, icon = 1) {
                    if (pet.hygiene >= 97f) refuse("E' GIA' PULITO") else startAnim(A_BATH, 2800L, 0, Scr.HOME)
                },
                Item("PULISCI", enabled = free, icon = 2) {
                    if (pet.poops > 0) startAnim(A_CLEAN, 2400L, 0, Scr.HOME) else say("TUTTO PULITO")
                },
                Item("COCCOLE", enabled = free, icon = 3) {
                    if (pet.happy >= 97f) refuse("E' GIA' FELICE") else startAnim(A_PET, 2400L, 0, Scr.HOME)
                },
                Item("GIOCA", enabled = free && child, icon = 4) { open(Scr.PLAYLIST) },
                Item("MAPPA", enabled = !egg && !asleep, icon = 5) { open(Scr.MAP) },
                Item("SFONDO", icon = 17) { open(Scr.BG) },
                Item(if (asleep) "SVEGLIA" else "DORMI", enabled = !egg && !gone, icon = 11) { toggleSleep() },
                Item("CURA", enabled = free && pet.sick, icon = 8) {
                    if (pet.coins >= 10) pet.coins -= 10
                    startAnim(A_MED, 2600L, 0, Scr.HOME)
                },
                Item("OPZIONI", icon = 9) { open(Scr.SETTINGS) }
            )

            Scr.FOOD -> FOODS.mapIndexed { i, f ->
                Item(f.name, "${f.price}C", pet.coins >= f.price, food = f.kind) {
                    if (pet.hunger >= 97f) {
                        refuse("NON HA FAME")
                    } else if (f.happy >= f.hunger && pet.happy >= 97f) {
                        refuse("E' GIA' FELICE")
                    } else {
                        pet.coins -= f.price
                        startAnim(A_FEED, 2400L, i, Scr.HOME)
                    }
                }
            } + Item("INDIETRO", icon = 10) { open(Scr.MENU) }

            Scr.PLAYLIST -> listOf(
                Item("DOLCETTI", "E-10", pet.energy >= 15f, icon = 15) {
                    if (pet.happy >= 97f) refuse("E' GIA' FELICE") else startCatch(now())
                },
                Item("RIMBALZO", "E-8", pet.energy >= 15f, icon = 18) {
                    if (pet.active >= 97f) refuse("E' GIA' IN FORMA") else startBounce(now())
                },
                Item("MEMORIA", "E-8", pet.energy >= 15f, icon = 16) {
                    if (pet.smarts >= 97f) refuse("SA GIA' TUTTO") else startMem(now())
                },
                Item("BATTUTA", "E-12", pet.energy >= 15f, icon = 4) {
                    if (pet.active >= 97f) refuse("E' GIA' IN FORMA") else startSwing(now())
                },
                Item("INDIETRO", icon = 10) { open(Scr.MENU) }
            )

            Scr.MAP -> listOf(
                Item("CASA") { scr = Scr.HOME; cursor = 0 },
                Item("PARCO", "", !gone && child) { open(Scr.PARK) },
                Item("SCUOLA", "", !gone && child) { open(Scr.SCHOOL) },
                Item("BANCARELLA", "LAVORO", !gone && teen) { open(Scr.STAND) },
                if (gone) {
                    Item("RICHIAMA") {
                        callBack()
                        scr = Scr.HOME
                    }
                } else {
                    Item(
                        "GENITORI", "3-18",
                        st.ordinal in Stage.CHILD.ordinal..Stage.TEEN.ordinal &&
                            (dbgAnyHour || hour() in 3..17)
                    ) {
                        pet.awayKind = 1
                        pet.awayUntil = now() + 6L * 3600000L
                        scr = Scr.HOME
                        commit()
                        say("CIAO CIAO")
                    }
                },
                Item("ALDILÀ", "10 CARAM.", !gone && teen && pet.candy >= 10) {
                    pet.candy -= 10
                    commit()
                    open(Scr.UNDER)
                },
                Item("VACANZA", "200C", !gone && !egg && pet.coins >= 200) {
                    pet.coins -= 200
                    pet.awayKind = 2
                    pet.awayUntil = now() + 3600000L
                    scr = Scr.HOME
                    commit()
                    say("BUON VIAGGIO")
                },
                Item("INDIETRO", icon = 10) { open(Scr.MENU) }
            )

            Scr.BG -> bgItems()

            Scr.NAME -> emptyList()

            Scr.EGG -> PERS.indices.map { i ->
                Item("UOVO ${EGG_COLORS[i]}", PERS[i]) { eggSel = i }
            } + Item("SCEGLI QUESTO UOVO") { pickEgg(eggView()) }

            Scr.SETTINGS -> listOf(
                Item(if (sound) "SUONO: SÌ" else "SUONO: NO") {
                    sound = !sound
                    prefs.edit().putBoolean("sound", sound).apply()
                },
                Item(if (prefs.getBoolean("notify", true)) "NOTIFICHE: SÌ" else "NOTIFICHE: NO") {
                    prefs.edit().putBoolean("notify", !prefs.getBoolean("notify", true)).apply()
                },
                Item("COLORE GUSCIO") {
                    shellIdx = (shellIdx + 1) % SHELLS.size
                    prefs.edit().putInt("shell", shellIdx).apply()
                },
                Item(if (showShell) "GUSCIO: SÌ" else "GUSCIO: NO") {
                    showShell = !showShell
                    prefs.edit().putBoolean("showShell", showShell).apply()
                },
                Item("ESPORTA DATI") { exportSave() },
                Item("IMPORTA DATI") { importSave() },
                Item(if (confirmReset) "SICURO? TOCCA ANCORA" else "NUOVO UOVO") {
                    if (confirmReset) newPet() else confirmReset = true
                },
                Item("INFORMAZIONI") { open(Scr.INFO) },
                *(if (pet.name == "TEST1990") arrayOf(Item("DEBUG") { open(Scr.DEBUG) }) else arrayOf<Item>()),
                Item("INDIETRO", icon = 10) { open(Scr.MENU) }
            )

            Scr.DEBUG -> listOf(
                Item("MODIFICA NOME") { showRename = true },
                Item("FASE SUCCESSIVA") { dbgNextStage() },
                Item("ETÀ +1 ORA") { dbgShift(60, false) },
                Item("ETÀ +1 GIORNO") { dbgShift(1440, false) },
                Item("ETÀ -1 ORA") { dbgShift(-60, false) },
                Item("ETÀ -1 GIORNO") { dbgShift(-1440, false) },
                Item("SALTA +1 ORA") { dbgShift(60, true) },
                Item("SALTA +1 GIORNO") { dbgShift(1440, true) },
                Item("MONETE +100") {
                    pet.coins += 100
                    dbg("+100 MONETE")
                },
                Item("MONETE -100") {
                    pet.coins -= 100
                    dbg("-100 MONETE")
                },
                Item("CARAMELLE -10") {
                    pet.candy -= 10
                    dbg("-10 CARAMELLE")
                },
                Item("STATISTICHE +20") { dbgStats(20f) },
                Item("STATISTICHE -20") { dbgStats(-20f) },
                Item("CARAMELLE +10") {
                    pet.candy += 10
                    dbg("+10 CARAMELLE")
                },
                Item("TUTTO PIENO") { dbgFill() },
                Item("TUTTO BASSO") { dbgLow() },
                Item(if (pet.sick) "GUARISCI" else "AMMALA") {
                    pet.sick = !pet.sick
                    pet.sickMin = 0f
                    dbg(if (pet.sick) "MALATO" else "GUARITO")
                },
                Item("CACCA +1") {
                    pet.poops = (pet.poops + 1).coerceAtMost(4)
                    dbg("CACCA")
                },
                Item("CARATTERE SUCC.") {
                    pet.pers = (pet.pers + 1) % 5
                    dbg(PERS[pet.pers])
                },
                Item(if (pet.angel) "ANGELO: NO" else "ANGELO: SÌ") {
                    pet.angel = !pet.angel
                    pet.immortal = pet.angel
                    dbg(if (pet.angel) "ANGELO" else "MORTALE")
                },
                Item(if (dbgAnyHour) "ORARIO LIBERO: SÌ" else "ORARIO LIBERO: NO") {
                    dbgAnyHour = !dbgAnyHour
                    say(if (dbgAnyHour) "GENITORI 24H" else "ORARI NORMALI")
                },
                Item("UCCIDI") { dbgKill() },
                Item("RIVIVI") { dbgRevive() },
                Item("PROVA NOTIFICA") {
                    Notifier.test(ctx, pet.name)
                    say("INVIATA")
                },
                Item("INDIETRO", icon = 10) { open(Scr.SETTINGS) }
            )

            Scr.PARK -> listOf(
                Item("GIOCA A PALLA", "E-10", pet.energy >= 15f) {
                    if (pet.active >= 97f) refuse("E' GIA' IN FORMA") else startAnim(A_BALL, 3000L, 0, Scr.PARK)
                },
                Item("INCONTRA", "DATE", teen) {
                    if (pet.happy >= 97f) {
                        refuse("E' GIA' FELICE")
                    } else {
                        val chance = 0.25f + pet.happy / 200f + (if (pet.pers == 0) 0.15f else 0f)
                        dateOk = rnd.nextFloat() < chance
                        startAnim(A_DATE, 3600L, 0, Scr.PARK)
                    }
                },
                Item("INDIETRO", icon = 10) { open(Scr.MAP) }
            )

            Scr.SCHOOL -> listOf(
                Item("SCUOLA 2 ORE", "MENTE 100", pet.energy >= 30f) {
                    if (pet.smarts >= 97f) refuse("SA GIA' TUTTO") else startSchool()
                },
                Item("STUDIA", "E-8", pet.energy >= 15f) {
                    if (pet.smarts >= 97f) refuse("SA GIA' TUTTO") else startAnim(A_STUDY, 5000L, 0, Scr.SCHOOL)
                },
                Item("INDIETRO", icon = 10) { open(Scr.MAP) }
            )

            Scr.STAND -> listOf(
                Item("LAVORA", "5S E-12", pet.energy >= 20f) { startAnim(A_WORK, 5000L, 0, Scr.STAND) },
                Item("LAVORA 10 MIN", "E-15", pet.energy >= 30f) { startWork(10) },
                Item("LAVORA 30 MIN", "E-25", pet.energy >= 40f) { startWork(30) },
                Item("INDIETRO", icon = 10) { open(Scr.MAP) }
            )

            Scr.UNDER -> listOf(
                Item("PARLA ANGELO") {
                    if (pet.happy >= 97f) refuse("E' GIA' FELICE") else startAnim(A_TALK, 2600L, 0, Scr.UNDER)
                },
                Item("PARLA DEMONE") { startAnim(A_TALK, 2600L, 1, Scr.UNDER) },
                Item("DIVENTA ANGELO", "30 CARAM.", !pet.angel && teen && pet.candy >= 30) {
                    pet.candy -= 30
                    startAnim(A_ANGEL, 3200L, 0, Scr.UNDER)
                },
                Item("ESCI", icon = 10) { open(Scr.MAP) }
            )

            Scr.G_HILO -> listOf(
                Item("BASSO") { hiloInput(0) },
                Item("ALTO") { hiloInput(2) }
            )

            Scr.G_BOUNCE -> listOf(
                Item("< SINISTRA") { bx = (bx - 12f).coerceIn(10f, 70f) },
                Item("DESTRA >") { bx = (bx + 12f).coerceIn(10f, 70f) }
            )

            Scr.G_MEM -> listOf(
                Item("ROSSO") { memInput(0) },
                Item("VERDE") { memInput(1) },
                Item("BLU") { memInput(2) },
                Item("GIALLO") { memInput(3) }
            )

            Scr.INFO -> INFO_LINES.map { Item(it) {} } + Item("INDIETRO", icon = 10) { open(Scr.SETTINGS) }

            Scr.G_SWING -> listOf(Item("COLPISCI!") { swingHit() })

            Scr.G_CATCH -> listOf(
                Item("< SINISTRA") { bx = (bx - 12f).coerceIn(6f, 74f) },
                Item("DESTRA >") { bx = (bx + 12f).coerceIn(6f, 74f) }
            )

            Scr.G_END -> listOf(
                Item("OK") {
                    scr = endBack
                    cursor = 0
                }
            )

            Scr.DEAD -> listOf(Item("NUOVO UOVO") { newPet() })

            else -> emptyList()
        }
    }

    fun activate() {
        val its = items()
        if (its.isEmpty()) return
        cursor = cursor.coerceIn(0, its.size - 1)
        val item = its[cursor]
        if (item.enabled) item.act() else say("ORA NO")
    }

    private fun toggleSleep() {
        if (pet.sleeping) {
            pet.sleeping = false
        } else if (pet.energy >= 97f) {
            say("NON HA SONNO")
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
            Scr.NAME -> if (b == 1) nameOk()
            Scr.G_HILO -> hiloInput(b)
            Scr.G_BOUNCE -> {
                if (b == 0) bx -= 9f
                if (b == 2) bx += 9f
                bx = bx.coerceIn(10f, 70f)
            }
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
            Scr.HOME, Scr.MENU, Scr.FOOD, Scr.PLAYLIST, Scr.MAP, Scr.SETTINGS, Scr.DEBUG, Scr.BG, Scr.INFO -> {
                val st = pet.stage(now())
                if (!pet.sleeping && !away() && st != Stage.EGG) {
                    beep()
                    startAnim(A_PET, 2400L, 0, scr)
                }
            }
            Scr.G_CATCH -> bx = x.toFloat().coerceIn(6f, 74f)
            Scr.G_HILO -> hiloInput(if (x < W / 2) 0 else 2)
            Scr.G_BOUNCE -> bx = x.toFloat().coerceIn(10f, 70f)
            Scr.G_MEM -> memInput((if (x >= W / 2) 1 else 0) + (if (y >= H / 2) 2 else 0))
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
        if (scr == Scr.G_BOUNCE) bx = (fx * W).coerceIn(10f, 70f)
    }

    // ------------------------------------------------------------ rinomina/salva

    /** Primo nome del pet: dopo la scelta parte la vita dell'uovo. */
    fun setFirstName(t: String) {
        val clean = t.trim().uppercase().filter { it.isLetterOrDigit() }.take(8)
        if (clean.isEmpty()) return
        pet.name = clean
        pet.named = true
        scr = Scr.EGG
        cursor = 0
        commit()
    }

    /** Scelta dell'uovo: decide colore, forma e carattere. La vita parte da qui. */
    fun pickEgg(i: Int) {
        val k = i.coerceIn(0, 4)
        pet.hue = k
        pet.pers = k
        pet.eggPicked = true
        pet.bornAt = now()
        pet.lastTick = pet.bornAt
        scr = Scr.HOME
        cursor = 0
        commit()
    }

    fun nameKey(c: Char) {
        if (nameBuf.length < 8) nameBuf += c
        beep()
    }

    fun nameDel() {
        nameBuf = nameBuf.dropLast(1)
        beep()
    }

    fun nameOk() {
        if (nameBuf.isEmpty()) say("SCRIVI UN NOME") else setFirstName(nameBuf)
    }

    fun eggView(): Int = if (cursor in 0..4) cursor else eggSel

    private fun owned(i: Int) = i == pet.bg || (bgOwned shr i) and 1 == 1

    private fun bgItems(): List<Item> = ROOM_NAMES.indices.map { i ->
        Item(
            ROOM_NAMES[i],
            if (i == pet.bg) "ATTIVO" else if (owned(i)) "TUO" else "50C",
            owned(i) || pet.coins >= 50,
            icon = 17
        ) {
            when {
                i == pet.bg -> say("GIA' ATTIVO")
                owned(i) -> {
                    pet.bg = i
                    say("SFONDO CAMBIATO")
                    commit()
                }
                bgSel != i -> {
                    bgSel = i
                    say("TOCCA ANCORA")
                }
                else -> {
                    pet.coins -= 50
                    bgOwned = bgOwned or (1 shl i)
                    prefs.edit().putInt("bgOwned", bgOwned).apply()
                    pet.bg = i
                    bgSel = -1
                    say("SFONDO CAMBIATO")
                    commit()
                }
            }
        }
    } + Item("INDIETRO", icon = 10) { open(Scr.MENU) }

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
            say("COPIATO")
        } catch (e: Throwable) {
            say("ERRORE")
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
            say("CARICATO")
        } catch (e: Throwable) {
            say("CODICE ERRATO")
        }
    }

    fun newPet() {
        val g = if (pet.dead) pet.generation + 1 else 1
        pet = newPetObj(now(), g)
        scr = Scr.NAME
        nameBuf = ""
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
                pet.hunger += f.hunger * (if (pet.pers == 3) 1.25f else 1f)
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
                pet.happy += if (pet.pers == 0) 15f else 10f
                pet.cPet++
            }
            A_MED -> {
                pet.sick = false
                pet.sickMin = 0f
                pet.happy -= 5f
            }
            A_STUDY -> {
                pet.smarts += if (pet.pers == 1) 18f else 12f
                pet.energy -= 8f
                pet.hunger -= 5f
                pet.cStudy++
            }
            A_WORK -> {
                var gain = 12 + rnd.nextInt(10) + (pet.smarts / 10f).toInt()
                if (pet.pers == 4) gain = (gain * 1.2f).toInt()
                pet.coins += gain
                pet.energy -= 12f
                pet.hygiene -= 6f
                say("+$gain MONETE")
            }
            A_BALL -> {
                pet.active += if (pet.pers == 2) 18f else 12f
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
                    say(if (pet.dates == 3) "SPEZZACUORI!" else "DATE RIUSCITO!")
                } else {
                    pet.happy -= 10f
                    say("RIFIUTATO")
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
                say("IMMORTALE!")
            }
        }
        // Finché non può andare a lavorare (prima dell'adolescenza) ogni attività da 5 secondi dà 1 moneta.
        if (a.kind in intArrayOf(A_FEED, A_BATH, A_CLEAN, A_PET, A_STUDY, A_BALL) &&
            !pet.stage(now()).atLeast(Stage.TEEN)
        ) {
            pet.coins += 1
            say("+1 MONETA")
        }
        anim = null
        scr = a.back
        cursor = 0
        commit()
    }

    private fun finishAway() {
        val before = pet.coins
        when (pet.finishAway(rnd)) {
            1 -> say("E' TORNATO")
            2 -> say("BENTORNATO")
            5 -> say("MENTE AL MASSIMO")
            3, 4 -> say("+${pet.coins - before} MONETE")
        }
        commit()
    }

    /** Richiamo anticipato: nessun premio. */
    private fun callBack() {
        if (now() < pet.awayUntil) {
            pet.cancelAway()
            say("RICHIAMATO")
            commit()
        } else {
            finishAway()
        }
    }

    private fun startSchool() {
        pet.awayKind = 5
        pet.awayUntil = now() + 2 * 3600000L
        scr = Scr.HOME
        cursor = 0
        commit()
        say("A SCUOLA")
    }

    private fun startWork(min: Int) {
        pet.awayKind = if (min >= 30) 4 else 3
        pet.awayUntil = now() + min * 60000L
        scr = Scr.HOME
        cursor = 0
        commit()
        say("AL LAVORO")
    }

    /** L'animaletto si rifiuta: scuote la testa e dice NO. */
    fun refuse(msg: String) {
        startAnim(A_REFUSE, 1500L, 0, scr)
        say(msg)
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
            say("MORTO")
            return
        }
        val next = (pet.stage(now()).ordinal + 1) % Stage.values().size
        pet.bornAt = now() - (stageStarts[next] * 60000f).toLong() - 1000L
        if (next < Stage.TEEN.ordinal) pet.pers = -1
        dbg("FASE " + Stage.values()[next].label)
    }

    /** simulate=false: cambia solo l'eta'. simulate=true: fa passare davvero il tempo (le barre calano). */
    private fun dbgShift(min: Int, simulate: Boolean) {
        val ms = min * 60000L
        pet.bornAt -= ms
        if (simulate) {
            pet.lastTick -= ms
            if (pet.awayKind != 0) pet.awayUntil -= ms
        }
        dbg(if (simulate) "TEMPO PASSATO" else "ETA' MODIFICATA")
    }

    private fun dbgStats(d: Float) {
        pet.hunger += d
        pet.hygiene += d
        pet.happy += d
        pet.energy += d
        pet.smarts += d
        pet.active += d
        dbg(if (d > 0) "STAT. +20" else "STAT. -20")
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
        dbg("TUTTO PIENO")
    }

    private fun dbgLow() {
        pet.hunger = 15f
        pet.hygiene = 15f
        pet.happy = 15f
        pet.energy = 15f
        dbg("TUTTO BASSO")
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
        dbg("RIVISSUTO")
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
            endLines = listOf("MONETE +$cScore", "CARAM. +$cCandy")
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
                endLines = listOf("GIUSTE $sCorrect/5", "MONETE +${sCorrect * 4}")
                finishGame()
            }
        }
    }

    // ------------------------------------------------------------ rimbalzo

    private fun serve(n: Long) {
        rx = 40f
        ry = 22f
        rvx = (if (rnd.nextBoolean()) 1 else -1) * (14f + rnd.nextInt(14))
        rvy = 34f
        rServe = n + 700L
    }

    private fun startBounce(n: Long) {
        scr = Scr.G_BOUNCE
        bx = 40f
        rHits = 0
        rLives = 3
        rEnd = n + 45000L
        rLast = n
        serve(n)
    }

    private fun updateBounce(n: Long) {
        val dt = (n - rLast).coerceIn(0L, 80L) / 1000f
        rLast = n
        if (n >= rServe) {
            val k = 1f + rHits * 0.025f
            rx += rvx * k * dt
            ry += rvy * k * dt
            if (rx < 3f) {
                rx = 3f
                rvx = abs(rvx)
            }
            if (rx > 77f) {
                rx = 77f
                rvx = -abs(rvx)
            }
            if (ry < 13f) {
                ry = 13f
                rvy = abs(rvy)
            }
            if (rvy > 0f && ry >= 66f && ry <= 72f && abs(rx - bx) <= 11f) {
                rvy = -abs(rvy)
                rvx = (rvx + (rx - bx) * 2f).coerceIn(-42f, 42f)
                ry = 65f
                rHits++
                beep()
            } else if (ry > 82f) {
                rLives--
                serve(n)
            }
        }
        if (n >= rEnd || rLives <= 0) {
            val gain = minOf(rHits, 30)
            pet.coins += gain
            pet.active += minOf(15f, rHits * 0.8f)
            pet.happy += minOf(10f, rHits * 0.5f)
            pet.energy -= 8f
            pet.hunger -= 4f
            pet.games++
            endLines = listOf("RIMBALZI $rHits", "MONETE +$gain")
            finishGame()
        }
    }

    // --------------------------------------------------------------- memoria

    private fun startMem(n: Long) {
        scr = Scr.G_MEM
        memSeq.clear()
        memSeq.add(rnd.nextInt(4))
        memPhase = 0
        memT0 = n
        memIn = 0
        memLit = -1
    }

    fun memInput(q: Int) {
        if (memPhase != 1) return
        memLit = q
        memLitT = now()
        beep()
        if (memSeq[memIn] == q) {
            memIn++
            if (memIn >= memSeq.size) {
                if (memSeq.size >= 7) {
                    memPhase = 3
                } else {
                    memSeq.add(rnd.nextInt(4))
                    memPhase = 4
                }
                memT0 = now()
            }
        } else {
            memPhase = 2
            memT0 = now()
        }
    }

    private fun updateMem(n: Long) {
        when (memPhase) {
            0 -> {
                val i = ((n - memT0 - 600L) / 700L).toInt()
                if (i >= memSeq.size) {
                    memPhase = 1
                    memIn = 0
                    memT0 = n
                }
            }
            4 -> if (n - memT0 > 900L) {
                memPhase = 0
                memT0 = n
            }
            2, 3 -> if (n - memT0 > 1500L) {
                val done = if (memPhase == 3) memSeq.size else memSeq.size - 1
                pet.coins += done * 4
                pet.smarts += 4f * done
                pet.happy += 2f * done
                pet.energy -= 8f
                pet.games++
                endLines = listOf("LIVELLI $done", "MONETE +${done * 4}")
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
                endLines = listOf("COLPI $wHits/5", "MONETE +${wHits * 5}")
                finishGame()
            }
        }
    }

    // --------------------------------------------------------------- ciclo

    private fun onStage(st: Stage, n: Long) {
        if (st != Stage.EGG) {
            say(if (lastStage == Stage.EGG) "E' NATO!" else "E' CRESCIUTO!")
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

        if (scr == Scr.EGG && cursor in 0..4) eggSel = cursor
        when (scr) {
            Scr.G_CATCH -> updateCatch(n)
            Scr.G_HILO -> updateHilo(n)
            Scr.G_BOUNCE -> updateBounce(n)
            Scr.G_MEM -> updateMem(n)
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
        Scr.FOOD -> "MANGIA"
        Scr.PLAYLIST -> "GIOCA"
        Scr.MAP -> "MAPPA"
        Scr.SETTINGS -> "OPZIONI"
        Scr.BG -> "SFONDO"
        Scr.DEBUG -> "DEBUG"
        Scr.STATS -> "STATO"
        Scr.PARK -> "PARCO"
        Scr.SCHOOL -> "SCUOLA"
        Scr.STAND -> "BANCARELLA"
        Scr.UNDER -> "ALDILÀ"
        Scr.G_CATCH -> "DOLCETTI"
        Scr.G_HILO -> "ALTO-BASSO"
        Scr.G_BOUNCE -> "RIMBALZO"
        Scr.G_MEM -> "MEMORIA"
        Scr.INFO -> "INFORMAZIONI"
        Scr.G_SWING -> "BATTUTA"
        Scr.G_END -> "RISULTATO"
        Scr.DEAD -> "R.I.P."
        Scr.NAME -> "NOME"
        Scr.EGG -> "SCEGLI L'UOVO"
        Scr.ANIM -> "..."
    }

    fun canGoBack(): Boolean = when (scr) {
        Scr.MENU, Scr.FOOD, Scr.PLAYLIST, Scr.MAP, Scr.SETTINGS, Scr.STATS, Scr.DEBUG, Scr.BG, Scr.INFO,
        Scr.PARK, Scr.SCHOOL, Scr.STAND, Scr.UNDER -> true
        else -> false
    }

    fun back() {
        when (scr) {
            Scr.MENU, Scr.FOOD, Scr.PLAYLIST, Scr.MAP, Scr.SETTINGS, Scr.STATS, Scr.BG -> open(Scr.HOME)
            Scr.DEBUG, Scr.INFO -> open(Scr.SETTINGS)
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

val INFO_LINES = listOf(
    "PIXIPET: il tuo animaletto virtuale. Vive in tempo reale, anche con l'app chiusa.",
    "VITA: uovo, neonato, bambino, adolescente, adulto, anziano. Dura circa 4 giorni.",
    "CURE: tieni alte fame, pulizia, felicità ed energia. Pulisci la cacca e cura le malattie.",
    "MANGIA: onigiri, hamburger, insalata, caramella, torta, pasta e pollo. Costano monete.",
    "MONETE: da piccolo 1 moneta per ogni attività breve; da adolescente lavori alla bancarella (5 secondi, 10 o 30 minuti).",
    "GIOCHI: Dolcetti (vinci caramelle), Rimbalzo (non far cadere la pallina), Memoria (ripeti la sequenza), Battuta.",
    "CARAMELLE: 10 aprono l'Aldilà, 30 fanno diventare angelo e immortale.",
    "MAPPA: Parco, Scuola, Bancarella e Aldilà. Dai genitori o in vacanza l'animaletto sta via per un po'.",
    "CARATTERE: dolce, furbo, sportivo, goloso o burbero. Dipende dall'uovo scelto e dà un piccolo bonus.",
    "RIFIUTO: se una statistica è al 97% o più, l'animaletto non ripete quell'azione.",
    "SFONDO: 5 stanze da 50 monete. Di notte, o quando dorme, la finestra mostra luna e stelle.",
    "NOTIFICHE: ti avvisano se ha fame, è sporco, malato, dorme o torna a casa.",
    "NOME: si sceglie all'inizio e non si può più cambiare.",
    "CONTROLLI: tocca lo schermo per le coccole, usa i tasti o il menu per il resto. Il tasto centrale conferma.",
    "APP: PIXIPET versione 1.0, un gioco di animaletto virtuale per Android.",
    "AUTORE: ideato da Marco e realizzato insieme a Claude (Anthropic).",
    "GRAFICA: tutti i personaggi, le stanze e le icone sono disegnati a pixel dentro il gioco.",
    "DATI: il salvataggio resta solo sul tuo telefono. Non servono account né internet.",
    "TRASFERIMENTO: ESPORTA DATI copia il salvataggio, IMPORTA DATI lo ripristina su un altro telefono.",
    "PERMESSI: serve solo quello per le notifiche, che puoi spegnere dalle opzioni.",
    "ISPIRAZIONE: i classici animaletti virtuali da tenere in tasca.",
    "VERSIONE 1.0 - creato con Claude"
)

val EGG_COLORS = arrayOf("ROSA", "BLU", "ARANCIO", "VERDE", "VIOLA")
