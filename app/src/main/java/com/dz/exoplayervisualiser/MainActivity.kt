package com.dz.exoplayervisualiser

import android.media.audiofx.Visualizer
import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.ui.PlayerView
import com.dz.exoplayervisualiser.ui.theme.ExoPlayerVisualiserTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow


class MainActivity : ComponentActivity() {

    private val vm by viewModels<HomeViewModel>()
    private val exoPlayer by lazy {
        ExoPlayer.Builder(application).build().apply {
            addAnalyticsListener(object : AnalyticsListener {

                override fun onRenderedFirstFrame(
                    eventTime: AnalyticsListener.EventTime,
                    output: Any,
                    renderTimeMs: Long
                ) {
                    super.onRenderedFirstFrame(eventTime, output, renderTimeMs)
                    startPlayerVisualizer(audioSessionId, vm)
                    visualizer.enabled = true
                }
            })
        }
    }


    private lateinit var visualizer: Visualizer

    @UnstableApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ExoPlayerVisualiserTheme {
                Scaffold(modifier = Modifier.fillMaxWidth()) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .padding(innerPadding)
                    ) {
                        AndroidView(modifier = Modifier
                            .fillMaxWidth(),
                            factory = { context ->
                                PlayerView(context).apply {
                                    this.player = exoPlayer.apply {
                                        playWhenReady = true
                                        preparePlayer("https://www.learningcontainer.com/wp-content/uploads/2020/05/sample-mp4-file.mp4")
                                    }
                                    layoutParams = ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.WRAP_CONTENT
                                    )
                                }
                            })

                        StatusBarVisualizer(modifier = Modifier, visualizer = vm.visualizer)
                    }
                }
            }
        }
    }

    @UnstableApi
    private fun ExoPlayer.preparePlayer(videoUri: String) {
        setMediaItem(MediaItem.fromUri(videoUri))
        prepare()
        play()
    }

    private fun startPlayerVisualizer(audioSessionId: Int, vm: HomeViewModel) {
        visualizer = Visualizer(audioSessionId).apply {
            enabled = false
            captureSize = Visualizer.getCaptureSizeRange()[1]
            setDataCaptureListener(
                object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(
                        visualizer: Visualizer, waveform: ByteArray, samplingRate: Int
                    ) {
                        var sum = 0
                        for (i in 1 until waveform.size) {
                            sum += waveform[i] + 128
                        }

                        val normalized = sum / waveform.size.toFloat()
                        vm.updateVisualizer(normalized.toInt())
                    }

                    override fun onFftDataCapture(
                        visualizer: Visualizer?,
                        fft: ByteArray?,
                        samplingRate: Int
                    ) {
                        // Do nothing
                    }
                }, Visualizer.getMaxCaptureRate() / 2, true, true
            )
        }
    }
}

@Composable
fun StatusBarVisualizer(modifier: Modifier = Modifier, visualizer: StateFlow<Int>) {
    val value = visualizer.collectAsState(initial = 0)
    Column(modifier = modifier) {
        // Visualizer bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(200.dp)
                .background(color = Color.Gray)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(value.value.dp)
                    .background(color = Color.Green)
            )
        }
    }
}

class HomeViewModel : ViewModel() {
    private val _visualizer = MutableStateFlow(0)
    val visualizer: StateFlow<Int> = _visualizer

    fun updateVisualizer(value: Int) {
        _visualizer.value = value
    }
}