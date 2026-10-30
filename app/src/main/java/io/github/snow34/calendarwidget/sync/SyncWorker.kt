package io.github.snow34.calendarwidget.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = when (val o = Syncer.run(applicationContext)) {
        SyncOutcome.Updated, SyncOutcome.NotModified, SyncOutcome.NoUrl -> Result.success()
        is SyncOutcome.Failed -> if (o.retryable && runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure()
    }

    companion object {
        private const val UNIQUE_NAME = "calendar_sync"
        private const val MAX_ATTEMPTS = 5

        fun schedule(context: Context, everyHours: Int) {
            val request = PeriodicWorkRequestBuilder<SyncWorker>(everyHours.toLong(), TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(UNIQUE_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_NAME)
        }
    }
}
