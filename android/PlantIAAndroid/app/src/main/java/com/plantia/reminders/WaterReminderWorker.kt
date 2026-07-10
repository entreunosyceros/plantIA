package com.plantia.reminders

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

class WaterReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val store = WaterReminderStore(applicationContext)
        NotificationHelper.ensureChannels(applicationContext)
        store.allEnabled().forEach { reminder ->
            if (store.shouldNotifyNow(reminder)) {
                NotificationHelper.showWaterReminder(
                    applicationContext,
                    reminder.plantId,
                    reminder.plantName,
                )
            }
        }
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "plantia_water_reminders"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<WaterReminderWorker>(1, TimeUnit.DAYS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
