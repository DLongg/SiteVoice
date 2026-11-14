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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.sitevoice.app.R
import com.sitevoice.app.data.TemplateItemEntity
import com.sitevoice.app.settings.SecurePreferences
import org.json.JSONObject
import java.util.Locale
import androidx.compose.material3.MaterialTheme
import com.sitevoice.app.ui.theme.SiteVoiceTheme
import com.sitevoice.app.ui.theme.TextSizes
import com.sitevoice.app.ui.theme.Spacing

@Composable
fun SettingsDialog(
    currentSttApiKey: String?,
    currentClassifyApiKey: String?,
    currentClassifyBaseUrl: String = "https://api.vilao.ai/v1",
    currentClassifyModel: String = "acc/gemini-3.8-flash",
    currentDocTemplateId: String = SecurePreferences.DEFAULT_DOC_TEMPLATE_ID,
    currentGoogleWebClientId: String? = null,
    currentGoogleAccountEmail: String? = null,
    activeTemplateName: String? = null,
    activeTemplate: TemplateItemEntity? = null,
    latestReportJson: String? = null,
    isDeveloperMode: Boolean = false,
    currentLlmTemperature: Float? = null,
    onTemperatureChange: ((Float) -> Unit)? = null,
    onToggleDeveloperMode: (Boolean) -> Unit = {},
    onSavePrompt: (templateId: String, newPrompt: String) -> Unit = { _, _ -> },
    onResetPrompt: (templateId: String) -> Unit = {},
    onOpenTemplateLibrary: () -> Unit = {},
    onAutoSetupFormClick: (docUrlOrId: String) -> Unit = {},
    onSaveSettings: (
        sttApiKey: String,
        classifyApiKey: String,
        classifyBaseUrl: String,
        classifyModel: String,
        docTemplateId: String,
        webClientId: String
    ) -> Unit,
    onSignInGoogleClick: () -> Unit,
    onSignOutGoogleClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val securePrefs = remember(context) { SecurePreferences(context) }
    var temperature by remember { mutableStateOf(currentLlmTemperature ?: securePrefs.llmTemperature) }

    var inputSttKey by remember { mutableStateOf(currentSttApiKey ?: "") }
    var inputClassifyKey by remember { mutableStateOf(currentClassifyApiKey ?: "") }
    var inputClassifyBaseUrl by remember { mutableStateOf(currentClassifyBaseUrl) }
    var inputClassifyModel by remember { mutableStateOf(currentClassifyModel) }
    var inputDocId by remember { mutableStateOf(currentDocTemplateId) }
    var inputWebClientId by remember { mutableStateOf(currentGoogleWebClientId ?: "") }

    var devModeEnabled by remember(isDeveloperMode) { mutableStateOf(isDeveloperMode) }
    var customPrompt by remember(activeTemplate?.systemPrompt) {
        mutableStateOf(activeTemplate?.systemPrompt.orEmpty())
    }
    var promptSavedMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.xl)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = stringResource(id = R.string.settings_dialog_title),
                    fontSize = TextSizes.title,
                    lineHeight = TextSizes.titleLine,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(Spacing.xs))

                Text(
                    text = stringResource(id = R.string.settings_security_note),
                    fontSize = TextSizes.caption,
                    lineHeight = TextSizes.captionLine,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(Spacing.lg))

                // Phần 1: Groq API Key (STT)
                Text(
                    text = stringResource(id = R.string.settings_stt_title),
                    fontSize = TextSizes.body,
                    lineHeight = TextSizes.bodyLine,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    text = stringResource(id = R.string.settings_stt_desc),
                    fontSize = TextSizes.caption,
                    lineHeight = TextSizes.captionLine,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(Spacing.sm))
                OutlinedTextField(
                    value = inputSttKey,
                    onValueChange = { inputSttKey = it },
                    placeholder = { Text("gsk_xxxxxxxxxxxxxxxxxxxx", fontSize = TextSizes.caption, lineHeight = TextSizes.captionLine) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )

                Spacer(modifier = Modifier.height(Spacing.lg))

                // Phần 2: Gemini API Phân loại (Vilao AI)
                Text(
                    text = stringResource(id = R.string.settings_gemini_title),
                    fontSize = TextSizes.body,
                    lineHeight = TextSizes.bodyLine,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    text = stringResource(id = R.string.settings_gemini_desc),
                    fontSize = TextSizes.caption,
                    lineHeight = TextSizes.captionLine,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(Spacing.sm))
                OutlinedTextField(
                    value = inputClassifyKey,
                    onValueChange = { inputClassifyKey = it },
                    label = { Text(stringResource(id = R.string.settings_gemini_key_label), fontSize = TextSizes.caption, lineHeight = TextSizes.captionLine) },
                    placeholder = { Text("sk-xxxxxxxxxxxxxxxxxxxx", fontSize = TextSizes.caption, lineHeight = TextSizes.captionLine) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )
                Spacer(modifier = Modifier.height(Spacing.sm))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    OutlinedTextField(
                        value = inputClassifyBaseUrl,
                        onValueChange = { inputClassifyBaseUrl = it },
                        label = { Text("Endpoint", fontSize = TextSizes.caption, lineHeight = TextSizes.captionLine) },
                        singleLine = true,
                        modifier = Modifier.weight(1.3f),
                        shape = MaterialTheme.shapes.medium,
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = TextSizes.caption, lineHeight = TextSizes.captionLine)
                    )
                    OutlinedTextField(
                        value = inputClassifyModel,
                        onValueChange = { inputClassifyModel = it },
                        label = { Text("Model", fontSize = TextSizes.caption, lineHeight = TextSizes.captionLine) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium,
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = TextSizes.caption, lineHeight = TextSizes.captionLine)
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.lg))

                // Nhiệt độ biên tập (Temperature)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.settings_temperature_title),
                        fontSize = TextSizes.body,
                        lineHeight = TextSizes.bodyLine,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Text(
                            text = String.format(Locale.US, "%.2f", temperature),
                            fontSize = TextSizes.caption,
                            lineHeight = TextSizes.captionLine,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    text = stringResource(R.string.settings_temperature_subtitle),
                    fontSize = TextSizes.caption,
                    lineHeight = TextSizes.captionLine,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(Spacing.xs))
                Slider(
                    value = temperature,
                    onValueChange = { newTemp ->
                        val rounded = (Math.round(newTemp * 100) / 100f).coerceIn(0.0f, 0.8f)
                        temperature = rounded
                        securePrefs.llmTemperature = rounded
                        onTemperatureChange?.invoke(rounded)
                    },
                    valueRange = 0.0f..0.8f,
                    steps = 15,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(id = R.string.settings_temperature_exact),
                        fontSize = TextSizes.caption,
                        lineHeight = TextSizes.captionLine,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(id = R.string.settings_temperature_creative),
                        fontSize = TextSizes.caption,
                        lineHeight = TextSizes.captionLine,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.lg))

                // Phần 3: Link Google Doc mẫu
                Text(
                    text = stringResource(id = R.string.settings_doc_template_title),
                    fontSize = TextSizes.body,
                    lineHeight = TextSizes.bodyLine,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(Spacing.sm))
                OutlinedTextField(
                    value = inputDocId,
                    onValueChange = { inputDocId = it },
                    placeholder = { Text(stringResource(id = R.string.settings_doc_template_hint), fontSize = TextSizes.caption, lineHeight = TextSizes.captionLine) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    text = stringResource(id = R.string.settings_doc_default_id, SecurePreferences.DEFAULT_DOC_TEMPLATE_ID),
                    fontSize = TextSizes.caption,
                    lineHeight = TextSizes.captionLine,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(Spacing.sm))
                Button(
                    onClick = { onAutoSetupFormClick(inputDocId) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                        Text(
                            text = stringResource(id = R.string.settings_btn_auto_setup),
                            fontSize = TextSizes.caption,
                            lineHeight = TextSizes.captionLine,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.lg))

                // Phần 4: Google Account & Web Client ID
                Text(
                    text = stringResource(id = R.string.settings_google_section),
                    fontSize = TextSizes.body,
                    lineHeight = TextSizes.bodyLine,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(Spacing.sm))

                OutlinedTextField(
                    value = inputWebClientId,
                    onValueChange = { inputWebClientId = it },
                    label = { Text(stringResource(id = R.string.settings_google_client_id), fontSize = TextSizes.caption, lineHeight = TextSizes.captionLine) },
                    placeholder = { Text(stringResource(id = R.string.settings_google_client_id_hint), fontSize = TextSizes.caption, lineHeight = TextSizes.captionLine) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )

                Spacer(modifier = Modifier.height(Spacing.md))

                // Thẻ trạng thái tài khoản Google
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background, MaterialTheme.shapes.medium)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
                        .padding(Spacing.md)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(id = R.string.settings_google_account),
                                fontSize = TextSizes.caption,
                                lineHeight = TextSizes.captionLine,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(Spacing.xs))
                            Text(
                                text = currentGoogleAccountEmail ?: stringResource(id = R.string.settings_google_not_signed_in),
                                fontSize = TextSizes.body,
                                lineHeight = TextSizes.bodyLine,
                                fontWeight = FontWeight.SemiBold,
                                color = if (currentGoogleAccountEmail != null) SiteVoiceTheme.colors.success else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (currentGoogleAccountEmail != null) {
                            OutlinedButton(
                                onClick = onSignOutGoogleClick,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(text = stringResource(id = R.string.settings_btn_sign_out_google), fontSize = TextSizes.caption, lineHeight = TextSizes.captionLine)
                            }
                        } else {
                            Button(
                                onClick = onSignInGoogleClick,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(text = stringResource(id = R.string.settings_btn_sign_in_google), fontSize = TextSizes.caption, lineHeight = TextSizes.captionLine)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.lg))

                // Phần 5: Thư viện Mẫu
                Text(
                    text = stringResource(id = R.string.settings_template_library_title),
                    fontSize = TextSizes.body,
                    lineHeight = TextSizes.bodyLine,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(Spacing.sm))
                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onOpenTemplateLibrary()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = activeTemplateName ?: stringResource(id = R.string.settings_template_manage_fallback),
                                fontSize = TextSizes.body,
                                lineHeight = TextSizes.bodyLine,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                        ) {
                            Text(text = stringResource(id = R.string.settings_template_action), fontSize = TextSizes.caption, lineHeight = TextSizes.captionLine, color = MaterialTheme.colorScheme.primary)
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.lg))

                // Phần 6: Chế độ Nhà phát triển (Developer Mode)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background, MaterialTheme.shapes.medium)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
                        .padding(Spacing.md)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(id = R.string.settings_dev_mode_title),
                                    fontSize = TextSizes.body,
                                    lineHeight = TextSizes.bodyLine,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(id = R.string.settings_dev_mode_desc),
                                    fontSize = TextSizes.caption,
                                    lineHeight = TextSizes.captionLine,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                            Spacer(modifier = Modifier.height(Spacing.md))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Build,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(id = R.string.settings_dev_current_prompt, activeTemplate.name),
                                    fontSize = TextSizes.caption,
                                    lineHeight = TextSizes.captionLine,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(Spacing.xs))
                            OutlinedTextField(
                                value = customPrompt,
                                onValueChange = {
                                    customPrompt = it
                                    promptSavedMessage = null
                                },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 7,
                                maxLines = 14,
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = TextSizes.caption, lineHeight = TextSizes.captionLine)
                            )

                            Spacer(modifier = Modifier.height(Spacing.sm))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = {
                                        onSavePrompt(activeTemplate.id, customPrompt)
                                        promptSavedMessage = context.getString(R.string.settings_prompt_saved)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = MaterialTheme.shapes.small,
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = Spacing.sm, vertical = Spacing.sm)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Save,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Spacer(modifier = Modifier.width(Spacing.xs))
                                    Text(stringResource(id = R.string.settings_btn_save_prompt), fontSize = TextSizes.caption, lineHeight = TextSizes.captionLine, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimary)
                                }

                                OutlinedButton(
                                    onClick = {
                                        onResetPrompt(activeTemplate.id)
                                        promptSavedMessage = context.getString(R.string.settings_prompt_reset)
                                    },
                                    shape = MaterialTheme.shapes.small,
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = Spacing.sm, vertical = Spacing.sm)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(Spacing.xs))
                                    Text(stringResource(id = R.string.settings_btn_reset_prompt), fontSize = TextSizes.caption, lineHeight = TextSizes.captionLine, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            promptSavedMessage?.let { msg ->
                                Spacer(modifier = Modifier.height(Spacing.xs))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = SiteVoiceTheme.colors.success
                                    )
                                    Text(
                                        text = msg,
                                        fontSize = TextSizes.caption,
                                        lineHeight = TextSizes.captionLine,
                                        color = SiteVoiceTheme.colors.success,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            if (!latestReportJson.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(Spacing.lg))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Analytics,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = stringResource(id = R.string.settings_dev_json_title),
                                        fontSize = TextSizes.caption,
                                        lineHeight = TextSizes.captionLine,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(modifier = Modifier.height(Spacing.xs))
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
                                        fontSize = TextSizes.caption,
                                        lineHeight = TextSizes.captionLine,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.xl))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(stringResource(id = R.string.common_close))
                    }

                    Spacer(modifier = Modifier.width(Spacing.sm))

                    Button(
                        onClick = {
                            securePrefs.llmTemperature = temperature
                            onTemperatureChange?.invoke(temperature)
                            onSaveSettings(
                                inputSttKey.trim(),
                                inputClassifyKey.trim(),
                                inputClassifyBaseUrl.trim(),
                                inputClassifyModel.trim(),
                                inputDocId.trim(),
                                inputWebClientId.trim()
                            )
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(stringResource(id = R.string.settings_btn_save))
                    }
                }
            }
        }
    }
}
