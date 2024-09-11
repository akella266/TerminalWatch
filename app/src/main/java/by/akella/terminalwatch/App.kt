package by.akella.terminalwatch

import android.app.Application
import by.akella.terminalwatch.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

const val TAG = "TerminalWatch"

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(applicationContext)
            modules(appModule)
        }
    }
}