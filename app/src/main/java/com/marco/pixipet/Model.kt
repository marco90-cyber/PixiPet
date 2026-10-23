package com.marco.pixipet

import org.json.JSONObject
import kotlin.random.Random

enum class Stage(val label: String) {
    EGG("UOVO"), BABY("NEONATO"), CHILD("BAMBINO"), TEEN("ADOLESC."), ADULT("ADULTO"), ELDER("ANZIANO")
}

fun Stage.atLeast(o: Stage) = ordinal >= o.ordinal

val PERS = arrayOf("DOLCE", "FURBO", "SPORTIVO", "GOLOSO", "BURBERO")
val NAMES = listOf("MOCHI", "PIXEL", "BIBI", "NORI", "TOFU", "LUNA", "KIKO", "MOMO", "PIP", "ZUZU", "COCO", "NANA")

/** Durate in minuti reali di vita. */
object Cfg {
    const val EGG_MIN = 2f
    const val BABY_MIN = 60f
    const val CHILD_MIN = 360f
    const val TEEN_MIN = 1440f
    const val ADULT_MIN = 4320f
    const val DEATH_MIN = 5760f
}

class Food(val name: String, val price: Int, val hunger: Float, val happy: Float, val weight: Int, val kind: Int)

val FOODS = listOf(
    Food("ONIGIRI", 5, 25f, 2f, 1, 0),
    Food("HAMBURGER", 12, 40f, 6f, 2, 1),
    Food("INSALATA", 8, 20f, 0f, 0, 2),
    Food("CARAMELLA", 3, 4f, 15f, 1, 3),
    Food("TORTA", 15, 12f, 25f, 2, 4)
)

class Pet(var bornAt: Long) {
    var name = "MOCHI"
    var hue = 0
    var lastTick = bornAt
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

    var coins = 50
    var candy = 0
    var pers = -1
    var cFood = 0
    var cStudy = 0
    var cPlay = 0
    var cPet = 0
    var immortal = false
    var angel = false
    var awayUntil = 0L
    var awayKind = 0
    var dates = 0
    var games = 0

    fun ageMin(now: Long): Float {
        val end = if (dead) diedAt else now
        return ((end - bornAt) / 60000f).coerceAtLeast(0f)
    }

    fun stage(now: Long): Stage {
        val a = ageMin(now)
        return when {
            a < Cfg.EGG_MIN -> Stage.EGG
            a < Cfg.BABY_MIN -> Stage.BABY
            a < Cfg.CHILD_MIN -> Stage.CHILD
            a < Cfg.TEEN_MIN -> Stage.TEEN
            a < Cfg.ADULT_MIN -> Stage.ADULT
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
        coins = coins.coerceIn(0, 99999)
        candy = candy.coerceIn(0, 9999)
    }

    /** La personalita' si decide in base a come e' stato cresciuto. */
    fun assignPers() {
        val sc = floatArrayOf(
            cPet * 2f + happy,
            smarts + cStudy * 3f,
            active + cPlay * 3f,
            cFood * 1.5f + weight,
            careMistakes * 8f + (100f - happy)
        )
        var best = 0
        for (i in 1 until sc.size) if (sc[i] > sc[best]) best = i
        pers = best
    }

    /** Fine di genitori/vacanza: applica i bonus e ritorna il tipo di assenza appena terminata. */
    fun finishAway(rnd: Random = Random.Default): Int {
        val kind = awayKind
        when (kind) {
            1 -> {
                hunger = maxOf(hunger, 60f)
                hygiene = maxOf(hygiene, 60f)
                smarts += 5f
                happy += 5f
            }
            2 -> {
                happy = 100f
                energy = 100f
                hygiene = 100f
                hunger = maxOf(hunger, 60f)
                coins += 20
            }
            3 -> { // lavoro da 10 minuti
                coins += 45 + rnd.nextInt(16) + (smarts / 10f).toInt()
                energy -= 15f
                hygiene -= 8f
                hunger -= 8f
                happy -= 5f
            }
            4 -> { // lavoro da 30 minuti
                coins += 150 + rnd.nextInt(41) + (smarts / 5f).toInt()
                energy -= 25f
                hygiene -= 15f
                hunger -= 12f
                happy -= 8f
            }
        }
        awayKind = 0
        awayUntil = 0L
        clamp()
        return kind
    }

    /** Richiamo anticipato: niente premio ne' bonus. */
    fun cancelAway() {
        awayKind = 0
        awayUntil = 0L
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
        if (awayKind != 0 && at <= awayUntil) return
        val age = ageMin(at)
        if (age < Cfg.EGG_MIN) return
        if (!immortal && age >= Cfg.DEATH_MIN) {
            die("VECCHIAIA", at)
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
            smarts -= 0.12f
            active -= 0.3f
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
            if (sickMin >= 240f && !immortal) {
                clamp()
                die("MALATTIA", at)
                return
            }
        }

        if (hunger <= 0f) {
            starveMin += 1f
            if (starveMin >= 180f && !immortal) {
                clamp()
                die("FAME", at)
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

    fun toJson(): JSONObject = JSONObject().apply {
        put("name", name); put("hue", hue); put("bornAt", bornAt); put("lastTick", lastTick)
        put("hunger", hunger.toDouble()); put("hygiene", hygiene.toDouble()); put("smarts", smarts.toDouble())
        put("active", active.toDouble()); put("energy", energy.toDouble()); put("happy", happy.toDouble())
        put("weight", weight); put("poops", poops); put("sick", sick); put("sleeping", sleeping)
        put("sickMin", sickMin.toDouble()); put("starveMin", starveMin.toDouble()); put("callMin", callMin.toDouble())
        put("care", careMistakes); put("dead", dead); put("cause", cause); put("diedAt", diedAt)
        put("gen", generation); put("coins", coins); put("candy", candy); put("pers", pers)
        put("cFood", cFood); put("cStudy", cStudy); put("cPlay", cPlay); put("cPet", cPet)
        put("immortal", immortal); put("angel", angel); put("awayUntil", awayUntil); put("awayKind", awayKind)
        put("dates", dates); put("games", games)
    }

    companion object {
        fun fromJson(j: JSONObject): Pet {
            val p = Pet(j.getLong("bornAt"))
            p.name = j.optString("name", "MOCHI")
            p.hue = j.optInt("hue", 0)
            p.lastTick = j.optLong("lastTick", p.bornAt)
            p.hunger = j.optDouble("hunger", 70.0).toFloat()
            p.hygiene = j.optDouble("hygiene", 90.0).toFloat()
            p.smarts = j.optDouble("smarts", 10.0).toFloat()
            p.active = j.optDouble("active", 25.0).toFloat()
            p.energy = j.optDouble("energy", 100.0).toFloat()
            p.happy = j.optDouble("happy", 60.0).toFloat()
            p.weight = j.optInt("weight", 5)
            p.poops = j.optInt("poops", 0)
            p.sick = j.optBoolean("sick", false)
            p.sleeping = j.optBoolean("sleeping", false)
            p.sickMin = j.optDouble("sickMin", 0.0).toFloat()
            p.starveMin = j.optDouble("starveMin", 0.0).toFloat()
            p.callMin = j.optDouble("callMin", 0.0).toFloat()
            p.careMistakes = j.optInt("care", 0)
            p.dead = j.optBoolean("dead", false)
            p.cause = j.optString("cause", "")
            p.diedAt = j.optLong("diedAt", 0L)
            p.generation = j.optInt("gen", 1)
            p.coins = j.optInt("coins", 50)
            p.candy = j.optInt("candy", 0)
            p.pers = j.optInt("pers", -1)
            p.cFood = j.optInt("cFood", 0)
            p.cStudy = j.optInt("cStudy", 0)
            p.cPlay = j.optInt("cPlay", 0)
            p.cPet = j.optInt("cPet", 0)
            p.immortal = j.optBoolean("immortal", false)
            p.angel = j.optBoolean("angel", false)
            p.awayUntil = j.optLong("awayUntil", 0L)
            p.awayKind = j.optInt("awayKind", 0)
            p.dates = j.optInt("dates", 0)
            p.games = j.optInt("games", 0)
            return p
        }
    }
}
