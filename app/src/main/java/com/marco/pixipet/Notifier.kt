package com.marco.pixipet

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/** Notifiche per l'animaletto, anche ad app chiusa (WorkManager). */
object Notifier {
    const val CHANNEL = "pixipet_care"

    const val F_HUNGRY = 1
    const val F_STARVING = 2
    const val F_DIRTY = 4
    const val F_POOP = 8
    const val F_SICK = 16
    const val F_SAD = 32
    const val F_TIRED = 64
    const val F_DYING = 128
    const val F_DEAD = 256
    const val F_ASLEEP = 512

    private const val ID_STAGE = 100
    private const val ID_AWAY = 101
    private const val ID_WOKE = 102
    private const val ID_REMIND = 103
    private const val ID_TEST = 104

    private const val PROBLEMS = F_HUNGRY or F_DIRTY or F_POOP or F_SICK or F_SAD or F_TIRED
    private const val REMIND_EVERY_MS = 3L * 3600000L

    /** True mentre l'app e' aperta: in quel caso il lavoro in background non fa nulla. */
    @Volatile
    var foreground = false

    fun ensureChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val ch = NotificationChannel(CHANNEL, "Cure dell'animaletto", NotificationManager.IMPORTANCE_HIGH)
            ch.description = "Fame, sporco, malattie, crescita e altro"
            val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(ch)
        }
    }

    private fun post(ctx: Context, id: Int, title: String, text: String) {
        ensureChannel(ctx)
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val intent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(
            ctx, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        try {
            NotificationManagerCompat.from(ctx).notify(id, n)
        } catch (e: SecurityException) {
        }
    }

    private fun cancel(ctx: Context, id: Int) {
        try {
            NotificationManagerCompat.from(ctx).cancel(id)
        } catch (e: Throwable) {
        }
    }

    fun test(ctx: Context, name: String) {
        post(ctx, ID_TEST, "$name ti chiama!", "Questa e' una notifica di prova di PixiPet.")
    }

    // --------------------------------------------------------------- stato

    fun flagsFor(p: Pet, now: Long): Int {
        if (p.dead) return F_DEAD
        if (p.stage(now) == Stage.EGG) return 0
        if (p.awayKind != 0) return 0
        var f = 0
        if (p.sleeping) f = f or F_ASLEEP
        if (p.hunger < 30f) f = f or F_HUNGRY
        if (p.hunger <= 0f) f = f or F_STARVING
        if (p.hygiene < 30f) f = f or F_DIRTY
        if (p.poops >= 2) f = f or F_POOP
        if (p.sick) f = f or F_SICK
        if (p.happy < 25f) f = f or F_SAD
        if (p.energy < 25f && !p.sleeping) f = f or F_TIRED
        if (!p.immortal && (p.starveMin >= Cfg.STARVE_DEATH - Cfg.WARN_BEFORE || p.sickMin >= Cfg.SICK_DEATH - Cfg.WARN_BEFORE)) f = f or F_DYING
        return f
    }

    private class Entry(val flag: Int, val id: Int, val title: (String) -> String, val text: (Pet) -> String)

    private val ENTRIES = listOf(
        Entry(F_HUNGRY, 1, { "$it ha fame" }) { "Dagli da mangiare prima che si indebolisca." },
        Entry(F_STARVING, 2, { "$it sta morendo di fame!" }) { "La pancia e' vuota: sfamalo subito." },
        Entry(F_DIRTY, 3, { "$it e' sporco" }) { "Fagli un bagno: puzza e rischia di ammalarsi." },
        Entry(F_POOP, 4, { "Cacche da pulire" }) { "C'e' da fare pulizia in casa." },
        Entry(F_SICK, 5, { "$it e' malato" }) { "Serve la medicina (menu CURA)." },
        Entry(F_SAD, 6, { "$it e' triste" }) { "Gioca con lui o fagli due coccole." },
        Entry(F_TIRED, 7, { "$it e' stanco" }) { "Falla dormire dal menu DORMI." },
        Entry(F_DYING, 8, { "ATTENZIONE: $it sta per morire!" }) {
            val causa = if (it.sickMin >= Cfg.SICK_DEATH - Cfg.WARN_BEFORE) "della malattia: dagli la cura" else "di fame: dagli da mangiare"
            "Gli resta circa 1 ora. Se non lo aiuti subito morira' $causa."
        },
        Entry(F_DEAD, 9, { "$it e' morto" }) { "Apri l'app per far nascere un nuovo uovo." }
    )

    private fun describe(flags: Int): String {
        val parts = ArrayList<String>()
        if (flags and F_HUNGRY != 0) parts.add("fame")
        if (flags and F_DIRTY != 0) parts.add("sporco")
        if (flags and F_POOP != 0) parts.add("cacche")
        if (flags and F_SICK != 0) parts.add("malattia")
        if (flags and F_SAD != 0) parts.add("tristezza")
        if (flags and F_TIRED != 0) parts.add("stanchezza")
        return parts.joinToString(", ")
    }

    private fun prefs(ctx: Context) = ctx.getSharedPreferences("pixipet2", Context.MODE_PRIVATE)

    /**
     * Confronta lo stato attuale con l'ultimo noto: notifica cio' che e' nuovo e
     * cancella le notifiche dei problemi risolti. Con notify=false si limita ad aggiornare.
     */
    fun check(ctx: Context, p: Pet, now: Long, notify: Boolean) {
        val sp = prefs(ctx)
        val st = p.stage(now).ordinal
        val prev = sp.getInt("nflags", 0)
        val prevStage = sp.getInt("nstage", st)
        val prevAway = sp.getInt("naway", 0)
        val prevGen = sp.getInt("ngen", p.generation)
        val cur = flagsFor(p, now)
        val on = notify && sp.getBoolean("notify", true)

        // Di notte (22-8) niente notifiche, tranne l'emergenza e la morte: le altre arrivano al mattino.
        val quiet = isProtectedHour(now)
        val added = cur and prev.inv()
        for (e in ENTRIES) {
            if (cur and e.flag != 0) {
                val allowed = on && (!quiet || e.flag == F_DYING || e.flag == F_DEAD)
                if (allowed && added and e.flag != 0) post(ctx, e.id, e.title(p.name), e.text(p))
            } else {
                cancel(ctx, e.id)
            }
        }

        if (prev and F_ASLEEP != 0 && cur and F_ASLEEP == 0 && !p.dead && p.awayKind == 0) {
            if (on && !quiet) post(ctx, ID_WOKE, "${p.name} si e' svegliato", "Ha recuperato le energie.")
        }

        if (p.generation == prevGen && st > prevStage && !p.dead && on) {
            val stage = p.stage(now)
            if (prevStage == Stage.EGG.ordinal) {
                post(ctx, ID_STAGE, "${p.name} e' nato!", "L'uovo si e' schiuso.")
            } else {
                val extra = if (stage.atLeast(Stage.TEEN) && p.pers >= 0) " Carattere: ${PERS[p.pers]}." else ""
                post(ctx, ID_STAGE, "${p.name} e' cresciuto!", "Ora e' ${stage.label}.$extra")
            }
        }

        if (prevAway != 0 && p.awayKind == 0 && !p.dead && on) {
            val txt = when (prevAway) {
                1 -> "E' tornato dai genitori."
                2 -> "E' tornato dalla vacanza con un souvenir."
                5 -> "Ha finito la scuola: ora e' molto piu' sapiente."
                else -> "Ha finito di lavorare e ha guadagnato delle monete."
            }
            post(ctx, ID_AWAY, "${p.name} e' tornato", txt)
        }

        // promemoria periodico se i problemi restano
        val problems = cur and PROBLEMS
        if (problems == 0) {
            cancel(ctx, ID_REMIND)
        } else if (on && !quiet && added and PROBLEMS == 0) {
            val last = sp.getLong("nrem", 0L)
            if (now - last >= REMIND_EVERY_MS) {
                post(ctx, ID_REMIND, "${p.name} ha bisogno di te", "Problemi: ${describe(problems)}.")
                sp.edit().putLong("nrem", now).apply()
            }
        } else if (on && !quiet && added and PROBLEMS != 0) {
            sp.edit().putLong("nrem", now).apply()
        }

        // I problemi non annunciati di notte restano "nuovi" e vengono notificati alle 8.
        val saved = if (quiet && on) cur and (prev or F_DYING or F_DEAD or F_ASLEEP) else cur
        sp.edit()
            .putInt("nflags", saved)
            .putInt("nstage", st)
            .putInt("naway", p.awayKind)
            .putInt("ngen", p.generation)
            .apply()
    }

    /** Quando l'app va in background: i problemi gia' presenti verranno notificati dal lavoro in background. */
    fun onLeave(ctx: Context, p: Pet, now: Long) {
        val sp = prefs(ctx)
        sp.edit()
            .putInt("nflags", flagsFor(p, now) and F_ASLEEP)
            .putInt("nstage", p.stage(now).ordinal)
            .putInt("naway", p.awayKind)
            .putInt("ngen", p.generation)
            .putLong("nrem", now)
            .apply()
    }

    // ------------------------------------------------------- simulazione

    fun advance(p: Pet, now: Long) {
        var g = 0
        while (now - p.lastTick >= 60000L && g < 300000) {
            p.step(Random.Default)
            g++
        }
        if (p.awayKind != 0 && now >= p.awayUntil) p.finishAway()
        if (p.pers < 0 && p.stage(now).atLeast(Stage.TEEN)) p.assignPers()
        p.clamp()
    }

    /** Tra quanto puo' succedere qualcosa di notificabile (stima, max 15 minuti). */
    fun nextDelayMs(p: Pet, now: Long): Long {
        var best = 15L * 60000L
        fun cand(minutes: Float) {
            if (minutes > 0f) {
                val ms = (minutes * 60000f).toLong() + 20000L
                if (ms < best) best = ms
            }
        }
        if (p.dead) return 60L * 60000L
        val age = p.ageMin(now)
        for (start in floatArrayOf(Cfg.EGG_MIN, Cfg.BABY_MIN, Cfg.CHILD_MIN, Cfg.TEEN_MIN, Cfg.ADULT_MIN)) {
            if (age < start) cand(start - age)
        }
        if (p.awayKind != 0) {
            cand((p.awayUntil - now) / 60000f)
        } else {
            val asleep = p.sleeping
            val hr = if (asleep) 0.25f else 0.9f
            if (p.hunger >= 30f) cand((p.hunger - 30f) / hr)
            if (p.hunger > 0f) cand(p.hunger / hr)
            val yr = if (asleep) 0.1f else 0.5f + 0.25f * p.poops
            if (p.hygiene >= 30f) cand((p.hygiene - 30f) / yr)
            val sr = if (asleep) 0.1f else 0.45f
            if (p.happy >= 25f) cand((p.happy - 25f) / sr)
            if (asleep) cand((100f - p.energy) / 1.5f) else if (p.energy >= 25f) cand((p.energy - 25f) / 0.5f)
            if (p.hunger <= 0f && !p.immortal) cand(Cfg.STARVE_DEATH - Cfg.WARN_BEFORE - p.starveMin)
            if (p.sick && !p.immortal) cand(Cfg.SICK_DEATH - Cfg.WARN_BEFORE - p.sickMin)
        }
        return best.coerceAtLeast(2L * 60000L)
    }

    fun schedule(ctx: Context, delayMs: Long) {
        try {
            val wm = WorkManager.getInstance(ctx)
            val one = OneTimeWorkRequestBuilder<PetWorker>()
                .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                .build()
            wm.enqueueUniqueWork("pet-tick", ExistingWorkPolicy.REPLACE, one)
            val per = PeriodicWorkRequestBuilder<PetWorker>(15, TimeUnit.MINUTES).build()
            wm.enqueueUniquePeriodicWork("pet-periodic", ExistingPeriodicWorkPolicy.KEEP, per)
        } catch (e: Throwable) {
        }
    }
}

class PetWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {
    override fun doWork(): Result {
        val ctx = applicationContext
        if (Notifier.foreground) {
            Notifier.schedule(ctx, 5L * 60000L)
            return Result.success()
        }
        try {
            val sp = ctx.getSharedPreferences("pixipet2", Context.MODE_PRIVATE)
            val s = sp.getString("pet", null)
            if (s != null) {
                val pet = Pet.fromJson(JSONObject(s))
                val now = System.currentTimeMillis()
                Notifier.advance(pet, now)
                sp.edit().putString("pet", pet.toJson().toString()).apply()
                Notifier.check(ctx, pet, now, true)
                Notifier.schedule(ctx, Notifier.nextDelayMs(pet, now))
            }
        } catch (e: Throwable) {
        }
        return Result.success()
    }
}
