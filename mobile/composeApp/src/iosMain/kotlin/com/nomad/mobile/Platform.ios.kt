package com.nomad.mobile

class IOSPlatform: Platform {
    override fun startStreaming(serverAddress: String) {
        println("Streaming to $serverAddress not supported on iOS yet.")
    }

    override fun stopStreaming() {
        println("Streaming stopped on iOS.")
    }
}