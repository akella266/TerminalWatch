package by.akella.terminalwatch.data

import androidx.health.services.client.PassiveListenerService
import androidx.health.services.client.data.DataPointContainer
import androidx.health.services.client.data.DataType
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.inject

class PassiveDataService : PassiveListenerService() {

    val repository: PassiveDataRepository by inject<PassiveDataRepository>()

    override fun onNewDataPointsReceived(dataPoints: DataPointContainer) {
        runBlocking {
            dataPoints.getData(DataType.STEPS_DAILY).latestSteps()?.let {
                repository.storeLatestSteps(it)
            }
            dataPoints.getData(DataType.CALORIES_DAILY).latestCalories()?.let {
                repository.storeLatestCalories(it)
            }
            dataPoints.getData(DataType.DISTANCE_DAILY).latestDistance()?.let {
                repository.storeLatestDistance(it)
            }
            dataPoints.getData(DataType.HEART_RATE_BPM).latestHeartRate()?.let {
                repository.storeLatestHeartRate(it)
            }
        }
    }

}