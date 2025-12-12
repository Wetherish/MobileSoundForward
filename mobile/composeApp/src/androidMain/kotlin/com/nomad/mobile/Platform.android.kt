package com.nomad.mobile

import android.content.Context
import android.content.Intent

class AndroidPlatform(
    private val context: Context,
    private val onStart: (String) -> Unit
) : Platform {
    override fun startStreaming(serverAddress: String) {
        onStart(serverAddress)
    }

    override fun stopStreaming() {
        val intent = Intent(context, AudioStreamService::class.java)
        context.stopService(intent)
    }
}