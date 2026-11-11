package pt.carlos.painel

import android.app.Application
import pt.carlos.painel.data.AppsRepository
import pt.carlos.painel.data.LayoutStore
import pt.carlos.painel.data.Prefs
import pt.carlos.painel.widgets.PainelHost

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
        prefs = Prefs(this)
        layout = LayoutStore(this)
        apps = AppsRepository(this)
        widgetHost = PainelHost(this)
    }

    companion object {
        lateinit var instance: PainelApp
            private set
    }
}
