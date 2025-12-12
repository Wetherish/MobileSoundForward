package com.nomad.mobile

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Mobile",
    ) {
        App(JVMPlatform(), isStreaming = false)
    }
}