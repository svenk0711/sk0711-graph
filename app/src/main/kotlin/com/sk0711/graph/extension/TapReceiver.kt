package com.sk0711.graph.extension

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class TapReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val ext = HrPowerExtension.instance() ?: return
        when (intent.action) {
            HrPowerExtension.ACTION_TOGGLE_HR -> ext.toggleHrWindow()
            HrPowerExtension.ACTION_TOGGLE_POWER -> ext.togglePowerWindow()
        }
    }
}
