package com.asla.denge

import android.app.Application
import com.asla.denge.di.appModule
import com.asla.denge.di.databaseModule
import com.asla.denge.di.networkModule
import com.asla.denge.di.playerModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class AdsFreeApp : Application() {
    override fun onCreate() {
        super.onCreate()

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            android.util.Log.e("Denge_Crash", "FATAL CRASH on thread ${thread.name}", throwable)
            try {
                val intent = android.content.Intent(this, CrashActivity::class.java).apply {
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    putExtra("error", "Thread: ${thread.name}\n\n${android.util.Log.getStackTraceString(throwable)}")
                }
                startActivity(intent)
                android.os.Process.killProcess(android.os.Process.myPid())
                System.exit(10)
            } catch (_: Exception) {
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }

        startKoin {
            androidLogger(org.koin.core.logger.Level.ERROR)
            androidContext(this@AdsFreeApp)
            modules(
                appModule,
                networkModule,
                databaseModule,
                playerModule,
            )
        }
    }
}
