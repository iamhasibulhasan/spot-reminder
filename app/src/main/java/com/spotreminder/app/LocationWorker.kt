package com.spotreminder.app

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

/** Runs about every 15 minutes in the background and shows a notification on arrival in a saved city. */
class LocationWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {
    override fun doWork(): Result {
        if (LocationChecker.hasLocationPermission(applicationContext)) {
            LocationChecker.run(applicationContext, true)
        }
        return Result.success()
    }
}
