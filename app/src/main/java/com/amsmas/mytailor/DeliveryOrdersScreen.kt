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
internal fun DeliveryOrdersScreen(database: LocalDatabase, onAddOrder: () -> Unit) {
    var search by remember { mutableStateOf("") }
    var refreshKey by remember { mutableIntStateOf(0) }
    var selectedOrder by remember { mutableStateOf<DeliveryOrder?>(null) }
    var paymentAmount by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    val orders = remember(search, refreshKey) { database.getReadyForDeliveryOrders(search) }

    Column(Modifier.fillMaxSize().background(Color(0xFFFAF7F4)).padding(16.dp)) {
        Text("تحویل سفارش‌ها", color = Color(0xFF55191E), fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("سفارش‌های تکمیل‌شده و آماده تحویل", color = Color(0xFF7D7070), fontSize = 13.sp, modifier = Modifier.padding(top = 5.dp, bottom = 12.dp))
        androidx.compose.material3.OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("جستجو با نام یا شماره تماس مشتری") },
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF702027),
                unfocusedBorderColor = Color(0xFFD9C6C5),
                focusedLabelColor = Color(0xFF702027),
                cursorColor = Color(0xFF702027)
            ),
            shape = RoundedCornerShape(16.dp)
        )
        Spacer(Modifier.height(12.dp))
        if (orders.isEmpty()) {
            Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(14.dp)).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (search.isBlank()) "سفارش آماده تحویل وجود ندارد." else "مشتری یا سفارش پیدا نشد.", color = Color(0xFF302627), fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                Text("ابتدا وضعیت سفارش را در بخش مشتریان «آماده» کنید.", color = Color(0xFF7D7070), fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
            }
        } else {
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
                orders.forEach { order ->
                    Column(Modifier.fillMaxWidth().padding(bottom = 10.dp).background(Color.White, RoundedCornerShape(14.dp)).padding(14.dp)) {
                        Text(order.customerName, color = Color(0xFF55191E), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("تماس: ${order.phone}", color = Color(0xFF7D7070), fontSize = 12.sp, modifier = Modifier.padding(top = 3.dp))
                        Text("مدل لباس: ${order.garmentModel}", color = Color(0xFF302627), fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
                        Text("قیمت: ${order.price.toLong()} • پرداخت‌شده: ${order.paid.toLong()}", color = Color(0xFF302627), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                        Text("باقی‌مانده: ${order.remaining.toLong()}", color = if (order.remaining > 0) Color(0xFFB3261E) else Color(0xFF237342), fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                        Button(
                            onClick = {
                                if (order.remaining <= 0.0001) {
                                    val delivered = database.collectRemainingAndDeliver(order.id, 0.0)
                                    if (delivered) { message = "سفارش تحویل شد."; refreshKey++ } else message = "وضعیت سفارش تغییر کرده است؛ فهرست را بررسی کنید."
                                } else {
                                    selectedOrder = order
                                    paymentAmount = order.remaining.toLong().toString()
                                    message = ""
                                }
                            },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF702027)),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text(if (order.remaining > 0.0001) "دریافت باقی‌مانده و تحویل" else "تحویل سفارش") }
                    }
                }
            }
        }
        if (message.isNotBlank()) Text(message, color = Color(0xFF237342), fontSize = 12.sp, modifier = Modifier.padding(vertical = 6.dp))
    }

    if (selectedOrder != null) {
        val order = selectedOrder!!
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { selectedOrder = null },
            title = { Text("دریافت باقی‌مانده پول") },
            text = {
                Column {
                    Text("مشتری: ${order.customerName}")
                    Text("باقی‌مانده قابل پرداخت: ${order.remaining.toLong()}")
                    androidx.compose.material3.OutlinedTextField(
                        value = paymentAmount,
                        onValueChange = { paymentAmount = it.filter(Char::isDigit) },
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                        singleLine = true,
                        label = { Text("مبلغ دریافتی") },
                        shape = RoundedCornerShape(16.dp)
                    )
                    Text("برای تحویل، تمام باقی‌مانده باید پرداخت شود.", color = Color(0xFFB3261E), fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    val entered = paymentAmount.toDoubleOrNull()
                    if (entered == null || entered < order.remaining) {
                        message = "مبلغ باید برابر یا بیشتر از باقی‌مانده باشد."
                    } else {
                        val success = database.collectRemainingAndDeliver(order.id, order.remaining)
                        if (success) {
                            message = "پول دریافت شد و سفارش تحویل گردید."
                            refreshKey++
                            selectedOrder = null
                        } else {
                            message = "ثبت پرداخت ناموفق بود؛ وضعیت سفارش را بررسی کنید."
                            selectedOrder = null
                        }
                    }
                }) { Text("ثبت دریافت و تحویل", color = Color(0xFF702027)) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { selectedOrder = null }) { Text("انصراف") }
            }
        )
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
