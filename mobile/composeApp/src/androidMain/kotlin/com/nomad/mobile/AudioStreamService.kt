package com.nomad.mobile

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class AudioStreamService : Service() {

    private val CHANNEL_ID = "AudioStreamServiceChannel"

    companion object {
        const val ACTION_CONNECTION_STATUS = "com.nomad.mobile.CONNECTION_STATUS"
        const val EXTRA_IS_CONNECTED = "isConnected"
        const val EXTRA_RESULT_CODE = "resultCode"
        const val EXTRA_RESULT_DATA = "resultData"
    }

    private var streamingThread: Thread? = null
    @Volatile
    private var isStreaming = false
    private var mediaProjection: android.media.projection.MediaProjection? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Audio Stream Active")
            .setContentText("Streaming system audio to server.")
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else {
            startForeground(1, notification)
        }

        val serverAddress = intent?.getStringExtra("serverAddress") ?: return START_NOT_STICKY
        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, 0) ?: 0
        val resultData = intent?.getParcelableExtra<Intent>(EXTRA_RESULT_DATA)

        if (resultCode != 0 && resultData != null) {
            startStreaming(serverAddress, resultCode, resultData)
            broadcastConnectionStatus(true)
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopStreaming()
        broadcastConnectionStatus(false)
    }

    private fun startStreaming(address: String, resultCode: Int, resultData: Intent) {
        if (isStreaming) return
        isStreaming = true
        streamingThread = Thread {
            streamAudio(address, resultCode, resultData)
        }.apply { start() }
    }

    private fun stopStreaming() {
        isStreaming = false
        streamingThread?.join()
        streamingThread = null
        mediaProjection?.stop()
        mediaProjection = null
    }

    private fun streamAudio(ipAddress: String, resultCode: Int, resultData: Intent) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return

        val sampleRate = 48000
        val channelConfig = android.media.AudioFormat.CHANNEL_IN_STEREO
        val audioFormat = android.media.AudioFormat.ENCODING_PCM_16BIT
        val bufferSize = android.media.AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        val actualBufferSize = maxOf(bufferSize, 4096) 

        val mpManager = getSystemService(android.media.projection.MediaProjectionManager::class.java)
        mediaProjection = mpManager.getMediaProjection(resultCode, resultData)

        val config = android.media.AudioPlaybackCaptureConfiguration.Builder(mediaProjection!!)
            .addMatchingUsage(android.media.AudioAttributes.USAGE_MEDIA)
            .addMatchingUsage(android.media.AudioAttributes.USAGE_GAME)
            .addMatchingUsage(android.media.AudioAttributes.USAGE_UNKNOWN)
            .build()

        val audioFormatObj = android.media.AudioFormat.Builder()
            .setEncoding(audioFormat)
            .setSampleRate(sampleRate)
            .setChannelMask(channelConfig)
            .build()

        val recorder = android.media.AudioRecord.Builder()
            .setAudioFormat(audioFormatObj)
            .setBufferSizeInBytes(actualBufferSize)
            .setAudioPlaybackCaptureConfig(config)
            .build()

        try {
            val address = java.net.InetAddress.getByName(ipAddress)
            val socket = java.net.DatagramSocket()
            val port = 12345

            recorder.startRecording()

            val payloadSize = 1400
            val headerSize = 8
            val buffer = ByteArray(payloadSize)
            val packetBuffer = java.nio.ByteBuffer.allocate(headerSize + payloadSize).order(java.nio.ByteOrder.BIG_ENDIAN)

            var seqNum = 0
            val startTime = System.currentTimeMillis()

            while (isStreaming) {
                val read = recorder.read(buffer, 0, payloadSize)
                if (read > 0) {
                    packetBuffer.clear()
                    
                    // Header
                    packetBuffer.putInt(seqNum++)
                    packetBuffer.putInt((System.currentTimeMillis() - startTime).toInt())
                    
                    // Payload
                    packetBuffer.put(buffer, 0, read)

                    val packet = java.net.DatagramPacket(
                        packetBuffer.array(),
                        packetBuffer.position(),
                        address,
                        port
                    )
                    socket.send(packet)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                recorder.stop()
                recorder.release()
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Audio Stream Service Channel",
                NotificationManager.IMPORTANCE_LOW 
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun broadcastConnectionStatus(isConnected: Boolean) {
        val intent = Intent(ACTION_CONNECTION_STATUS).apply {
            putExtra(EXTRA_IS_CONNECTED, isConnected)
        }
        sendBroadcast(intent)
    }
}