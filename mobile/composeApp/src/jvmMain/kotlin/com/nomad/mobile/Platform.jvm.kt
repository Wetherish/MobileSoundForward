package com.nomad.mobile

class JVMPlatform: Platform {
    override fun startStreaming(serverAddress: String) {
        println("Streaming to $serverAddress not supported on JVM yet.")
    }

    override fun stopStreaming() {
        println("Streaming stopped on JVM.")
    }
}