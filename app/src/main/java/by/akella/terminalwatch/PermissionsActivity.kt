package by.akella.terminalwatch

import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit

class PermissionsActivity : ComponentActivity() {

    private val scope = CoroutineScope(Dispatchers.Main.immediate)

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions: Map<String, Boolean> ->
            if (permissions.values.all { it }) {
                // Permission granted, proceed with accessing health services
                startWorker()
                this.finish()
            } else {
                // Permission denied, handle accordingly (e.g., show an error message)
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        modifier = Modifier.wrapContentWidth(),
                        onClick = { requestHealthPermissionsAndReadData() }) {
                        Text("Request Health Permissions")
                    }
                }
            }
        }
    }

    private fun requestHealthPermissionsAndReadData() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.BODY_SENSORS
            ) == PackageManager.PERMISSION_GRANTED -> {
                // You can start accessing health services here
                startWorker()
            }
            shouldShowRequestPermissionRationale(android.Manifest.permission.BODY_SENSORS) -> {
                // Explain to the user why the permission is needed
            }
            else -> {
                requestPermissionLauncher.launch(
                    arrayOf(android.Manifest.permission.BODY_SENSORS,
                    android.Manifest.permission.ACTIVITY_RECOGNITION)
                )
            }
        }
    }

    private fun startWorker() {
        val healthDataRequest = PeriodicWorkRequestBuilder<HealthDataWorker>(15, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "healthDataWork",
            ExistingPeriodicWorkPolicy.KEEP,
            healthDataRequest
        )
    }
}

class HealthDataWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    private val healthConnectClient: HealthConnectClient by lazy { HealthConnectClient.getOrCreate(appContext) }
    override suspend fun doWork(): Result {

        val steps = getSteps()
        val distance = getDistance()
        val calories = getCalories()

        // Send data to watch face
        val putReq = PutDataMapRequest.create("/data").apply {
            dataMap.putLong("steps", steps)
            dataMap.putDouble("dist", distance)
            dataMap.putDouble("calories", calories)
        }.asPutDataRequest()
        Wearable.getDataClient(applicationContext).putDataItem(putReq)

        return Result.success()
    }

    private suspend fun getSteps(): Long {
        val today = LocalDate.now()
        val startOfDay = today.atStartOfDay(ZoneId.systemDefault()).toInstant()
        val endOfDay = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()

        val stepsRequest = ReadRecordsRequest(
            StepsRecord::class,
            timeRangeFilter = TimeRangeFilter.between(startOfDay, endOfDay)
        )

        val stepsResponse = healthConnectClient.readRecords(stepsRequest)
        val totalSteps = stepsResponse.records.sumOf { it.count }
        return totalSteps
    }

    private suspend fun getDistance(): Double {
        val today = LocalDate.now()
        val startOfDay = today.atStartOfDay(ZoneId.systemDefault()).toInstant()
        val endOfDay = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()

        val distRequest = ReadRecordsRequest(
            DistanceRecord::class,
            timeRangeFilter = TimeRangeFilter.between(startOfDay, endOfDay)
        )

        val distResponse = healthConnectClient.readRecords(distRequest)
        val totalDist = distResponse.records.sumOf { it.distance.inMeters }

        return totalDist
    }

    private suspend fun getCalories(): Double {
        val today = LocalDate.now()
        val startOfDay = today.atStartOfDay(ZoneId.systemDefault()).toInstant()
        val endOfDay = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()

        val caloriesRequest = ReadRecordsRequest(
            ActiveCaloriesBurnedRecord::class,
            timeRangeFilter = TimeRangeFilter.between(startOfDay, endOfDay)
        )

        val caloriesResponse = healthConnectClient.readRecords(caloriesRequest)
        val totalCalories = caloriesResponse.records.sumOf { it.energy.inCalories }

        return totalCalories
    }
}