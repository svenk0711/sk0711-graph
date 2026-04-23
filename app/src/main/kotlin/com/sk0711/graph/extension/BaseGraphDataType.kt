package com.sk0711.graph.extension

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.widget.RemoteViews
import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.extension.DataTypeImpl
import io.hammerhead.karooext.internal.Emitter
import io.hammerhead.karooext.internal.ViewEmitter
import io.hammerhead.karooext.models.RideState
import io.hammerhead.karooext.models.StreamState
import io.hammerhead.karooext.models.UpdateGraphicConfig
import io.hammerhead.karooext.models.ViewConfig
import com.sk0711.graph.R
import com.sk0711.graph.graph.DataBuffer
import com.sk0711.graph.graph.GraphRenderer
import com.sk0711.graph.graph.PowerSmoother
import com.sk0711.graph.graph.Sample
import com.sk0711.graph.graph.SyntheticData
import com.sk0711.graph.graph.TimeWindow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

abstract class BaseGraphDataType(
    private val karooSystem: KarooSystemService,
    private val timeWindowState: StateFlow<TimeWindow>,
    extension: String,
    typeId: String,
) : DataTypeImpl(extension, typeId) {

    protected abstract val valueDataType: String
    protected abstract val zoneDataType: String
    protected abstract val avgDataType: String
    protected abstract val maxDataType: String
    protected abstract val kind: GraphRenderer.Kind

    private val buffer = DataBuffer()
    private var smoother: PowerSmoother? = null
    private val sampleSignal = MutableStateFlow(0L)
    private val avgFlow = MutableStateFlow(0)
    private val maxFlow = MutableStateFlow(0)
    private var cachedTogglePendingIntent: PendingIntent? = null

    fun startCollecting(scope: CoroutineScope) {
        if (kind == GraphRenderer.Kind.POWER) smoother = PowerSmoother()

        scope.launch {
            combine(
                karooSystem.streamDataFlow(valueDataType),
                karooSystem.streamDataFlow(zoneDataType),
                karooSystem.consumerFlow<RideState>().onStart { emit(RideState.Recording) },
            ) { v, z, r -> Triple(v, z, r) }.collect { (valueState, zoneState, rideState) ->
                if (rideState is RideState.Paused) return@collect
                val raw = (valueState as? StreamState.Streaming)?.dataPoint?.singleValue?.toFloat()
                    ?: return@collect
                val zone = (zoneState as? StreamState.Streaming)?.dataPoint?.singleValue?.toInt() ?: 0
                val now = System.currentTimeMillis()
                val smoothed = smoother?.add(now, raw) ?: raw
                buffer.add(Sample(now, smoothed, zone))
                sampleSignal.value = now
            }
        }

        scope.launch {
            karooSystem.streamDataFlow(avgDataType).collect { s ->
                (s as? StreamState.Streaming)?.dataPoint?.singleValue?.toInt()?.let { avgFlow.value = it }
            }
        }
        scope.launch {
            karooSystem.streamDataFlow(maxDataType).collect { s ->
                (s as? StreamState.Streaming)?.dataPoint?.singleValue?.toInt()?.let { maxFlow.value = it }
            }
        }
    }

    override fun startStream(emitter: Emitter<StreamState>) {
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            karooSystem.streamDataFlow(valueDataType).collect { state ->
                emitter.onNext(state)
            }
        }
        emitter.setCancellable { scope.cancel() }
    }

    override fun startView(context: Context, config: ViewConfig, emitter: ViewEmitter) {
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            emitter.onNext(UpdateGraphicConfig(showHeader = false, formatDataTypeId = null))

            if (config.preview) {
                emitter.updateView(renderPreview(context, config))
                return@launch
            }

            combine(
                sampleSignal,
                timeWindowState,
                avgFlow,
                maxFlow,
            ) { _, window, avg, max -> Stats(window, avg, max) }
                .collect { stats ->
                    val now = System.currentTimeMillis()
                    emitter.updateView(
                        buildRemoteViews(context, config, buffer.snapshot(), stats.window, stats.avg, stats.max, now),
                    )
                }
        }
        emitter.setCancellable { scope.cancel() }
    }

    private data class Stats(val window: TimeWindow, val avg: Int, val max: Int)

    private fun renderPreview(context: Context, config: ViewConfig): RemoteViews {
        val now = System.currentTimeMillis()
        val samples = when (kind) {
            GraphRenderer.Kind.HR -> SyntheticData.hrSamples(nowMs = now)
            GraphRenderer.Kind.POWER -> SyntheticData.powerSamples(nowMs = now)
        }
        val avg = if (samples.isEmpty()) 0 else (samples.sumOf { it.value.toDouble() } / samples.size).toInt()
        val max = samples.maxOfOrNull { it.value }?.toInt() ?: 0
        return buildRemoteViews(context, config, samples, timeWindowState.value, avg, max, now)
    }

    private fun buildRemoteViews(
        context: Context,
        config: ViewConfig,
        samples: List<Sample>,
        window: TimeWindow,
        avg: Int,
        max: Int,
        nowMs: Long,
    ): RemoteViews {
        val (w, h) = config.viewSize
        val isDark = (context.resources.configuration.uiMode
            and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val bitmap = GraphRenderer.render(
            samples = samples,
            avg = avg,
            max = max,
            widthPx = w,
            heightPx = h,
            timeWindowSec = window.seconds,
            nowMs = nowMs,
            kind = kind,
            isDark = isDark,
            windowLabel = window.label,
        )
        val rv = RemoteViews(context.packageName, R.layout.field_graph)
        rv.setImageViewBitmap(R.id.graph, bitmap)
        if (!config.preview) {
            rv.setOnClickPendingIntent(R.id.graph, togglePendingIntent(context))
        }
        return rv
    }

    private fun togglePendingIntent(context: Context): PendingIntent {
        cachedTogglePendingIntent?.let { return it }
        val action = when (kind) {
            GraphRenderer.Kind.HR -> HrPowerExtension.ACTION_TOGGLE_HR
            GraphRenderer.Kind.POWER -> HrPowerExtension.ACTION_TOGGLE_POWER
        }
        val requestCode = when (kind) {
            GraphRenderer.Kind.HR -> 101
            GraphRenderer.Kind.POWER -> 102
        }
        val intent = Intent(action).setPackage(context.packageName)
        val pi = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        cachedTogglePendingIntent = pi
        return pi
    }
}
