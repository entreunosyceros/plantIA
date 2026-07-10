package com.plantia

import android.app.Application
import com.plantia.reminders.NotificationHelper
import com.plantia.reminders.WaterReminderWorker

class PlantIAApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.ensureChannels(this)
        WaterReminderWorker.schedule(this)
    }
}
