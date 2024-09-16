package by.akella.terminalwatch.data

import android.content.Context
import android.os.BatteryManager
import android.util.Log
import by.akella.terminalwatch.TAG
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import java.util.concurrent.atomic.AtomicBoolean

class BatteryRepository(
    private val context: Context,
) {

    private val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager

    private val _batteryLevel = MutableStateFlow<Int>(0)
    val batteryLevel: StateFlow<Int> = _batteryLevel.asStateFlow()

    var hasSubscription = AtomicBoolean(false)

    private fun updateBatteryLevel() {
        _batteryLevel.value = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        Log.e(TAG, "Battery level: ${_batteryLevel.value}")
    }

    fun registerBatteryLevelListener(delayMs: Long = 300_000) =
        flow<Unit> {
            Log.e(TAG, "Battery listening started")
            while (true) {
                updateBatteryLevel()
                delay(delayMs)
            }

            hasSubscription.set(true)
        }
            .catch { hasSubscription.set(false) }
            .onCompletion { hasSubscription.set(false) }
}