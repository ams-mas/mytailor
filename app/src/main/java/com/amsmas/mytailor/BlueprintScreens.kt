package com.amsmas.mytailor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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

private val BlueprintPayTypes = listOf("daily", "weekly", "monthly", "piece_rate")
private fun payTypeLabel(type: String): String = when (type) {
    "daily" -> "روزانه"
    "weekly" -> "هفتگی"
    "monthly" -> "ماهانه"
    else -> "به‌ازای هر مرحله"
}
private fun blueprintMoney(value: Double): String = String.format(java.util.Locale.US, "%,.0f", value)

@Composable
internal fun WorkerManagementScreen(database: LocalDatabase, onBack: () -> Unit) {
    val revision = database.revision.intValue
    val workers = remember(revision) { database.getWorkers(includeInactive = true) }
    var editingId by remember { mutableIntStateOf(0) }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var payType by remember { mutableStateOf("piece_rate") }
    var payRate by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text("مدیریت کارمندان", color = BurgundyDark, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                Text("ثبت خیاط و تعیین روش معاش", color = TextMuted, fontSize = 12.sp)
            }
            TextButton(onClick = onBack) { Text("برگشت", color = Burgundy) }
        }
        Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(18.dp)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text(if (editingId > 0) "ویرایش کارمند" else "کارمند جدید", color = BurgundyDark, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            OutlinedTextField(name, { name = it; message = "" }, Modifier.fillMaxWidth(), label = { Text("نام کارمند *") }, singleLine = true, colors = appFieldColors(), shape = RoundedCornerShape(12.dp))
            OutlinedTextField(phone, { phone = it }, Modifier.fillMaxWidth(), label = { Text("شماره تماس") }, singleLine = true, colors = appFieldColors(), shape = RoundedCornerShape(12.dp))
            OutlinedTextField(address, { address = it }, Modifier.fillMaxWidth(), label = { Text("آدرس") }, singleLine = true, colors = appFieldColors(), shape = RoundedCornerShape(12.dp))
            Text("روش پرداخت معاش", color = TextDark, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            BlueprintPayTypes.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { type ->
                        Column(Modifier.weight(1f).background(if (payType == type) Burgundy else BurgundySoft, RoundedCornerShape(10.dp)).clickable { payType = type }.padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(payTypeLabel(type), color = if (payType == type) Color.White else BurgundyDark, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            OutlinedTextField(payRate, { payRate = it }, Modifier.fillMaxWidth(), label = { Text("مبلغ معاش / نرخ هر مرحله (افغانی)") }, singleLine = true, colors = appFieldColors(), shape = RoundedCornerShape(12.dp))
            Text(when (payType) {
                "daily" -> "محاسبه بر اساس روزهای حاضری ثبت‌شده انجام می‌شود."
                "weekly" -> "نرخ برای هر هفته در بازه انتخاب‌شده محاسبه می‌شود."
                "monthly" -> "نرخ برای هر ماه در بازه انتخاب‌شده محاسبه می‌شود."
                else -> "نرخ در تعداد مراحل تولید تکمیل‌شده ضرب می‌شود."
            }, color = TextMuted, fontSize = 11.sp)
            Button(onClick = {
                val rate = payRate.toEnglishDigits().replace(",", "").toDoubleOrNull()
                when {
                    name.isBlank() -> message = "نام کارمند را وارد کنید."
                    rate == null || rate < 0 -> message = "مبلغ معتبر وارد کنید."
                    else -> {
                        database.saveWorker(WorkerRecord(editingId, name.trim(), phone.trim(), address.trim(), payType, rate, true))
                        editingId = 0; name = ""; phone = ""; address = ""; payRate = ""; payType = "piece_rate"
                        message = "معلومات کارمند ذخیره شد."
                    }
                }
            }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Burgundy), shape = RoundedCornerShape(12.dp)) { Text("ذخیره کارمند") }
            if (message.isNotBlank()) Text(message, color = BurgundyDark, fontSize = 12.sp)
            if (editingId > 0) TextButton(onClick = { editingId = 0; name = ""; phone = ""; address = ""; payRate = ""; payType = "piece_rate"; message = "" }) { Text("لغو ویرایش", color = TextMuted) }
        }
        Text("فهرست کارمندان (${workers.size})", color = BurgundyDark, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        if (workers.isEmpty()) Text("هنوز کارمندی ثبت نشده است.", color = TextMuted)
        workers.forEach { worker ->
            Row(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(14.dp)).padding(13.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(worker.name, color = TextDark, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(if (worker.phone.isBlank()) "بدون شماره تماس" else worker.phone, color = TextMuted, fontSize = 12.sp)
                    Text("${payTypeLabel(worker.payType)} · ${blueprintMoney(worker.payRate)} افغانی", color = BurgundyDark, fontSize = 12.sp)
                    if (!worker.active) Text("غیرفعال", color = TextMuted, fontSize = 11.sp)
                }
                TextButton(onClick = { editingId = worker.id; name = worker.name; phone = worker.phone; address = worker.address; payType = worker.payType; payRate = worker.payRate.toString(); message = "" }) { Text("ویرایش", color = Burgundy) }
            }
        }
    }
}

@Composable
internal fun WorkflowSettingsScreen(database: LocalDatabase, onBack: () -> Unit) {
    val revision = database.revision.intValue
    val steps = remember(revision) { database.getWorkflowSteps() }
    var stepName by remember { mutableStateOf("") }
    var stepOrder by remember { mutableStateOf((steps.size + 1).toString()) }
    var message by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text("مراحل تولید سفارش", color = BurgundyDark, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                Text("ترتیب مراحل کاری کارگاه را تنظیم کنید", color = TextMuted, fontSize = 12.sp)
            }
            TextButton(onClick = onBack) { Text("برگشت", color = Burgundy) }
        }
        Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(16.dp)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            OutlinedTextField(stepName, { stepName = it; message = "" }, Modifier.fillMaxWidth(), label = { Text("نام مرحله (برش، دوخت، اتو)") }, singleLine = true, colors = appFieldColors(), shape = RoundedCornerShape(12.dp))
            OutlinedTextField(stepOrder, { stepOrder = it.toEnglishDigits() }, Modifier.fillMaxWidth(), label = { Text("شماره ترتیب") }, singleLine = true, colors = appFieldColors(), shape = RoundedCornerShape(12.dp))
            Button(onClick = {
                val order = stepOrder.toIntOrNull()
                if (stepName.isBlank() || order == null || order < 1) message = "نام مرحله و شماره ترتیب معتبر وارد کنید."
                else {
                    database.saveWorkflowStep(stepName, order)
                    stepName = ""; stepOrder = (database.getWorkflowSteps().size + 1).toString(); message = "مرحله ذخیره شد."
                }
            }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Burgundy), shape = RoundedCornerShape(12.dp)) { Text("افزودن / ذخیره مرحله") }
            if (message.isNotBlank()) Text(message, color = BurgundyDark, fontSize = 12.sp)
        }
        Text("ترتیب مراحل", color = BurgundyDark, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        steps.forEach { step ->
            Row(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(12.dp)).padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(step.stepOrder.toString(), color = Burgundy, fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.width(32.dp))
                Text(step.name, color = TextDark, fontSize = 14.sp, modifier = Modifier.weight(1f))
                TextButton(onClick = { stepName = step.name; stepOrder = step.stepOrder.toString() }) { Text("ویرایش", color = Burgundy) }
            }
        }
        Text("تغییر مراحل پیش‌فرض روی سفارش‌های تازه اعمال می‌شود؛ مراحل سفارش‌های قبلی حفظ می‌شوند.", color = TextMuted, fontSize = 11.sp)
    }
}

@Composable
internal fun ProductionWorkflowScreen(database: LocalDatabase, onBack: () -> Unit) {
    val revision = database.revision.intValue
    val orders = remember(revision) { database.getActiveOrders() }
    val workers = remember(revision) { database.getWorkers() }
    var selectedOrder by remember { mutableStateOf<DeliveryOrder?>(null) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text("مراحل اجرای سفارش", color = BurgundyDark, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                Text("کارمند هر مرحله را تعیین و پیشرفت را ثبت کنید", color = TextMuted, fontSize = 12.sp)
            }
            TextButton(onClick = { if (selectedOrder != null) selectedOrder = null else onBack() }) { Text(if (selectedOrder != null) "فهرست" else "برگشت", color = Burgundy) }
        }
        if (selectedOrder == null) {
            if (orders.isEmpty()) Text("سفارش فعال برای پیگیری وجود ندارد.", color = TextMuted)
            orders.forEach { order ->
                Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(14.dp)).clickable { selectedOrder = order }.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(order.customerName, color = BurgundyDark, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("${order.garmentModel} · سفارش #${order.id}", color = TextDark, fontSize = 12.sp)
                    Text("تحویل: ${order.deliveryDate}", color = TextMuted, fontSize = 11.sp)
                    Text("برای مدیریت مراحل لمس کنید  ›", color = Burgundy, fontSize = 12.sp)
                }
            }
        } else {
            val order = selectedOrder!!
            val workflow = remember(order.id, revision) { database.getOrderWorkflow(order.id) }
            Text("${order.customerName} — ${order.garmentModel}", color = BurgundyDark, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            if (workflow.isEmpty()) Text("برای این سفارش مرحله‌ای ثبت نشده است.", color = TextMuted)
            workflow.forEach { step ->
                var menuExpanded by remember(step.assignmentId) { mutableStateOf(false) }
                Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(14.dp)).padding(13.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(step.stepName, color = TextDark, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text(when (step.status) { "done" -> "تکمیل‌شده"; "in_progress" -> "در حال انجام"; else -> "در انتظار" }, color = if (step.status == "done") Color(0xFF2E7D32) else Burgundy, fontSize = 12.sp)
                        }
                        Box {
                            TextButton(onClick = { menuExpanded = true }) { Text(if (step.workerName.isBlank()) "تعیین کارمند ▾" else "${step.workerName} ▾", color = Burgundy, fontSize = 12.sp) }
                            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                                DropdownMenuItem(text = { Text("بدون مسئول") }, onClick = { database.assignWorkflowStep(step.assignmentId, null); menuExpanded = false })
                                workers.forEach { worker ->
                                    DropdownMenuItem(text = { Text(worker.name) }, onClick = { database.assignWorkflowStep(step.assignmentId, worker.id); menuExpanded = false })
                                }
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("pending" to "در انتظار", "in_progress" to "شروع", "done" to "تکمیل").forEach { (status, label) ->
                            Button(onClick = { database.updateWorkflowStepStatus(step.assignmentId, status) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = if (step.status == status) BurgundyDark else BurgundySoft, contentColor = if (step.status == status) Color.White else BurgundyDark), shape = RoundedCornerShape(10.dp), contentPadding = PaddingValues(horizontal = 4.dp, vertical = 7.dp)) { Text(label, fontSize = 11.sp) }
                        }
                    }
                    if (step.status == "done" && step.completedAt.isNotBlank()) Text("تاریخ تکمیل: ${step.completedAt}", color = TextMuted, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
internal fun WorkerPayrollScreen(database: LocalDatabase, onBack: () -> Unit) {
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(java.util.Date()) }
    val monthStart = remember {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)
    }
    var startDate by remember { mutableStateOf(monthStart) }
    var endDate by remember { mutableStateOf(today) }
    var attendanceDate by remember { mutableStateOf(today) }
    var message by remember { mutableStateOf("") }
    val revision = database.revision.intValue
    val payroll = remember(revision, startDate, endDate) { database.getWorkerPayroll(startDate, endDate) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text("معاش و پرداخت کارمندان", color = BurgundyDark, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("محاسبه بر اساس روش معاش هر کارمند", color = TextMuted, fontSize = 12.sp)
            }
            TextButton(onClick = onBack) { Text("برگشت", color = Burgundy) }
        }
        Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(16.dp)).padding(13.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(startDate, { startDate = it }, Modifier.fillMaxWidth(), label = { Text("از تاریخ (YYYY-MM-DD)") }, singleLine = true, colors = appFieldColors(), shape = RoundedCornerShape(12.dp))
            OutlinedTextField(endDate, { endDate = it }, Modifier.fillMaxWidth(), label = { Text("تا تاریخ (YYYY-MM-DD)") }, singleLine = true, colors = appFieldColors(), shape = RoundedCornerShape(12.dp))
            OutlinedTextField(attendanceDate, { attendanceDate = it }, modifier = Modifier.fillMaxWidth(), label = { Text("تاریخ حاضری (YYYY-MM-DD)") }, singleLine = true, colors = appFieldColors(), shape = RoundedCornerShape(12.dp))
            Text("برای کارمند روزانه، تاریخ حاضری را وارد و ثبت کنید.", color = TextMuted, fontSize = 11.sp)
        }
        if (message.isNotBlank()) Text(message, color = BurgundyDark, fontSize = 12.sp)
        if (payroll.isEmpty()) Text("ابتدا از تنظیمات کارمندان را ثبت کنید.", color = TextMuted)
        payroll.forEach { row ->
            Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(15.dp)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(row.name, color = BurgundyDark, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("${payTypeLabel(row.payType)} · نرخ ${blueprintMoney(row.payRate)} افغانی", color = TextMuted, fontSize = 12.sp)
                Text("واحد محاسبه: ${row.units} · استحقاق: ${blueprintMoney(row.earned)} افغانی", color = TextDark, fontSize = 13.sp)
                Text("پرداخت‌شده: ${blueprintMoney(row.paid)} افغانی", color = TextMuted, fontSize = 12.sp)
                Text("باقی معاش: ${blueprintMoney(row.due)} افغانی", color = BurgundyDark, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (row.payType == "daily") {
                        Button(onClick = {
                            database.recordWorkerAttendance(row.workerId, attendanceDate)
                            message = "حاضری ${attendanceDate} برای ${row.name} ثبت شد."
                        }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = BurgundySoft, contentColor = BurgundyDark), shape = RoundedCornerShape(10.dp), contentPadding = PaddingValues(4.dp)) { Text("ثبت حاضری امروز", fontSize = 11.sp) }
                    }
                    Button(onClick = {
                        if (row.due > 0) {
                            database.markWorkerPaid(row.workerId, row.due, startDate, endDate, "پرداخت معاش بازه انتخاب‌شده")
                            message = "پرداخت معاش ${row.name} ثبت شد."
                        } else message = "برای این بازه معاش باقی نمانده است."
                    }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Burgundy, contentColor = Color.White), shape = RoundedCornerShape(10.dp), contentPadding = PaddingValues(4.dp)) { Text("علامت‌گذاری پرداخت", fontSize = 11.sp) }
                }
            }
        }
    }
}
