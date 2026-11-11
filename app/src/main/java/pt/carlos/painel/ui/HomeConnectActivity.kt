package pt.carlos.painel.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pt.carlos.painel.PainelApp
import pt.carlos.painel.home.HomeRepo
import pt.carlos.painel.theme.PainelTheme

/**
 * Ecrã curto que abre a autorização do Google Home e volta ao modo Display.
 * O cliente do Google Home é preparado em segundo plano; quando está pronto, este ecrã
 * recomeça e liga-se ao ecrã da Google (isso tem de acontecer no arranque da atividade).
 */
class HomeConnectActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HomeRepo.init(this)
        val registered = HomeRepo.registerIfReady(this)
        setContent {
            PainelTheme(PainelApp.instance.prefs, forceDark = true) {
                Surface(Modifier.fillMaxSize()) {
                    Column(
                        Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(16.dp))
                        Text("A ligar ao Google Home…", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
        lifecycleScope.launch {
            if (registered) {
                HomeRepo.requestPermission()
                finish()
            } else {
                // Espera até 15 s que o cliente fique pronto e recomeça; senão volta com o erro.
                repeat(30) {
                    delay(500)
                    if (HomeRepo.ready) { recreate(); return@launch }
                }
                finish()
            }
        }
    }
}
