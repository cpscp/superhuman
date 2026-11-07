package pt.carlos.partidas.widget

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pt.carlos.partidas.data.Repo
import pt.carlos.partidas.data.WidgetLinks
import java.util.concurrent.TimeUnit

class RefreshWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val ctx = applicationContext
        val boards = inputData.getStringArray(KEY_BOARDS)?.takeIf { it.isNotEmpty() }?.toList()
            ?: linkedBoards(ctx)
        for (b in boards) {
            Repo.refresh(ctx, b)
            WidgetRenderer.renderBoard(ctx, b)
        }
        Result.success()
    }

    companion object {
        private const val KEY_BOARDS = "boards"
        private const val PERIODIC = "partidas-periodic"

        private val net = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        /** Quadros que estão em algum widget. */
        fun linkedBoards(ctx: Context): List<String> =
            WidgetRenderer.allIds(ctx).toList().mapNotNull { WidgetLinks.get(ctx, it) }.distinct()

        /** boards == null → todos os quadros que estão em widgets. */
        fun refresh(ctx: Context, boards: Array<String>?, replace: Boolean = true) {
            val req = OneTimeWorkRequestBuilder<RefreshWorker>()
                .setInputData(workDataOf(KEY_BOARDS to (boards ?: emptyArray())))
                .setConstraints(net)
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
            val name = if (boards == null) "partidas-all" else "partidas-" + boards.joinToString("-")
            WorkManager.getInstance(ctx).enqueueUniqueWork(
                name, if (replace) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP, req
            )
        }

        fun schedulePeriodic(ctx: Context) {
            val req = PeriodicWorkRequestBuilder<RefreshWorker>(15, TimeUnit.MINUTES)
                .setConstraints(net)
                .build()
            WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.KEEP, req)
        }

        fun cancelAll(ctx: Context) {
            WorkManager.getInstance(ctx).cancelUniqueWork(PERIODIC)
        }
    }
}
