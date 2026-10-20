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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sitevoice.app.R
import com.sitevoice.app.data.DailyInputEntity

@Composable
fun DailyInputScreen(
    currentDate: String,
    initialDailyInput: DailyInputEntity?,
    onSaveDailyInput: (DailyInputEntity) -> Unit,
    onGenerateReportClick: () -> Unit,
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
        color = Color(0xFFF8FAFC)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(id = R.string.tab_report),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Ngày: $currentDate",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B)
                    )
                }

                if (isSavedNotification) {
                    Text(
                        text = "✓ Đã lưu",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF16A34A)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Thẻ 1: Thời tiết trong ngày
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(id = R.string.weather_title),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Buổi Sáng
                    WeatherRow(
                        periodLabel = stringResource(id = R.string.weather_morning),
                        currentSelection = weatherMorning,
                        option1Key = "nang",
                        option1Label = "☀️ Trời nắng",
                        option2Key = "mua",
                        option2Label = "🌧️ Trời mưa",
                        onSelect = { weatherMorning = it }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Buổi Chiều
                    WeatherRow(
                        periodLabel = stringResource(id = R.string.weather_afternoon),
                        currentSelection = weatherAfternoon,
                        option1Key = "nang",
                        option1Label = "☀️ Trời nắng",
                        option2Key = "mua",
                        option2Label = "🌧️ Trời mưa",
                        onSelect = { weatherAfternoon = it }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Buổi Tối
                    WeatherRow(
                        periodLabel = stringResource(id = R.string.weather_evening),
                        currentSelection = weatherEvening,
                        option1Key = "binh_thuong",
                        option1Label = "🌙 Bình thường",
                        option2Key = "mua",
                        option2Label = "🌧️ Trời mưa",
                        onSelect = { weatherEvening = it }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Thẻ 2: Lực lượng thi công
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(id = R.string.staff_title),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )

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
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = if (workerCount > 0) workerCount.toString() else "",
                            onValueChange = {
                                workerCount = it.filter { ch -> ch.isDigit() }.toIntOrNull() ?: 0
                            },
                            label = { Text("Công nhân", fontSize = 12.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Máy móc - Thiết bị
                    Text(
                        text = stringResource(id = R.string.equipment_title),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF334155)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = equipmentList,
                        onValueChange = { equipmentList = it },
                        placeholder = {
                            Text(
                                text = stringResource(id = R.string.equipment_hint),
                                fontSize = 12.sp,
                                color = Color(0xFF94A3B8)
                            )
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(88.dp),
                        maxLines = 3
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

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
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.btn_save_daily_input),
                    color = Color(0xFF1E293B),
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

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
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
            ) {
                Text(
                    text = "⚡ " + stringResource(id = R.string.btn_generate_report),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
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
            fontWeight = FontWeight.Medium,
            color = Color(0xFF475569),
            modifier = Modifier.width(60.dp)
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
    val bg = if (isSelected) Color(0xFFEFF6FF) else Color(0xFFF8FAFC)
    val borderCol = if (isSelected) Color(0xFF2563EB) else Color(0xFFE2E8F0)
    val textCol = if (isSelected) Color(0xFF1D4ED8) else Color(0xFF64748B)

    Box(
        modifier = modifier
            .background(bg, RoundedCornerShape(8.dp))
            .border(1.dp, borderCol, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = textCol
        )
    }
}
