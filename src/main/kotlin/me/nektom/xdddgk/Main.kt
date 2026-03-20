package me.nektom.xdddgk

import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import me.nektom.xdddgk.ui.App
import me.nektom.xdddgk.ui.AppState

fun main() = application {
    val windowState = rememberWindowState(width = 1200.dp, height = 700.dp)
    Window(
        onCloseRequest = ::exitApplication,
        title = "Cloudflare DNS Editor",
        state = windowState
    ) {
        val scope = rememberCoroutineScope()
        val appState = remember { AppState(scope) }
        App(appState)
    }
}
