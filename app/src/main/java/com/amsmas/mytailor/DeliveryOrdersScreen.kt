package com.amsmas.mytailor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private enum class DeliveryView { DAY, WEEK, CALENDAR }

@Composable
internal fun DeliveryOrdersScreen(onAddOrder: () -> Unit) {
    var view by remember { mutableStateOf(DeliveryView.DAY) }
    var selectedOffset by remember { mutableIntStateOf(0) }
    val selectedCalendar = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, selectedOffset) }
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd", Locale("fa", "AF")) }
    val weekdays = listOf("شنبه", "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنجشنبه", "جمعه")
    val today = Calendar.getInstance()
    val weekStart = (today.clone() as Calendar).apply {
        val day = get(Calendar.DAY_OF_WEEK)
        val daysFromSaturday = day % 7
        add(Calendar.DAY_OF_YEAR, -daysFromSaturday)
    }

    Column(Modifier.fillMaxSize().background(Color(0xFFFAF7F4)).verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("تحویل سفارش‌ها", color = Color(0xFF55191E), fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("برنامه‌ریزی تحویل لباس‌ها بر اساس روز و تاریخ", color = Color(0xFF7D7070), fontSize = 13.sp, modifier = Modifier.padding(top = 5.dp, bottom = 16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            DeliverySummaryCard("امروز", "۰", Color(0xFF702027), Modifier.weight(1f))
            DeliverySummaryCard("فردا", "۰", Color(0xFFB7791F), Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            DeliverySummaryCard("این هفته", "۰", Color(0xFF2870D8), Modifier.weight(1f))
            DeliverySummaryCard("عقب‌افتاده", "۰", Color(0xFFD34A4A), Modifier.weight(1f))
        }

        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            DeliveryView.entries.forEach { option ->
                val label = when (option) {
                    DeliveryView.DAY -> "روزانه"
                    DeliveryView.WEEK -> "هفتگی"
                    DeliveryView.CALENDAR -> "انتخاب تاریخ"
                }
                if (view == option) {
                    Button(onClick = { view = option }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF702027))) { Text(label, fontSize = 12.sp) }
                } else {
                    OutlinedButton(onClick = { view = option }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)) { Text(label, fontSize = 12.sp, color = Color(0xFF702027)) }
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        when (view) {
            DeliveryView.DAY -> {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(dateFormat.format(selectedCalendar.time), color = Color(0xFF302627), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = { selectedOffset-- }, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) { Text("روز قبل") }
                        OutlinedButton(onClick = { selectedOffset++ }, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) { Text("روز بعد") }
                    }
                }
                Spacer(Modifier.height(12.dp))
                DeliveryEmptyState(onAddOrder)
            }
            DeliveryView.WEEK -> {
                Text("برنامه این هفته", color = Color(0xFF302627), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(8.dp))
                repeat(7) { index ->
                    val day = (weekStart.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, index) }
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).background(Color.White, RoundedCornerShape(12.dp)).clickable { selectedOffset = ((day.timeInMillis - today.timeInMillis) / 86400000L).toInt(); view = DeliveryView.DAY }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(weekdays[index], color = Color(0xFF302627), fontWeight = FontWeight.SemiBold)
                            Text(dateFormat.format(day.time), color = Color(0xFF7D7070), fontSize = 12.sp)
                        }
                        Text("۰ سفارش", color = Color(0xFF702027), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
                Text("با انتخاب هر روز، فهرست سفارش‌های موعددار همان روز باز می‌شود.", color = Color(0xFF7D7070), fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
            }
            DeliveryView.CALENDAR -> {
                Text("انتخاب تاریخ", color = Color(0xFF302627), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { selectedOffset -= 7 }, modifier = Modifier.weight(1f)) { Text("هفته قبل") }
                    Text(dateFormat.format(selectedCalendar.time), modifier = Modifier.weight(1.5f), textAlign = TextAlign.Center, fontSize = 13.sp)
                    OutlinedButton(onClick = { selectedOffset += 7 }, modifier = Modifier.weight(1f)) { Text("هفته بعد") }
                }
                Spacer(Modifier.height(12.dp))
                DeliveryEmptyState(onAddOrder)
            }
        }
        Spacer(Modifier.height(18.dp))
        Text("نکته: تعدادها پس از ثبت سفارش و تعیین تاریخ تحویل، به‌صورت خودکار محاسبه می‌شوند.", color = Color(0xFF7D7070), fontSize = 12.sp)
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun DeliverySummaryCard(title: String, count: String, accent: Color, modifier: Modifier = Modifier) {
    Column(modifier.background(Color.White, RoundedCornerShape(14.dp)).padding(14.dp)) {
        Text(title, color = Color(0xFF7D7070), fontSize = 12.sp)
        Spacer(Modifier.height(5.dp))
        Text(count, color = accent, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
        Text("سفارش", color = Color(0xFF7D7070), fontSize = 11.sp)
    }
}

@Composable
private fun DeliveryEmptyState(onAddOrder: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(14.dp)).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("▦", color = Color(0xFF702027), fontSize = 34.sp)
        Spacer(Modifier.height(8.dp))
        Text("برای این تاریخ سفارشی ثبت نشده است", color = Color(0xFF302627), fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        Text("با ثبت سفارش و تعیین تاریخ تحویل، سفارش در این بخش نمایش داده می‌شود.", color = Color(0xFF7D7070), fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp, bottom = 14.dp))
        Button(onClick = onAddOrder, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF702027))) { Text("ثبت سفارش جدید") }
    }
}
