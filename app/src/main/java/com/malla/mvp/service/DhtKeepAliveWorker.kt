package com.malla.mvp.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class DhtKeepAliveWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = Result.success()
}
