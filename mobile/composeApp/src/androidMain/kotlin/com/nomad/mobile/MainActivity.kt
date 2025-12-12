package com.nomad.mobile

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview

class MainActivity : ComponentActivity() {

    private var isStreaming by mutableStateOf(false)
    private var pendingServerAddress: String? = null

    private val connectionStatusReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioStreamService.ACTION_CONNECTION_STATUS) {
                isStreaming = intent.getBooleanExtra(AudioStreamService.EXTRA_IS_CONNECTED, false)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val mediaProjectionManager = getSystemService(android.media.projection.MediaProjectionManager::class.java)

        val platform = AndroidPlatform(applicationContext) { address ->
            pendingServerAddress = address
            mediaProjectionLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
        }

        setContent {
            App(platform, isStreaming)
        }
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter(AudioStreamService.ACTION_CONNECTION_STATUS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(connectionStatusReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(connectionStatusReceiver, filter)
        }

        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
             requestPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (!isGranted) {
                // TODO: Inform user that permission is needed
            }
        }

    private val mediaProjectionLauncher =
        registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                val intent = Intent(this, AudioStreamService::class.java).apply {
                    putExtra("serverAddress", pendingServerAddress)
                    putExtra(AudioStreamService.EXTRA_RESULT_CODE, result.resultCode)
                    putExtra(AudioStreamService.EXTRA_RESULT_DATA, result.data)
                }
                startForegroundService(intent)
            }
        }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(connectionStatusReceiver)
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    val previewPlatform = object : Platform {
        override fun startStreaming(serverAddress: String) {}
        override fun stopStreaming() {}
    }
    App(previewPlatform, isStreaming = false)
}
