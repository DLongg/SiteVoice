package com.sitevoice.app.queue

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.sitevoice.app.data.AppDatabase
import com.sitevoice.app.export.GoogleAuthManager
import com.sitevoice.app.export.GoogleDocsExporter
import com.sitevoice.app.settings.SecurePreferences
import com.sitevoice.app.template.DailyReportContent
import java.util.concurrent.TimeUnit

class ExportWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "ExportWorker"
        const val KEY_DATE = "report_date"

        fun createWorkRequest(date: String): OneTimeWorkRequest {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val inputData = Data.Builder()
                .putString(KEY_DATE, date)
                .build()

            return OneTimeWorkRequestBuilder<ExportWorker>()
                .setConstraints(constraints)
                .setInputData(inputData)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    15, TimeUnit.SECONDS
                )
                .addTag(TAG)
                .addTag("export_$date")
                .build()
        }

        fun enqueue(context: Context, date: String) {
            val request = createWorkRequest(date)
            WorkManager.getInstance(context).enqueueUniqueWork(
                "export_work_$date",
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }

    override suspend fun doWork(): Result {
        val date = inputData.getString(KEY_DATE) ?: return Result.failure()

        val database = AppDatabase.getDatabase(applicationContext)
        val dailyReportDao = database.dailyReportDao()
        val dailyInputDao = database.dailyInputDao()

        val report = dailyReportDao.getByDateSync(date) ?: run {
            Log.e(TAG, "Không tìm thấy báo cáo cho ngày $date")
            return Result.failure()
        }

        val account = GoogleAuthManager.getLastSignedInAccount(applicationContext)
        val acc = account?.account ?: run {
            Log.e(TAG, "Chưa đăng nhập tài khoản Google")
            return Result.failure()
        }

        val tokenResult = GoogleAuthManager.fetchAccessToken(applicationContext, acc)
        if (tokenResult.isFailure) {
            Log.e(TAG, "Không lấy được token: ${tokenResult.exceptionOrNull()?.message}")
            return if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
        val token = tokenResult.getOrThrow()

        val dailyInput = dailyInputDao.getByDateSync(date)
        val reportContent = DailyReportContent.fromJson(report.contentJson)
        val securePreferences = SecurePreferences(applicationContext)
        val templateId = securePreferences.docTemplateId

        val exporter = GoogleDocsExporter()
        val exportResult = exporter.exportReport(
            date = date,
            dailyInput = dailyInput,
            reportContent = reportContent,
            templateDocId = templateId,
            accessToken = token
        )

        return if (exportResult.isSuccess) {
            val result = exportResult.getOrThrow()
            dailyReportDao.updateDocUrl(date, result.docUrl, "EXPORTED")
            Log.i(TAG, "Xuất Doc thành công: ${result.docUrl}")
            Result.success()
        } else {
            Log.e(TAG, "Xuất Doc thất bại: ${exportResult.exceptionOrNull()?.message}")
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}
