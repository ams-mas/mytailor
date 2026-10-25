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
import androidx.compose.ui.graphics.Brush
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
private fun validIsoDate(value: String): Boolean = runCatching {
    val f = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
    f.parse(value) != null && f.format(f.parse(value)!!) == value
}.getOrDefault(false)

@Composable
internal fun WorkerManagementScreen(database: LocalDatabase, onBack: () -> Unit) {
    val revision = database.revision.intValue
    var editingId by remember { mutableIntStateOf(0) }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var payType by remember { mutableStateOf("piece_rate") }
    var payRate by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(java.util.Date())) }
    var endDate by remember { mutableStateOf("") }
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
            if (payType == "daily") {
                Text("قرارداد کارمند روزانه", color = BurgundyDark, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                JalaliDateInput(startDate, { startDate = it }, "تاریخ شروع کار")
                JalaliDateInput(endDate, { endDate = it }, "تاریخ ختم کار (اختیاری)", allowBlank = true)
            }
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
                    payType == "daily" && !validIsoDate(startDate) -> message = "تاریخ شروع را به شکل YYYY-MM-DD وارد کنید."
                    payType == "daily" && endDate.isNotBlank() && (!validIsoDate(endDate) || endDate < startDate) -> message = "تاریخ ختم معتبر و مساوی یا بعد از تاریخ شروع باشد."
                    else -> {
                        database.saveWorker(WorkerRecord(editingId, name.trim(), phone.trim(), address.trim(), payType, rate, true, if (payType == "daily") startDate else "", if (payType == "daily") endDate else ""))
                        editingId = 0; name = ""; phone = ""; address = ""; payRate = ""; payType = "piece_rate"
                        startDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(java.util.Date()); endDate = ""
                        message = "معلومات کارمند ذخیره شد."
                    }
                }
            }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Burgundy), shape = RoundedCornerShape(12.dp)) { Text("ذخیره کارمند") }
            if (message.isNotBlank()) Text(message, color = BurgundyDark, fontSize = 12.sp)
            if (editingId > 0) TextButton(onClick = { editingId = 0; name = ""; phone = ""; address = ""; payRate = ""; payType = "piece_rate"; startDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(java.util.Date()); endDate = ""; message = "" }) { Text("لغو ویرایش", color = TextMuted) }
        }
    }
}

@Composable
internal fun WorkflowSettingsScreen(database: LocalDatabase, onBack: () -> Unit) {
    val revision = database.revision.intValue
    val steps = remember(revision) { database.getWorkflowSteps() }
    var stepName by remember { mutableStateOf("") }
    var stepOrder by remember { mutableStateOf((steps.size + 1).toString()) }
    var editingStepId by remember { mutableIntStateOf(0) }
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
                    database.saveWorkflowStep(stepName, order, editingStepId)
                    editingStepId = 0; stepName = ""; stepOrder = (database.getWorkflowSteps().size + 1).toString(); message = "مرحله ذخیره شد."
                }
            }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Burgundy), shape = RoundedCornerShape(12.dp)) { Text("افزودن / ذخیره مرحله") }
            if (message.isNotBlank()) Text(message, color = BurgundyDark, fontSize = 12.sp)
        }
        Text("ترتیب مراحل", color = BurgundyDark, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        steps.forEach { step ->
            Row(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(12.dp)).padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(step.stepOrder.toString(), color = Burgundy, fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.width(32.dp))
                Text(step.name, color = TextDark, fontSize = 14.sp, modifier = Modifier.weight(1f))
                TextButton(onClick = { editingStepId = step.id; stepName = step.name; stepOrder = step.stepOrder.toString() }) { Text("ویرایش", color = Burgundy) }
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
                    Text("تحویل: ${displayShopDate(order.deliveryDate)}", color = TextMuted, fontSize = 11.sp)
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
    val monthStart = remember { currentJalaliMonthStartIso() }
    var startDate by remember { mutableStateOf(monthStart) }
    var endDate by remember { mutableStateOf(today) }
    var workerType by remember { mutableStateOf("monthly") }
    var paymentAmounts by remember { mutableStateOf(mapOf<Int, String>()) }
    var message by remember { mutableStateOf("") }
    val revision = database.revision.intValue
    val allPayroll = remember(revision, startDate, endDate) { database.getWorkerPayroll(startDate, endDate) }
    val payroll = allPayroll.filter { it.payType == workerType }
    val monthlyRows = allPayroll.filter { it.payType == "monthly" }
    val dailyRows = allPayroll.filter { it.payType == "daily" }
    val monthlyDue = monthlyRows.sumOf { it.due }
    val dailyDue = dailyRows.sumOf { it.due }
    val paidTotal = payroll.sumOf { it.paid }
    val allDue = monthlyDue + dailyDue
    val money = { amount: Double -> String.format(Locale.US, "%,.0f", amount) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text("معاش و پرداخت کارمندان", color = BurgundyDark, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("محاسبه بر اساس روش معاش هر کارمند", color = TextMuted, fontSize = 12.sp)
            }
            TextButton(onClick = onBack) { Text("برگشت", color = Burgundy) }
        }
        Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(16.dp)).padding(13.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("monthly" to "کارمند ماهانه", "daily" to "کارمند روزانه").forEach { (type, label) ->
                    Button(onClick = { workerType = type; message = "" }, modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = if (workerType == type) Burgundy else BurgundySoft, contentColor = if (workerType == type) Color.White else BurgundyDark),
                        contentPadding = PaddingValues(horizontal = 5.dp, vertical = 9.dp)) { Text(label, fontSize = 12.sp) }
                }
            }
            JalaliDateInput(startDate, { startDate = it }, "از تاریخ")
            JalaliDateInput(endDate, { endDate = it }, "تا تاریخ")
            Text("نوع کارمند را انتخاب کنید و مقدار پرداخت را برای هر نفر جداگانه وارد کنید.", color = TextMuted, fontSize = 11.sp)
        }
        Column(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(BurgundyDark, Burgundy)), RoundedCornerShape(20.dp)).padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("معاش قابل پرداخت کارمندان ماهانه", color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp)
                Text("↗", color = Color.White, fontSize = 22.sp)
            }
            Text(money(monthlyDue) + " افغانی", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("تعداد کارمندان: ${monthlyRows.size}", color = Color.White.copy(alpha = 0.88f), fontSize = 12.sp)
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.22f)))
            Text("مجموع باقی‌مانده معاش ماهانه در بازه انتخاب‌شده", color = Color.White.copy(alpha = 0.86f), fontSize = 11.sp)
        }
        Column(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(BurgundyDark, Burgundy)), RoundedCornerShape(20.dp)).padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("معاش قابل پرداخت کارمندان روزانه", color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp)
                Text("↗", color = Color.White, fontSize = 22.sp)
            }
            Text(money(dailyDue) + " افغانی", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("تعداد کارمندان: ${dailyRows.size}", color = Color.White.copy(alpha = 0.88f), fontSize = 12.sp)
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.22f)))
            Text("بر اساس روزهای حاضری ثبت‌شده", color = Color.White.copy(alpha = 0.86f), fontSize = 11.sp)
        }
        Column(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF5D626B), Color(0xFF343941))), RoundedCornerShape(20.dp)).padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("مجموع معاش پرداخت‌شده", color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp)
                Text("✓", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
            Text(money(paidTotal) + " افغانی", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("مجموع پرداخت‌های ثبت‌شده برای کارمندان", color = Color.White.copy(alpha = 0.88f), fontSize = 12.sp)
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.22f)))
            Text("باقی‌مانده معاش ماهانه و روزانه: ${money(allDue)} افغانی", color = Color.White.copy(alpha = 0.86f), fontSize = 11.sp)
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
                OutlinedTextField(
                    value = paymentAmounts[row.workerId].orEmpty(),
                    onValueChange = { value -> paymentAmounts = paymentAmounts + (row.workerId to value.filter { it.isDigit() || it == '.' }) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("مقدار پرداخت فعلی (افغانی)") },
                    singleLine = true,
                    colors = appFieldColors(),
                    shape = RoundedCornerShape(12.dp)
                )
                Button(onClick = {
                    val amount = paymentAmounts[row.workerId]?.toDoubleOrNull()
                    when {
                        amount == null || amount <= 0 -> message = "مقدار پرداخت را وارد کنید."
                        row.due <= 0 -> message = "برای این بازه معاش باقی نمانده است."
                        amount > row.due -> message = "مقدار پرداخت نمی‌تواند بیشتر از باقی‌مانده معاش (${blueprintMoney(row.due)} افغانی) باشد."
                        database.markWorkerPaid(row.workerId, amount, startDate, endDate, "پرداخت جزئی معاش") -> {
                            paymentAmounts = paymentAmounts - row.workerId
                            message = "پرداخت ${blueprintMoney(amount)} افغانی برای ${row.name} ثبت شد."
                        }
                        else -> message = "ثبت پرداخت ناموفق بود."
                    }
                }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Burgundy, contentColor = Color.White), shape = RoundedCornerShape(10.dp)) {
                    Text("ثبت پرداخت معاش")
                }
            }
        }
    }
}


@Composable
internal fun WorkerDailyAttendanceScreen(database: LocalDatabase, onBack: () -> Unit, onOpenReports: () -> Unit) {
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(java.util.Date()) }
    var attendanceDate by remember { mutableStateOf(today) }
    var workerType by remember { mutableStateOf("monthly") }
    var message by remember { mutableStateOf("") }
    val revision = database.revision.intValue
    val workers = remember(revision, workerType) { database.getWorkers(includeInactive = true).filter { it.payType == workerType } }
    val statuses = remember(revision, attendanceDate) { database.getWorkerAttendanceStatusesForDate(attendanceDate) }
    val presentCount = workers.count { statuses[it.id] == "present" }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text("مرکز حاضری کارمندان", color = BurgundyDark, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("ثبت حضور، غیابت و رخصتی", color = TextMuted, fontSize = 12.sp)
            }
            TextButton(onClick = onBack) { Text("برگشت", color = Burgundy) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("monthly" to "کارمندان ماهانه", "daily" to "کارمندان روزانه").forEach { (type,label) ->
                Button(onClick = { workerType = type; message = "" }, modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = if(workerType==type) Burgundy else BurgundySoft, contentColor = if(workerType==type) Color.White else BurgundyDark)) { Text(label, fontSize = 11.sp) }
            }
        }
        JalaliDateInput(attendanceDate, { attendanceDate = it; message = "" }, "تاریخ حاضری")
        Button(onClick = onOpenReports, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = BurgundySoft, contentColor = BurgundyDark)) { Text("گزارش‌های حاضری") }
        Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(14.dp)).padding(13.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("حاضر: ${presentCount} از ${workers.size}", color = BurgundyDark, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(if(workerType=="daily") "فقط کارمندانی نمایش داده می‌شوند که تاریخ انتخاب‌شده در مدت قراردادشان باشد." else "برای کارمندان ماهانه وضعیت هر روز ثبت می‌شود.", color = TextMuted, fontSize = 11.sp)
        }
        if(message.isNotBlank()) Text(message, color = BurgundyDark, fontSize = 12.sp)
        if(workers.isEmpty()) Text(if(workerType=="daily") "برای این بخش کارمند روزانه فعال وجود ندارد." else "کارمند ماهانه فعال ثبت نشده است.", color = TextMuted)
        workers.forEach { worker ->
            val inContract = worker.payType != "daily" || (attendanceDate >= worker.startDate && (worker.endDate.isBlank() || attendanceDate <= worker.endDate))
            val status = statuses[worker.id]
            if(inContract) Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(14.dp)).padding(13.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(worker.name, color = BurgundyDark, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text("${payTypeLabel(worker.payType)} · ${blueprintMoney(worker.payRate)} افغانی", color = TextMuted, fontSize = 11.sp)
                    }
                    Text(when(status) { "present" -> "حاضر"; "absent" -> "غایب"; "leave" -> "رخصتی"; else -> "ثبت نشده" },
                        color = when(status) { "present" -> Color(0xFF287447); "absent" -> Color(0xFFB33A32); else -> TextMuted }, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("present" to "حاضر", "absent" to "غایب", "leave" to "رخصتی").forEach { (value,label) ->
                        Button(onClick = {
                            val ok = database.setWorkerAttendanceStatus(worker.id, attendanceDate, value)
                            message = if(ok) "وضعیت ${worker.name} ثبت شد." else "این تاریخ معتبر نیست یا خارج از قرارداد کارمند است."
                        }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(
                            containerColor = if(status==value) Burgundy else BurgundySoft,
                            contentColor = if(status==value) Color.White else BurgundyDark
                        ), contentPadding = PaddingValues(horizontal=4.dp, vertical=7.dp)) { Text(label, fontSize=11.sp) }
                    }
                }
            }
        }
    }
}

@Composable
@Composable
internal fun WorkerAttendanceReportsScreen(database: LocalDatabase, onBack: () -> Unit) {
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(java.util.Date()) }
    val monthStart = remember { currentJalaliMonthStartIso() }
    var startDate by remember { mutableStateOf(monthStart) }
    var endDate by remember { mutableStateOf(today) }
    var workerType by remember { mutableStateOf("all") }
    var workerQuery by remember { mutableStateOf("") }
    var expandedWorkerId by remember { mutableStateOf<Int?>(null) }
    val revision = database.revision.intValue
    val workers = remember(revision) { database.getWorkers().filter { it.payType == "monthly" || it.payType == "daily" } }
    val selectedWorker = workers.firstOrNull { it.name.contains(workerQuery.trim(), ignoreCase = true) || (workerQuery.toIntOrNull() != null && it.id == workerQuery.toInt()) }
    val validRange = validIsoDate(startDate) && validIsoDate(endDate) && startDate <= endDate
    val rows = remember(revision, startDate, endDate, workerType, selectedWorker?.id, workerQuery) {
        if (validRange && (workerQuery.isBlank() || selectedWorker != null))
            database.getWorkerAttendanceReport(startDate, endDate, workerType, if (workerQuery.isBlank()) null else selectedWorker?.id)
        else emptyList()
    }
    val monthlyRows = rows.filter { it.payType == "monthly" }
    val dailyRows = rows.filter { it.payType == "daily" }
    val monthlyBaseTotal = monthlyRows.sumOf { it.rate }
    val monthlyDeductionTotal = monthlyRows.sumOf { (it.absent * (it.rate / 30.0)).coerceAtMost(it.rate) }
    val monthlyNetTotal = (monthlyBaseTotal - monthlyDeductionTotal).coerceAtLeast(0.0)
    val dailyWageTotal = dailyRows.sumOf { it.wageTotal }
    val grandTotal = monthlyNetTotal + dailyWageTotal
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("گزارش حاضری و معاش", color = BurgundyDark, fontSize = 21.sp, fontWeight = FontWeight.Bold)
            TextButton(onClick = onBack) { Text("برگشت", color = Burgundy) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("all" to "همه کارمندان", "monthly" to "ماهانه", "daily" to "روزانه").forEach { (type, label) ->
                Button(onClick = { workerType = type }, modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = if (workerType == type) Burgundy else BurgundySoft, contentColor = if (workerType == type) Color.White else BurgundyDark),
                    contentPadding = PaddingValues(4.dp)) { Text(label, fontSize = 10.sp) }
            }
        }
        OutlinedTextField(workerQuery, { workerQuery = it }, Modifier.fillMaxWidth(), label = { Text("نام یا شماره کارمند (اختیاری)") }, singleLine = true, colors = appFieldColors(), shape = RoundedCornerShape(12.dp))
        JalaliDateInput(startDate, { startDate = it }, "از تاریخ")
        JalaliDateInput(endDate, { endDate = it }, "تا تاریخ")
        if (validRange) Text("بازه گزارش: ${isoDateToJalali(startDate)} تا ${isoDateToJalali(endDate)}", color = TextMuted, fontSize = 11.sp)
        else Text("بازه تاریخ معتبر نیست.", color = Color(0xFFB33A32), fontSize = 12.sp)
        Column(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(BurgundyDark, Burgundy)), RoundedCornerShape(18.dp)).padding(15.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("معاش خالص کارمندان ماهانه", color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp)
            Text("${blueprintMoney(monthlyNetTotal)} افغانی", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold)
            Text("معاش پایه: ${blueprintMoney(monthlyBaseTotal)} · کسر غیابت: ${blueprintMoney(monthlyDeductionTotal)}", color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp)
        }
        Column(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(BurgundyDark, Burgundy)), RoundedCornerShape(18.dp)).padding(15.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("معاش کسب‌شده کارمندان روزانه", color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp)
            Text("${blueprintMoney(dailyWageTotal)} افغانی", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold)
            Text("بر اساس روزهای حاضر ثبت‌شده", color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp)
        }
        Column(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF454951), Color(0xFF343941))), RoundedCornerShape(18.dp)).padding(15.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("مجموع کل معاش قابل پرداخت", color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp)
            Text("${blueprintMoney(grandTotal)} افغانی", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold)
            Text("معاش ماهانه پس از کسر غیابت + معاش روزانه", color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp)
        }
        if (rows.isEmpty()) Text("برای فیلترهای انتخاب‌شده گزارشی وجود ندارد.", color = TextMuted)
        rows.forEach { row ->
            val isMonthly = row.payType == "monthly"
            val deduction = if (isMonthly) (row.absent * (row.rate / 30.0)).coerceAtMost(row.rate) else 0.0
            val netMonthly = if (isMonthly) (row.rate - deduction).coerceAtLeast(0.0) else 0.0
            Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(15.dp)).clickable { expandedWorkerId = if (expandedWorkerId == row.workerId) null else row.workerId }.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(row.name, color = BurgundyDark, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text(if (isMonthly) "کارمند ماهانه" else "کارمند روزانه", color = TextMuted, fontSize = 11.sp)
                    }
                    Text(if (expandedWorkerId == row.workerId) "−" else "+", color = Burgundy, fontSize = 20.sp)
                }
                Text("معاش پایه/نرخ روزانه: ${blueprintMoney(row.rate)} افغانی", color = TextDark, fontSize = 12.sp)
                Text("حاضر: ${row.present}  ·  غایب: ${row.absent}  ·  رخصتی: ${row.leave}", color = TextDark, fontSize = 12.sp)
                if (isMonthly) {
                    Text("کسر غیابت: ${blueprintMoney(deduction)} افغانی", color = Color(0xFFB33A32), fontSize = 12.sp)
                    Text("معاش خالص قابل پرداخت: ${blueprintMoney(netMonthly)} افغانی", color = BurgundyDark, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                } else {
                    Text("روزهای کار: ${row.present}", color = TextDark, fontSize = 12.sp)
                    Text("مجموع معاش کسب‌شده: ${blueprintMoney(row.wageTotal)} افغانی", color = BurgundyDark, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                if (expandedWorkerId == row.workerId) {
                    Text("جزئیات گزارش", color = BurgundyDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("روزهای حاضر، غایب و رخصتی بالا از سوابق ثبت‌شده در بازه انتخاب‌شده محاسبه شده‌اند.", color = TextMuted, fontSize = 11.sp)
                    Text("برای معاش ماهانه: معاش پایه ÷ ۳۰ × روزهای غیابت. برای معاش روزانه: روزهای حاضر × نرخ روزانه.", color = TextMuted, fontSize = 11.sp)
                }
            }
        }
        Text("کسر معاش ماهانه بر اساس ۳۰ روز معیار محاسبه می‌شود. برای اعمال کسر، روز باید در مرکز حاضری با وضعیت «غایب» ثبت شده باشد؛ روزهای بدون ثبت خودکار غیابت محسوب نمی‌شوند.", color = TextMuted, fontSize = 11.sp)
    }
}

@Composable
internal fun WorkerListScreen(database: LocalDatabase, onBack: () -> Unit) {
    var filter by remember { mutableStateOf("all") }
    val revision = database.revision.intValue
    val workers = remember(revision, filter) {
        database.getWorkers(includeInactive = true).filter { filter == "all" || it.payType == filter }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("فهرست کارمندان", color = BurgundyDark, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            TextButton(onClick = onBack) { Text("برگشت", color = Burgundy) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("all" to "تمام کارمندان", "monthly" to "کارمندان ماهانه", "daily" to "کارمندان روزانه").forEach { (type, label) ->
                Button(onClick = { filter = type }, modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = if (filter == type) Burgundy else BurgundySoft, contentColor = if (filter == type) Color.White else BurgundyDark),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 9.dp)) { Text(label, fontSize = 10.sp) }
            }
        }
        Text("تعداد کارمندان: ${workers.size}", color = TextMuted, fontSize = 12.sp)
        if (workers.isEmpty()) Text("کارمندی در این فهرست وجود ندارد.", color = TextMuted)
        workers.forEach { worker ->
            Row(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(14.dp)).padding(13.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(worker.name, color = BurgundyDark, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    if (worker.phone.isNotBlank()) Text(worker.phone, color = TextMuted, fontSize = 12.sp)
                    Text("${payTypeLabel(worker.payType)} · ${blueprintMoney(worker.payRate)} افغانی", color = TextDark, fontSize = 12.sp)
                    if (worker.payType == "daily") Text("قرارداد: ${worker.startDate} تا ${worker.endDate.ifBlank { "بدون تاریخ ختم" }}", color = TextMuted, fontSize = 10.sp)
                    if (!worker.active) Text("غیرفعال", color = TextMuted, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
internal fun JalaliDateInput(valueIso: String, onValueChange: (String) -> Unit, label: String, allowBlank: Boolean = false) {
    var showPicker by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = if (valueIso.isBlank()) "" else isoDateToJalali(valueIso),
        onValueChange = {},
        modifier = Modifier.fillMaxWidth().clickable { showPicker = true },
        label = { Text(label) }, readOnly = true, singleLine = true,
        trailingIcon = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (allowBlank && valueIso.isNotBlank()) TextButton(onClick = { onValueChange("") }) { Text("پاک", color = TextMuted, fontSize = 10.sp) }
                TextButton(onClick = { showPicker = true }) { Text("▦", color = BurgundyDark, fontSize = 18.sp) }
            }
        },
        colors = appFieldColors(), shape = RoundedCornerShape(12.dp)
    )
    if (showPicker) {
        val todayJalali = remember { isoDateToJalali(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(java.util.Date())) }
        JalaliDatePickerDialog(
            initialDate = if (valueIso.isBlank()) todayJalali else isoDateToJalali(valueIso),
            onDismiss = { showPicker = false },
            onDateSelected = { selected -> onValueChange(jalaliDateToIso(selected)); showPicker = false }
        )
    }
}
