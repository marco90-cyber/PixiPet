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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
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
    val bodyLight = tone(body, 0.45f, Color.White)
    val bodyDark = tone(body, 0.28f, Color.Black)
    val shell = game.showShell

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF3A2F5B), Color(0xFF1E1833))))
            .windowInsetsPadding(WindowInsets.systemBars),
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val side = minOf(
                maxWidth * (if (shell) 0.80f else 0.96f),
                maxHeight - (if (shell) 230.dp else 130.dp)
            ).coerceAtLeast(180.dp)

            val deviceModifier = if (shell) {
                val shape = RoundedCornerShape(
                    topStart = 160.dp, topEnd = 160.dp, bottomStart = 120.dp, bottomEnd = 120.dp
                )
                Modifier
                    .background(Brush.verticalGradient(listOf(bodyLight, body)), shape)
                    .border(3.dp, bodyDark, shape)
                    .padding(start = 22.dp, end = 22.dp, top = 38.dp, bottom = 26.dp)
            } else {
                Modifier
            }

            Column(deviceModifier, horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .size(side)
                        .background(Color(0xFF15131C), RoundedCornerShape(14.dp))
                        .padding(8.dp)
                ) {
                    Canvas(
                        Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(6.dp))
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

                Spacer(Modifier.height(22.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(22.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ShellButton("◀", bodyLight, bodyDark, shell) { game.press(0) }
                    ShellButton("●", bodyLight, bodyDark, shell) { game.press(1) }
                    ShellButton("▶", bodyLight, bodyDark, shell) { game.press(2) }
                }
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

@Composable
private fun ShellButton(label: String, light: Color, dark: Color, shell: Boolean, onClick: () -> Unit) {
    val face = if (shell) tone(light, 0.0f, light) else Color(0xFF3A3358)
    val rim = if (shell) dark else Color(0xFF5B4B8A)
    Box(
        Modifier
            .size(56.dp)
            .background(face, CircleShape)
            .border(3.dp, rim, CircleShape)
            .clip(CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (shell) dark else Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}
