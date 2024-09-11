package by.akella.terminalwatch.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.HeartRateAccuracy
import androidx.health.services.client.data.HeartRateAccuracy.SensorStatus.Companion.ACCURACY_HIGH
import androidx.health.services.client.data.HeartRateAccuracy.SensorStatus.Companion.ACCURACY_MEDIUM
import androidx.health.services.client.data.IntervalDataPoint
import androidx.health.services.client.data.SampleDataPoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

const val PREFERENCES_DATA_STORE_NAME = "passive_data_store"

class PassiveDataRepository(
    private val dataStore: DataStore<Preferences>
) {
    val passiveDataEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[PASSIVE_DATA_ENABLED] ?: false
    }

    suspend fun setPassiveDataEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[PASSIVE_DATA_ENABLED] = enabled
        }
    }

    val latestHeartRate: Flow<Double> = dataStore.data.map {
        prefs -> prefs[LATEST_HEART_RATE] ?: 0.0
    }

    suspend fun storeLatestHeartRate(heartRate: Double) = dataStore.edit { prefs ->
        prefs[LATEST_HEART_RATE] = heartRate
    }

    val latestSteps: Flow<Long> = dataStore.data.map {
        prefs -> prefs[LATEST_STEPS] ?: 0L
    }

    suspend fun storeLatestSteps(steps: Long) = dataStore.edit { prefs ->
        prefs[LATEST_STEPS] = steps
    }

    val latestCalories: Flow<Double> = dataStore.data.map {
        prefs -> prefs[LATEST_CALORIES] ?: 0.0
    }

    suspend fun storeLatestCalories(calories: Double) = dataStore.edit { prefs ->
        prefs[LATEST_CALORIES] = calories
    }

    val latestDistance: Flow<Double> = dataStore.data.map {
        prefs -> prefs[LATEST_DISTANCE] ?: 0.0
    }

    suspend fun storeLatestDistance(distance: Double) = dataStore.edit { prefs ->
        prefs[LATEST_DISTANCE] = distance
    }

    companion object {

        private val LATEST_HEART_RATE = doublePreferencesKey("latest_heart_rate")
        private val LATEST_CALORIES = doublePreferencesKey("latest_calories")
        private val LATEST_STEPS = longPreferencesKey("latest_steps")
        private val LATEST_DISTANCE = doublePreferencesKey("latest_distance")
        private val PASSIVE_DATA_ENABLED = booleanPreferencesKey("passive_data_enabled")
    }
}

fun List<SampleDataPoint<Double>>.latestHeartRate(): Double? =
    this.filter { it.dataType == DataType.HEART_RATE_BPM }
        .filter {
            it.accuracy == null ||
                    setOf(ACCURACY_HIGH, ACCURACY_MEDIUM).contains((it.accuracy as HeartRateAccuracy).sensorStatus)
        }
        .maxByOrNull { it.timeDurationFromBoot }?.value


fun List<IntervalDataPoint<Long>>.latestSteps(): Long? =
    this.filter { it.dataType == DataType.STEPS_DAILY }
        .maxByOrNull { it.endDurationFromBoot }?.value

fun List<IntervalDataPoint<Double>>.latestCalories(): Double? =
    this.filter { it.dataType == DataType.CALORIES_DAILY }
        .maxByOrNull { it.endDurationFromBoot }?.value

fun List<IntervalDataPoint<Double>>.latestDistance(): Double? =
    this.filter { it.dataType == DataType.DISTANCE_DAILY }
        .maxByOrNull { it.endDurationFromBoot }?.value

