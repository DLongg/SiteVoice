package com.sitevoice.app.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.sitevoice.app.R
import com.sitevoice.app.data.TemplateItemEntity
import com.sitevoice.app.export.GoogleAuthManager
import com.sitevoice.app.settings.SecurePreferences
import com.sitevoice.app.ui.daily.DailyInputScreen
import com.sitevoice.app.ui.daily.DailyReportPreviewScreen
import com.sitevoice.app.ui.notes.NotesListScreen
import com.sitevoice.app.ui.record.RecordScreen
import com.sitevoice.app.ui.settings.SettingsDialog
import com.sitevoice.app.ui.template.TemplateEditorScreen
import com.sitevoice.app.ui.template.TemplateLibraryScreen

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    securePreferences: SecurePreferences
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showReportPreview by remember { mutableStateOf(false) }
    var showTemplateLibrary by remember { mutableStateOf(false) }
    var showTemplateEditor by remember { mutableStateOf(false) }
    var editingTemplate by remember { mutableStateOf<TemplateItemEntity?>(null) }

    val activeNotes by viewModel.activeNotes.collectAsState()
    val dailyInput by viewModel.dailyInput.collectAsState()
    val dailyReport by viewModel.dailyReport.collectAsState()
    val templates by viewModel.templates.collectAsState()
    val activeTemplate by viewModel.activeTemplate.collectAsState()
    val isSynthesizing by viewModel.isSynthesizing.collectAsState()
    val synthesisError by viewModel.synthesisError.collectAsState()
    val isExporting by viewModel.isExporting.collectAsState()
    val exportError by viewModel.exportError.collectAsState()

    // Activity Result Launcher cho Google Sign-In
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            if (account != null) {
                securePreferences.googleAccountEmail = account.email
                Toast.makeText(context, "Đăng nhập Google thành công: ${account.email}", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Lỗi đăng nhập Google: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

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
                    activeTemplateName = activeTemplate?.name,
                    onOpenSettings = { showSettingsDialog = true },
                    onOpenTemplateLibrary = { showTemplateLibrary = true }
                )
                1 -> NotesListScreen(
                    notes = activeNotes,
                    audioPlayer = viewModel.audioPlayer,
                    onUpdateText = { id, text -> viewModel.updateNoteText(id, text) },
                    onUpdateLabel = { id, label -> viewModel.updateNoteLabel(id, label, "user") },
                    onDeleteNote = { id -> viewModel.deleteNote(id) },
                    onRetryStt = { note -> viewModel.retryStt(note) }
                )
                2 -> {
                    if (showReportPreview) {
                        DailyReportPreviewScreen(
                            currentDate = viewModel.currentDate,
                            reportEntity = dailyReport,
                            dailyInput = dailyInput,
                            isSynthesizing = isSynthesizing,
                            isExporting = isExporting,
                            onApproveClick = {
                                viewModel.approveDailyReport()
                                Toast.makeText(context, "✓ Đã phê duyệt nhật ký thi công!", Toast.LENGTH_SHORT).show()
                            },
                            onResynthesizeClick = {
                                viewModel.synthesizeDailyReport()
                            },
                            onBackToEditClick = {
                                showReportPreview = false
                            },
                            onExportDocClick = {
                                viewModel.exportDailyReport(context) { docUrl ->
                                    Toast.makeText(context, "✓ Đã xuất Google Doc thành công!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onOpenDocClick = { docUrl ->
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(docUrl))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Không thể mở trình duyệt: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    } else {
                        DailyInputScreen(
                            currentDate = viewModel.currentDate,
                            initialDailyInput = dailyInput,
                            dailyReport = dailyReport,
                            onSaveDailyInput = { viewModel.saveDailyInput(it) },
                            onGenerateReportClick = {
                                showReportPreview = true
                                viewModel.synthesizeDailyReport()
                            },
                            onViewReportClick = {
                                showReportPreview = true
                            }
                        )
                    }
                }
            }
        }

        // Hộp thoại cài đặt API key, Doc mẫu & Google Sign-In
        if (showSettingsDialog) {
            SettingsDialog(
                currentApiKey = securePreferences.groqApiKey,
                currentDocTemplateId = securePreferences.docTemplateId,
                currentGoogleWebClientId = securePreferences.googleWebClientId,
                currentGoogleAccountEmail = securePreferences.googleAccountEmail,
                activeTemplateName = activeTemplate?.name,
                onOpenTemplateLibrary = { showTemplateLibrary = true },
                onSaveSettings = { newKey, newDocId, newClientId ->
                    securePreferences.groqApiKey = newKey
                    securePreferences.docTemplateId = newDocId
                    securePreferences.googleWebClientId = newClientId
                },
                onSignInGoogleClick = {
                    val client = GoogleAuthManager.getSignInClient(context, securePreferences.googleWebClientId)
                    googleSignInLauncher.launch(client.signInIntent)
                },
                onSignOutGoogleClick = {
                    GoogleAuthManager.signOut(context) {
                        securePreferences.googleAccountEmail = null
                        securePreferences.googleAccessToken = null
                        Toast.makeText(context, "Đã đăng xuất tài khoản Google", Toast.LENGTH_SHORT).show()
                    }
                },
                onDismiss = { showSettingsDialog = false }
            )
        }

        // Màn hình Thư viện Mẫu Báo cáo
        if (showTemplateLibrary) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF8FAFC))
            ) {
                TemplateLibraryScreen(
                    templates = templates,
                    onClose = { showTemplateLibrary = false },
                    onActivateTemplate = { id ->
                        viewModel.setActiveTemplate(id)
                        val name = templates.find { it.id == id }?.name ?: id
                        Toast.makeText(context, "Đã áp dụng mẫu: $name", Toast.LENGTH_SHORT).show()
                    },
                    onCreateNewTemplate = {
                        editingTemplate = null
                        showTemplateEditor = true
                    },
                    onEditTemplate = { t ->
                        editingTemplate = t
                        showTemplateEditor = true
                    },
                    onDuplicateTemplate = { t ->
                        viewModel.duplicateTemplate(t) {
                            Toast.makeText(context, "Đã nhân bản mẫu thành công", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onDeleteTemplate = { id ->
                        viewModel.deleteTemplate(id) {
                            Toast.makeText(context, "Đã xóa mẫu tùy chỉnh", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }

        // Màn hình Tạo / Căn chỉnh Mẫu Báo cáo
        if (showTemplateEditor) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF8FAFC))
            ) {
                TemplateEditorScreen(
                    template = editingTemplate,
                    onClose = { showTemplateEditor = false },
                    onSaveTemplate = { updatedTemplate ->
                        viewModel.saveCustomTemplate(updatedTemplate) {
                            showTemplateEditor = false
                            Toast.makeText(context, "Đã lưu mẫu vào thư viện!", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }

        // Hộp thoại báo lỗi khi tổng hợp LLM thất bại
        if (synthesisError != null) {
            AlertDialog(
                onDismissRequest = { viewModel.clearSynthesisError() },
                title = { Text("Thông báo", fontWeight = FontWeight.Bold) },
                text = { Text(synthesisError ?: "") },
                confirmButton = {
                    TextButton(
                        onClick = { viewModel.clearSynthesisError() }
                    ) {
                        Text("Đóng")
                    }
                }
            )
        }

        // Hộp thoại báo lỗi khi xuất Google Doc thất bại
        if (exportError != null) {
            AlertDialog(
                onDismissRequest = { viewModel.clearExportError() },
                title = { Text("Lỗi xuất tài liệu", fontWeight = FontWeight.Bold) },
                text = { Text(exportError ?: "") },
                confirmButton = {
                    TextButton(
                        onClick = { viewModel.clearExportError() }
                    ) {
                        Text("Đóng")
                    }
                }
            )
        }
    }
}
