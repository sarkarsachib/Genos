package ai.genos.core

import android.app.Application

class GenosApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: GenosApplication
            private set
    }
}
