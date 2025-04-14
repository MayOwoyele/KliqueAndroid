package com.justself.klique

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import org.json.JSONObject
import java.time.ZoneId
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.WorkManager
import java.time.LocalDateTime
import java.time.Duration
import java.util.concurrent.TimeUnit

class DiaryBackupWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        try {
            val diaryEntryDao =
                DatabaseProvider.getDiaryDatabase(applicationContext).diaryEntryDao()
            val listOfUnSyncedDiaryEntries = diaryEntryDao.getUnsyncedEntries()
            val emptyEntries = listOfUnSyncedDiaryEntries.filter { it.content.isEmpty() }
            val nonEmptyEntries = listOfUnSyncedDiaryEntries.filter { it.content.isNotEmpty() }
            val newList = mutableListOf<JSONObject>()
            val newEmptyList = mutableListOf<JSONObject>()
            for (entry in emptyEntries) {
                val json = JSONObject()
                val localDate = entry.date
                json.put("localDate", localDate.toString())
                newEmptyList.add(json)
            }
            for (entry in nonEmptyEntries) {
                val json = JSONObject()
                val timestampKey = "timestamp"
                val localDate = entry.date
                if (entry.timestampMillis != 0L) {
                    json.put(timestampKey, entry.timestampMillis)
                } else {
                    val timestampMillis = entry.date
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()
                    json.put(timestampKey, timestampMillis)
                }
                json.put("content", entry.content)
                json.put("localDate", localDate.toString())
                newList.add(json)
            }

            val action: suspend (NetworkUtils.JwtTriple) -> Unit =
                { /* success action can be handled here if needed */ }
            val errorAction: suspend (NetworkUtils.JwtTriple) -> Unit = { /* error handling here */ }
            networkRequest(
                JSONObject().put("list", newList),
                DiaryViewModel.RequestType.Syncing,
                action,
                errorAction
            )

            val currentTimestamp = System.currentTimeMillis()
            diaryEntryDao.updateSyncStatus(
                nonEmptyEntries.map { it.date },
                currentTimestamp
            )
            val action2: suspend (NetworkUtils.JwtTriple) -> Unit =
                { /* success action can be handled here if needed */ }
            val errorAction2: suspend (NetworkUtils.JwtTriple) -> Unit = { /* error handling here */ }
            constructDeletionJsonAndSend(newEmptyList, action2, errorAction2)
            diaryEntryDao.deleteEntries(emptyEntries.map { it.date })
            return Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.retry()
        }
    }
}

fun scheduleDiaryBackupWork(context: Context) {
    val backupWorkRequest = PeriodicWorkRequestBuilder<DiaryBackupWorker>(1, TimeUnit.DAYS)
        .setInitialDelay(calculateInitialDelay(), TimeUnit.MILLISECONDS)
        .build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "DiaryBackupWork",
        ExistingPeriodicWorkPolicy.UPDATE,
        backupWorkRequest
    )
}

private fun calculateInitialDelay(): Long {
    val now = LocalDateTime.now()
    val nextRun = now.withHour(2).withMinute(0).withSecond(0).withNano(0)
    val adjustedNextRun = if (now >= nextRun) nextRun.plusDays(1) else nextRun
    return Duration.between(now, adjustedNextRun).toMillis()
}

suspend fun constructDeletionJsonAndSend(theList: List<JSONObject>, action: suspend (NetworkUtils.JwtTriple) -> Unit, errorAction: suspend (NetworkUtils.JwtTriple) -> Unit) {
    val json = JSONObject().put("list", theList)
    return networkRequest(json, DiaryViewModel.RequestType.Deletion, action, errorAction)
}