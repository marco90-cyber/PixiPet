package com.marco.pixipet

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private fun shellColor(i: Int): Color = Color(0xFF000000L or SHELLS[i % SHELLS.size])

private fun tone(c: Color, t: Float, toward: Color): Color = Color(
    red = c.red + (toward.red - c.red) * t,
    green = c.green + (toward.green - c.green) * t,
    blue = c.blue + (toward.blue - c.blue) * t,
    alpha = 1f
)

@Composable
fun PixiPetApp(game: PixiGame) {
    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { game.frame(System.currentTimeMillis()) }
        }
    }

    val body = shellColor(game.shellIdx)
    val light = tone(body, 0.5f, Color.White)
    val dark = tone(body, 0.3f, Color.Black)
    val shell = game.showShell

    val bg = if (shell) {
        Brush.verticalGradient(listOf(light, body))
    } else {
        Brush.verticalGradient(listOf(Color(0xFF1E1833), Color(0xFF14111F)))
    }

    // Il guscio occupa tutta la finestra (anche sotto le barre di sistema).
    Box(Modifier.fillMaxSize().background(bg)) {
        if (shell) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(5.dp)
                    .border(2.dp, dark.copy(alpha = 0.55f), RoundedCornerShape(36.dp))
            )
        }
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
        ) {
            val landscape = maxWidth > maxHeight * 1.05f
            if (landscape) {
                LandscapeLayout(game, maxWidth, maxHeight, shell, light, dark)
            } else {
                PortraitLayout(game, maxWidth, maxHeight, shell, light, dark)
            }
        }
    }

    if (game.showRename) {
        var txt by remember { mutableStateOf(game.pet.name) }
        AlertDialog(
            onDismissRequest = { game.showRename = false },
            confirmButton = {
                TextButton(onClick = {
                    game.rename(txt)
                    game.showRename = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { game.showRename = false }) { Text("CANCEL") }
            },
            title = { Text("Rename") },
            text = {
                OutlinedTextField(
                    value = txt,
                    onValueChange = { txt = it.take(8) },
                    singleLine = true
                )
            }
        )
    }
}

/** Telefono in verticale: logo in alto, schermo grande, 3 pulsanti in basso. */
@Composable
private fun PortraitLayout(
    game: PixiGame, w: Dp, h: Dp, shell: Boolean, light: Color, dark: Color
) {
    val margin = if (shell) 18.dp else 8.dp
    val btn = (w * 0.18f).coerceIn(52.dp, 88.dp)
    val header = if (shell) 56.dp else 0.dp
    val availH = h - btn - header - 48.dp
    val side = minOf(w - margin * 2, availH).coerceAtLeast(160.dp)

    Column(
        Modifier.fillMaxSize().padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        if (shell) Header(dark)
        ScreenView(game, side, shell)
        Row(
            horizontalArrangement = Arrangement.spacedBy(btn * 0.5f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ShellButton("◀", btn, light, dark, shell) { game.press(0) }
            ShellButton("●", btn, light, dark, shell) { game.press(1) }
            ShellButton("▶", btn, light, dark, shell) { game.press(2) }
        }
    }
}

/** Tablet o telefono girato: schermo al centro, ◀ a sinistra, ● e ▶ a destra. */
@Composable
private fun LandscapeLayout(
    game: PixiGame, w: Dp, h: Dp, shell: Boolean, light: Color, dark: Color
) {
    val btn = (h * 0.2f).coerceIn(48.dp, 80.dp)
    val side = minOf(h - 24.dp, w - btn * 2 - 64.dp).coerceAtLeast(140.dp)

    Row(
        Modifier.fillMaxSize().padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ShellButton("◀", btn, light, dark, shell) { game.press(0) }
        ScreenView(game, side, shell)
        Column(
            verticalArrangement = Arrangement.spacedBy(btn * 0.4f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ShellButton("●", btn, light, dark, shell) { game.press(1) }
            ShellButton("▶", btn, light, dark, shell) { game.press(2) }
        }
    }
}

@Composable
private fun Header(dark: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(22.dp)
                .border(3.dp, dark, CircleShape)
        )
        Text(
            "PIXIPET",
            color = dark,
            fontSize = 13.sp,
            letterSpacing = 5.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun ScreenView(game: PixiGame, side: Dp, shell: Boolean) {
    val bezel = if (shell) 10.dp else 0.dp
    Box(
        Modifier
            .size(side)
            .background(Color(0xFF15131C), RoundedCornerShape(if (shell) 18.dp else 8.dp))
            .padding(bezel)
    ) {
        Canvas(
            Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(if (shell) 8.dp else 6.dp))
                .pointerInput(Unit) {
                    detectTapGestures { o ->
                        game.tapScreen(o.x / size.width, o.y / size.height)
                    }
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { change, _ ->
                        game.dragScreen(change.position.x / size.width)
                    }
                }
        ) {
            val img = game.image
            if (img != null) {
                drawImage(
                    img,
                    dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                    filterQuality = FilterQuality.None
                )
            }
        }
    }
}

@Composable
private fun ShellButton(
    label: String, size: Dp, light: Color, dark: Color, shell: Boolean, onClick: () -> Unit
) {
    val face = if (shell) light else Color(0xFF3A3358)
    val rim = if (shell) dark else Color(0xFF5B4B8A)
    Box(
        Modifier
            .size(size)
            .background(face, CircleShape)
            .border(3.dp, rim, CircleShape)
            .clip(CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (shell) dark else Color.White,
            fontSize = (size.value * 0.36f).sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}
