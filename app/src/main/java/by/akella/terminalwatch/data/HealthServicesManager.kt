package by.akella.terminalwatch.data

import android.content.Context
import android.util.Log
import androidx.concurrent.futures.await
import androidx.health.services.client.HealthServices
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.PassiveListenerConfig
import by.akella.terminalwatch.TAG

class HealthServicesManager(
    context: Context
) {
    private val passiveMonitoringClient = HealthServices.getClient(context).passiveMonitoringClient
    private val dataTypes = setOf(
        DataType.HEART_RATE_BPM,
        DataType.STEPS_DAILY,
        DataType.DISTANCE_DAILY,
        DataType.CALORIES_DAILY,
    )

    suspend fun registerForHeartRateData() {
        val passiveListenerConfig = PassiveListenerConfig.builder()
            .setDataTypes(dataTypes)
            .build()

        Log.e(TAG, "Registering listener")
        passiveMonitoringClient.setPassiveListenerServiceAsync(
            PassiveDataService::class.java,
            passiveListenerConfig
        ).await()
    }

    suspend fun unregisterForHeartRateData() {
        Log.e(TAG, "Unregistering listeners")
        passiveMonitoringClient.clearPassiveListenerServiceAsync().await()
    }
}