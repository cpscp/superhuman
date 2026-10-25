package com.carlos.superhuman

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.carlos.superhuman.data.Store
import com.carlos.superhuman.net.Strava
import com.carlos.superhuman.ui.AppRoot
import com.carlos.superhuman.ui.Nav
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Store.init(this)
        enableEdgeToEdge()
        handleIntent(intent)
        setContent { AppRoot() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onPause() {
        super.onPause()
        Store.saveNow()
    }

    private fun handleIntent(intent: Intent?) {
        val data = intent?.data
        if (data != null && data.scheme == "superhuman" && data.path?.startsWith("/strava") == true) {
            val code = data.getQueryParameter("code")
            if (code != null) {
                lifecycleScope.launch {
                    val ok = Strava.exchange(code)
                    if (ok) Strava.refreshActivities()
                    Toast.makeText(this@MainActivity, if (ok) "Strava ligado" else "Falha a ligar o Strava", Toast.LENGTH_SHORT).show()
                    Nav.go("health")
                }
            }
        }
        intent?.getStringExtra("route")?.let { Nav.go(it, intent.getStringExtra("arg")) }
    }
}
