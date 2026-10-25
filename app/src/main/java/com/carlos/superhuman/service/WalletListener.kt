package com.carlos.superhuman.service

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationManagerCompat
import com.carlos.superhuman.data.Expense
import com.carlos.superhuman.data.ExpenseSource
import com.carlos.superhuman.data.Store

/**
 * O Google Wallet não tem API de transações. Este serviço lê as notificações de pagamento
 * do Wallet (e de outras apps que escolheres nas Definições) e cria despesas "por confirmar".
 */
class WalletListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        Store.init(applicationContext)
        val s = Store.data.settings
        if (sbn.packageName !in s.walletPackages) return
        val extras = sbn.notification?.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT) ?: extras.getCharSequence(Notification.EXTRA_TEXT))?.toString().orEmpty()
        val parsed = parse(title, text) ?: return
        val key = "${sbn.packageName}|${sbn.postTime}|${parsed.first}"
        if (Store.data.expenses.any { it.dedupKey == key }) return
        val exp = Expense(
            time = sbn.postTime,
            amount = parsed.first,
            merchant = parsed.second,
            category = guessCategory(parsed.second),
            source = ExpenseSource.WALLET,
            pending = !s.walletAutoConfirm,
            dedupKey = key,
        )
        Store.update { it.copy(expenses = it.expenses + exp) }
    }

    companion object {
        private val amountRegex = Regex("""(?:€\s?([0-9]{1,6}(?:[.,][0-9]{3})*[.,][0-9]{2}))|(?:([0-9]{1,6}(?:[.,][0-9]{3})*[.,][0-9]{2})\s?€)|(?:EUR\s?([0-9]+[.,][0-9]{2}))""")

        /** Devolve (valor, comerciante) a partir do título/texto da notificação. */
        fun parse(title: String, text: String): Pair<Double, String>? {
            val all = "$title $text"
            val m = amountRegex.find(all) ?: return null
            val raw = m.groupValues.drop(1).firstOrNull { it.isNotBlank() } ?: return null
            val normalized = if (raw.length > 3 && (raw[raw.length - 3] == ',' || raw[raw.length - 3] == '.')) {
                raw.substring(0, raw.length - 3).replace(".", "").replace(",", "") + "." + raw.takeLast(2)
            } else raw.replace(",", ".")
            val amount = normalized.toDoubleOrNull() ?: return null
            // O Wallet costuma pôr o comerciante no título e o valor no texto.
            val merchant = when {
                title.isNotBlank() && !amountRegex.containsMatchIn(title) -> title
                else -> text.replace(m.value, "").replace(Regex("""(com|with)\s.*$"""), "").trim().ifBlank { "Pagamento" }
            }
            return amount to merchant.take(60)
        }

        fun guessCategory(merchant: String): String {
            val m = merchant.lowercase()
            return when {
                listOf("continente", "pingo doce", "lidl", "auchan", "mercadona", "aldi", "minipreço", "intermarché", "spar").any { it in m } -> "Supermercado"
                listOf("uber", "bolt", "cp ", "carris", "metro", "galp", "repsol", "bp ", "via verde", "prio").any { it in m } -> "Transportes"
                listOf("restaurante", "café", "cafe", "mcdonald", "burger", "pizza", "glovo", "uber eats", "sushi", "tasca", "pastelaria").any { it in m } -> "Restauração"
                listOf("farmácia", "farmacia", "clínica", "hospital").any { it in m } -> "Saúde"
                listOf("netflix", "spotify", "apple", "google", "disney", "hbo", "amazon prime").any { it in m } -> "Subscrições"
                listOf("worten", "fnac", "zara", "ikea", "decathlon", "primark", "amazon").any { it in m } -> "Compras"
                else -> "Outros"
            }
        }

        fun isEnabled(ctx: Context): Boolean =
            NotificationManagerCompat.getEnabledListenerPackages(ctx).contains(ctx.packageName)

        fun component(ctx: Context) = ComponentName(ctx, WalletListener::class.java)
    }
}
