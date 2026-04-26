package com.sk0711.graph.extension

import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.extension.DataTypeImpl
import io.hammerhead.karooext.extension.KarooExtension
import com.sk0711.graph.graph.TimeWindow
import com.sk0711.graph.settings.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class HrPowerExtension : KarooExtension(EXTENSION_ID, "0.1.5") {

    private lateinit var karooSystem: KarooSystemService
    private val collectScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _hrTimeWindow = MutableStateFlow(TimeWindow.FIVE_MIN)
    private val hrTimeWindow: StateFlow<TimeWindow> = _hrTimeWindow.asStateFlow()

    private val _powerTimeWindow = MutableStateFlow(TimeWindow.FIVE_MIN)
    private val powerTimeWindow: StateFlow<TimeWindow> = _powerTimeWindow.asStateFlow()

    override val types: List<DataTypeImpl> by lazy {
        listOf(
            HrZoneGraphDataType(karooSystem, hrTimeWindow, EXTENSION_ID),
            PowerZoneGraphDataType(karooSystem, powerTimeWindow, EXTENSION_ID, useNp = false),
            PowerZoneGraphDataType(karooSystem, powerTimeWindow, EXTENSION_ID, useNp = true),
        )
    }

    fun toggleHrWindow() {
        _hrTimeWindow.value = _hrTimeWindow.value.next()
    }

    fun togglePowerWindow() {
        _powerTimeWindow.value = _powerTimeWindow.value.next()
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        karooSystem = KarooSystemService(applicationContext)
        karooSystem.connect()
        collectScope.launch {
            val saved = AppSettings.defaultTimeWindowFlow(applicationContext).first()
            _hrTimeWindow.value = saved
            _powerTimeWindow.value = saved
        }
        types.filterIsInstance<BaseGraphDataType>().forEach { it.startCollecting(collectScope) }
    }

    override fun onDestroy() {
        instance = null
        collectScope.cancel()
        karooSystem.disconnect()
        super.onDestroy()
    }

    companion object {
        const val EXTENSION_ID = "sk0711-graph"
        const val ACTION_TOGGLE_HR = "com.sk0711.graph.TOGGLE_HR"
        const val ACTION_TOGGLE_POWER = "com.sk0711.graph.TOGGLE_POWER"

        @Volatile private var instance: HrPowerExtension? = null
        fun instance(): HrPowerExtension? = instance
    }
}
