package com.marco.pixipet

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    private lateinit var game: PixiGame

    private var pendingText = ""
    private var pendingSave: ((Boolean) -> Unit)? = null
    private var pendingOpen: ((String?) -> Unit)? = null

    private val createDoc =
        registerForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
            val done = pendingSave
            pendingSave = null
            var ok = false
            if (uri != null) {
                try {
                    contentResolver.openOutputStream(uri, "wt")?.use { it.write(pendingText.toByteArray()) }
                    ok = true
                } catch (e: Throwable) {
                }
            }
            pendingText = ""
            done?.invoke(ok)
        }

    private val openDoc =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            val done = pendingOpen
            pendingOpen = null
            var text: String? = null
            if (uri != null) {
                try {
                    text = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                } catch (e: Throwable) {
                }
            }
            done?.invoke(text)
        }

    private val permLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        val host = AndroidHost(applicationContext)
        host.saver = { name, text, done ->
            pendingText = text
            pendingSave = done
            createDoc.launch(name)
        }
        host.opener = { done ->
            pendingOpen = done
            openDoc.launch(arrayOf("text/plain", "*/*"))
        }
        Platform.host = host
        game = PixiGame()
        setContent { PixiPetApp(game) }

        Notifier.ensureChannel(this)
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    override fun onResume() {
        super.onResume()
        Notifier.foreground = true
        game.onResume()
    }

    override fun onPause() {
        super.onPause()
        Notifier.foreground = false
        game.onPause()
    }
}
