package com.sitevoice.app.data

import kotlinx.coroutines.flow.Flow

interface DailyReportRepository {
    fun getReport(date: String): Flow<DailyReportEntity?>
    suspend fun getReportSync(date: String): DailyReportEntity?
    suspend fun saveReport(report: DailyReportEntity)
    suspend fun approveReport(date: String)
    suspend fun markExported(date: String, docUrl: String)
}

class DefaultDailyReportRepository(
    private val dao: DailyReportDao
) : DailyReportRepository {

    override fun getReport(date: String): Flow<DailyReportEntity?> {
        return dao.getByDate(date)
    }

    override suspend fun getReportSync(date: String): DailyReportEntity? {
        return dao.getByDateSync(date)
    }

    override suspend fun saveReport(report: DailyReportEntity) {
        dao.insertOrUpdate(report)
    }

    override suspend fun approveReport(date: String) {
        dao.updateStatus(date, "APPROVED")
    }

    override suspend fun markExported(date: String, docUrl: String) {
        dao.updateDocUrl(date, docUrl, "EXPORTED")
    }
}
