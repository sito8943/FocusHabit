package dev.sito8943.focushabit

import android.app.Application
import dev.sito8943.focushabit.core.di.AppContainer

class FocusHabitApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.syncCoordinator.start()
    }
}
