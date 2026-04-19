package com.sk0711.graph.extension

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.extension.DataTypeImpl
import io.hammerhead.karooext.extension.KarooExtension
import com.sk0711.graph.graph.TimeWindow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class HrPowerExtension : KarooExtension(EXTENSION_ID, "1.3") {

    private lateinit var karooSystem: KarooSystemService
    private val collectScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _hrTimeWindow = MutableStateFlow(TimeWindow.FIVE_MIN)
    private val hrTimeWindow: StateFlow<TimeWindow> = _hrTimeWindow.asStateFlow()

    private val _powerTimeWindow = MutableStateFlow(TimeWindow.FIVE_MIN)
    private val powerTimeWindow: StateFlow<TimeWindow> = _powerTimeWindow.asStateFlow()

    private val toggleReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.getStringExtra(EXTRA_FIELD_ID)) {
                HrZoneGraphDataType.TYPE_ID -> _hrTimeWindow.value = _hrTimeWindow.value.next()
                PowerZoneGraphDataType.TYPE_ID -> _powerTimeWindow.value = _powerTimeWindow.value.next()
            }
        }
    }

    override val types: List<DataTypeImpl> by lazy {
        listOf(
            HrZoneGraphDataType(karooSystem, hrTimeWindow, EXTENSION_ID),
            PowerZoneGraphDataType(karooSystem, powerTimeWindow, EXTENSION_ID),
        )
    }

    override fun onCreate() {
        super.onCreate()
        karooSystem = KarooSystemService(applicationContext)
        karooSystem.connect()
        ContextCompat.registerReceiver(
            this,
            toggleReceiver,
            IntentFilter(ACTION_TOGGLE_WINDOW),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        types.filterIsInstance<BaseGraphDataType>().forEach { it.startCollecting(collectScope) }
    }

    override fun onDestroy() {
        collectScope.cancel()
        unregisterReceiver(toggleReceiver)
        karooSystem.disconnect()
        super.onDestroy()
    }

    companion object {
        const val EXTENSION_ID = "sk0711-graph"
        const val ACTION_TOGGLE_WINDOW = "com.sk0711.graph.TOGGLE_WINDOW"
        const val EXTRA_FIELD_ID = "field_id"
    }
}
