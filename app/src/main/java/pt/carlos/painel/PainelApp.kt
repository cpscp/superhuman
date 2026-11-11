package pt.carlos.painel

import android.app.Application
import pt.carlos.painel.data.AppsRepository
import pt.carlos.painel.data.LayoutStore
import pt.carlos.painel.data.Prefs
import pt.carlos.painel.widgets.PainelHost
import pt.carlos.painel.lock.LockController

/** Objetos partilhados por todo o Painel (um só processo, carregados uma vez). */
class PainelApp : Application() {
    lateinit var prefs: Prefs
        private set
    lateinit var layout: LayoutStore
        private set
    lateinit var apps: AppsRepository
        private set
    lateinit var widgetHost: PainelHost
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        // Guarda o último erro fatal, para se ver nas Definições (diagnóstico à distância).
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            runCatching {
                java.io.File(filesDir, "ultimo-erro.txt").writeText(
                    java.time.LocalDateTime.now().toString() + "\n" + android.util.Log.getStackTraceString(e).take(6000)
                )
            }
            previous?.uncaughtException(t, e)
        }
        prefs = Prefs(this)
        layout = LayoutStore(this)
        apps = AppsRepository(this, prefs)
        widgetHost = PainelHost(this)
        LockController.sync(this)
        pt.carlos.painel.media.Media.start(this)
    }

    companion object {
        lateinit var instance: PainelApp
            private set
    }
}
