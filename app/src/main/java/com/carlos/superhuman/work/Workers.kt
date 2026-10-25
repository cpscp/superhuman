package com.carlos.superhuman.work

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.carlos.superhuman.MainActivity
import com.carlos.superhuman.R
import com.carlos.superhuman.SuperhumanApp
import com.carlos.superhuman.data.Store
import com.carlos.superhuman.data.beerFeedback
import com.carlos.superhuman.data.monthSummary
import com.carlos.superhuman.data.euro
import com.carlos.superhuman.data.tasksForToday
import com.carlos.superhuman.data.todayEpoch
import com.carlos.superhuman.device.CalendarRepo
import com.carlos.superhuman.net.Market
import com.carlos.superhuman.net.Weather
import com.carlos.superhuman.widget.WidgetSync
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

object Workers {
    private fun delayTo(hour: Int, minute: Int): Long {
        val now = LocalDateTime.now()
        var target = LocalDate.now().atTime(LocalTime.of(hour, minute))
        if (!target.isAfter(now)) target = target.plusDays(1)
        return Duration.between(now, target).toMillis()
    }

    fun scheduleAll(ctx: Context) {
        Store.init(ctx)
        val s = Store.data.settings
        val wm = WorkManager.getInstance(ctx)
        val net = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        wm.enqueueUniquePeriodicWork(
            "daily_review", ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
            PeriodicWorkRequestBuilder<DailyReviewWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(delayTo(s.dailyReviewHour, s.dailyReviewMinute), TimeUnit.MILLISECONDS).build(),
        )
        wm.enqueueUniquePeriodicWork(
            "weekly_review", ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
            PeriodicWorkRequestBuilder<WeeklyReviewWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(delayTo(s.weeklyReviewHour, 0), TimeUnit.MILLISECONDS).build(),
        )
        wm.enqueueUniquePeriodicWork(
            "morning_brief", ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<MorningWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(delayTo(7, 45), TimeUnit.MILLISECONDS).setConstraints(net).build(),
        )
        wm.enqueueUniquePeriodicWork(
            "refresh", ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<RefreshWorker>(1, TimeUnit.HOURS).setConstraints(net).build(),
        )
    }

    fun notify(ctx: Context, id: Int, channel: String, title: String, text: String, route: String, arg: String? = null) {
        val intent = Intent(ctx, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra("route", route).putExtra("arg", arg)
        val pi = PendingIntent.getActivity(ctx, id, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val n = NotificationCompat.Builder(ctx, channel)
            .setSmallIcon(R.drawable.ic_launcher_fg)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        runCatching { ctx.getSystemService(NotificationManager::class.java).notify(id, n) }
    }
}

class DailyReviewWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        Store.init(applicationContext)
        val d = Store.data
        if (d.reviews.any { it.key == "D-${todayEpoch()}" }) return Result.success()
        val pending = d.tasksForToday().size
        val done = d.tasks.count { it.done && (it.completedAt ?: 0L) >= LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() }
        Workers.notify(
            applicationContext, 101, SuperhumanApp.CHANNEL_REVIEW, "Daily review 🌙",
            "Fizeste $done tasks hoje, faltam $pending. ${d.beerFeedback().title}. 2 minutos para fechar o dia?",
            "review", "daily",
        )
        return Result.success()
    }
}

class WeeklyReviewWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        if (LocalDate.now().dayOfWeek != DayOfWeek.FRIDAY) return Result.success()
        Store.init(applicationContext)
        val m = Store.data.monthSummary()
        Workers.notify(
            applicationContext, 102, SuperhumanApp.CHANNEL_REVIEW, "Weekly review 📊",
            "Sexta-feira: revê a semana e define as prioridades. Plafond restante: ${euro(m.remaining)}.",
            "review", "weekly",
        )
        return Result.success()
    }
}

class MorningWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        Store.init(applicationContext)
        Weather.refresh()
        CalendarRepo.cacheToday(applicationContext)
        if (Store.data.holdings.isNotEmpty()) Market.refreshQuotes()
        val feed = Market.refreshFeed().getOrNull()
        if (feed != null) {
            val tip = feed.tips.firstOrNull()
            Workers.notify(
                applicationContext, 103, SuperhumanApp.CHANNEL_INVEST, "Invest · ${feed.mood.ifBlank { "mercado" }}",
                (feed.marketSummary.take(180) + (tip?.let { "\n💡 ${it.title}" } ?: "")),
                "invest",
            )
        }
        WidgetSync.refresh(applicationContext)
        return Result.success()
    }
}

class RefreshWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        Store.init(applicationContext)
        if (System.currentTimeMillis() - Store.data.weather.updatedAt > 45 * 60_000) Weather.refresh()
        CalendarRepo.cacheToday(applicationContext)
        WidgetSync.refresh(applicationContext)
        return Result.success()
    }
}
