package com.sk0711.graph.extension

import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.models.DataType
import com.sk0711.graph.graph.GraphRenderer
import com.sk0711.graph.graph.TimeWindow
import kotlinx.coroutines.flow.StateFlow

class HrZoneGraphDataType(
    karooSystem: KarooSystemService,
    timeWindowState: StateFlow<TimeWindow>,
    extension: String,
) : BaseGraphDataType(karooSystem, timeWindowState, extension, TYPE_ID) {

    override val valueDataType: String = DataType.Type.HEART_RATE
    override val zoneDataType: String = DataType.Type.HR_ZONE
    override val avgDataType: String = DataType.Type.AVERAGE_HR
    override val maxDataType: String = DataType.Type.MAX_HR
    override val kind: GraphRenderer.Kind = GraphRenderer.Kind.HR

    companion object {
        const val TYPE_ID = "hr-zone-graph"
    }
}
