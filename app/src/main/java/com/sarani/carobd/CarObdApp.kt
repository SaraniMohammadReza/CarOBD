package com.sarani.carobd

import android.app.Application
import com.sarani.carobd.core.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class CarObdApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@CarObdApp)
            modules(appModule)
        }
    }
}
