package com.sitevoice.app.export

interface ExportService {
    suspend fun exportToGoogleDoc(templateId: String, replacements: Map<String, String>): Result<String>
}
