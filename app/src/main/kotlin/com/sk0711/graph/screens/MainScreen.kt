package com.sk0711.graph.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sk0711.graph.R
import com.sk0711.graph.graph.GraphRenderer
import com.sk0711.graph.graph.Sample
import com.sk0711.graph.graph.SyntheticData
import com.sk0711.graph.graph.TimeWindow
import com.sk0711.graph.graph.ZoneGraph
import com.sk0711.graph.settings.AppSettings
import com.sk0711.graph.theme.AppTheme
import kotlinx.coroutines.launch

@Composable
fun MainScreen(close: () -> Unit = {}) {
    val window = TimeWindow.FIVE_MIN
    val now = 600_000L
    val hr = remember { SyntheticData.hrSamples(now) }
    val power = remember { SyntheticData.powerSamples(now) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val savedDefault by AppSettings.defaultTimeWindowFlow(context)
        .collectAsStateWithLifecycle(initialValue = TimeWindow.FIVE_MIN)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 12.dp)
                .padding(bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("HR — ${window.label}", color = MaterialTheme.colorScheme.onBackground)
            ZoneGraph(
                samples = hr,
                timeWindowSec = window.seconds,
                nowMs = now,
                kind = GraphRenderer.Kind.HR,
                windowLabel = window.label,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
            )

            Text("Power — ${window.label}", color = MaterialTheme.colorScheme.onBackground)
            ZoneGraph(
                samples = power,
                timeWindowSec = window.seconds,
                nowMs = now,
                kind = GraphRenderer.Kind.POWER,
                windowLabel = window.label,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
            )

            Text(
                "Adds two graphical data fields to your Karoo profile editor: " +
                    "heart-rate and power history with zone colouring.",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 12.sp,
            )
            Text(
                "Tap a field on the ride screen to cycle the time window " +
                    "(1 min / 5 min / full ride).",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 12.sp,
            )

            Text(
                "Default time window",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 12.sp,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                TimeWindow.entries.forEach { tw ->
                    val selected = tw == savedDefault
                    val modifier = Modifier.weight(1f)
                    if (selected) {
                        Button(
                            onClick = { /* already selected */ },
                            modifier = modifier,
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                        ) {
                            Text(tw.label, fontSize = 11.sp)
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    AppSettings.setDefaultTimeWindow(context, tw)
                                }
                            },
                            modifier = modifier,
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.onBackground,
                            ),
                        ) {
                            Text(tw.label, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        Icon(
            painter = painterResource(id = R.drawable.ic_back),
            contentDescription = "Back",
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 8.dp, bottom = 8.dp)
                .size(30.dp)
                .clickable { close() },
        )
    }
}

@Preview(widthDp = 256, heightDp = 426)
@Composable
fun PreviewMainScreen() {
    AppTheme { MainScreen() }
}

@Preview(widthDp = 200, heightDp = 80, backgroundColor = 0xFFFFFFFF, showBackground = true)
@Composable
fun PreviewHrTileLight() {
    val now = 600_000L
    val samples: List<Sample> = SyntheticData.hrSamples(now)
    AppTheme(darkTheme = false) {
        ZoneGraph(
            samples = samples,
            timeWindowSec = TimeWindow.ONE_MIN.seconds,
            nowMs = now,
            kind = GraphRenderer.Kind.HR,
            modifier = Modifier.fillMaxSize(),
            windowLabel = TimeWindow.ONE_MIN.label,
            isDark = false,
        )
    }
}

@Preview(widthDp = 200, heightDp = 80, backgroundColor = 0xFF000000, showBackground = true)
@Composable
fun PreviewHrTileDark() {
    val now = 600_000L
    val samples: List<Sample> = SyntheticData.hrSamples(now)
    AppTheme(darkTheme = true) {
        ZoneGraph(
            samples = samples,
            timeWindowSec = TimeWindow.ONE_MIN.seconds,
            nowMs = now,
            kind = GraphRenderer.Kind.HR,
            modifier = Modifier.fillMaxSize(),
            windowLabel = TimeWindow.ONE_MIN.label,
            isDark = true,
        )
    }
}

@Preview(widthDp = 200, heightDp = 80, backgroundColor = 0xFFFFFFFF, showBackground = true)
@Composable
fun PreviewPowerTileLight() {
    val now = 600_000L
    val samples: List<Sample> = SyntheticData.powerSamples(now)
    AppTheme(darkTheme = false) {
        ZoneGraph(
            samples = samples,
            timeWindowSec = TimeWindow.FIVE_MIN.seconds,
            nowMs = now,
            kind = GraphRenderer.Kind.POWER,
            modifier = Modifier.fillMaxSize(),
            windowLabel = TimeWindow.FIVE_MIN.label,
            isDark = false,
        )
    }
}

@Preview(widthDp = 200, heightDp = 80, backgroundColor = 0xFF000000, showBackground = true)
@Composable
fun PreviewPowerTileDark() {
    val now = 600_000L
    val samples: List<Sample> = SyntheticData.powerSamples(now)
    AppTheme(darkTheme = true) {
        ZoneGraph(
            samples = samples,
            timeWindowSec = TimeWindow.FIVE_MIN.seconds,
            nowMs = now,
            kind = GraphRenderer.Kind.POWER,
            modifier = Modifier.fillMaxSize(),
            windowLabel = TimeWindow.FIVE_MIN.label,
            isDark = true,
        )
    }
}

@Preview(widthDp = 200, heightDp = 80, backgroundColor = 0xFFFFFFFF, showBackground = true)
@Composable
fun PreviewFullRide() {
    val now = 600_000L
    val samples: List<Sample> = SyntheticData.powerSamples(now, durationSec = 3600)
    AppTheme(darkTheme = false) {
        ZoneGraph(
            samples = samples,
            timeWindowSec = TimeWindow.FULL.seconds,
            nowMs = now,
            kind = GraphRenderer.Kind.POWER,
            modifier = Modifier.fillMaxSize(),
            windowLabel = TimeWindow.FULL.label,
            isDark = false,
        )
    }
}
