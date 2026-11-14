package com.sitevoice.app.ui.daily

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sitevoice.app.R
import com.sitevoice.app.data.DailyInputEntity
import com.sitevoice.app.data.DailyReportEntity
import androidx.compose.material3.MaterialTheme
import com.sitevoice.app.ui.theme.SiteVoiceTheme
import com.sitevoice.app.ui.theme.TextSizes
import com.sitevoice.app.ui.theme.Spacing

@Composable
fun DailyInputScreen(
    currentDate: String,
    initialDailyInput: DailyInputEntity?,
    dailyReport: DailyReportEntity? = null,
    activeTemplateName: String? = null,
    onOpenTemplateLibrary: () -> Unit = {},
    onSaveDailyInput: (DailyInputEntity) -> Unit,
    onGenerateReportClick: () -> Unit,
    onViewReportClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var weatherMorning by remember { mutableStateOf("nang") }
    var weatherAfternoon by remember { mutableStateOf("nang") }
    var weatherEvening by remember { mutableStateOf("binh_thuong") }
    var commandStaffCount by remember { mutableIntStateOf(0) }
    var workerCount by remember { mutableIntStateOf(0) }
    var equipmentList by remember { mutableStateOf("") }

    var isSavedNotification by remember { mutableStateOf(false) }

    // Đồng bộ giá trị khởi tạo khi có bản ghi (hoặc bản ghi kế thừa)
    LaunchedEffect(initialDailyInput) {
        if (initialDailyInput != null) {
            weatherMorning = initialDailyInput.weatherMorning
            weatherAfternoon = initialDailyInput.weatherAfternoon
            weatherEvening = initialDailyInput.weatherEvening
            commandStaffCount = initialDailyInput.commandStaffCount
            workerCount = initialDailyInput.workerCount
            equipmentList = initialDailyInput.equipmentList
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Spacing.lg)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Spacing.md),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(id = R.string.tab_report),
                        fontSize = TextSizes.title,
                        lineHeight = TextSizes.titleLine,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Text(
                        text = "Ngày: $currentDate",
                        fontSize = TextSizes.body,
                        lineHeight = TextSizes.bodyLine,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (isSavedNotification) {
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
                            text = "Đã lưu",
                            fontSize = TextSizes.caption,
                            lineHeight = TextSizes.captionLine,
                            fontWeight = FontWeight.Bold,
                            color = SiteVoiceTheme.colors.success
                        )
                    }
                }
            }

            // Banner chọn / đổi Mẫu Báo cáo (TemplateSelector)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Spacing.lg)
                    .clickable { onOpenTemplateLibrary() },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.medium,
                border = CardDefaults.outlinedCardBorder()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "MẪU BÁO CÁO ÁP DỤNG",
                            fontSize = TextSizes.caption,
                            lineHeight = TextSizes.captionLine,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(Spacing.xs))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                        ) {
                            Icon(
                                imageVector = if (activeTemplateName != null) Icons.Default.Description else Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = if (activeTemplateName != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (activeTemplateName != null) activeTemplateName else "Chưa thiết lập Mẫu Google Doc",
                                fontSize = TextSizes.body,
                                lineHeight = TextSizes.bodyLine,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                    ) {
                        Text(
                            text = if (activeTemplateName != null) "Chỉnh sửa" else "Tạo tự động",
                            fontSize = TextSizes.caption,
                            lineHeight = TextSizes.captionLine,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Thẻ trạng thái báo cáo hôm nay (nếu đã tổng hợp)
            if (dailyReport != null) {
                val isApproved = dailyReport.status == "APPROVED" || dailyReport.status == "EXPORTED"
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = Spacing.md),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isApproved) SiteVoiceTheme.colors.successContainer else MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = MaterialTheme.shapes.medium,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isApproved) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.lg),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            val badgeTitle = if (isApproved) "Đã duyệt nhật ký" else "Bản dự thảo sẵn sàng"
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                            ) {
                                Icon(
                                    imageVector = if (isApproved) Icons.Default.CheckCircle else Icons.Default.Description,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = if (isApproved) SiteVoiceTheme.colors.onSuccessContainer else MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = badgeTitle,
                                    fontSize = TextSizes.body,
                                    lineHeight = TextSizes.bodyLine,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isApproved) SiteVoiceTheme.colors.onSuccessContainer else MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Spacer(modifier = Modifier.height(Spacing.xs))
                            Text(
                                text = if (isApproved) "Nhật ký đã được duyệt 1 lần duy nhất." else "Bấm để xem và duyệt trước khi xuất Doc.",
                                fontSize = TextSizes.caption,
                                lineHeight = TextSizes.captionLine,
                                color = if (isApproved) SiteVoiceTheme.colors.success else MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.width(Spacing.sm))
                        Button(
                            onClick = { onViewReportClick?.invoke() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isApproved) SiteVoiceTheme.colors.success else MaterialTheme.colorScheme.primary
                            ),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text(text = "Xem", fontSize = TextSizes.caption, lineHeight = TextSizes.captionLine, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.xs))

            // Thẻ 1: Thời tiết trong ngày
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.large,
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(Spacing.lg)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(id = R.string.weather_title),
                            fontSize = TextSizes.subtitle,
                            lineHeight = TextSizes.subtitleLine,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "{{SANG_*}} • {{CHIEU_*}} • {{TOI_*}}",
                            fontSize = TextSizes.caption,
                            lineHeight = TextSizes.captionLine,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(Spacing.lg))

                    // Buổi Sáng
                    WeatherRow(
                        periodLabel = stringResource(id = R.string.weather_morning),
                        currentSelection = weatherMorning,
                        option1Key = "nang",
                        option1Icon = Icons.Default.WbSunny,
                        option1Label = "Trời nắng",
                        option2Key = "mua",
                        option2Icon = Icons.Default.WaterDrop,
                        option2Label = "Trời mưa",
                        onSelect = { weatherMorning = it }
                    )

                    Spacer(modifier = Modifier.height(Spacing.md))

                    // Buổi Chiều
                    WeatherRow(
                        periodLabel = stringResource(id = R.string.weather_afternoon),
                        currentSelection = weatherAfternoon,
                        option1Key = "nang",
                        option1Icon = Icons.Default.WbSunny,
                        option1Label = "Trời nắng",
                        option2Key = "mua",
                        option2Icon = Icons.Default.WaterDrop,
                        option2Label = "Trời mưa",
                        onSelect = { weatherAfternoon = it }
                    )

                    Spacer(modifier = Modifier.height(Spacing.md))

                    // Buổi Tối
                    WeatherRow(
                        periodLabel = stringResource(id = R.string.weather_evening),
                        currentSelection = weatherEvening,
                        option1Key = "binh_thuong",
                        option1Icon = Icons.Default.NightsStay,
                        option1Label = "Bình thường",
                        option2Key = "mua",
                        option2Icon = Icons.Default.WaterDrop,
                        option2Label = "Trời mưa",
                        onSelect = { weatherEvening = it }
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.lg))

            // Thẻ 2: Lực lượng thi công
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.large,
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(Spacing.lg)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(id = R.string.staff_title),
                            fontSize = TextSizes.subtitle,
                            lineHeight = TextSizes.subtitleLine,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "{{BAN_CHI_HUY}} • {{CONG_NHAN}}",
                            fontSize = TextSizes.caption,
                            lineHeight = TextSizes.captionLine,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(Spacing.md))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                    ) {
                        OutlinedTextField(
                            value = if (commandStaffCount > 0) commandStaffCount.toString() else "",
                            onValueChange = {
                                commandStaffCount = it.filter { ch -> ch.isDigit() }.toIntOrNull() ?: 0
                            },
                            label = { Text("Ban chỉ huy", fontSize = TextSizes.caption, lineHeight = TextSizes.captionLine) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = if (workerCount > 0) workerCount.toString() else "",
                            onValueChange = {
                                workerCount = it.filter { ch -> ch.isDigit() }.toIntOrNull() ?: 0
                            },
                            label = { Text("Công nhân", fontSize = TextSizes.caption, lineHeight = TextSizes.captionLine) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(Spacing.lg))

                    // Máy móc - Thiết bị
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(id = R.string.equipment_title),
                            fontSize = TextSizes.body,
                            lineHeight = TextSizes.bodyLine,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "{{MAY_MOC}}",
                            fontSize = TextSizes.caption,
                            lineHeight = TextSizes.captionLine,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(Spacing.sm))

                    OutlinedTextField(
                        value = equipmentList,
                        onValueChange = { equipmentList = it },
                        placeholder = {
                            Text(
                                text = stringResource(id = R.string.equipment_hint),
                                fontSize = TextSizes.caption,
                                lineHeight = TextSizes.captionLine,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(88.dp),
                        maxLines = 3
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.xl))

            // Nút Lưu thông tin cố định
            OutlinedButton(
                onClick = {
                    val entity = DailyInputEntity(
                        date = currentDate,
                        weatherMorning = weatherMorning,
                        weatherAfternoon = weatherAfternoon,
                        weatherEvening = weatherEvening,
                        commandStaffCount = commandStaffCount,
                        workerCount = workerCount,
                        equipmentList = equipmentList.trim()
                    )
                    onSaveDailyInput(entity)
                    isSavedNotification = true
                },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = stringResource(id = R.string.btn_save_daily_input),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(Spacing.lg))

            // Nút Lớn: Tổng hợp nhật ký hôm nay
            Button(
                onClick = {
                    // Tự động lưu trước khi bắt đầu tổng hợp
                    val entity = DailyInputEntity(
                        date = currentDate,
                        weatherMorning = weatherMorning,
                        weatherAfternoon = weatherAfternoon,
                        weatherEvening = weatherEvening,
                        commandStaffCount = commandStaffCount,
                        workerCount = workerCount,
                        equipmentList = equipmentList.trim()
                    )
                    onSaveDailyInput(entity)
                    onGenerateReportClick()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    Icon(
                        imageVector = if (dailyReport != null) Icons.Default.Refresh else Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                    Text(
                        text = if (dailyReport != null) {
                            stringResource(id = R.string.report_btn_resynthesize)
                        } else {
                            stringResource(id = R.string.btn_generate_report)
                        },
                        fontSize = TextSizes.subtitle,
                        lineHeight = TextSizes.subtitleLine,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.xxl))
        }
    }
}

@Composable
fun WeatherRow(
    periodLabel: String,
    currentSelection: String,
    option1Key: String,
    option1Icon: ImageVector,
    option1Label: String,
    option2Key: String,
    option2Icon: ImageVector,
    option2Label: String,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = periodLabel,
            fontSize = TextSizes.body,
            lineHeight = TextSizes.bodyLine,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(60.dp)
        )

        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            WeatherChip(
                icon = option1Icon,
                label = option1Label,
                isSelected = currentSelection == option1Key,
                onClick = { onSelect(option1Key) },
                modifier = Modifier.weight(1f)
            )

            WeatherChip(
                icon = option2Icon,
                label = option2Label,
                isSelected = currentSelection == option2Key,
                onClick = { onSelect(option2Key) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun WeatherChip(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.background
    val borderCol = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val textCol = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = modifier
            .background(bg, MaterialTheme.shapes.medium)
            .border(1.dp, borderCol, MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(vertical = Spacing.sm),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = textCol
            )
            Text(
                text = label,
                fontSize = TextSizes.caption,
                lineHeight = TextSizes.captionLine,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = textCol
            )
        }
    }
}
