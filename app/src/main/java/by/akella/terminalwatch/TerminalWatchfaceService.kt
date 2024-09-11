package by.akella.terminalwatch

import android.view.SurfaceHolder
import androidx.wear.watchface.CanvasType
import androidx.wear.watchface.ComplicationSlotsManager
import androidx.wear.watchface.WatchFace
import androidx.wear.watchface.WatchFaceService
import androidx.wear.watchface.WatchFaceType
import androidx.wear.watchface.WatchState
import androidx.wear.watchface.style.CurrentUserStyleRepository
import by.akella.terminalwatch.data.BatteryRepository
import by.akella.terminalwatch.data.CalendarRepository
import by.akella.terminalwatch.data.HealthServicesManager
import by.akella.terminalwatch.data.PassiveDataRepository
import by.akella.terminalwatch.watchface.TerminalWatchfaceRenderer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class TerminalWatchfaceService : WatchFaceService() {

    private val healthyMaanger: HealthServicesManager by inject<HealthServicesManager>()
    private val passiveDataRepository: PassiveDataRepository by inject<PassiveDataRepository>()
    private val batteryRepository: BatteryRepository by inject<BatteryRepository>()
    private val calendarRepository: CalendarRepository by inject<CalendarRepository>()
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()

        batteryRepository.registerBatteryLevelListener()
            .launchIn(coroutineScope)

        if (checkSelfPermission(android.Manifest.permission.BODY_SENSORS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            coroutineScope.launch {
                healthyMaanger.registerForHeartRateData()
            }
        }
    }

    override suspend fun createWatchFace(
        surfaceHolder: SurfaceHolder,
        watchState: WatchState,
        complicationSlotsManager: ComplicationSlotsManager,
        currentUserStyleRepository: CurrentUserStyleRepository
    ): WatchFace {

        val renderer = TerminalWatchfaceRenderer(
            context = applicationContext,
            surfaceHolder = surfaceHolder,
            watchState = watchState,
            currentUserStyleRepository = currentUserStyleRepository,
            passiveDataRepository = passiveDataRepository,
            batteryRepository = batteryRepository,
            calendarRepository = calendarRepository,
            canvasType = CanvasType.HARDWARE
        )
        return WatchFace(
            watchFaceType = WatchFaceType.DIGITAL,
            renderer = renderer
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }
}