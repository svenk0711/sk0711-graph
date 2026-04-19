package com.sk0711.graph.extension

import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.models.DataType
import com.sk0711.graph.graph.GraphRenderer
import com.sk0711.graph.graph.TimeWindow
import kotlinx.coroutines.flow.StateFlow

class PowerZoneGraphDataType(
    karooSystem: KarooSystemService,
    timeWindowState: StateFlow<TimeWindow>,
    extension: String,
) : BaseGraphDataType(karooSystem, timeWindowState, extension, TYPE_ID) {

    override val valueDataType: String = DataType.Type.POWER
    override val zoneDataType: String = DataType.Type.POWER_ZONE
    override val avgDataType: String = DataType.Type.AVERAGE_POWER
    override val maxDataType: String = DataType.Type.MAX_POWER
    override val kind: GraphRenderer.Kind = GraphRenderer.Kind.POWER

    companion object {
        const val TYPE_ID = "power-zone-graph"
    }
}
