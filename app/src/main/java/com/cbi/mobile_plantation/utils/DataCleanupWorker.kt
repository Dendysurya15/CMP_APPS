package com.cbi.mobile_plantation.worker

import android.content.Context
import android.os.Environment
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.cbi.mobile_plantation.data.database.AppDatabase
import com.cbi.mobile_plantation.utils.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

class DataCleanupWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val WORK_NAME = "com.cbi.mobile_plantation.DATA_CLEANUP_WORKER"
        private const val RETENTION_DAYS = 7

        fun schedule(context: Context) {
            val workRequest = PeriodicWorkRequestBuilder<DataCleanupWorker>(
                12, TimeUnit.HOURS // Run once per day
            ).build()
//            val workRequest = PeriodicWorkRequestBuilder<DataCleanupWorker>(
//                3, TimeUnit.MINUTES // Run once per day
//            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP, // Only schedule if it doesn't exist yet
                workRequest
            )

            AppLogger.d("DataCleanupWorker", "Scheduled daily cleanup worker")
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            AppLogger.d("DataCleanupWorker", "Starting data cleanup process")

            // 1. Delete old panen records from database
            cleanupOldPanenRecords()

            // 2. Delete old panen photos
            cleanupOldPanenPhotos()

            // 3. Delete old zip files
            cleanupOldZipFiles()

            AppLogger.d("DataCleanupWorker", "Data cleanup completed successfully")
            Result.success()
        } catch (e: Exception) {
            AppLogger.e("DataCleanupWorker", "Error during cleanup: ${e.message}")
            e.printStackTrace()
            Result.failure()
        }
    }

    private suspend fun cleanupOldPanenRecords() {
        val database = AppDatabase.getDatabase(applicationContext)
        val panenDao = database.panenDao()

        // Calculate date 7 days ago
        val dateSevenDaysAgo = LocalDate.now().minusDays(RETENTION_DAYS.toLong())
        val dateFormatted = dateSevenDaysAgo.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

        // Find all panen records older than 7 days
        val oldPanenRecords = panenDao.getPanenOlderThan(dateFormatted)

        if (oldPanenRecords.isNotEmpty()) {
            // Collect IDs of records to delete
            val idsToDelete = oldPanenRecords.map { it.panen.id }

            // Delete the records
            val deletedCount = panenDao.deleteByListID(idsToDelete)
            AppLogger.d("DataCleanupWorker", "Deleted $deletedCount panen records older than $dateFormatted")
        } else {
            AppLogger.d("DataCleanupWorker", "No panen records found older than $dateFormatted")
        }
    }

    private fun cleanupOldPanenPhotos() {
        val photosDir = File(
            applicationContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            "CMP-PANEN TPH"
        )

        if (!photosDir.exists() || !photosDir.isDirectory) {
            AppLogger.d("DataCleanupWorker", "Photos directory does not exist: ${photosDir.absolutePath}")
            return
        }

        val now = LocalDateTime.now()
        var deletedCount = 0

        photosDir.listFiles()?.forEach { file ->
            if (file.isFile) {
                val lastModified = LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(file.lastModified()),
                    java.time.ZoneId.systemDefault()
                )

                val daysBetween = ChronoUnit.DAYS.between(lastModified, now)

                if (daysBetween > RETENTION_DAYS) {
                    val deleted = file.delete()
                    if (deleted) {
                        deletedCount++
                    } else {
                        AppLogger.e("DataCleanupWorker", "Failed to delete photo: ${file.absolutePath}")
                    }
                }
            }
        }

        AppLogger.d("DataCleanupWorker", "Deleted $deletedCount photos older than $RETENTION_DAYS days")
    }

    private fun cleanupOldZipFiles() {
        val uploadDir = File(applicationContext.getExternalFilesDir(null), "Upload")

        if (!uploadDir.exists() || !uploadDir.isDirectory) {
            AppLogger.d("DataCleanupWorker", "Upload directory does not exist: ${uploadDir.absolutePath}")
            return
        }

        val now = LocalDateTime.now()
        var deletedCount = 0

        uploadDir.listFiles()?.forEach { file ->
            if (file.isFile && file.name.endsWith(".zip")) {
                val lastModified = LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(file.lastModified()),
                    java.time.ZoneId.systemDefault()
                )

                val daysBetween = ChronoUnit.DAYS.between(lastModified, now)

                if (daysBetween > RETENTION_DAYS) {
                    val deleted = file.delete()
                    if (deleted) {
                        deletedCount++
                    } else {
                        AppLogger.e("DataCleanupWorker", "Failed to delete zip file: ${file.absolutePath}")
                    }
                }
            }
        }

        AppLogger.d("DataCleanupWorker", "Deleted $deletedCount zip files older than $RETENTION_DAYS days")
    }
}