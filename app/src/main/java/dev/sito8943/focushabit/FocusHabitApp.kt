package dev.sito8943.focushabit

import android.app.Application
import dev.sito8943.focushabit.core.di.AppContainer
import dev.sito8943.focushabit.core.icons.FaIcons

class FocusHabitApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        FaIcons.init(this)
        container = AppContainer(this)
        container.syncCoordinator.start()
    }
}
