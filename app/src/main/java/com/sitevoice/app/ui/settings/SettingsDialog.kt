package com.sitevoice.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.sitevoice.app.R
import com.sitevoice.app.data.TemplateItemEntity
import com.sitevoice.app.settings.SecurePreferences
import org.json.JSONObject

@Composable
fun SettingsDialog(
    currentApiKey: String?,
    currentDocTemplateId: String = SecurePreferences.DEFAULT_DOC_TEMPLATE_ID,
    currentGoogleWebClientId: String? = null,
    currentGoogleAccountEmail: String? = null,
    activeTemplateName: String? = null,
    activeTemplate: TemplateItemEntity? = null,
    latestReportJson: String? = null,
    isDeveloperMode: Boolean = false,
    onToggleDeveloperMode: (Boolean) -> Unit = {},
    onSavePrompt: (templateId: String, newPrompt: String) -> Unit = { _, _ -> },
    onResetPrompt: (templateId: String) -> Unit = {},
    onOpenTemplateLibrary: () -> Unit = {},
    onAutoSetupFormClick: (docUrlOrId: String) -> Unit = {},
    onSaveSettings: (apiKey: String, docTemplateId: String, webClientId: String) -> Unit,
    onSignInGoogleClick: () -> Unit,
    onSignOutGoogleClick: () -> Unit,
    onDismiss: () -> Unit
) {
    var inputKey by remember { mutableStateOf(currentApiKey ?: "") }
    var inputDocId by remember { mutableStateOf(currentDocTemplateId) }
    var inputWebClientId by remember { mutableStateOf(currentGoogleWebClientId ?: "") }

    var devModeEnabled by remember(isDeveloperMode) { mutableStateOf(isDeveloperMode) }
    var customPrompt by remember(activeTemplate?.systemPrompt) {
        mutableStateOf(activeTemplate?.systemPrompt.orEmpty())
    }
    var promptSavedMessage by remember { mutableStateOf<String?>(null) }

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

                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { onAutoSetupFormClick(inputDocId) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "✨ Tự động nhận diện & Tạo Mẫu từ Doc này",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }

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

                // Phần 4: Thư viện Mẫu
                Text(
                    text = "4. Thư viện Mẫu Báo cáo",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1E293B)
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onOpenTemplateLibrary()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📑 " + (activeTemplateName ?: "Quản lý Thư viện Mẫu"),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF1E293B)
                        )
                        Text(text = "Đổi / Căn chỉnh →", fontSize = 12.sp, color = Color(0xFF2563EB))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Phần 5: Chế độ Nhà phát triển (Developer Mode)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "5. Chế độ Nhà phát triển (Developer Mode)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF1E293B)
                                )
                                Text(
                                    text = "Xem & chỉnh sửa trực tiếp System Prompt của AI",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                            Switch(
                                checked = devModeEnabled,
                                onCheckedChange = {
                                    devModeEnabled = it
                                    onToggleDeveloperMode(it)
                                }
                            )
                        }

                        if (devModeEnabled && activeTemplate != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "🛠️ System Prompt hiện tại (${activeTemplate.name}):",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = customPrompt,
                                onValueChange = {
                                    customPrompt = it
                                    promptSavedMessage = null
                                },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 7,
                                maxLines = 14,
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = {
                                        onSavePrompt(activeTemplate.id, customPrompt)
                                        promptSavedMessage = "Đã lưu prompt thành công!"
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Text("💾 Lưu Prompt", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                                }

                                OutlinedButton(
                                    onClick = {
                                        onResetPrompt(activeTemplate.id)
                                        promptSavedMessage = "Đã khôi phục prompt gốc!"
                                    },
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Text("🔄 Khôi phục gốc", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF475569))
                                }
                            }

                            promptSavedMessage?.let { msg ->
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "✓ $msg",
                                    fontSize = 11.sp,
                                    color = Color(0xFF15803D),
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            if (!latestReportJson.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "📊 Dữ liệu JSON gần nhất do LLM trả về:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                val formattedJson = try {
                                    JSONObject(latestReportJson).toString(2)
                                } catch (_: Exception) {
                                    latestReportJson
                                }
                                OutlinedTextField(
                                    value = formattedJson,
                                    onValueChange = {},
                                    readOnly = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 6,
                                    maxLines = 12,
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        fontSize = 10.sp,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                    )
                                )
                            }
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
                                inputWebClientId.trim()
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
