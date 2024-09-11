package by.akella.terminalwatch

import android.content.Intent
import android.net.Uri
import android.view.SurfaceHolder
import androidx.wear.watchface.CanvasType
import androidx.wear.watchface.ComplicationSlotsManager
import androidx.wear.watchface.WatchFace
import androidx.wear.watchface.WatchFaceService
import androidx.wear.watchface.WatchFaceType
import androidx.wear.watchface.WatchState
import androidx.wear.watchface.style.CurrentUserStyleRepository
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import by.akella.terminalwatch.data.BatteryRepository
import by.akella.terminalwatch.data.CalendarRepository
import by.akella.terminalwatch.data.HealthServicesManager
import by.akella.terminalwatch.data.PassiveDataRepository
import by.akella.terminalwatch.watchface.TerminalWatchfaceRenderer
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import java.util.concurrent.TimeUnit

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

        Wearable.getDataClient(this)
            .addListener { dataEvents ->
                dataEvents.forEach { event ->
                    if (event.type == DataEvent.TYPE_CHANGED) {
                        val item = event.dataItem
                        if (item.uri.path == "/data") {
                            val dataMap = DataMapItem.fromDataItem(item).dataMap
                            val steps = dataMap.getLong("steps")
                            val distance = dataMap.getDouble("dist")
                            val calories = dataMap.getDouble("calories")

                            coroutineScope.launch {
                                passiveDataRepository.storeLatestSteps(steps)
                                passiveDataRepository.storeLatestDistance(distance)
                                passiveDataRepository.storeLatestCalories(calories)
                            }
                        }
                    }
                }
            }

        if (checkSelfPermission(android.Manifest.permission.BODY_SENSORS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
//            val healthDataRequest = PeriodicWorkRequestBuilder<HealthDataWorker>(15, TimeUnit.MINUTES)
//                .build()
//
//            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
//                "healthDataWork",
//                ExistingPeriodicWorkPolicy.KEEP,
//                healthDataRequest
//            )

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