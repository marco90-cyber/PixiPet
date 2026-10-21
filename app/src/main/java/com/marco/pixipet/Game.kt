package com.marco.pixipet

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.random.Random

enum class Stage(val label: String) {
    EGG("EGG"), BABY("BABY"), CHILD("CHILD"), TEEN("TEEN"), ADULT("ADULT"), ELDER("ELDER"), DEAD("DEAD")
}

enum class Mode { HOME, FEED, PLAY, SMART, INFO }

enum class Anim { NONE, MEAL, SNACK, CLEAN, MED }

/** Durate degli stadi, in minuti reali. */
object Config {
    const val EGG_MIN = 2f
    const val BABY_MIN = 60f
    const val CHILD_MIN = 360f
    const val TEEN_MIN = 1440f
    const val ADULT_MIN = 4320f
    const val DEATH_MIN = 5760f
    const val ANIM_MS = 2200L
}

class Pet(var bornAt: Long) {
    var lastTick: Long = bornAt
    var hunger = 70f
    var hygiene = 90f
    var smarts = 10f
    var active = 25f
    var energy = 100f
    var happy = 60f
    var weight = 5
    var poops = 0
    var sick = false
    var sleeping = false
    var sickMin = 0f
    var starveMin = 0f
    var callMin = 0f
    var careMistakes = 0
    var dead = false
    var cause = ""
    var diedAt = 0L
    var generation = 1

    fun ageMin(now: Long): Float {
        val end = if (dead) diedAt else now
        return ((end - bornAt) / 60000f).coerceAtLeast(0f)
    }

    fun stage(now: Long): Stage {
        if (dead) return Stage.DEAD
        val a = ageMin(now)
        return when {
            a < Config.EGG_MIN -> Stage.EGG
            a < Config.BABY_MIN -> Stage.BABY
            a < Config.CHILD_MIN -> Stage.CHILD
            a < Config.TEEN_MIN -> Stage.TEEN
            a < Config.ADULT_MIN -> Stage.ADULT
            else -> Stage.ELDER
        }
    }

    val calling: Boolean
        get() = hunger < 20f || happy < 20f || hygiene < 20f || sick || poops >= 2

    fun clamp() {
        hunger = hunger.coerceIn(0f, 100f)
        hygiene = hygiene.coerceIn(0f, 100f)
        smarts = smarts.coerceIn(0f, 100f)
        active = active.coerceIn(0f, 100f)
        energy = energy.coerceIn(0f, 100f)
        happy = happy.coerceIn(0f, 100f)
        weight = weight.coerceIn(1, 99)
    }

    private fun die(why: String, at: Long) {
        dead = true
        cause = why
        diedAt = at
    }

    /** Simula esattamente un minuto di vita. */
    fun step(rnd: Random) {
        val at = lastTick + 60000L
        lastTick = at
        if (dead) return
        val age = ageMin(at)
        if (age < Config.EGG_MIN) return
        if (age >= Config.DEATH_MIN) {
            die("OLD AGE", at)
            return
        }

        if (sleeping) {
            hunger -= 0.25f
            hygiene -= 0.1f
            energy += 1.5f
            happy -= 0.1f
            if (energy >= 100f) sleeping = false
        } else {
            hunger -= 0.9f
            hygiene -= 0.5f + 0.25f * poops
            smarts -= 0.15f
            active -= 0.35f
            energy -= 0.5f
            happy -= 0.45f
            if (hunger < 25f || hygiene < 25f) happy -= 0.3f
            if (energy <= 0f) {
                energy = 0f
                sleeping = true
            }
            if (poops < 4 && rnd.nextInt(90) == 0) poops++
        }

        if (!sick) {
            val risk = hygiene < 15f || poops >= 3 || hunger < 8f
            if ((risk && rnd.nextInt(60) == 0) || rnd.nextInt(2500) == 0) {
                sick = true
                careMistakes++
            }
        } else {
            sickMin += 1f
            happy -= 0.4f
            if (sickMin >= 240f) {
                clamp()
                die("ILLNESS", at)
                return
            }
        }

        if (hunger <= 0f) {
            starveMin += 1f
            if (starveMin >= 180f) {
                clamp()
                die("HUNGER", at)
                return
            }
        } else {
            starveMin = (starveMin - 2f).coerceAtLeast(0f)
        }

        if (calling && !sleeping) {
            callMin += 1f
            if (callMin >= 30f) {
                careMistakes++
                callMin = 0f
            }
        } else {
            callMin = 0f
        }

        clamp()
    }

    fun save(p: SharedPreferences) {
        p.edit()
            .putLong("bornAt", bornAt).putLong("lastTick", lastTick)
            .putFloat("hunger", hunger).putFloat("hygiene", hygiene).putFloat("smarts", smarts)
            .putFloat("active", active).putFloat("energy", energy).putFloat("happy", happy)
            .putInt("weight", weight).putInt("poops", poops)
            .putBoolean("sick", sick).putBoolean("sleeping", sleeping)
            .putFloat("sickMin", sickMin).putFloat("starveMin", starveMin).putFloat("callMin", callMin)
            .putInt("care", careMistakes).putBoolean("dead", dead).putString("cause", cause)
            .putLong("diedAt", diedAt).putInt("gen", generation)
            .apply()
    }

    companion object {
        fun load(p: SharedPreferences): Pet? {
            if (!p.contains("bornAt")) return null
            val pet = Pet(p.getLong("bornAt", System.currentTimeMillis()))
            pet.lastTick = p.getLong("lastTick", pet.bornAt)
            pet.hunger = p.getFloat("hunger", 70f)
            pet.hygiene = p.getFloat("hygiene", 90f)
            pet.smarts = p.getFloat("smarts", 10f)
            pet.active = p.getFloat("active", 25f)
            pet.energy = p.getFloat("energy", 100f)
            pet.happy = p.getFloat("happy", 60f)
            pet.weight = p.getInt("weight", 5)
            pet.poops = p.getInt("poops", 0)
            pet.sick = p.getBoolean("sick", false)
            pet.sleeping = p.getBoolean("sleeping", false)
            pet.sickMin = p.getFloat("sickMin", 0f)
            pet.starveMin = p.getFloat("starveMin", 0f)
            pet.callMin = p.getFloat("callMin", 0f)
            pet.careMistakes = p.getInt("care", 0)
            pet.dead = p.getBoolean("dead", false)
            pet.cause = p.getString("cause", "") ?: ""
            pet.diedAt = p.getLong("diedAt", 0L)
            pet.generation = p.getInt("gen", 1)
            return pet
        }
    }
}

class PixiGame(ctx: Context) {
    private val prefs: SharedPreferences = ctx.getSharedPreferences("pixipet", Context.MODE_PRIVATE)
    private val rnd = Random(System.nanoTime())
    private val tone: ToneGenerator? = try {
        ToneGenerator(AudioManager.STREAM_MUSIC, 35)
    } catch (e: Throwable) {
        null
    }

    var pet by mutableStateOf(Pet.load(prefs) ?: Pet(System.currentTimeMillis()))
    var rev by mutableIntStateOf(0)
    var frame by mutableLongStateOf(0L)

    var mode by mutableStateOf(Mode.HOME)
    var sel by mutableIntStateOf(0)
    var sub by mutableIntStateOf(0)

    var anim by mutableStateOf(Anim.NONE)
    var animT0 = 0L

    var toast by mutableStateOf("")
    private var toastUntil = 0L

    var petX by mutableIntStateOf(8)
    var facingLeft by mutableStateOf(false)
    private var nextMove = 0L
    private var lastStage: Stage = pet.stage(System.currentTimeMillis())

    // --- minigioco PLAY (baseball) ---
    var pRound = 1
    var pHits = 0
    var ballX = 31f
    var pPhase = 0          // 0 corsa, 1 colpito, 2 mancato, 3 finale
    var pPhaseT0 = 0L
    var swingUntil = 0L
    private var swung = false
    private var lastFrame = 0L

    // --- minigioco SMART (alto/basso) ---
    var sRound = 1
    var sCorrect = 0
    var sNum = 5
    var sNext = 5
    var sPhase = 0          // 0 scelta, 1 giusto, 2 sbagliato, 3 finale
    var sT0 = 0L

    init {
        save()
    }

    fun save() {
        pet.save(prefs)
    }

    private fun commit() {
        pet.clamp()
        rev++
        save()
    }

    private fun beep() {
        try {
            tone?.startTone(ToneGenerator.TONE_PROP_BEEP, 40)
        } catch (e: Throwable) {
        }
    }

    private fun say(text: String, now: Long) {
        toast = text
        toastUntil = now + 1500L
    }

    // ------------------------------------------------------------------ loop

    fun update(now: Long) {
        frame = now

        var changed = false
        var guard = 0
        while (now - pet.lastTick >= 60000L && guard < 200000) {
            pet.step(rnd)
            guard++
            changed = true
        }
        if (changed) commit()

        val st = pet.stage(now)
        if (st != lastStage) {
            if (st != Stage.DEAD) {
                say(if (lastStage == Stage.EGG) "HATCH!" else "GROW!", now)
                beep()
            }
            lastStage = st
            rev++
        }

        if (toast.isNotEmpty() && now > toastUntil) toast = ""
        if (anim != Anim.NONE && now - animT0 >= Config.ANIM_MS) finishAnim()

        when (mode) {
            Mode.PLAY -> updatePlay(now)
            Mode.SMART -> updateSmart(now)
            else -> {}
        }

        // passeggiata dell'animaletto
        if (mode == Mode.HOME && anim == Anim.NONE && !pet.sleeping && !pet.dead &&
            st != Stage.EGG && now > nextMove
        ) {
            if (rnd.nextInt(100) < 65) {
                val dir = if (facingLeft) -1 else 1
                var nx = petX + dir
                if (nx < 3 || nx > 13) {
                    facingLeft = !facingLeft
                    nx = petX - dir
                }
                petX = nx
            } else if (rnd.nextInt(100) < 30) {
                facingLeft = !facingLeft
            }
            nextMove = now + 600L + rnd.nextInt(900)
        }
    }

    // ----------------------------------------------------------------- input

    fun onLeft() {
        beep()
        input(-1)
    }

    fun onRight() {
        beep()
        input(1)
    }

    fun onSelect() {
        beep()
        input(0)
    }

    fun pickIcon(i: Int) {
        if (mode == Mode.HOME && anim == Anim.NONE && !pet.dead) {
            sel = i
            beep()
        }
    }

    private fun input(d: Int) {
        val now = System.currentTimeMillis()
        if (pet.dead) {
            if (d == 0) newPet(now)
            return
        }
        if (anim != Anim.NONE) return
        when (mode) {
            Mode.HOME -> if (d != 0) sel = (sel + d + 7) % 7 else activate(now)
            Mode.FEED -> if (d != 0) sub = (sub + d + 3) % 3 else feedChoose(now)
            Mode.INFO -> if (d != 0) sub = (sub + d + 3) % 3 else mode = Mode.HOME
            Mode.PLAY -> playInput(d, now)
            Mode.SMART -> smartInput(d, now)
        }
    }

    private fun activate(now: Long) {
        val st = pet.stage(now)
        if (st == Stage.EGG && sel != 6) {
            say("WAIT", now)
            return
        }
        if (pet.sleeping && sel != 4 && sel != 6) {
            say("ZZZ", now)
            return
        }
        when (sel) {
            0 -> {
                mode = Mode.FEED
                sub = 0
            }
            1 -> startAnim(Anim.CLEAN, now)
            2 -> startSmart(now)
            3 -> startPlay(now)
            4 -> {
                if (pet.sleeping) {
                    pet.sleeping = false
                    commit()
                } else if (pet.energy >= 90f) {
                    say("AWAKE", now)
                } else {
                    pet.sleeping = true
                    commit()
                }
            }
            5 -> if (pet.sick) startAnim(Anim.MED, now) else say("NOT SICK", now)
            6 -> {
                mode = Mode.INFO
                sub = 0
            }
        }
    }

    private fun feedChoose(now: Long) {
        when (sub) {
            0 -> {
                mode = Mode.HOME
                if (pet.hunger >= 95f) say("FULL", now) else startAnim(Anim.MEAL, now)
            }
            1 -> {
                mode = Mode.HOME
                if (pet.happy >= 95f && pet.hunger >= 95f) say("FULL", now) else startAnim(Anim.SNACK, now)
            }
            else -> mode = Mode.HOME
        }
    }

    private fun startAnim(a: Anim, now: Long) {
        anim = a
        animT0 = now
    }

    private fun finishAnim() {
        when (anim) {
            Anim.MEAL -> {
                pet.hunger += 35f
                pet.weight += 1
                pet.happy += 3f
            }
            Anim.SNACK -> {
                pet.hunger += 10f
                pet.happy += 15f
                pet.weight += 2
            }
            Anim.CLEAN -> {
                pet.poops = 0
                pet.hygiene = 100f
            }
            Anim.MED -> {
                pet.sick = false
                pet.sickMin = 0f
                pet.happy -= 5f
            }
            Anim.NONE -> {}
        }
        anim = Anim.NONE
        commit()
    }

    private fun newPet(now: Long) {
        val gen = pet.generation + 1
        val p = Pet(now)
        p.generation = gen
        pet = p
        mode = Mode.HOME
        sel = 0
        anim = Anim.NONE
        lastStage = Stage.EGG
        petX = 8
        commit()
    }

    // ------------------------------------------------------------- PLAY game

    private fun startPlay(now: Long) {
        mode = Mode.PLAY
        pRound = 1
        pHits = 0
        ballX = 31f
        pPhase = 0
        swung = false
        swingUntil = 0L
        lastFrame = now
    }

    private fun playInput(d: Int, now: Long) {
        if (d != 0 || pPhase != 0 || swung) return
        swung = true
        swingUntil = now + 260L
        if (ballX in 13f..20f) {
            pHits++
            pPhase = 1
        } else {
            pPhase = 2
        }
        pPhaseT0 = now
    }

    private fun updatePlay(now: Long) {
        val dt = (now - lastFrame).coerceIn(0L, 100L) / 1000f
        lastFrame = now
        when (pPhase) {
            0 -> {
                ballX -= (14f + 2f * pRound) * dt
                if (ballX < 11f) {
                    pPhase = 2
                    pPhaseT0 = now
                }
            }
            1, 2 -> if (now - pPhaseT0 > 900L) {
                if (pRound >= 5) {
                    pPhase = 3
                    pPhaseT0 = now
                } else {
                    pRound++
                    ballX = 31f
                    pPhase = 0
                    swung = false
                }
            }
            3 -> if (now - pPhaseT0 > 1800L) endPlay(now)
        }
    }

    private fun endPlay(now: Long) {
        pet.active += 8f * pHits + 5f
        pet.happy += 5f * pHits
        pet.energy -= 12f
        if (pHits >= 3 && pet.weight > 1) pet.weight -= 1
        mode = Mode.HOME
        say(if (pHits >= 3) "WIN" else "LOSE", now)
        commit()
    }

    // ------------------------------------------------------------ SMART game

    private fun pickNext(): Int {
        var n: Int
        do {
            n = 1 + rnd.nextInt(9)
        } while (n == sNum)
        return n
    }

    private fun startSmart(now: Long) {
        mode = Mode.SMART
        sRound = 1
        sCorrect = 0
        sNum = 1 + rnd.nextInt(9)
        sNext = pickNext()
        sPhase = 0
        sT0 = now
    }

    private fun smartInput(d: Int, now: Long) {
        if (sPhase != 0 || d == 0) return
        val guessHigh = d > 0
        val ok = (sNext > sNum) == guessHigh
        if (ok) sCorrect++
        sPhase = if (ok) 1 else 2
        sT0 = now
    }

    private fun updateSmart(now: Long) {
        when (sPhase) {
            1, 2 -> if (now - sT0 > 1100L) {
                if (sRound >= 5) {
                    sPhase = 3
                    sT0 = now
                } else {
                    sRound++
                    sNum = sNext
                    sNext = pickNext()
                    sPhase = 0
                }
            }
            3 -> if (now - sT0 > 1800L) endSmart(now)
        }
    }

    private fun endSmart(now: Long) {
        pet.smarts += 8f * sCorrect
        pet.happy += 3f * sCorrect
        pet.energy -= 8f
        mode = Mode.HOME
        say(if (sCorrect >= 3) "WIN" else "LOSE", now)
        commit()
    }
}
