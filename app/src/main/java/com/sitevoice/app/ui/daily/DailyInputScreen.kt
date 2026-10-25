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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sitevoice.app.R
import com.sitevoice.app.data.DailyInputEntity
import com.sitevoice.app.data.DailyReportEntity

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
        color = Color(0xFFF3EFE6)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Top Architectural Ledger Banner
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF14171A))
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = "TỔNG HỢP CUỐI NGÀY • $currentDate",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            color = Color(0xFFD84315)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(id = R.string.tab_report),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFAF8F3)
                        )
                    }

                    if (isSavedNotification) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF1B4332), RoundedCornerShape(4.dp))
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "ĐÃ LƯU",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF74C69D)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Active Template Strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF21262B), RoundedCornerShape(4.dp))
                        .clickable { onOpenTemplateLibrary() }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "MẪU BÁO CÁO ÁP DỤNG",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF949A9E)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = activeTemplateName ?: "Nhật ký thi công hằng ngày",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFAF8F3)
                        )
                    }

                    Text(
                        text = "QUẢN LÝ MẪU →",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD84315)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Report Draft/Approved Banner
                if (dailyReport != null) {
                    val isApproved = dailyReport.status == "APPROVED" || dailyReport.status == "EXPORTED"
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isApproved) Color(0xFF1B4332) else Color(0xFF14171A)
                        ),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isApproved) "ĐÃ PHÊ DUYỆT NHẬT KÝ" else "BẢN DỰ THẢO ĐÃ SẴN SÀNG",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp,
                                    color = if (isApproved) Color(0xFF74C69D) else Color(0xFFD84315)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isApproved)
                                        "Nhật ký đã được duyệt và sẵn sàng xuất Google Doc."
                                    else
                                        "Mở bản xem trước để kiểm tra và xuất Google Doc.",
                                    fontSize = 13.sp,
                                    color = Color(0xFFFAF8F3)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Button(
                                onClick = { onViewReportClick?.invoke() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD84315)),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "MỞ XEM →",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                // Panel 1: Weather
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFAF8F3)),
                    shape = RoundedCornerShape(4.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD6CFC2))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(id = R.string.weather_title).uppercase(),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.6.sp,
                                color = Color(0xFF14171A)
                            )
                            Text(
                                text = "{{SANG_*}} • {{CHIEU_*}} • {{TOI_*}}",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF5C6268)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        WeatherRow(
                            periodLabel = stringResource(id = R.string.weather_morning),
                            currentSelection = weatherMorning,
                            option1Key = "nang",
                            option1Label = "TRỜI NẮNG",
                            option2Key = "mua",
                            option2Label = "TRỜI MƯA",
                            onSelect = { weatherMorning = it }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        WeatherRow(
                            periodLabel = stringResource(id = R.string.weather_afternoon),
                            currentSelection = weatherAfternoon,
                            option1Key = "nang",
                            option1Label = "TRỜI NẮNG",
                            option2Key = "mua",
                            option2Label = "TRỜI MƯA",
                            onSelect = { weatherAfternoon = it }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        WeatherRow(
                            periodLabel = stringResource(id = R.string.weather_evening),
                            currentSelection = weatherEvening,
                            option1Key = "binh_thuong",
                            option1Label = "BÌNH THƯỜNG",
                            option2Key = "mua",
                            option2Label = "TRỜI MƯA",
                            onSelect = { weatherEvening = it }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Panel 2: Workforce & Equipment
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFAF8F3)),
                    shape = RoundedCornerShape(4.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD6CFC2))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(id = R.string.staff_title).uppercase(),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.6.sp,
                                color = Color(0xFF14171A)
                            )
                            Text(
                                text = "{{BAN_CHI_HUY}} • {{CONG_NHAN}}",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF5C6268)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = if (commandStaffCount > 0) commandStaffCount.toString() else "",
                                onValueChange = {
                                    commandStaffCount = it.filter { ch -> ch.isDigit() }.toIntOrNull() ?: 0
                                },
                                label = { Text("Ban chỉ huy", fontSize = 12.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.weight(1f)
                            )

                            OutlinedTextField(
                                value = if (workerCount > 0) workerCount.toString() else "",
                                onValueChange = {
                                    workerCount = it.filter { ch -> ch.isDigit() }.toIntOrNull() ?: 0
                                },
                                label = { Text("Công nhân", fontSize = 12.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(id = R.string.equipment_title).uppercase(),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF14171A)
                            )
                            Text(
                                text = "{{MAY_MOC}}",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD84315)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = equipmentList,
                            onValueChange = { equipmentList = it },
                            placeholder = {
                                Text(
                                    text = stringResource(id = R.string.equipment_hint),
                                    fontSize = 12.sp,
                                    color = Color(0xFF5C6268)
                                )
                            },
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(88.dp),
                            maxLines = 3
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

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
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.btn_save_daily_input).uppercase(),
                        color = Color(0xFF14171A),
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
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
                        onGenerateReportClick()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD84315))
                ) {
                    val btnText = if (dailyReport != null) {
                        stringResource(id = R.string.report_btn_resynthesize).uppercase()
                    } else {
                        stringResource(id = R.string.btn_generate_report).uppercase()
                    }
                    Text(
                        text = btnText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.8.sp,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun WeatherRow(
    periodLabel: String,
    currentSelection: String,
    option1Key: String,
    option1Label: String,
    option2Key: String,
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
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF14171A),
            modifier = Modifier.width(64.dp)
        )

        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            WeatherChip(
                label = option1Label,
                isSelected = currentSelection == option1Key,
                onClick = { onSelect(option1Key) },
                modifier = Modifier.weight(1f)
            )

            WeatherChip(
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
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (isSelected) Color(0xFF14171A) else Color(0xFFEBE5D8)
    val borderCol = if (isSelected) Color(0xFF14171A) else Color(0xFFD6CFC2)
    val textCol = if (isSelected) Color(0xFFFAF8F3) else Color(0xFF5C6268)

    Box(
        modifier = modifier
            .background(bg, RoundedCornerShape(4.dp))
            .border(1.dp, borderCol, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            color = textCol
        )
    }
}
