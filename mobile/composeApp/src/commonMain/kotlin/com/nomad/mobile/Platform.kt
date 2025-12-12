package com.nomad.mobile

interface Platform {
    fun startStreaming(serverAddress: String)
    fun stopStreaming()
}