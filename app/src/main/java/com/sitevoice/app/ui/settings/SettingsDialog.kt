package com.sitevoice.app.ui.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.sitevoice.app.R
import com.sitevoice.app.settings.SecurePreferences
import com.sitevoice.app.stt.WhisperModelManager
import kotlinx.coroutines.launch

@Composable
fun SettingsDialog(
    currentApiKey: String?,
    currentDocTemplateId: String = SecurePreferences.DEFAULT_DOC_TEMPLATE_ID,
    currentGoogleWebClientId: String? = null,
    currentGoogleAccountEmail: String? = null,
    initialOfflineSttEnabled: Boolean = false,
    initialOfflineModelId: String = SecurePreferences.DEFAULT_OFFLINE_MODEL_ID,
    onSaveSettings: (apiKey: String, docTemplateId: String, webClientId: String, isOfflineEnabled: Boolean, modelId: String) -> Unit,
    onSignInGoogleClick: () -> Unit,
    onSignOutGoogleClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var inputKey by remember { mutableStateOf(currentApiKey ?: "") }
    var inputDocId by remember { mutableStateOf(currentDocTemplateId) }
    var inputWebClientId by remember { mutableStateOf(currentGoogleWebClientId ?: "") }

    var isOfflineEnabled by remember { mutableStateOf(initialOfflineSttEnabled) }
    var selectedModelId by remember { mutableStateOf(initialOfflineModelId) }

    var isDownloadingModel by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableIntStateOf(0) }
    var modelRefreshTrigger by remember { mutableIntStateOf(0) }

    val isCurrentModelDownloaded = remember(selectedModelId, modelRefreshTrigger) {
        WhisperModelManager.isModelDownloaded(context, selectedModelId)
    }

    val currentModelSizeText = remember(selectedModelId, modelRefreshTrigger) {
        WhisperModelManager.getModelSizeFormatted(context, selectedModelId)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Cài đặt hệ thống",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Dữ liệu được mã hóa an toàn trên thiết bị bằng EncryptedSharedPreferences.",
                    fontSize = 12.sp,
                    color = Color(0xFF6B7280)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Phần 1: Groq API Key
                Text(
                    text = "1. Groq API Key (STT & LLM)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1E293B)
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = inputKey,
                    onValueChange = { inputKey = it },
                    placeholder = { Text("gsk_xxxxxxxxxxxxxxxxxxxx", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Phần 2: Link Google Doc mẫu
                Text(
                    text = "2. Google Doc mẫu (A4 ≤ 2 trang)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1E293B)
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = inputDocId,
                    onValueChange = { inputDocId = it },
                    placeholder = { Text("Dán link hoặc Doc ID mẫu", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "ID mẫu mặc định: ${SecurePreferences.DEFAULT_DOC_TEMPLATE_ID}",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Phần 3: Google Account & Web Client ID
                Text(
                    text = stringResource(id = R.string.settings_google_section),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1E293B)
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = inputWebClientId,
                    onValueChange = { inputWebClientId = it },
                    label = { Text(stringResource(id = R.string.settings_google_client_id), fontSize = 12.sp) },
                    placeholder = { Text(stringResource(id = R.string.settings_google_client_id_hint), fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Thẻ trạng thái tài khoản Google
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(id = R.string.settings_google_account),
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = currentGoogleAccountEmail ?: stringResource(id = R.string.settings_google_not_signed_in),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (currentGoogleAccountEmail != null) Color(0xFF15803D) else Color(0xFF94A3B8)
                            )
                        }

                        if (currentGoogleAccountEmail != null) {
                            OutlinedButton(
                                onClick = onSignOutGoogleClick,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(text = stringResource(id = R.string.settings_btn_sign_out_google), fontSize = 11.sp)
                            }
                        } else {
                            Button(
                                onClick = onSignInGoogleClick,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(text = stringResource(id = R.string.settings_btn_sign_in_google), fontSize = 11.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Phần 4: Nhận diện giọng nói ngoại tuyến (Whisper.cpp)
                Text(
                    text = stringResource(id = R.string.settings_offline_stt_title),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1E293B)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(id = R.string.settings_offline_stt_desc),
                    fontSize = 11.sp,
                    color = Color(0xFF64748B),
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Thẻ cấu hình mô hình offline
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        // Switch bật/tắt offline
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(id = R.string.settings_offline_stt_switch),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF1E293B)
                            )

                            Switch(
                                checked = isOfflineEnabled,
                                onCheckedChange = { checked ->
                                    if (checked && !isCurrentModelDownloaded) {
                                        Toast.makeText(context, "Vui lòng bấm 'Tải mô hình' bên dưới trước", Toast.LENGTH_SHORT).show()
                                    }
                                    isOfflineEnabled = checked
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF0284C7)
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Lựa chọn phiên bản model (Tiny / Base / Small / Turbo-Q5)
                        Text(
                            text = stringResource(id = R.string.settings_offline_model_label),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF64748B)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Lưới 2 hàng x 2 cột hiển thị gọn gàng, rõ ràng
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Hàng 1: Tiny & Base
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                WhisperModelManager.AVAILABLE_MODELS.take(2).forEach { model ->
                                    val isSelected = model.id == selectedModelId
                                    val bg = if (isSelected) Color(0xFFE0F2FE) else Color.White
                                    val border = if (isSelected) Color(0xFF0284C7) else Color(0xFFCBD5E1)
                                    val textCol = if (isSelected) Color(0xFF0369A1) else Color(0xFF475569)

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(bg, RoundedCornerShape(6.dp))
                                            .border(1.dp, border, RoundedCornerShape(6.dp))
                                            .clickable { selectedModelId = model.id }
                                            .padding(vertical = 8.dp, horizontal = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = model.id.uppercase(),
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = textCol
                                            )
                                            Text(
                                                text = "~${model.approximateSizeBytes / (1024 * 1024)}MB",
                                                fontSize = 10.sp,
                                                color = if (isSelected) Color(0xFF0284C7) else Color(0xFF94A3B8)
                                            )
                                        }
                                    }
                                }
                            }

                            // Hàng 2: Small & Turbo-Q5
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                WhisperModelManager.AVAILABLE_MODELS.drop(2).forEach { model ->
                                    val isSelected = model.id == selectedModelId
                                    val bg = if (isSelected) Color(0xFFE0F2FE) else Color.White
                                    val border = if (isSelected) Color(0xFF0284C7) else Color(0xFFCBD5E1)
                                    val textCol = if (isSelected) Color(0xFF0369A1) else Color(0xFF475569)

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(bg, RoundedCornerShape(6.dp))
                                            .border(1.dp, border, RoundedCornerShape(6.dp))
                                            .clickable { selectedModelId = model.id }
                                            .padding(vertical = 8.dp, horizontal = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = model.id.uppercase(),
                                                    fontSize = 12.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = textCol
                                                )
                                                if (model.isRecommended) {
                                                    Spacer(modifier = Modifier.width(3.dp))
                                                    Text(
                                                        text = "★",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFFD97706)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = if (model.isRecommended) "~${model.approximateSizeBytes / (1024 * 1024)}MB (Tốt nhất)"
                                                else "~${model.approximateSizeBytes / (1024 * 1024)}MB",
                                                fontSize = 10.sp,
                                                color = if (isSelected) Color(0xFF0284C7) else Color(0xFF94A3B8)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Trạng thái và nút tải / xóa model
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (isCurrentModelDownloaded)
                                        stringResource(id = R.string.settings_offline_model_downloaded, currentModelSizeText)
                                    else
                                        stringResource(id = R.string.settings_offline_model_not_downloaded),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isCurrentModelDownloaded) Color(0xFF15803D) else Color(0xFFB45309)
                                )
                            }

                            if (isCurrentModelDownloaded) {
                                OutlinedButton(
                                    onClick = {
                                        WhisperModelManager.deleteModel(context, selectedModelId)
                                        modelRefreshTrigger++
                                        Toast.makeText(context, "Đã xóa mô hình offline", Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = stringResource(id = R.string.settings_offline_btn_delete),
                                        fontSize = 11.sp,
                                        color = Color(0xFFDC2626)
                                    )
                                }
                            } else {
                                Button(
                                    onClick = {
                                        if (isDownloadingModel) return@Button
                                        isDownloadingModel = true
                                        downloadProgress = 0
                                        coroutineScope.launch {
                                            val result = WhisperModelManager.downloadModel(context, selectedModelId) { percent ->
                                                downloadProgress = percent
                                            }
                                            isDownloadingModel = false
                                            if (result.isSuccess) {
                                                modelRefreshTrigger++
                                                Toast.makeText(context, "Đã tải mô hình Whisper thành công!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "Lỗi tải mô hình: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    },
                                    enabled = !isDownloadingModel,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    if (isDownloadingModel) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(12.dp),
                                            strokeWidth = 1.5.dp,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = stringResource(id = R.string.settings_offline_downloading, downloadProgress),
                                            fontSize = 11.sp,
                                            color = Color.White
                                        )
                                    } else {
                                        Text(
                                            text = stringResource(id = R.string.settings_offline_btn_download),
                                            fontSize = 11.sp,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }

                        if (isDownloadingModel) {
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { downloadProgress / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp),
                                color = Color(0xFF0284C7)
                            )
                        }

                        if (isOfflineEnabled && !isCurrentModelDownloaded) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "⚠️ " + stringResource(id = R.string.settings_offline_no_model_warning),
                                fontSize = 11.sp,
                                color = Color(0xFFB45309),
                                lineHeight = 15.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Đóng")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            onSaveSettings(
                                inputKey.trim(),
                                inputDocId.trim(),
                                inputWebClientId.trim(),
                                isOfflineEnabled,
                                selectedModelId
                            )
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Lưu cài đặt")
                    }
                }
            }
        }
    }
}
