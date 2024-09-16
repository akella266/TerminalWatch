package by.akella.terminalwatch.data

import android.content.Context
import android.util.Log
import androidx.concurrent.futures.await
import androidx.health.services.client.HealthServices
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.PassiveListenerConfig
import by.akella.terminalwatch.TAG
import java.util.concurrent.atomic.AtomicBoolean

class HealthServicesManager(
    context: Context
) {
    val client = HealthServices.getClient(context)
    private val passiveMonitoringClient = client.passiveMonitoringClient
    private val dataTypes = setOf(
        DataType.STEPS_DAILY,
        DataType.DISTANCE_DAILY,
        DataType.CALORIES_DAILY,
    )

    var hasSubscription = AtomicBoolean(false)

    suspend fun registerForHeartRateData() {
        val passiveListenerConfig = PassiveListenerConfig.builder()
            .setDataTypes(dataTypes)
            .build()

        Log.e(TAG, "Registering listener")
        passiveMonitoringClient.setPassiveListenerServiceAsync(
            PassiveDataService::class.java,
            passiveListenerConfig
        ).await()
        hasSubscription.set(true)
    }

    suspend fun unregisterForHeartRateData() {
        Log.e(TAG, "Unregistering listeners")
        passiveMonitoringClient.clearPassiveListenerServiceAsync().await()
        hasSubscription.set(false)
    }
}