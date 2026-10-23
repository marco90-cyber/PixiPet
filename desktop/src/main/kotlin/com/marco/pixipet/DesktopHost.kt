package com.marco.pixipet

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import java.awt.Desktop
import java.awt.FileDialog
import java.awt.Frame
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.image.BufferedImage
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.util.Properties
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem

class FilePrefs(private val file: File) : Prefs {
    private val props = Properties()

    init {
        try {
            if (file.exists()) file.reader(Charsets.UTF_8).use { props.load(it) }
        } catch (e: Throwable) {
        }
    }

    @Synchronized
    private fun save() {
        try {
            file.parentFile?.mkdirs()
            file.writer(Charsets.UTF_8).use { props.store(it, "PixiPet") }
        } catch (e: Throwable) {
        }
    }

    override fun getInt(k: String, d: Int) = props.getProperty(k)?.toIntOrNull() ?: d
    override fun getBoolean(k: String, d: Boolean) = props.getProperty(k)?.toBooleanStrictOrNull() ?: d
    override fun getString(k: String, d: String?): String? = props.getProperty(k) ?: d

    override fun edit(): PrefsEditor = object : PrefsEditor {
        override fun putInt(k: String, v: Int): PrefsEditor { props.setProperty(k, v.toString()); return this }
        override fun putBoolean(k: String, v: Boolean): PrefsEditor { props.setProperty(k, v.toString()); return this }
        override fun putString(k: String, v: String): PrefsEditor { props.setProperty(k, v); return this }
        override fun apply() = save()
    }
}

class DesktopHost : PlatformHost {
    override val supportsNotifications = false

    private val dir = File(System.getProperty("user.home"), ".pixipet")

    override fun prefs(name: String): Prefs = FilePrefs(File(dir, "$name.properties"))

    override fun beep() {
        Thread {
            try {
                val rate = 22050f
                val n = (rate * 0.05f).toInt()
                val buf = ByteArray(n)
                for (i in 0 until n) {
                    val fade = 1f - i / n.toFloat()
                    buf[i] = (Math.sin(2.0 * Math.PI * 880.0 * i / rate) * 40 * fade).toInt().toByte()
                }
                val line = AudioSystem.getSourceDataLine(AudioFormat(rate, 8, 1, true, false))
                line.open()
                line.start()
                line.write(buf, 0, n)
                line.drain()
                line.close()
            } catch (e: Throwable) {
            }
        }.start()
    }

    override fun copyText(t: String) {
        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(t), null)
    }

    override fun saveFile(suggestedName: String, text: String, done: (Boolean) -> Unit) {
        Thread {
            var ok = false
            try {
                val d = FileDialog(null as Frame?, "Salva partita", FileDialog.SAVE)
                d.file = suggestedName
                d.isVisible = true
                val f = d.file
                if (f != null) {
                    File(d.directory, f).writeText(text, Charsets.UTF_8)
                    ok = true
                }
            } catch (e: Throwable) {
            }
            done(ok)
        }.start()
    }

    override fun openFile(done: (String?) -> Unit) {
        Thread {
            var text: String? = null
            try {
                val d = FileDialog(null as Frame?, "Carica partita", FileDialog.LOAD)
                d.isVisible = true
                val f = d.file
                if (f != null) text = File(d.directory, f).readText(Charsets.UTF_8)
            } catch (e: Throwable) {
            }
            done(text)
        }.start()
    }

    override fun pasteText(): String = try {
        Toolkit.getDefaultToolkit().systemClipboard.getData(DataFlavor.stringFlavor) as String
    } catch (e: Throwable) {
        ""
    }

    override fun toImage(px: IntArray, w: Int, h: Int): ImageBitmap {
        val img = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
        img.setRGB(0, 0, w, h, px, 0, w)
        return img.toComposeImageBitmap()
    }

    override fun openUrl(url: String) {
        try {
            Desktop.getDesktop().browse(URI(url))
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

    override fun notifyTest(name: String) {}
    override fun onResume(pet: Pet, now: Long) {}
    override fun onPause(pet: Pet, now: Long) {}

    @Composable
    override fun BackHandlerCompat(enabled: Boolean, onBack: () -> Unit) {
    }

    @Composable
    override fun insetsModifier(): Modifier = Modifier
}
