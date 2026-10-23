package com.marco.pixipet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import java.awt.Dimension

// Dimensioni "di riferimento" del telefono: la finestra le scala mantenendo le proporzioni.
private val REF_W = 412.dp
private val REF_H = 860.dp

private fun handleKey(game: PixiGame, e: KeyEvent): Boolean {
    if (e.type != KeyEventType.KeyDown) return false
    if (game.scr == Scr.NAME) {
        when (e.key) {
            Key.Backspace -> { game.nameDel(); return true }
            Key.Enter, Key.NumPadEnter -> { game.nameOk(); return true }
            else -> {}
        }
        val c = e.utf16CodePoint.toChar().uppercaseChar()
        if (c in 'A'..'Z' || c in '0'..'9') {
            game.nameKey(c)
            return true
        }
        return false
    }
    return when (e.key) {
        Key.Escape -> if (game.canGoBack()) { game.back(); true } else false
        Key.DirectionLeft -> { game.press(0); true }
        Key.DirectionRight -> { game.press(2); true }
        Key.Enter, Key.NumPadEnter, Key.Spacebar -> { game.press(1); true }
        else -> false
    }
}

fun main() {
    Platform.host = DesktopHost()
    val game = PixiGame()
    game.onResume()
    application {
        val state = rememberWindowState(size = DpSize(440.dp, 900.dp))
        Window(
            onCloseRequest = {
                game.onPause()
                exitApplication()
            },
            state = state,
            title = "PixiPet",
            onKeyEvent = { handleKey(game, it) }
        ) {
            window.minimumSize = Dimension(330, 640)
            BoxWithConstraints(
                Modifier.fillMaxSize().background(Color(0xFF0F0C1A)),
                contentAlignment = Alignment.Center
            ) {
                val base = LocalDensity.current
                val s = minOf(maxWidth / REF_W, maxHeight / REF_H)
                CompositionLocalProvider(LocalDensity provides Density(base.density * s, base.fontScale)) {
                    Box(Modifier.size(REF_W, REF_H)) { PixiPetApp(game) }
                }
            }
        }
    }
}
