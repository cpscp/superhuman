package com.carlos.liquid.lock

import android.app.Notification
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.carlos.liquid.data.Store
import kotlinx.coroutines.flow.MutableStateFlow

data class LNotif(
    val key: String, val pkg: String, val app: String, val title: String, val text: String,
    val time: Long, val intent: PendingIntent?, val clearable: Boolean,
)

/** Lê notificações: ecrã bloqueado + indicadores nos ícones. Também mantém o ecrã bloqueado ativo. */
class NotifService : NotificationListenerService() {
    companion object {
        val notifs = MutableStateFlow<List<LNotif>>(emptyList())
        val badges = MutableStateFlow<Map<String, Int>>(emptyMap())
        @Volatile var instance: NotifService? = null

        fun enabled(ctx: Context) = NotificationManagerCompat.getEnabledListenerPackages(ctx).contains(ctx.packageName)
        fun dismiss(key: String) { runCatching { instance?.cancelNotification(key) } }
        fun dnd(): Boolean = runCatching { instance?.currentInterruptionFilter?.let { it != INTERRUPTION_FILTER_ALL && it != 0 } ?: false }.getOrDefault(false)
        fun toggleDnd() {
            val i = instance ?: return
            runCatching { i.requestInterruptionFilter(if (dnd()) INTERRUPTION_FILTER_ALL else INTERRUPTION_FILTER_PRIORITY) }
        }
    }

    override fun onListenerConnected() {
        instance = this
        refresh()
        LockController.register(applicationContext)
    }

    override fun onListenerDisconnected() { instance = null }
    override fun onNotificationPosted(sbn: StatusBarNotification?) = refresh()
    override fun onNotificationRemoved(sbn: StatusBarNotification?) = refresh()

    private fun refresh() {
        val arr = runCatching { activeNotifications }.getOrNull() ?: return
        val pm = packageManager
        val labels = HashMap<String, String>()
        val list = arr.filter { s ->
            s.packageName != packageName && !s.isOngoing &&
                (s.notification.flags and Notification.FLAG_GROUP_SUMMARY) == 0 &&
                s.notification.extras.getCharSequence(Notification.EXTRA_TITLE) != null
        }
        badges.value = list.groupingBy { it.packageName }.eachCount()
        notifs.value = list.sortedByDescending { it.postTime }.map { s ->
            val ex = s.notification.extras
            val app = labels.getOrPut(s.packageName) {
                runCatching { pm.getApplicationLabel(pm.getApplicationInfo(s.packageName, 0)).toString() }.getOrDefault(s.packageName)
            }
            LNotif(
                s.key, s.packageName, app,
                ex.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty(),
                (ex.getCharSequence(Notification.EXTRA_BIG_TEXT) ?: ex.getCharSequence(Notification.EXTRA_TEXT))?.toString().orEmpty(),
                s.postTime, s.notification.contentIntent, s.isClearable,
            )
        }
    }
}

/** Mostra o ecrã bloqueado do Liquid sempre que o ecrã se desliga. */
object LockController {
    @Volatile private var registered = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_SCREEN_OFF) show(context)
        }
    }

    fun register(ctx: Context) {
        if (registered) return
        registered = true
        ContextCompat.registerReceiver(ctx.applicationContext, receiver, IntentFilter(Intent.ACTION_SCREEN_OFF), ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    fun canShow(ctx: Context) = Settings.canDrawOverlays(ctx)

    fun show(ctx: Context) {
        if (!Store.s.lock.enabled || !canShow(ctx)) return
        val am = ctx.getSystemService(AudioManager::class.java)
        if (am.mode == AudioManager.MODE_IN_CALL || am.mode == AudioManager.MODE_IN_COMMUNICATION) return
        runCatching {
            ctx.startActivity(Intent(ctx, LockActivity::class.java).addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS or Intent.FLAG_ACTIVITY_NO_USER_ACTION))
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Só acorda o processo; o LiquidApp regista o ecrã bloqueado.
        LockController.register(context)
    }
}
