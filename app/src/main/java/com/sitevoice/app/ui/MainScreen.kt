package com.sitevoice.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sitevoice.app.R
import com.sitevoice.app.settings.SecurePreferences
import com.sitevoice.app.ui.daily.DailyInputScreen
import com.sitevoice.app.ui.notes.NotesListScreen
import com.sitevoice.app.ui.record.RecordScreen
import com.sitevoice.app.ui.settings.SettingsDialog

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    securePreferences: SecurePreferences
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    val activeNotes by viewModel.activeNotes.collectAsState()
    val dailyInput by viewModel.dailyInput.collectAsState()

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 4.dp
            ) {
                // Tab 0: Ghi âm
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = {
                        Text(
                            text = "🎙️",
                            fontSize = 18.sp
                        )
                    },
                    label = {
                        Text(
                            text = stringResource(id = R.string.tab_record),
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedTextColor = Color(0xFF1E293B),
                        selectedIconColor = Color(0xFF1E293B),
                        indicatorColor = Color(0xFFE2E8F0)
                    )
                )

                // Tab 1: Danh sách ghi chú hôm nay
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (activeNotes.isNotEmpty()) {
                                    Badge(
                                        containerColor = Color(0xFF2563EB),
                                        contentColor = Color.White
                                    ) {
                                        Text(text = activeNotes.size.toString())
                                    }
                                }
                            }
                        ) {
                            Text(
                                text = "📋",
                                fontSize = 18.sp
                            )
                        }
                    },
                    label = {
                        Text(
                            text = stringResource(id = R.string.tab_notes),
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedTextColor = Color(0xFF1E293B),
                        selectedIconColor = Color(0xFF1E293B),
                        indicatorColor = Color(0xFFE2E8F0)
                    )
                )

                // Tab 2: Nhật ký cuối ngày & Tạo báo cáo
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = {
                        Text(
                            text = "📑",
                            fontSize = 18.sp
                        )
                    },
                    label = {
                        Text(
                            text = stringResource(id = R.string.tab_report),
                            fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedTextColor = Color(0xFF1E293B),
                        selectedIconColor = Color(0xFF1E293B),
                        indicatorColor = Color(0xFFE2E8F0)
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> RecordScreen(
                    viewModel = viewModel,
                    securePreferences = securePreferences,
                    onOpenSettings = { showSettingsDialog = true }
                )
                1 -> NotesListScreen(
                    notes = activeNotes,
                    audioPlayer = viewModel.audioPlayer,
                    onUpdateText = { id, text -> viewModel.updateNoteText(id, text) },
                    onUpdateLabel = { id, label -> viewModel.updateNoteLabel(id, label, "user") },
                    onDeleteNote = { id -> viewModel.deleteNote(id) },
                    onRetryStt = { note -> viewModel.retryStt(note) }
                )
                2 -> DailyInputScreen(
                    currentDate = viewModel.currentDate,
                    initialDailyInput = dailyInput,
                    onSaveDailyInput = { viewModel.saveDailyInput(it) },
                    onGenerateReportClick = {
                        // Chuẩn bị kích hoạt luồng tổng hợp LLM ở Task 2.2
                    }
                )
            }
        }

        // Hộp thoại cài đặt API key
        if (showSettingsDialog) {
            SettingsDialog(
                currentApiKey = securePreferences.groqApiKey,
                onSaveKey = { newKey ->
                    securePreferences.groqApiKey = newKey
                },
                onDismiss = { showSettingsDialog = false }
            )
        }
    }
}
