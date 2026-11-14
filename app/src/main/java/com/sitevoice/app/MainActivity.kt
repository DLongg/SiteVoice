package com.sitevoice.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.lifecycle.lifecycleScope
import com.sitevoice.app.data.AppDatabase
import com.sitevoice.app.data.DefaultDailyInputRepository
import com.sitevoice.app.data.DefaultDailyReportRepository
import com.sitevoice.app.data.DefaultNoteRepository
import com.sitevoice.app.settings.SecurePreferences
import com.sitevoice.app.template.DefaultTemplateRepository
import com.sitevoice.app.template.TemplateSchema
import com.sitevoice.app.ui.MainScreen
import com.sitevoice.app.ui.MainViewModel
import com.sitevoice.app.ui.MainViewModelFactory
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val securePreferences by lazy { SecurePreferences(applicationContext) }
    private val database by lazy { AppDatabase.getDatabase(applicationContext) }
    private val noteRepository by lazy { DefaultNoteRepository(database.noteDao()) }
    private val dailyInputRepository by lazy { DefaultDailyInputRepository(database.dailyInputDao()) }
    private val dailyReportRepository by lazy { DefaultDailyReportRepository(database.dailyReportDao()) }
    private val templateRepository by lazy { DefaultTemplateRepository(applicationContext, database.templateDao()) }
    private val templateSchema by lazy {
        val json = assets.open("templates/nhat-ky-cong-truong/schema.json").bufferedReader().use { it.readText() }
        TemplateSchema.fromJson(json)
    }

    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(
            applicationContext,
            noteRepository,
            dailyInputRepository,
            dailyReportRepository,
            securePreferences,
            templateSchema
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (BuildConfig.DEBUG) {
            intent?.getStringExtra("classify_api_key")?.takeIf { it.isNotBlank() }?.let {
                securePreferences.geminiApiKey = it
            }
            intent?.getStringExtra("classify_base_url")?.takeIf { it.isNotBlank() }?.let {
                securePreferences.geminiBaseUrl = it
            }
        }

        lifecycleScope.launch {
            templateRepository.purgeBuiltInAndEnsureSingleActive()
        }

        setContent {
            MaterialTheme {
                MainScreen(
                    viewModel = viewModel,
                    securePreferences = securePreferences
                )
            }
        }
    }
}
