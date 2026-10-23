package com.marco.pixipet

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap

/** Preferenze semplici, uguali su Android e su Windows. */
interface Prefs {
    fun getInt(k: String, d: Int): Int
    fun getBoolean(k: String, d: Boolean): Boolean
    fun getString(k: String, d: String?): String?
    fun edit(): PrefsEditor
}

interface PrefsEditor {
    fun putInt(k: String, v: Int): PrefsEditor
    fun putBoolean(k: String, v: Boolean): PrefsEditor
    fun putString(k: String, v: String): PrefsEditor
    fun apply()
}

/** Tutto cio' che cambia da una piattaforma all'altra. Il resto del gioco e' condiviso. */
interface PlatformHost {
    val supportsNotifications: Boolean
    fun prefs(name: String): Prefs
    fun beep()
    fun copyText(t: String)
    fun pasteText(): String
    fun saveFile(suggestedName: String, text: String, done: (Boolean) -> Unit)
    fun openFile(done: (String?) -> Unit)
    fun toImage(px: IntArray, w: Int, h: Int): ImageBitmap
    fun openUrl(url: String)
    fun httpGet(url: String): String?
    fun notifyTest(name: String)
    fun onResume(pet: Pet, now: Long)
    fun onPause(pet: Pet, now: Long)

    @Composable
    fun BackHandlerCompat(enabled: Boolean, onBack: () -> Unit)

    @Composable
    fun insetsModifier(): Modifier
}

object Platform {
    lateinit var host: PlatformHost
}
