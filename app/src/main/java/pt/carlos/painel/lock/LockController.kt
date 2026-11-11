package pt.carlos.painel.lock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import pt.carlos.painel.PainelApp
import pt.carlos.painel.ui.LockScreenActivity

/**
 * Quando o ecrã se apaga, prepara o ecrã de bloqueio do Painel por cima do bloqueio do Android.
 * Ao acender, é ele que aparece; deslizar para cima pede o desbloqueio ao sistema (PIN, se tiveres).
 */
object LockController {
    private var registered = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_SCREEN_OFF && PainelApp.instance.prefs.lockScreen) {
                runCatching {
                    context.startActivity(
                        Intent(context, LockScreenActivity::class.java).addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION
                        )
                    )
                }
            }
        }
    }

    fun sync(ctx: Context) {
        val app = ctx.applicationContext
        val want = PainelApp.instance.prefs.lockScreen
        if (want && !registered) {
            app.registerReceiver(receiver, IntentFilter(Intent.ACTION_SCREEN_OFF), Context.RECEIVER_NOT_EXPORTED)
            registered = true
        } else if (!want && registered) {
            runCatching { app.unregisterReceiver(receiver) }
            registered = false
        }
    }
}
