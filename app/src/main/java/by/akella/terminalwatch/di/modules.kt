package by.akella.terminalwatch.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import by.akella.terminalwatch.data.BatteryRepository
import by.akella.terminalwatch.data.CalendarRepository
import by.akella.terminalwatch.data.HealthServicesManager
import by.akella.terminalwatch.data.PREFERENCES_DATA_STORE_NAME
import by.akella.terminalwatch.data.PassiveDataRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.dsl.module

val appModule = module {

    single<DataStore<Preferences>> {
        PreferenceDataStoreFactory.create(
            corruptionHandler = null,
            migrations = listOf(),
            scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        ) {
            get<Context>().preferencesDataStoreFile(PREFERENCES_DATA_STORE_NAME)
        }
    }
    factory { PassiveDataRepository(get<DataStore<Preferences>>()) }
    single { HealthServicesManager(get<Context>()) }
    single { BatteryRepository(get<Context>()) }
    factory { CalendarRepository(get<Context>()) }
}