package com.marco.pixipet

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.net.HttpURLConnection
import java.net.URL

class AndroidPrefs(private val sp: android.content.SharedPreferences) : Prefs {
    override fun getInt(k: String, d: Int) = sp.getInt(k, d)
    override fun getBoolean(k: String, d: Boolean) = sp.getBoolean(k, d)
    override fun getString(k: String, d: String?) = sp.getString(k, d)
    override fun edit(): PrefsEditor {
        val e = sp.edit()
        return object : PrefsEditor {
            override fun putInt(k: String, v: Int): PrefsEditor { e.putInt(k, v); return this }
            override fun putBoolean(k: String, v: Boolean): PrefsEditor { e.putBoolean(k, v); return this }
            override fun putString(k: String, v: String): PrefsEditor { e.putString(k, v); return this }
            override fun apply() = e.apply()
        }
    }
}

class AndroidHost(private val ctx: Context) : PlatformHost {
    override val supportsNotifications = true

    private val tone: ToneGenerator? = try {
        ToneGenerator(AudioManager.STREAM_MUSIC, 35)
    } catch (e: Throwable) {
        null
    }

    override fun prefs(name: String): Prefs =
        AndroidPrefs(ctx.getSharedPreferences(name, Context.MODE_PRIVATE))

    override fun beep() {
        try {
            tone?.startTone(ToneGenerator.TONE_PROP_BEEP, 40)
        } catch (e: Throwable) {
        }
    }

    override fun copyText(t: String) {
        val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("PixiPet", t))
    }

    override fun pasteText(): String {
        val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        return cm.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
    }

    override fun toImage(px: IntArray, w: Int, h: Int): ImageBitmap =
        Bitmap.createBitmap(px, w, h, Bitmap.Config.ARGB_8888).asImageBitmap()

    override fun openUrl(url: String) {
        try {
            ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Throwable) {
        }
    }

    override fun httpGet(url: String): String? = try {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 8000
        c.readTimeout = 8000
        c.setRequestProperty("User-Agent", "PixiPet/$APP_VERSION")
        c.setRequestProperty("Accept", "application/vnd.github+json")
        if (c.responseCode == 200) c.inputStream.bufferedReader().use { it.readText() } else null
    } catch (e: Throwable) {
        null
    }

    override fun notifyTest(name: String) = Notifier.test(ctx, name)

    override fun onResume(pet: Pet, now: Long) = Notifier.check(ctx, pet, now, false)

    override fun onPause(pet: Pet, now: Long) {
        Notifier.onLeave(ctx, pet, now)
        Notifier.schedule(ctx, Notifier.nextDelayMs(pet, now))
    }

    @Composable
    override fun BackHandlerCompat(enabled: Boolean, onBack: () -> Unit) = BackHandler(enabled = enabled, onBack = onBack)

    @Composable
    override fun insetsModifier(): Modifier = Modifier.windowInsetsPadding(WindowInsets.systemBars)
}
