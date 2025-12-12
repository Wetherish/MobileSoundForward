package com.nomad.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun App(platform: Platform, isStreaming: Boolean) {
    MaterialTheme {
        var serverAddress by remember { mutableStateOf("") }

        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primaryContainer)
                .safeContentPadding()
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            OutlinedTextField(
                value = serverAddress,
                onValueChange = { serverAddress = it },
                label = { Text("Server Address") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = {
                    platform.startStreaming(serverAddress)
                }) {
                    Text("Start Streaming")
                }
                Button(onClick = {
                    platform.stopStreaming()
                }) {
                    Text("Stop Streaming")
                }
            }
            Text(
                text = if (isStreaming) "Connected" else "Not Connected",
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview
@Composable
fun AppPreview() {
    val previewPlatform = object : Platform {
        override fun startStreaming(serverAddress: String) {}
        override fun stopStreaming() {}
    }
    App(platform = previewPlatform, isStreaming = true)
}