package by.akella.terminalwatch

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import by.akella.terminalwatch.data.HealthServicesManager
import by.akella.terminalwatch.data.PassiveDataRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

const val PERMISSION = android.Manifest.permission.BODY_SENSORS

class StartUpReceiver : BroadcastReceiver(), KoinComponent {

    private val repository: PassiveDataRepository by inject<PassiveDataRepository>()

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return

        runBlocking {
            if (repository.passiveDataEnabled.first()) {
                val result = context?.checkSelfPermission(PERMISSION)
                if (result == PackageManager.PERMISSION_GRANTED) {
                    scheduleWorker(context)
                } else {
//                    repository.setPassiveDataEnabled(false)
                }
            }
        }
    }

    private fun scheduleWorker(context: Context) {
        Log.i(TAG, "Scheduling worker")

        WorkManager.getInstance(context).enqueue(
            OneTimeWorkRequestBuilder<RegisterForBackgroundDataWorker>().build()
        )
    }
}

class RegisterForBackgroundDataWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.i(TAG, "DataWorker running")
        val healthServicesRepository = HealthServicesManager(appContext)
        healthServicesRepository.registerForHeartRateData()
        return Result.success()
    }
}