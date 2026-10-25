package com.amsmas.mytailor

import android.os.Bundle
import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

@Composable
private fun loginFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Color.White,
    unfocusedBorderColor = Color(0xCCFFFFFF),
    focusedLabelColor = Color.White,
    unfocusedLabelColor = Color(0xFFECECEC),
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    cursorColor = Color.White,
    focusedContainerColor = Color(0x33201818),
    unfocusedContainerColor = Color(0x33201818)
)

// MyTailor unified theme: burgundy, warm ivory, and muted rose.
internal val Burgundy: Color = Color(0xFF702027)
internal val BurgundyDark: Color = Color(0xFF55191E)
internal val BurgundySoft = Color(0xFFF4E7E8)
private val PageBg = Color.Transparent
internal val CardBg = Color(0xFFFFFFFF)
internal val BorderSoft = Color(0xFFD9C6C5)
internal val TextDark = Color(0xFF302627)
internal val TextMuted = Color(0xFF7D7070)
internal data class TailorRecord(val name: String, val phone: String, val address: String, val skills: List<String>)
private data class ShopSkill(val name: String)
internal data class GarmentModel(val name: String, val measurements: List<String>)
internal data class TailorOrder(val customerName: String, val phone: String, val garmentModel: String, val measurements: Map<String, String>, val notes: String, val deliveryDate: String, val price: Double, val deposit: Double, val paid: Double, val address: String = "")
private enum class MainTab(val title: String, val symbol: String) { HOME("خانه", "⌂"), CUSTOMERS("مشتریان", "♙"), ORDERS("سفارشات", "▤"), DELIVERY("تحویل", "▣"), REPORTS("گزارشات", "▥"), FINANCE("مالی", "؋"), SETTINGS("تنظیمات", "⚙") }
private const val APP_BACKGROUND_IMAGE_URL =
    "https://raw.githubusercontent.com/ams-mas/mytailor/main/app/src/main/res/drawable/images/tailoring_wallpaper_4K.png"
private const val WORKSHOP_IMAGE_URL =
    "https://raw.githubusercontent.com/ams-mas/mytailor/main/app/src/main/res/drawable/images/login_background.png"

class MainActivity : ComponentActivity() {
    private lateinit var localDatabase: LocalDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        localDatabase = LocalDatabase(this)
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                var loggedIn by remember { mutableStateOf(false) }
                val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
                    if (uri != null) {
                        runCatching {
                            val json = localDatabase.createBackupJson()
                            contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray(Charsets.UTF_8)) }
                                ?: error("امکان نوشتن فایل وجود ندارد.")
                        }.onSuccess {
                            Toast.makeText(this, "نسخه پشتیبان ذخیره شد", Toast.LENGTH_LONG).show()
                        }.onFailure {
                            Toast.makeText(this, "ذخیره نسخه پشتیبان ناموفق بود: ${it.localizedMessage}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
                val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                    if (uri != null) {
                        runCatching {
                            val json = contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                                ?: error("امکان خواندن فایل وجود ندارد.")
                            localDatabase.restoreBackupJson(json)
                        }.onSuccess {
                            Toast.makeText(this, "بازیابی انجام شد. صفحه را دوباره باز کنید تا معلومات تازه نمایش داده شود.", Toast.LENGTH_LONG).show()
                        }.onFailure {
                            Toast.makeText(this, "بازیابی ناموفق بود: ${it.localizedMessage}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
                if (loggedIn) {
                    TailorHome(
                        context = this@MainActivity,
                        database = localDatabase,
                        onBackup = { backupLauncher.launch("mytailor-backup.json") },
                        onRestore = { restoreLauncher.launch(arrayOf("application/json", "text/json", "text/plain")) },
                        onLogout = { loggedIn = false },
                        onAddOrder = { Toast.makeText(this, "فرم ثبت سفارش در مرحله بعد اضافه می‌شود", Toast.LENGTH_SHORT).show() }
                    )
                } else {
                    LoginScreen(
                        onLogin = { _, _ -> loggedIn = true },
                        onRegister = { Toast.makeText(this, "ثبت نام در نسخه بعدی فعال می‌شود", Toast.LENGTH_SHORT).show() }
                    )
                }
            }
        }
    }
}

@Composable
private fun LoginScreen(onLogin: (String, String) -> Unit, onRegister: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var cutting by remember { mutableStateOf(false) }
    val cutProgress = androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (cutting) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 1050, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "fabricCutProgress"
    )
    LaunchedEffect(cutting) {
        if (cutting) {
            kotlinx.coroutines.delay(1100)
            onLogin(email.trim(), password)
        }
    }

    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {

        AsyncImage(
            model = WORKSHOP_IMAGE_URL,
            contentDescription = "تصویر کارگاه خیاطی",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0x55301818), Color(0x77301818), Color(0x55301818))
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 26.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            // Position the first field around the middle of the screen.
            Spacer(Modifier.height(screenHeight * 0.45f))
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("ایمیل یا شماره تماس") },
                colors = loginFieldColors(),
                shape = RoundedCornerShape(16.dp)
            )
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("رمز عبور") },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    androidx.compose.material3.TextButton(onClick = { passwordVisible = !passwordVisible }) {
                        Text(if (passwordVisible) "پنهان" else "نمایش", color = Color.White)
                    }
                },
                colors = loginFieldColors(),
                shape = RoundedCornerShape(16.dp)
            )
            Text(
                "رمز عبور خود را فراموش کرده‌اید؟",
                color = Color.White,
                fontSize = 13.sp,
                modifier = Modifier.align(Alignment.Start).padding(top = 10.dp, bottom = 18.dp)
            )
            Button(
                onClick = { if (!cutting) cutting = true },
                enabled = !cutting,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Burgundy)
            ) {
                Text("ورود به حساب", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onRegister,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0x33FFFFFF),
                    contentColor = Color.White
                )
            ) {
                Text("ثبت نام", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        }
        if (cutting) {
            FabricScissorTransition(progress = cutProgress.value)
        }
    }
}

@Composable
private fun FabricScissorTransition(progress: Float) {
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        drawRect(brush = Brush.linearGradient(colors = listOf(Color(0xFF3C2920), Color(0xFF876448), Color(0xFF4A3024), Color(0xFF241B18)), start = Offset(0f, 0f), end = Offset(w, h)))
        val spacing = 12.dp.toPx()
        var x = 0f
        while (x < w) {
            drawLine(Color(0x22F4D9B2), Offset(x, 0f), Offset(x + w * 0.12f, h), strokeWidth = 0.7.dp.toPx())
            x += spacing
        }
        var y = 0f
        while (y < h) {
            drawLine(Color(0x187B5138), Offset(0f, y), Offset(w, y + h * 0.025f), strokeWidth = 1.dp.toPx())
            y += spacing
        }
        val cutY = h * (1f - progress)
        val cx = w * 0.5f
        val glow = Color(0xFFFFC66B)
        drawLine(Color(0x66FFB94D), Offset(cx, h), Offset(cx, cutY), strokeWidth = 15.dp.toPx(), cap = StrokeCap.Round)
        drawLine(glow, Offset(cx, h), Offset(cx, cutY), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
        val spread = w * 0.30f * progress
        val left = Path().apply { moveTo(cx, h); lineTo(cx - spread, cutY + 26.dp.toPx()); lineTo(cx - spread * 0.72f, cutY + 64.dp.toPx()); lineTo(cx - 5.dp.toPx(), h); close() }
        val right = Path().apply { moveTo(cx, h); lineTo(cx + spread, cutY + 26.dp.toPx()); lineTo(cx + spread * 0.72f, cutY + 64.dp.toPx()); lineTo(cx + 5.dp.toPx(), h); close() }
        drawPath(left, Color(0xFFB58A61)); drawPath(left, Color(0x66F5D7B0), style = Stroke(width = 2.dp.toPx()))
        drawPath(right, Color(0xFF9B704D)); drawPath(right, Color(0x66F5D7B0), style = Stroke(width = 2.dp.toPx()))
        val sy = h * (1f - progress * 0.94f)
        val pivot = Offset(cx, sy + 18.dp.toPx())
        val length = 38.dp.toPx()
        val angle = 0.30f + (1f - progress) * 0.35f
        val dx = kotlin.math.sin(angle.toDouble()).toFloat() * length
        val dy = kotlin.math.cos(angle.toDouble()).toFloat() * length
        drawLine(glow, pivot, Offset(pivot.x - dx, pivot.y - dy), strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round)
        drawLine(glow, pivot, Offset(pivot.x + dx, pivot.y - dy), strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round)
        drawCircle(Color(0xFF38231A), radius = 9.dp.toPx(), center = Offset(pivot.x - 13.dp.toPx(), pivot.y + 25.dp.toPx()), style = Stroke(width = 4.dp.toPx()))
        drawCircle(Color(0xFF38231A), radius = 9.dp.toPx(), center = Offset(pivot.x + 13.dp.toPx(), pivot.y + 25.dp.toPx()), style = Stroke(width = 4.dp.toPx()))
        drawCircle(glow, radius = 3.dp.toPx(), center = pivot)
    }
}

@Composable
private fun TailorHome(context: Context, database: LocalDatabase, onBackup: () -> Unit, onRestore: () -> Unit, onLogout: () -> Unit, onAddOrder: () -> Unit) {
    var selected by remember { mutableStateOf(MainTab.ORDERS) }
    var ordersView by remember { mutableStateOf("active") }
    var showTailorForm by remember { mutableStateOf(false) }
    var showSkillsForm by remember { mutableStateOf(false) }
    var showGarmentModels by remember { mutableStateOf(false) }
    var showWorkshopSettings by remember { mutableStateOf(false) }
    var showBackupRestore by remember { mutableStateOf(false) }
    var showWorkerManagement by remember { mutableStateOf(false) }
    var showWorkerMenu by remember { mutableStateOf(false) }
    var showWorkerList by remember { mutableStateOf(false) }
    var showWorkerPayroll by remember { mutableStateOf(false) }
    var showWorkerAttendance by remember { mutableStateOf(false) }
    var showWorkerAttendanceReports by remember { mutableStateOf(false) }
    var showWorkflowSettings by remember { mutableStateOf(false) }
    var showProductionWorkflow by remember { mutableStateOf(false) }
    var showOrderForm by remember { mutableStateOf(false) }
    var savedOrderForReceipt by remember { mutableStateOf<TailorOrder?>(null) }
    var selectedSettingsCategory by remember { mutableStateOf<String?>(null) }
    var shopSkills by remember { mutableStateOf(listOf<ShopSkill>()) }
    var registeredTailors by remember { mutableStateOf(listOf<TailorRecord>()) }
    LaunchedEffect(database) {
        shopSkills = database.getSkills().map { ShopSkill(it) }
        registeredTailors = database.getTailors()
    }
    Box(Modifier.fillMaxSize()) {
        AsyncImage(
            model = APP_BACKGROUND_IMAGE_URL,
            contentDescription = "پس‌زمینه خیاطی",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Column(Modifier.fillMaxSize().background(Color.Transparent).windowInsetsPadding(WindowInsets.statusBars)) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                savedOrderForReceipt != null -> OrderSuccessScreen(order = savedOrderForReceipt!!, onPrint = { printOrderReceipt(context, savedOrderForReceipt!!) }, onDone = { savedOrderForReceipt = null; showOrderForm = false; selected = MainTab.ORDERS; ordersView = "active" })
                showOrderForm -> OrderRegistrationScreen(database = database, onBack = { showOrderForm = false }, onSave = { order -> savedOrderForReceipt = order })
                showWorkerAttendanceReports -> WorkerAttendanceReportsScreen(database = database, onBack = { showWorkerAttendanceReports = false })
                showWorkerAttendance -> WorkerDailyAttendanceScreen(database = database, onBack = { showWorkerAttendance = false }, onOpenReports = { showWorkerAttendanceReports = true })
                showWorkerPayroll -> WorkerPayrollScreen(database = database, onBack = { showWorkerPayroll = false })
                showWorkerList -> WorkerListScreen(database = database, onBack = { showWorkerList = false })
                showWorkerMenu -> WorkerMenuScreen(
                    onBack = { showWorkerMenu = false },
                    onRegister = { showWorkerMenu = false; showWorkerManagement = true },
                    onList = { showWorkerMenu = false; showWorkerList = true },
                    onPayroll = { showWorkerMenu = false; showWorkerPayroll = true },
                    onAttendance = { showWorkerMenu = false; showWorkerAttendance = true }
                )
                showWorkerManagement -> WorkerManagementScreen(database = database, onBack = { showWorkerManagement = false })
                showWorkflowSettings -> WorkflowSettingsScreen(database = database, onBack = { showWorkflowSettings = false })
                showProductionWorkflow -> ProductionWorkflowScreen(database = database, onBack = { showProductionWorkflow = false })
                selected == MainTab.HOME -> DashboardContent(database = database, onOpenDeliveries = { selected = MainTab.ORDERS; ordersView = "delivery" })
                selected == MainTab.ORDERS -> Column(Modifier.fillMaxSize()) {
                    Row(Modifier.fillMaxWidth().background(Color.White.copy(alpha = 0.94f)).padding(horizontal = 5.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("dashboard" to "داشبورد", "active" to "سفارشات", "delivery" to "تحویل", "reports" to "گزارشات").forEach { (key, label) ->
                            Column(Modifier.weight(1f).background(if (ordersView == key) Burgundy else BurgundySoft, RoundedCornerShape(9.dp)).clickable { ordersView = key }.padding(vertical = 9.dp, horizontal = 2.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(label, color = if (ordersView == key) Color.White else BurgundyDark, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        when (ordersView) {
                            "dashboard" -> DashboardContent(database = database, onOpenDeliveries = { ordersView = "delivery" })
                            "delivery" -> DeliveryOrdersScreen(database = database, onAddOrder = { showOrderForm = true })
                            "reports" -> ReportsScreen(database = database)
                            else -> ActiveOrdersScreen(database = database, onOpenWorkflow = { showProductionWorkflow = true }, onAddOrder = { showOrderForm = true })
                        }
                    }
                }
                selected == MainTab.DELIVERY -> DeliveryOrdersScreen(database = database, onAddOrder = { showOrderForm = true })
                selected == MainTab.CUSTOMERS -> CustomerListScreen(database = database)
                selected == MainTab.REPORTS -> ReportsScreen(database = database)
                selected == MainTab.FINANCE -> FinanceScreen(database = database)
                selected == MainTab.SETTINGS -> {
                    if (selectedSettingsCategory != null) {
                        SettingsCategoryScreen(
                            title = selectedSettingsCategory!!,
                            onBack = { selectedSettingsCategory = null }
                        )
                    } else if (showBackupRestore) {
                        BackupRestoreScreen(onBack = { showBackupRestore = false }, onBackup = onBackup, onRestore = onRestore)
                    } else if (showWorkshopSettings) {
                        WorkshopSettingsScreen(onBack = { showWorkshopSettings = false }, onRegisterTailor = { showWorkshopSettings = false; showTailorForm = true }, onRegisterSkills = { showWorkshopSettings = false; showSkillsForm = true }, onGarmentModels = { showWorkshopSettings = false; showGarmentModels = true })
                    } else if (showGarmentModels) {
                        GarmentModelsScreen(database = database, onBack = { showGarmentModels = false })
                    } else if (showSkillsForm) {
                        RegisterSkillsForm(
                            initialSkills = shopSkills,
                            onBack = { showSkillsForm = false },
                            onSave = { skills -> database.replaceSkills(skills.map { it.name }); shopSkills = skills; showSkillsForm = false }
                        )
                    } else if (showTailorForm) {
                        RegisterTailorForm(
                            availableSkills = shopSkills.map { it.name },
                            onBack = { showTailorForm = false },
                            onSave = { record ->
                                database.addTailor(record)
                                registeredTailors = database.getTailors()
                                showTailorForm = false
                            }
                        )
                    } else {
                        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
                            Text("تنظیمات", color = BurgundyDark, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            Text("مدیریت و تنظیم بخش‌های مختلف کارگاه", color = TextMuted, fontSize = 13.sp, modifier = Modifier.padding(top = 5.dp, bottom = 18.dp))
                            val settingsItems = listOf(
                                Triple("تنظیمات کارگاه", "⚙", "workshop"),
                                Triple("مدیریت کارمندان و معاش", "♙", "workers"),
                                Triple("حاضری کارمندان", "✓", "attendance"),
                                Triple("گزارش حاضری", "▤", "attendanceReports"),
                                Triple("مراحل تولید سفارش", "⇢", "workflow"),
                                Triple("پروفایل و معلومات کارگاه", "▣", "profile"),
                                Triple("پشتیبان‌گیری و بازیابی", "⟳", "backup"),
                                Triple("ظاهر برنامه", "◐", "appearance"),
                                Triple("اعلان‌ها و یادآورها", "♧", "notifications"),
                                Triple("امنیت و حریم خصوصی", "▣", "security"),
                                Triple("تنظیمات عمومی", "☷", "general"),
                                Triple("چاپ و رسیدها", "▤", "printing"),
                                Triple("درباره و پشتیبانی", "ⓘ", "about")
                            )
                            settingsItems.chunked(2).forEach { rowItems ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    rowItems.forEach { item ->
                                        SettingsMenuItem(item.first, item.second, when (item.third) {
                                            "backup" -> Color(0xFFE7F3E8)
                                            "appearance" -> Color(0xFFE7EEF9)
                                            "notifications" -> Color(0xFFFFF0D8)
                                            "security" -> Color(0xFFF6E7E8)
                                            else -> Color(0xFFF1E9E4)
                                        }, Modifier.weight(1f)) {
                                            when (item.third) {
                                                "workshop" -> showWorkshopSettings = true
                                                "workers" -> showWorkerMenu = true
                                                "attendance" -> showWorkerAttendance = true
                                                "attendanceReports" -> showWorkerAttendanceReports = true
                                                "workflow" -> showWorkflowSettings = true
                                                "backup" -> showBackupRestore = true
                                                else -> selectedSettingsCategory = item.first
                                            }
                                        }
                                    }
                                    if (rowItems.size == 1) Spacer(Modifier.weight(1f))
                                }
                                Spacer(Modifier.height(10.dp))
                            }
                            Spacer(Modifier.height(18.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                InfoCountCard("مهارت‌های کارگاه", shopSkills.size.toString(), Modifier.weight(1f))
                                InfoCountCard("خیاطان ثبت‌شده", registeredTailors.size.toString(), Modifier.weight(1f))
                            }
                            Spacer(Modifier.height(22.dp))
                            Button(onClick = onLogout, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE8E1DC), contentColor = TextDark)) { Text("خروج از حساب") }
                        }
                    }
                }
            }
            if (selected == MainTab.HOME && !showOrderForm) {
                Button(
                    onClick = { showOrderForm = true },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(18.dp).size(56.dp),
                    shape = CircleShape, colors = ButtonDefaults.buttonColors(containerColor = Burgundy)
                ) { Text("+", fontSize = 30.sp, color = Color.White) }
            }
        }
        Row(Modifier.fillMaxWidth().background(Color.White).windowInsetsPadding(WindowInsets.navigationBars).padding(vertical = 7.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            listOf(MainTab.ORDERS, MainTab.CUSTOMERS, MainTab.FINANCE, MainTab.SETTINGS).forEach { tab ->
                Column(Modifier.weight(1f).clickable { selected = tab }.padding(vertical = 2.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(tab.symbol, color = if (selected == tab) BurgundyDark else Color.Gray, fontSize = 22.sp)
                    Text(tab.title, color = if (selected == tab) BurgundyDark else Color.Gray, fontSize = 10.sp, fontWeight = if (selected == tab) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
    }
}
}

@Composable
private fun ReportsScreen(database: LocalDatabase) {
    val revision = database.revision.intValue
    val counts = remember(revision) { database.getOrderCounts() }
    val customers = remember(revision) { database.getCustomers() }
    val tailors = remember(revision) { database.getWorkers() }
    val skills = remember(revision) { database.getSkills() }
    val models = remember(revision) { database.getGarmentModels() }
    val financial = remember(revision) { database.getFinancialReport() }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("گزارشات", color = BurgundyDark, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("خلاصه معلومات و وضعیت کارگاه", color = TextMuted, fontSize = 13.sp)
        ReportRow("مجموع سفارشات", counts.first.toString(), "سفارش‌های ثبت‌شده")
        ReportRow("سفارش‌های تکمیل‌شده", counts.second.toString(), "آماده تحویل یا تحویل‌شده")
        ReportRow("سفارش‌های در جریان", counts.third.toString(), "هنوز تکمیل نشده")
        ReportRow("مجموع مشتریان", customers.size.toString(), "مشتریان ثبت‌شده")
        ReportRow("مجموع خیاطان", tailors.size.toString(), "خیاطان ثبت‌شده")
        ReportRow("مدل‌های لباس", models.size.toString(), "مدل‌های ثبت‌شده")
        ReportRow("خدمات و مهارت‌ها", skills.size.toString(), "مهارت‌های تعریف‌شده")
        ReportRow("مجموع قیمت سفارشات", String.format(java.util.Locale.US, "%,.0f افغانی", financial.first), "ارزش تمام سفارشات")
        ReportRow("باقی‌داری مشتریان", String.format(java.util.Locale.US, "%,.0f افغانی", financial.second), "مبلغ باقی‌مانده برای دریافت")
    }
}


@Composable
private fun FinanceScreen(database: LocalDatabase) {
    var showPayroll by remember { mutableStateOf(false) }
    if (showPayroll) {
        WorkerPayrollScreen(database = database, onBack = { showPayroll = false })
        return
    }
    val revision = database.revision.intValue
    var period by remember { mutableStateOf("month") }
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    val paymentInputs = remember { mutableStateMapOf<String, String>() }
    val paymentMessages = remember { mutableStateMapOf<String, String>() }
    val income = remember(revision, period, startDate, endDate) { database.getIncomeForPeriod(period, startDate, endDate) }
    val debtors = remember(revision) { database.getDebtCustomers() }
    val totalDebt = remember(debtors) { debtors.sumOf { it.second } }
    val financial = remember(revision) { database.getFinancialReport() }
    val money: (Double) -> String = { amount -> String.format(java.util.Locale.US, "%,.0f", amount) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.size(54.dp).background(Burgundy, RoundedCornerShape(18.dp)), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text("؋", color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.Bold)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("مدیریت مالی", color = BurgundyDark, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                Text("درآمد، پرداخت‌ها و قرض مشتریان", color = TextMuted, fontSize = 13.sp)
            }
        }

        Column(
            Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(BurgundyDark, Burgundy)), RoundedCornerShape(22.dp)).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("درآمد دریافت‌شده", color = Color.White.copy(alpha = 0.88f), fontSize = 14.sp)
                Text("↗", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
            Text(money(income), color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            Text("افغانی", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.22f)))
            Text("محاسبه بر اساس تاریخ واقعی دریافت پول", color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp)
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f).background(Color.White, RoundedCornerShape(18.dp)).padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("قرض باقی‌مانده", color = TextMuted, fontSize = 12.sp)
                Text(money(totalDebt), color = BurgundyDark, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                Text("افغانی", color = TextMuted, fontSize = 11.sp)
            }
            Column(Modifier.weight(1f).background(BurgundySoft, RoundedCornerShape(18.dp)).padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("مشتریان قرض‌دار", color = BurgundyDark, fontSize = 12.sp)
                Text("${debtors.size}", color = BurgundyDark, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                Text("مشتری", color = BurgundyDark, fontSize = 11.sp)
            }
        }

        Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(16.dp)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("ارزش کل سفارش‌های ثبت‌شده", color = TextMuted, fontSize = 12.sp)
            Text(money(financial.first), color = BurgundyDark, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("افغانی · مجموع قیمت سفارش‌ها", color = TextMuted, fontSize = 11.sp)
        }

        Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(18.dp)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("گزارش درآمد", color = BurgundyDark, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text("بازه زمانی را انتخاب کنید", color = TextMuted, fontSize = 12.sp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                listOf("today" to "امروز", "week" to "هفته", "month" to "ماه", "year" to "سال", "custom" to "دلخواه").forEach { (key, label) ->
                    Column(
                        Modifier.weight(1f).background(if (period == key) Burgundy else BurgundySoft, RoundedCornerShape(10.dp)).clickable { period = key }.padding(vertical = 11.dp, horizontal = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(label, color = if (period == key) Color.White else BurgundyDark, fontSize = 10.sp, fontWeight = if (period == key) FontWeight.Bold else FontWeight.Medium)
                    }
                }
            }
            if (period == "custom") {
                JalaliDateInput(startDate, { startDate = it }, "از تاریخ")
                JalaliDateInput(endDate, { endDate = it }, "تا تاریخ")
            }
        }

        Row(Modifier.fillMaxWidth().padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("قرض مشتریان", color = BurgundyDark, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("پرداخت‌های جدید را همین‌جا ثبت کنید", color = TextMuted, fontSize = 12.sp)
            }
            Box(Modifier.background(BurgundySoft, RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 7.dp)) {
                Text("${debtors.size}", color = BurgundyDark, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (debtors.isEmpty()) {
            Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(18.dp)).padding(vertical = 28.dp, horizontal = 18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("✓", color = Burgundy, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                Text("همه حساب‌ها تسویه است", color = BurgundyDark, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("در حال حاضر مشتری قرض‌دار وجود ندارد.", color = TextMuted, fontSize = 12.sp, textAlign = TextAlign.Center)
            }
        } else {
            debtors.forEach { (customer, debt) ->
                val customerKey = customer.name + "|" + customer.phone
                Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(18.dp)).padding(15.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(Modifier.size(42.dp).background(BurgundySoft, RoundedCornerShape(14.dp)), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Text(customer.name.firstOrNull()?.toString() ?: "م", color = BurgundyDark, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(customer.name, color = TextDark, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(customer.phone, color = TextMuted, fontSize = 12.sp)
                            Text("${customer.orderCount} سفارش", color = TextMuted, fontSize = 11.sp)
                        }
                        Column(Modifier.background(BurgundySoft, RoundedCornerShape(12.dp)).padding(horizontal = 10.dp, vertical = 8.dp), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("باقی‌داری", color = BurgundyDark, fontSize = 10.sp)
                            Text(money(debt), color = BurgundyDark, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("افغانی", color = BurgundyDark, fontSize = 10.sp)
                        }
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(BorderSoft.copy(alpha = 0.65f)))
                    Text("ثبت پرداخت", color = TextDark, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(value = paymentInputs[customerKey] ?: "", onValueChange = { paymentInputs[customerKey] = it; paymentMessages.remove(customerKey) }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("مبلغ پرداختی (افغانی)") }, colors = appFieldColors(), shape = RoundedCornerShape(13.dp))
                    Button(
                        onClick = {
                            val amount = paymentInputs[customerKey]?.trim()?.replace(",", "")?.toDoubleOrNull()
                            when {
                                amount == null || amount <= 0 -> paymentMessages[customerKey] = "مبلغ معتبر وارد کنید."
                                amount > debt + 0.0001 -> paymentMessages[customerKey] = "مبلغ از قرض باقی‌مانده بیشتر است."
                                database.recordCustomerPayment(customer.name, customer.phone, amount) -> {
                                    paymentInputs.remove(customerKey)
                                    paymentMessages[customerKey] = "پرداخت ثبت شد."
                                }
                                else -> paymentMessages[customerKey] = "پرداخت ثبت نشد؛ معلومات را بررسی کنید."
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(13.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Burgundy, contentColor = Color.White)
                    ) { Text("ثبت پرداخت", fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                    paymentMessages[customerKey]?.let { message ->
                        Text(message, color = if (message == "پرداخت ثبت شد.") Burgundy else Color(0xFFB3261E), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}
@Composable
private fun ReportRow(title: String, value: String, description: String) {
    Row(
        Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(14.dp)).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = TextDark, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(description, color = TextMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
        }
        Text(value, color = BurgundyDark, fontSize = 17.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SettingsMenuItem(title: String, symbol: String, tileColor: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .height(122.dp)
            .background(Color.White, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(Modifier.size(42.dp).background(tileColor, RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) {
            Text(symbol, color = BurgundyDark, fontSize = 23.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(10.dp))
        Text(title, color = TextDark, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, lineHeight = 15.sp)
    }
}

@Composable
private fun InfoCountCard(title: String, count: String, modifier: Modifier = Modifier) {
    Column(modifier.background(Color.White, RoundedCornerShape(13.dp)).padding(12.dp)) {
        Text(count, color = BurgundyDark, fontSize = 21.sp, fontWeight = FontWeight.Bold)
        Text(title, color = TextMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 3.dp))
    }
}

@Composable
private fun BackupRestoreScreen(onBack: () -> Unit, onBackup: () -> Unit, onRestore: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("پشتیبان‌گیری و بازیابی", color = BurgundyDark, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("از معلومات کارگاه نسخه پشتیبان بسازید یا معلومات قبلی را از فایل بازیابی کنید.", color = TextMuted, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp, bottom = 18.dp))
        SettingsMenuItem("ایجاد نسخه پشتیبان", "⇧", Color(0xFFE7F3E8), Modifier.fillMaxWidth().height(122.dp)) { onBackup() }
        Spacer(Modifier.height(12.dp))
        SettingsMenuItem("بازیابی از فایل", "⇩", Color(0xFFF6E7E8), Modifier.fillMaxWidth().height(122.dp)) { onRestore() }
        Spacer(Modifier.height(12.dp))
        Text("توجه: بازیابی، فهرست مهارت‌ها و خیاطان فعلی را با معلومات موجود در فایل جایگزین می‌کند.", color = Color(0xFF9A4B16), fontSize = 12.sp, modifier = Modifier.padding(vertical = 8.dp))
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE8E1DC), contentColor = TextDark)) { Text("برگشت به تنظیمات") }
    }
}

@Composable
private fun SettingsCategoryScreen(title: String, onBack: () -> Unit) {
    val descriptionAndItems = when (title) {
        "پروفایل و معلومات کارگاه" -> "هویت و معلومات تماس کارگاه" to listOf("نام و لوگوی کارگاه", "نام مالک و شماره تماس", "آدرس کارگاه", "ویرایش پروفایل")
        "ظاهر برنامه" -> "شخصی‌سازی شکل و زبان برنامه" to listOf("حالت روشن / تاریک / مطابق سیستم", "زبان: دری، فارسی، English", "اندازه فونت", "رنگ برنامه")
        "اعلان‌ها و یادآورها" -> "کنترل یادآوری‌های مهم" to listOf("یادآوری تحویل سفارش", "مانده حساب پرداخت‌نشده مشتریان", "یادآوری پرداخت معاش کارمندان", "خلاصه روزانه کارگاه", "فعال یا غیرفعال‌کردن اعلان‌ها")
        "امنیت و حریم خصوصی" -> "محافظت از معلومات کارگاه" to listOf("قفل برنامه با PIN یا اثر انگشت", "تغییر رمز عبور حساب", "قفل خودکار پس از بی‌کاری", "مدیریت اجازه‌های برنامه")
        "تنظیمات عمومی" -> "تنظیم رفتار عمومی برنامه" to listOf("واحد پول پیش‌فرض: AFN، USD و غیره", "قالب تاریخ و نوع تقویم", "وضعیت پیش‌فرض سفارش", "واحد پیش‌فرض اندازه‌گیری", "تأیید پیش از حذف معلومات")
        "چاپ و رسیدها" -> "تنظیم اسناد مشتریان" to listOf("سرصفحه و پاصفحه رسید", "نمایش لوگوی کارگاه روی رسید", "اندازه کاغذ", "زبان چاپ", "قالب پیش‌فرض رسید")
        else -> "معلومات برنامه و راه‌های دریافت کمک" to listOf("نام و نسخه برنامه", "بررسی به‌روزرسانی", "راهنما و دستور استفاده", "تماس با پشتیبانی", "سیاست حریم خصوصی")
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text(title, color = BurgundyDark, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(descriptionAndItems.first, color = TextMuted, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp, bottom = 16.dp))
        descriptionAndItems.second.forEach { item ->
            Row(
                Modifier.fillMaxWidth().padding(bottom = 8.dp).background(Color.White, RoundedCornerShape(12.dp)).padding(horizontal = 14.dp, vertical = 15.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(item, color = TextDark, fontSize = 14.sp, modifier = Modifier.weight(1f))
                Text("›", color = Burgundy, fontSize = 22.sp)
            }
        }
        Text("بعضی از این گزینه‌ها فعلاً فقط به‌صورت منو نمایش داده می‌شوند و عملکرد آن‌ها در مرحله بعد پیاده‌سازی می‌شود.", color = TextMuted, fontSize = 12.sp, modifier = Modifier.padding(vertical = 10.dp))
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE8E1DC), contentColor = TextDark)) { Text("برگشت به تنظیمات") }
    }
}

@Composable
private fun WorkshopSettingsScreen(onBack: () -> Unit, onRegisterTailor: () -> Unit, onRegisterSkills: () -> Unit, onGarmentModels: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("تنظیمات کارگاه", color = BurgundyDark, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("مدیریت خیاطان، مهارت‌ها و مدل‌های لباس کارگاه", color = TextMuted, fontSize = 13.sp, modifier = Modifier.padding(top = 5.dp, bottom = 18.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SettingsMenuItem("ثبت خیاط", "♙", Color(0xFFE7EEF9), Modifier.weight(1f), onRegisterTailor)
            SettingsMenuItem("ثبت مهارت‌ها", "✂", Color(0xFFE7F3E8), Modifier.weight(1f), onRegisterSkills)
        }
        Spacer(Modifier.height(10.dp))
        SettingsMenuItem("مدل‌های لباس و اندازه‌گیری", "📏", Color(0xFFF6E7E8), Modifier.fillMaxWidth(), onGarmentModels)
        Spacer(Modifier.height(22.dp))
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE8E1DC), contentColor = TextDark)) { Text("برگشت به تنظیمات") }
    }
}

@Composable
private fun GarmentModelsScreen(database: LocalDatabase, onBack: () -> Unit) {
    val sampleModels = listOf(
        GarmentModel("پیراهن‌تنبان مردانه", listOf("دور سینه", "دور کمر", "دور باسن", "قد پیراهن", "سرشانه", "قد آستین", "دور بازو", "دور یخن", "قد تنبان", "دور دم‌پا", "فاق تنبان")),
        GarmentModel("لباس ساده مردانه", listOf("سرشانه", "قد آستین", "قد پیراهن", "دور سینه")),
        GarmentModel("واسکت مردانه", listOf("سرشانه", "قد واسکت", "دور سینه"))
    )
    val storedModels = database.getGarmentModels()
    val initialModels = if (storedModels.isEmpty()) {
        sampleModels
    } else {
        storedModels + sampleModels.filter { sample -> storedModels.none { it.name.equals(sample.name, ignoreCase = true) } }
    }
    if (initialModels != storedModels) database.saveGarmentModels(initialModels)
    var models by remember { mutableStateOf(initialModels) }
    var selectedModel by remember { mutableIntStateOf(0) }
    var newModelName by remember { mutableStateOf("") }
    var newMeasurementName by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf("") }
    var editMeasurements by remember { mutableStateOf(listOf<String>()) }
    var message by remember { mutableStateOf("") }

    fun persist(updated: List<GarmentModel>) {
        database.saveGarmentModels(updated)
        models = updated
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("مدل‌های لباس و اندازه‌گیری", color = BurgundyDark, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("هر مدل را جداگانه ویرایش کنید. تغییرات پس از ذخیره در دیتابیس باقی می‌ماند.", color = TextMuted, fontSize = 13.sp, modifier = Modifier.padding(top = 5.dp, bottom = 16.dp))

        Text("مدل‌های موجود", color = TextDark, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        models.forEachIndexed { index, model ->
            Column(Modifier.fillMaxWidth().padding(top = 8.dp).background(if (selectedModel == index) BurgundySoft else Color.White, RoundedCornerShape(12.dp)).padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f).clickable { selectedModel = index; editing = false; message = "" }) {
                        Text(model.name, color = TextDark, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("${model.measurements.size} اندازه", color = TextMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 3.dp))
                    }
                    Text("ویرایش", color = Burgundy, fontSize = 12.sp, modifier = Modifier.clickable {
                        selectedModel = index
                        editName = model.name
                        editMeasurements = model.measurements.toList()
                        editing = true
                        message = ""
                    }.padding(horizontal = 9.dp, vertical = 8.dp))
                    Text("حذف", color = Color(0xFFB3261E), fontSize = 12.sp, modifier = Modifier.clickable {
                        val updated = models.filterIndexed { itemIndex, _ -> itemIndex != index }
                        persist(updated)
                        selectedModel = if (updated.isEmpty()) 0 else selectedModel.coerceAtMost(updated.lastIndex)
                        editing = false
                        message = "مدل لباس حذف شد."
                    }.padding(horizontal = 9.dp, vertical = 8.dp))
                }
            }
        }

        if (editing && models.getOrNull(selectedModel) != null) {
            Spacer(Modifier.height(18.dp))
            Text("ویرایش مدل لباس", color = TextDark, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            OutlinedTextField(value = editName, onValueChange = { editName = it; message = "" }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), singleLine = true, label = { Text("نام مدل لباس") }, colors = appFieldColors(), shape = RoundedCornerShape(16.dp))
            Text("ترتیب اندازه‌گیری", color = TextDark, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
            Text("برای جابه‌جایی، انگشت خود را روی خودِ تکست‌باکس اندازه نگه دارید و آن را بالا یا پایین بکشید؛ سپس ذخیره را بزنید.", color = TextMuted, fontSize = 12.sp, modifier = Modifier.padding(bottom = 6.dp))
            var draggingMeasurementIndex by remember { mutableIntStateOf(-1) }
            var dragDistancePx by remember { mutableFloatStateOf(0f) }
            val dragThreshold = with(androidx.compose.ui.platform.LocalDensity.current) { 58.dp.toPx() }
            val latestMeasurements by rememberUpdatedState(editMeasurements)
            editMeasurements.forEachIndexed { index, measurement ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (draggingMeasurementIndex == index) BurgundySoft else Color.Transparent,
                            RoundedCornerShape(10.dp)
                        )
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = measurement,
                        onValueChange = { value -> editMeasurements = editMeasurements.toMutableList().also { it[index] = value }; message = "" },
                        modifier = Modifier
                            .weight(1f)
                            .padding(top = 5.dp)
                            .pointerInput(Unit) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        draggingMeasurementIndex = latestMeasurements.indexOf(measurement)
                                        dragDistancePx = 0f
                                    },
                                    onDragEnd = {
                                        draggingMeasurementIndex = -1
                                        dragDistancePx = 0f
                                    },
                                    onDragCancel = {
                                        draggingMeasurementIndex = -1
                                        dragDistancePx = 0f
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        dragDistancePx += dragAmount.y
                                        val currentList = latestMeasurements
                                        val currentIndex = currentList.indexOf(measurement)
                                        if (currentIndex >= 0 && dragDistancePx > dragThreshold && currentIndex < currentList.lastIndex) {
                                            val updated = currentList.toMutableList()
                                            val item = updated.removeAt(currentIndex)
                                            updated.add(currentIndex + 1, item)
                                            editMeasurements = updated
                                            draggingMeasurementIndex = currentIndex + 1
                                            dragDistancePx = 0f
                                        } else if (currentIndex > 0 && dragDistancePx < -dragThreshold) {
                                            val updated = currentList.toMutableList()
                                            val item = updated.removeAt(currentIndex)
                                            updated.add(currentIndex - 1, item)
                                            editMeasurements = updated
                                            draggingMeasurementIndex = currentIndex - 1
                                            dragDistancePx = 0f
                                        }
                                    }
                                )
                            },
                        singleLine = true,
                        label = { Text("اندازه ${index + 1}") },
                        colors = appFieldColors(),
                        shape = RoundedCornerShape(16.dp),
                    )
                    Text("حذف", color = Color(0xFFB3261E), fontSize = 12.sp, modifier = Modifier.clickable { editMeasurements = editMeasurements.filterIndexed { itemIndex, _ -> itemIndex != index }; message = "" }.padding(start = 8.dp))
                }
            }
            OutlinedTextField(value = newMeasurementName, onValueChange = { newMeasurementName = it }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), singleLine = true, label = { Text("افزودن اندازه جدید") }, colors = appFieldColors(),
                        shape = RoundedCornerShape(16.dp),)
            Button(onClick = {
                val value = newMeasurementName.trim()
                when {
                    value.isBlank() -> message = "نام اندازه را وارد کنید."
                    editMeasurements.any { it.equals(value, ignoreCase = true) } -> message = "این اندازه در مدل موجود است."
                    else -> { editMeasurements = editMeasurements + value; newMeasurementName = ""; message = "اندازه به فهرست ویرایش اضافه شد؛ برای ثبت نهایی ذخیره را بزنید." }
                }
            }, modifier = Modifier.fillMaxWidth().padding(top = 6.dp), colors = ButtonDefaults.buttonColors(containerColor = Burgundy)) { Text("افزودن اندازه") }
            Button(onClick = {
                val name = editName.trim()
                val measurements = editMeasurements.map { it.trim() }.filter { it.isNotBlank() }.distinctBy { it.lowercase() }
                when {
                    name.isBlank() -> message = "نام مدل لباس را وارد کنید."
                    models.withIndex().any { it.index != selectedModel && it.value.name.equals(name, ignoreCase = true) } -> message = "مدلی با این نام قبلاً وجود دارد."
                    else -> {
                        val updated = models.toMutableList()
                        updated[selectedModel] = GarmentModel(name, measurements)
                        persist(updated)
                        editing = false
                        newMeasurementName = ""
                        message = "تغییرات مدل و اندازه‌ها ذخیره شد."
                    }
                }
            }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), colors = ButtonDefaults.buttonColors(containerColor = Burgundy)) { Text("ذخیره تغییرات") }
            Button(onClick = { editing = false; newMeasurementName = ""; message = "" }, modifier = Modifier.fillMaxWidth().padding(top = 6.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE8E1DC), contentColor = TextDark)) { Text("لغو ویرایش") }
        }

        Spacer(Modifier.height(20.dp))
        Text("افزودن مدل جدید", color = TextDark, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        OutlinedTextField(value = newModelName, onValueChange = { newModelName = it; message = "" }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), singleLine = true, label = { Text("نام مدل لباس") }, colors = appFieldColors(),
                        shape = RoundedCornerShape(16.dp),)
        Button(onClick = {
            val name = newModelName.trim()
            when {
                name.isBlank() -> message = "نام مدل لباس را وارد کنید."
                models.any { it.name.equals(name, ignoreCase = true) } -> message = "این مدل قبلاً اضافه شده است."
                else -> {
                    val updated = models + GarmentModel(name, emptyList())
                    persist(updated)
                    selectedModel = updated.lastIndex
                    newModelName = ""
                    editing = false
                    message = "مدل جدید ذخیره شد."
                }
            }
        }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), colors = ButtonDefaults.buttonColors(containerColor = Burgundy)) { Text("افزودن و ذخیره مدل") }

        if (message.isNotBlank()) Text(message, color = if (message.contains("وارد") || message.contains("قبلاً") || message.contains("موجود است")) Color(0xFFB3261E) else Burgundy, fontSize = 12.sp, modifier = Modifier.padding(top = 12.dp))
        Spacer(Modifier.height(18.dp))
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE8E1DC), contentColor = TextDark)) { Text("برگشت به تنظیمات کارگاه") }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun RegisterSkillsForm(
    initialSkills: List<ShopSkill>,
    onBack: () -> Unit,
    onSave: (List<ShopSkill>) -> Unit
) {
    var skills by remember { mutableStateOf(initialSkills) }
    var newSkill by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp)) {
        Text("ثبت مهارت‌ها", color = BurgundyDark, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("مالک می‌تواند کارهایی را که در کارگاه انجام می‌شود تعریف کند.", color = TextMuted, fontSize = 13.sp, modifier = Modifier.padding(top = 5.dp, bottom = 16.dp))
        OutlinedTextField(value = newSkill, onValueChange = { newSkill = it; error = false }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("نام مهارت یا نوع کار") }, colors = appFieldColors(),
                        shape = RoundedCornerShape(16.dp),)
        if (error) Text("نام مهارت را وارد کنید.", color = Color(0xFFB3261E), fontSize = 12.sp)
        Button(onClick = {
            val value = newSkill.trim()
            if (value.isBlank()) error = true
            else if (skills.none { it.name.equals(value, ignoreCase = true) }) {
                skills = skills + ShopSkill(value)
                newSkill = ""
                error = false
            } else {
                newSkill = ""
                error = false
            }
        }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), colors = ButtonDefaults.buttonColors(containerColor = Burgundy)) { Text("افزودن مهارت") }

        Spacer(Modifier.height(18.dp))
        Text("مهارت‌های کارگاه", color = TextDark, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        if (skills.isEmpty()) Text("هنوز مهارتی ثبت نشده است.", color = TextMuted, fontSize = 13.sp, modifier = Modifier.padding(top = 10.dp))
        skills.forEach { skill ->
            Row(Modifier.fillMaxWidth().padding(top = 8.dp).background(Color.White, RoundedCornerShape(10.dp)).padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(skill.name, color = TextDark, fontSize = 14.sp, modifier = Modifier.weight(1f))
                Text("حذف", color = Color(0xFFB3261E), fontSize = 13.sp, modifier = Modifier.clickable { skills = skills.filterNot { it == skill } }.padding(8.dp))
            }
        }
        Spacer(Modifier.height(18.dp))
        Button(onClick = { onSave(skills) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Burgundy)) { Text("ذخیره تغییرات") }
        Spacer(Modifier.height(8.dp))
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE8E1DC), contentColor = TextDark)) { Text("برگشت") }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun RegisterTailorForm(availableSkills: List<String>, onBack: () -> Unit, onSave: (TailorRecord) -> Unit) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var selectedSkills by remember { mutableStateOf(setOf<String>()) }
    var error by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), horizontalAlignment = Alignment.Start) {
        Text("ثبت خیاط", color = BurgundyDark, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("معلومات خیاط و بخش‌های کاری او را وارد کنید.", color = TextMuted, fontSize = 13.sp, modifier = Modifier.padding(top = 5.dp, bottom = 16.dp))
        OutlinedTextField(value = name, onValueChange = { name = it; error = false }, modifier = Modifier.fillMaxWidth(), label = { Text("نام خیاط") }, singleLine = true, colors = appFieldColors(),
                        shape = RoundedCornerShape(16.dp),)
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(value = phone, onValueChange = { phone = it; error = false }, modifier = Modifier.fillMaxWidth(), label = { Text("شماره تماس") }, singleLine = true, colors = appFieldColors(),
                        shape = RoundedCornerShape(16.dp),)
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(value = address, onValueChange = { address = it }, modifier = Modifier.fillMaxWidth(), label = { Text("آدرس") }, minLines = 2, colors = appFieldColors(),
                        shape = RoundedCornerShape(16.dp),)
        Spacer(Modifier.height(18.dp))
        Text("در کدام بخش‌های خیاطی مهارت دارد؟", color = TextDark, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        if (availableSkills.isEmpty()) {
            Text("ابتدا در تنظیمات بخش «ثبت مهارت‌ها» مهارت‌های کارگاه را تعریف کنید.", color = TextMuted, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
        }
        availableSkills.forEach { skill ->
            Row(Modifier.fillMaxWidth().clickable { selectedSkills = if (skill in selectedSkills) selectedSkills - skill else selectedSkills + skill }.padding(vertical = 1.dp), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = skill in selectedSkills, onCheckedChange = { checked -> selectedSkills = if (checked) selectedSkills + skill else selectedSkills - skill })
                Text(skill, color = TextDark, fontSize = 14.sp)
            }
        }
        if (error) Text("لطفاً نام، شماره تماس و حداقل یک مهارت را وارد کنید.", color = Color(0xFFB3261E), fontSize = 12.sp, modifier = Modifier.padding(vertical = 8.dp))
        Spacer(Modifier.height(12.dp))
        Button(onClick = {
            if (name.isBlank() || phone.isBlank() || selectedSkills.isEmpty()) error = true
            else onSave(TailorRecord(name.trim(), phone.trim(), address.trim(), selectedSkills.toList()))
        }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Burgundy)) { Text("ذخیره خیاط") }
        Spacer(Modifier.height(8.dp))
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE8E1DC), contentColor = TextDark)) { Text("برگشت") }
        Spacer(Modifier.height(24.dp))
    }
}


@Composable
private fun OrderRegistrationScreen(database: LocalDatabase, onBack: () -> Unit, onSave: (TailorOrder) -> Unit) {
    var customerName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var customerMenuExpanded by remember { mutableStateOf(false) }
    val savedCustomers = remember(database.revision.intValue) { database.getCustomers() }
    var notes by remember { mutableStateOf("") }
    var deliveryDate by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var price by remember { mutableStateOf("") }
    var paid by remember { mutableStateOf("") }
    var selectedModelName by remember { mutableStateOf("") }
    var measurements by remember { mutableStateOf(mapOf<String, String>()) }
    var message by remember { mutableStateOf("") }
    var modelMenuExpanded by remember { mutableStateOf(false) }
    val models = remember(database) { database.getGarmentModels() }
    val selectedModel = models.firstOrNull { it.name == selectedModelName }
    val total = price.toEnglishDigits().toDoubleOrNull() ?: 0.0
    val payment = paid.toEnglishDigits().toDoubleOrNull() ?: 0.0
    val remaining = (total - payment).coerceAtLeast(0.0)
    val remainingText = (if (remaining % 1.0 == 0.0) remaining.toLong().toString() else remaining.toString()).toPersianDigits()
    val fieldShape = RoundedCornerShape(14.dp)

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Box(Modifier.fillMaxWidth().height(52.dp), contentAlignment = Alignment.Center) {
                Text("ثبت سفارش", color = BurgundyDark, fontSize = 23.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Button(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Burgundy)) {
                    Text("‹", fontSize = 24.sp)
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Box(Modifier.fillMaxWidth()) {
            Button(onClick = { customerMenuExpanded = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = BurgundySoft, contentColor = BurgundyDark), shape = RoundedCornerShape(12.dp)) {
                Text(if (savedCustomers.isEmpty()) "انتخاب مشتری ثبت‌شده (فهرست خالی است)" else "انتخاب مشتری ثبت‌شده  ▾")
            }
            androidx.compose.material3.DropdownMenu(expanded = customerMenuExpanded, onDismissRequest = { customerMenuExpanded = false }) {
                savedCustomers.forEach { savedCustomer ->
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text(savedCustomer.name + if (savedCustomer.phone.isBlank()) "" else " · " + savedCustomer.phone) },
                        onClick = {
                            customerName = savedCustomer.name
                            phone = savedCustomer.phone
                            address = savedCustomer.address
                            customerMenuExpanded = false
                            message = ""
                        }
                    )
                }
            }
        }
        OutlinedTextField(value = customerName, onValueChange = { customerName = it; message = "" }, modifier = Modifier.fillMaxWidth(), label = { Text("اسم مشتری") }, singleLine = true, colors = appFieldColors(), shape = fieldShape)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = phone, onValueChange = { phone = it }, modifier = Modifier.fillMaxWidth(), label = { Text("شماره تماس") }, singleLine = true, colors = appFieldColors(), shape = fieldShape)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = address, onValueChange = { address = it }, modifier = Modifier.fillMaxWidth(), label = { Text("آدرس مشتری (اختیاری)") }, singleLine = true, colors = appFieldColors(), shape = fieldShape)
        Text("انتخاب مدل لباس", color = BurgundyDark, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
        Box {
            OutlinedTextField(
                value = selectedModelName,
                onValueChange = {},
                readOnly = true,
                enabled = models.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().clickable { if (models.isNotEmpty()) modelMenuExpanded = true },
                label = { Text("مدل لباس") },
                placeholder = { Text(if (models.isEmpty()) "ابتدا مدل لباس را در تنظیمات ثبت کنید" else "یک مدل انتخاب کنید") },
                trailingIcon = { Text("▾", color = BurgundyDark, modifier = Modifier.clickable { if (models.isNotEmpty()) modelMenuExpanded = true }.padding(horizontal = 12.dp)) },
                colors = appFieldColors(),
                shape = fieldShape
            )
            Box(Modifier.matchParentSize().clickable { if (models.isNotEmpty()) modelMenuExpanded = true })
            androidx.compose.material3.DropdownMenu(
                expanded = modelMenuExpanded,
                onDismissRequest = { modelMenuExpanded = false },
                modifier = Modifier.fillMaxWidth(0.9f).background(Color.White)
            ) {
                models.forEach { model ->
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text(model.name, color = TextDark) },
                        onClick = {
                            selectedModelName = model.name
                            val savedMeasurements = database.getLatestCustomerMeasurements(customerName, phone, model.name)
                            measurements = model.measurements.associateWith { field ->
                                savedMeasurements[field] ?: measurements[field] ?: ""
                            }
                            modelMenuExpanded = false
                            message = ""
                        }
                    )
                }
            }
        }
        if (selectedModel != null) {
            Text("اندازه‌های لباس", color = BurgundyDark, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
            selectedModel.measurements.forEach { measurement ->
                OutlinedTextField(value = measurements[measurement] ?: "", onValueChange = { value -> measurements = measurements.toMutableMap().also { it[measurement] = value } }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), label = { Text(measurement) }, singleLine = true, colors = appFieldColors(), shape = fieldShape)
            }
        }
        OutlinedTextField(value = notes, onValueChange = { notes = it }, modifier = Modifier.fillMaxWidth(), label = { Text("توضیحات بیشتر") }, minLines = 3, colors = appFieldColors(), shape = fieldShape)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = deliveryDate,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true },
            label = { Text("تاریخ تحویل سفارش (هجری شمسی)") },
            placeholder = { Text("از تقویم انتخاب کنید") },
            trailingIcon = { androidx.compose.material3.TextButton(onClick = { showDatePicker = true }) { Text("▦", color = BurgundyDark, fontSize = 20.sp) } },
            singleLine = true,
            colors = appFieldColors(),
            shape = fieldShape
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = price, onValueChange = { price = it.filter { ch -> ch.isDigit() || ch == '.' } }, modifier = Modifier.fillMaxWidth(), label = { Text("قیمت") }, singleLine = true, colors = appFieldColors(), shape = fieldShape)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = paid, onValueChange = { paid = it.filter { ch -> ch.isDigit() || ch == '.' } }, modifier = Modifier.fillMaxWidth(), label = { Text("پرداخت") }, singleLine = true, colors = appFieldColors(), shape = fieldShape)
        Text("باقیمانده: $remainingText", color = if (remaining > 0) Color(0xFFB33A32) else Color(0xFF237342), fontSize = 19.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth().background(if (remaining > 0) Color(0xFFFFE9E6) else Color(0xFFE4F4E8), RoundedCornerShape(14.dp)).padding(horizontal = 16.dp, vertical = 16.dp))
        if (message.isNotBlank()) Text(message, color = Color(0xFFB33A32), fontSize = 13.sp)
        Button(onClick = {
            when {
                customerName.isBlank() -> message = "اسم مشتری را وارد کنید."
                selectedModel == null -> message = "مدل لباس را انتخاب کنید."
                deliveryDate.isBlank() -> message = "تاریخ تحویل را انتخاب کنید."
                total <= 0 -> message = "قیمت را وارد کنید."
                payment < 0 || payment > total -> message = "مقدار پرداخت نباید از قیمت بیشتر باشد."
                else -> {
                    val order = TailorOrder(customerName.trim(), phone.trim(), selectedModel.name, measurements, notes.trim(), deliveryDate.trim(), total, 0.0, payment, address.trim())
                    runCatching {
                        database.addOrder(order)
                    }.onSuccess { onSave(order) }.onFailure { message = "ذخیره سفارش ناموفق بود: ${it.localizedMessage}" }
                }
            }
        }, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = Burgundy)) { Text("ثبت سفارش", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
        Spacer(Modifier.height(24.dp))
    }

    if (showDatePicker) {
        JalaliDatePickerDialog(
            initialDate = deliveryDate,
            onDismiss = { showDatePicker = false },
            onDateSelected = { selected -> deliveryDate = selected; showDatePicker = false }
        )
    }
}


@Composable
private fun OrderSuccessScreen(order: TailorOrder, onPrint: () -> Unit, onDone: () -> Unit) {
    val remaining = (order.price - order.paid).coerceAtLeast(0.0)
    Column(Modifier.fillMaxSize().background(PageBg).verticalScroll(rememberScrollState()).padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(82.dp).background(Color(0xFFE4F4E8), CircleShape), contentAlignment = Alignment.Center) {
            Text("✓", color = Color(0xFF237342), fontSize = 48.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(18.dp))
        Text("سفارش با موفقیت ثبت شد", color = BurgundyDark, fontSize = 23.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text("رسید سفارش را می‌توانید چاپ کنید.", color = TextMuted, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp, bottom = 22.dp))
        Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(18.dp)).padding(18.dp)) {
            ReceiptRow("مشتری", order.customerName)
            ReceiptRow("شماره تماس", order.phone.ifBlank { "—" })
            ReceiptRow("مدل لباس", order.garmentModel)
            ReceiptRow("تاریخ تحویل", order.deliveryDate)
            ReceiptRow("قیمت", formatReceiptAmount(order.price))
            ReceiptRow("پرداخت", formatReceiptAmount(order.paid))
            ReceiptRow("باقیمانده", formatReceiptAmount(remaining))
        }
        Spacer(Modifier.height(22.dp))
        Button(onClick = onPrint, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = Burgundy)) { Text("چاپ رسید", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(10.dp))
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE8E1DC), contentColor = TextDark)) { Text("بستن") }
    }
}

@Composable
private fun ReceiptRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = TextMuted, fontSize = 14.sp)
        Text(value, color = TextDark, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

private fun formatReceiptAmount(value: Double): String =
    (if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()).toPersianDigits()

private fun escapeReceiptHtml(value: String): String =
    value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

private fun printOrderReceipt(context: Context, order: TailorOrder) {
    val remaining = (order.price - order.paid).coerceAtLeast(0.0)
    val html = """
        <html><head><meta charset="utf-8"><style>
        body{font-family:sans-serif;direction:rtl;padding:28px;color:#302627}
        h1{text-align:center;color:#702027;font-size:24px}
        .sub{text-align:center;color:#7D7070;margin-bottom:24px}
        table{width:100%;border-collapse:collapse}td{padding:12px 8px;border-bottom:1px solid #D9C6C5}
        td:first-child{color:#7D7070;width:38%}.total{font-weight:bold;color:#702027}
        .footer{text-align:center;margin-top:30px;color:#7D7070;font-size:12px}
        </style></head><body>
        <h1>خیاطی من</h1><div class="sub">رسید ثبت سفارش</div>
        <table>
        <tr><td>نام مشتری</td><td>${escapeReceiptHtml(order.customerName)}</td></tr>
        <tr><td>شماره تماس</td><td>${escapeReceiptHtml(order.phone.ifBlank { "—" })}</td></tr>
        <tr><td>مدل لباس</td><td>${escapeReceiptHtml(order.garmentModel)}</td></tr>
        <tr><td>تاریخ تحویل</td><td>${escapeReceiptHtml(order.deliveryDate)}</td></tr>
        <tr><td>قیمت</td><td>${formatReceiptAmount(order.price)}</td></tr>
        <tr><td>پرداخت</td><td>${formatReceiptAmount(order.paid)}</td></tr>
        <tr class="total"><td>باقیمانده</td><td>${formatReceiptAmount(remaining)}</td></tr>
        </table><div class="footer">با تشکر از اعتماد شما</div>
        </body></html>
    """.trimIndent()
    val webView = WebView(context)
    webView.settings.defaultTextEncodingName = "UTF-8"
    webView.webViewClient = object : WebViewClient() {
        override fun onPageFinished(view: WebView, url: String?) {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
            printManager.print("MyTailor-Receipt", view.createPrintDocumentAdapter("رسید سفارش"), PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4).build())
        }
    }
    webView.loadDataWithBaseURL(null, html, "text/HTML", "UTF-8", null)
}

@Composable
internal fun JalaliDatePickerDialog(
    initialDate: String,
    onDismiss: () -> Unit,
    onDateSelected: (String) -> Unit
) {
    val months = listOf("حمل", "ثور", "جوزا", "سرطان", "اسد", "سنبله", "میزان", "عقرب", "قوس", "جدی", "دلو", "حوت")
    val weekdays = listOf("ش", "ی", "د", "س", "چ", "پ", "ج")
    val today = remember { gregorianToJalali(java.util.Calendar.getInstance()) }
    val initialParts = initialDate.toEnglishDigits().split("/")
    var year by remember { mutableIntStateOf(initialParts.getOrNull(0)?.toIntOrNull() ?: today.first) }
    var month by remember { mutableIntStateOf((initialParts.getOrNull(1)?.toIntOrNull() ?: today.second) - 1) }
    var day by remember { mutableIntStateOf(initialParts.getOrNull(2)?.toIntOrNull() ?: today.third) }
    val daysInMonth = if (month < 6) 31 else if (month < 11) 30 else if (isJalaliLeapYear(year)) 30 else 29
    val firstDayOffset = remember(year, month) {
        val gregorian = jalaliToGregorian(year, month + 1, 1)
        val cal = java.util.GregorianCalendar(gregorian.first, gregorian.second - 1, gregorian.third)
        (cal.get(java.util.Calendar.DAY_OF_WEEK) + 1) % 7
    }
    val selectedDay = day.coerceIn(1, daysInMonth)

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().background(PageBg, RoundedCornerShape(24.dp)).padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("انتخاب تاریخ هجری شمسی", color = BurgundyDark, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                androidx.compose.material3.TextButton(onClick = {
                    if (month == 0) { month = 11; year-- } else month--
                    day = 1
                }) { Text("‹", color = Burgundy, fontSize = 26.sp) }
                Text("${months[month]} $year".toPersianDigits(), color = BurgundyDark, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                androidx.compose.material3.TextButton(onClick = {
                    if (month == 11) { month = 0; year++ } else month++
                    day = 1
                }) { Text("›", color = Burgundy, fontSize = 26.sp) }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                weekdays.forEach { weekday ->
                    Box(Modifier.weight(1f).height(34.dp), contentAlignment = Alignment.Center) {
                        Text(weekday, color = Burgundy, fontWeight = FontWeight.Bold)
                    }
                }
            }
            val rows = (firstDayOffset + daysInMonth + 6) / 7
            repeat(rows) { rowIndex ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    repeat(7) { columnIndex ->
                        val dateNumber = rowIndex * 7 + columnIndex - firstDayOffset + 1
                        Box(Modifier.weight(1f).height(42.dp), contentAlignment = Alignment.Center) {
                            if (dateNumber in 1..daysInMonth) {
                                val isSelected = dateNumber == selectedDay
                                Box(
                                    Modifier.fillMaxSize().padding(2.dp)
                                        .background(if (isSelected) Burgundy else Color.Transparent, RoundedCornerShape(12.dp))
                                        .clickable { day = dateNumber },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(dateNumber.toString().toPersianDigits(), color = if (isSelected) Color.White else TextDark, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onDismiss, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE8E1DC), contentColor = TextDark)) { Text("لغو") }
                Button(onClick = { onDateSelected("$year/${(month + 1).toString().padStart(2, '0')}/${selectedDay.toString().padStart(2, '0')}".toPersianDigits()) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Burgundy)) { Text("انتخاب تاریخ") }
            }
        }
    }
}

internal fun String.toEnglishDigits(): String = map { ch ->
    when (ch) {
        in '۰'..'۹' -> ('0'.code + (ch - '۰')).toChar()
        in '٠'..'٩' -> ('0'.code + (ch - '٠')).toChar()
        else -> ch
    }
}.joinToString("")

private fun isJalaliLeapYear(year: Int): Boolean {
    val current = jalaliToGregorian(year, 1, 1)
    val next = jalaliToGregorian(year + 1, 1, 1)
    return java.util.GregorianCalendar(next.first, next.second - 1, next.third).timeInMillis -
        java.util.GregorianCalendar(current.first, current.second - 1, current.third).timeInMillis > 365L * 24 * 60 * 60 * 1000
}

internal fun jalaliToGregorian(jyInput: Int, jm: Int, jd: Int): Triple<Int, Int, Int> {
    var jy = jyInput + 1595
    var days = -355668 + 365 * jy + (jy / 33) * 8 + ((jy % 33 + 3) / 4) + jd
    days += if (jm < 7) (jm - 1) * 31 else (jm - 7) * 30 + 186
    var gy = 400 * (days / 146097)
    days %= 146097
    if (days > 36524) {
        gy += 100 * (--days / 36524)
        days %= 36524
        if (days >= 365) days++
    }
    gy += 4 * (days / 1461)
    days %= 1461
    if (days > 365) {
        gy += (days - 1) / 365
        days = (days - 1) % 365
    }
    var gd = days + 1
    val leap = gy % 4 == 0 && (gy % 100 != 0 || gy % 400 == 0)
    val gDays = intArrayOf(0, 31, if (leap) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
    var gm = 1
    while (gm <= 12 && gd > gDays[gm]) {
        gd -= gDays[gm]
        gm++
    }
    return Triple(gy, gm, gd)
}

internal fun gregorianToJalali(calendar: java.util.Calendar): Triple<Int, Int, Int> {
    val gy = calendar.get(java.util.Calendar.YEAR)
    val gm = calendar.get(java.util.Calendar.MONTH) + 1
    val gd = calendar.get(java.util.Calendar.DAY_OF_MONTH)
    val gDaysInMonth = intArrayOf(0, 31, if (gy % 4 == 0 && (gy % 100 != 0 || gy % 400 == 0)) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
    var days = 355666 + 365 * gy + (gy + 3) / 4 - (gy + 99) / 100 + (gy + 399) / 400 + gd
    for (i in 1 until gm) days += gDaysInMonth[i]
    var jy = -1595 + 33 * (days / 12053)
    days %= 12053
    jy += 4 * (days / 1461)
    days %= 1461
    if (days > 365) {
        jy += (days - 1) / 365
        days = (days - 1) % 365
    }
    val jm: Int
    val jd: Int
    if (days < 186) {
        jm = 1 + days / 31
        jd = 1 + days % 31
    } else {
        jm = 7 + (days - 186) / 30
        jd = 1 + (days - 186) % 30
    }
    return Triple(jy, jm, jd)
}


internal fun isoDateToJalali(iso: String): String {
    val parts = iso.toEnglishDigits().split("-")
    if (parts.size != 3) return iso
    val gy = parts[0].toIntOrNull() ?: return iso
    val gm = parts[1].toIntOrNull() ?: return iso
    val gd = parts[2].toIntOrNull() ?: return iso
    val cal = java.util.GregorianCalendar(gy, gm - 1, gd)
    val j = gregorianToJalali(cal)
    return "${j.first}/${j.second.toString().padStart(2,'0')}/${j.third.toString().padStart(2,'0')}".toPersianDigits()
}

internal fun jalaliDateToIso(jalali: String): String {
    val parts = jalali.toEnglishDigits().split("/", "-")
    if (parts.size != 3) return jalali
    val jy = parts[0].toIntOrNull() ?: return jalali
    val jm = parts[1].toIntOrNull() ?: return jalali
    val jd = parts[2].toIntOrNull() ?: return jalali
    val g = jalaliToGregorian(jy, jm, jd)
    return "${g.first}-${g.second.toString().padStart(2,'0')}-${g.third.toString().padStart(2,'0')}"
}

private fun String.toPersianDigits(): String = map { ch -> if (ch in '0'..'9') ('۰'.code + (ch - '0')).toChar() else ch }.joinToString("")

@Composable
private fun DashboardContent(database: LocalDatabase, onOpenDeliveries: () -> Unit) {
    val orderCounts = remember(database.revision.intValue) { database.getOrderCounts() }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(
            Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Burgundy, BurgundyDark))).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("☰", color = Color.White, fontSize = 22.sp)
            Text("خیاطی من", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            Text("♧", color = Color.White, fontSize = 22.sp)
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 20.dp)) {
            OrderSummaryCard(
                title = "تعداد کل سفارش‌ها",
                count = orderCounts.first.toString().toPersianDigits(),
                subtitle = "",
                background = Color(0xFFE8EEF9),
                accent = Color(0xFF2856A6),
                onClick = onOpenDeliveries
            )
            Spacer(Modifier.height(10.dp))
            OrderSummaryCard(
                title = "تعداد سفارش‌های آماده",
                count = orderCounts.second.toString().toPersianDigits(),
                subtitle = "",
                background = Color(0xFFE4F4E8),
                accent = Color(0xFF237342),
                onClick = onOpenDeliveries
            )
            Spacer(Modifier.height(10.dp))
            OrderSummaryCard(
                title = "تعداد سفارش‌های تکمیل‌نشده",
                count = orderCounts.third.toString().toPersianDigits(),
                subtitle = "",
                background = Color(0xFFFFE9E6),
                accent = Color(0xFFB33A32),
                onClick = onOpenDeliveries
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun OrderSummaryCard(
    title: String,
    count: String,
    subtitle: String,
    background: Color,
    accent: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(background, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 17.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = accent, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = TextDark, fontSize = 12.sp, modifier = Modifier.padding(top = 5.dp))
        }
        Spacer(Modifier.width(12.dp))
        Box(
            modifier = Modifier.size(58.dp).background(Color.White.copy(alpha = 0.85f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(count, color = accent, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun DeliveryReportCard(
    title: String,
    description: String,
    count: String,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(modifier = modifier.background(Color.White, RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = TextDark, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(description, color = TextMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 5.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(count, color = accent, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold)
            Text("سفارش", color = TextMuted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun StatCard(number: String, title: String, symbol: String, tile: Color, icon: Color, modifier: Modifier) {
    Row(modifier.height(74.dp).background(tile, RoundedCornerShape(13.dp)).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(number, color = icon, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            Text(title, color = TextDark, fontSize = 11.sp, maxLines = 1)
        }
        Box(Modifier.size(32.dp).background(icon.copy(alpha = 0.14f), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
            Text(symbol, color = icon, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun RecentOrder(name: String, work: String, date: String, status: String, emoji: String, statusColor: Color) {
    Row(Modifier.fillMaxWidth().padding(bottom = 8.dp).background(Color.White, RoundedCornerShape(12.dp)).padding(9.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(54.dp).background(Color(0xFFF0EAE5), RoundedCornerShape(9.dp)), contentAlignment = Alignment.Center) { Text(emoji, fontSize = 28.sp) }
        Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
            Text(name, color = TextDark, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(work, color = TextMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 3.dp))
            Text(date, color = TextMuted, fontSize = 10.sp, modifier = Modifier.padding(top = 3.dp))
        }
        Text(status, color = TextDark, fontSize = 10.sp, modifier = Modifier.background(statusColor, RoundedCornerShape(18.dp)).padding(horizontal = 8.dp, vertical = 6.dp))
    }
}

@Composable
private fun ActiveOrdersScreen(database: LocalDatabase, onOpenWorkflow: () -> Unit = {}, onAddOrder: () -> Unit = {}) {
    var search by remember { mutableStateOf("") }
    var refreshKey by remember { mutableIntStateOf(0) }
    var pendingOrder by remember { mutableStateOf<DeliveryOrder?>(null) }
    val orders = remember(search, refreshKey, database.revision.intValue) { database.getActiveOrders(search) }

    Column(Modifier.fillMaxSize().background(PageBg).padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text("سفارشات فعال", color = BurgundyDark, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("سفارش‌ها و مراحل تولید", color = TextMuted, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
            }
            TextButton(onClick = onOpenWorkflow) { Text("مراحل تولید", color = Burgundy, fontSize = 12.sp) }
        }
        Button(onClick = onAddOrder, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), colors = ButtonDefaults.buttonColors(containerColor = Burgundy), shape = RoundedCornerShape(12.dp)) { Text("+ ثبت سفارش جدید") }
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("جستجو با نام یا شماره تماس مشتری") },
            colors = appFieldColors(),
            shape = RoundedCornerShape(16.dp)
        )
        Spacer(Modifier.height(12.dp))
        if (orders.isEmpty()) {
            Text(if (search.isBlank()) "سفارش فعال وجود ندارد." else "سفارشی با این نام یا شماره تماس پیدا نشد.", color = TextMuted, modifier = Modifier.padding(top = 16.dp))
        } else {
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
                orders.forEach { order ->
                    Column(Modifier.fillMaxWidth().padding(bottom = 10.dp).background(CardBg, RoundedCornerShape(14.dp)).padding(14.dp)) {
                        Text(order.customerName, color = BurgundyDark, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("شماره تماس: ${order.phone}", color = TextMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 3.dp))
                        Text("مدل لباس: ${order.garmentModel}", color = TextDark, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
                        Text("شماره سفارش: ${order.id.toString().toPersianDigits()}", color = TextMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 3.dp))
                        Text("وضعیت: در حال انجام", color = Color(0xFFB7791F), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp))
                        Button(
                            onClick = { pendingOrder = order },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Burgundy),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text("تغییر به تکمیل‌شده") }
                    }
                }
            }
        }
    }
    if (pendingOrder != null) {
        val order = pendingOrder!!
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { pendingOrder = null },
            title = { Text("تأیید تکمیل سفارش") },
            text = { Text("آیا سفارش «${order.garmentModel}» برای مشتری ${order.customerName} تکمیل شده و آماده تحویل است؟") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    database.updateOrderStatus(order.id, "ready")
                    pendingOrder = null
                    refreshKey++
                }) { Text("بله، تکمیل شده", color = Burgundy) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { pendingOrder = null }) { Text("انصراف") }
            }
        )
    }
}

@Composable
private fun CustomerListScreen(database: LocalDatabase) {
    var search by remember { mutableStateOf("") }
    var selectedCustomer by remember { mutableStateOf<CustomerSummary?>(null) }
    var activeOnly by remember { mutableStateOf(false) }
    var refreshKey by remember { mutableIntStateOf(0) }
    var showAddCustomer by remember { mutableStateOf(false) }
    var newCustomerName by remember { mutableStateOf("") }
    var newCustomerPhone by remember { mutableStateOf("") }
    var newCustomerAddress by remember { mutableStateOf("") }
    var addCustomerMessage by remember { mutableStateOf("") }
    val customers = remember(search, refreshKey, database.revision.intValue) { database.getCustomers(search.toEnglishDigits()) }
    if (showAddCustomer) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("ثبت مشتری جدید", color = BurgundyDark, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                TextButton(onClick = { showAddCustomer = false }) { Text("برگشت", color = Burgundy) }
            }
            OutlinedTextField(newCustomerName, { newCustomerName = it; addCustomerMessage = "" }, Modifier.fillMaxWidth(), label = { Text("نام مشتری *") }, singleLine = true, colors = appFieldColors(), shape = RoundedCornerShape(14.dp))
            OutlinedTextField(newCustomerPhone, { newCustomerPhone = it }, Modifier.fillMaxWidth(), label = { Text("شماره تماس") }, singleLine = true, colors = appFieldColors(), shape = RoundedCornerShape(14.dp))
            OutlinedTextField(newCustomerAddress, { newCustomerAddress = it }, Modifier.fillMaxWidth(), label = { Text("آدرس") }, singleLine = true, colors = appFieldColors(), shape = RoundedCornerShape(14.dp))
            Button(onClick = {
                if (database.saveCustomer(newCustomerName, newCustomerPhone, newCustomerAddress)) {
                    showAddCustomer = false; search = newCustomerName; newCustomerName = ""; newCustomerPhone = ""; newCustomerAddress = ""; addCustomerMessage = ""
                } else addCustomerMessage = "نام مشتری را وارد کنید."
            }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Burgundy), shape = RoundedCornerShape(12.dp)) { Text("ذخیره مشتری") }
            if (addCustomerMessage.isNotBlank()) Text(addCustomerMessage, color = BurgundyDark, fontSize = 12.sp)
        }
        return
    }
    if (selectedCustomer != null) {
        CustomerOrderHistoryScreen(database, selectedCustomer!!, activeOnly) { selectedCustomer = null }
        return
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("لیست مشتریان", color = BurgundyDark, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Button(onClick = { showAddCustomer = true }, colors = ButtonDefaults.buttonColors(containerColor = Burgundy), shape = RoundedCornerShape(10.dp), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) { Text("+ مشتری", fontSize = 12.sp) }
        }
        OutlinedTextField(value = search, onValueChange = { search = it }, modifier = Modifier.fillMaxWidth(), label = { Text("جستجو با نام یا شماره تلفن") }, singleLine = true, trailingIcon = {
            if (search.isNotEmpty()) androidx.compose.material3.TextButton(onClick = { search = "" }) { Text("پاک", color = Burgundy) }
            else Text("⌕", color = BurgundyDark, fontSize = 24.sp, modifier = Modifier.padding(horizontal = 12.dp))
        }, colors = appFieldColors(), shape = RoundedCornerShape(16.dp))
        Text("تعداد مشتریان: ${customers.size.toString().toPersianDigits()}", color = TextMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 5.dp, bottom = 6.dp))
        if (customers.isEmpty()) {
            Text(if (search.isBlank()) "هنوز سفارشی ثبت نشده است" else "مشتری پیدا نشد", color = BurgundyDark, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 28.dp))
        } else {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                customers.forEachIndexed { index, customer ->
                    Column(Modifier.fillMaxWidth().padding(bottom = 5.dp).background(if (index % 2 == 0) Color.White else BurgundySoft.copy(alpha = 0.55f), RoundedCornerShape(10.dp)).padding(7.dp)) {
                        Row(
                            Modifier.fillMaxWidth().clickable { selectedCustomer = customer; activeOnly = false },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text((index + 1).toString().toPersianDigits(), color = Burgundy, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text(customer.name, Modifier.weight(1f), color = TextDark, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            Text(customer.phone.ifBlank { "ثبت نشده" }, color = TextMuted, fontSize = 10.sp, maxLines = 1)
                        }
                        Row(Modifier.fillMaxWidth().padding(top = 5.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(Modifier.weight(1f).background(BurgundySoft, RoundedCornerShape(7.dp)).clickable { selectedCustomer = customer; activeOnly = true }.padding(horizontal = 7.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("سفارش‌های فعال", color = TextMuted, fontSize = 10.sp)
                                Text(customer.activeOrderCount.toString().toPersianDigits(), color = BurgundyDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(Modifier.weight(1f).background(Color(0xFFE8EEF9), RoundedCornerShape(7.dp)).clickable { selectedCustomer = customer; activeOnly = false }.padding(horizontal = 7.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("کل سفارشات", color = Color(0xFF526A91), fontSize = 10.sp)
                                Text(customer.orderCount.toString().toPersianDigits(), color = Color(0xFF2856A6), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (customer.lastDeliveryDate.isNotBlank()) {
                            Text("آخرین تاریخ تحویل: ${customer.lastDeliveryDate}", color = TextMuted, fontSize = 9.sp, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomerOrderHistoryScreen(database: LocalDatabase, customer: CustomerSummary, activeOnly: Boolean, onBack: () -> Unit) {
    var selectedOrder by remember { mutableStateOf<CustomerOrderHistory?>(null) }
    var refreshKey by remember { mutableIntStateOf(0) }
    var editCustomer by remember { mutableStateOf(false) }
    var confirmCustomerSave by remember { mutableStateOf(false) }
    var confirmCustomerDelete by remember { mutableStateOf(false) }
    var pendingMeasurements by remember { mutableStateOf<Map<String, String>?>(null) }
    var statusOrder by remember { mutableStateOf<CustomerOrderHistory?>(null) }
    var pendingStatus by remember { mutableStateOf<String?>(null) }
    var pendingCustomerName by remember { mutableStateOf(customer.name) }
    var pendingCustomerPhone by remember { mutableStateOf(customer.phone) }
    var pendingCustomerAddress by remember { mutableStateOf(customer.address) }
    val orders = remember(customer.name, customer.phone, activeOnly, refreshKey, database.revision.intValue) {
        database.getCustomerOrders(customer.name, customer.phone, activeOnly)
    }
    val measurementHistory = remember(customer.name, customer.phone, database.revision.intValue) {
        database.getCustomerMeasurementHistory(customer.name, customer.phone)
    }
    if (selectedOrder != null) {
        MeasurementEditorScreen(
            order = selectedOrder!!,
            onBack = { selectedOrder = null },
            onSave = { measurements -> pendingMeasurements = measurements }
        )
        return
    }
    if (editCustomer) {
        CustomerDetailsEditor(
            customer = customer,
            onBack = { editCustomer = false },
            onSaveRequest = { newName, newPhone, newAddress ->
                pendingCustomerName = newName
                pendingCustomerPhone = newPhone
                pendingCustomerAddress = newAddress
                confirmCustomerSave = true
            }
        )
        if (confirmCustomerSave) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { confirmCustomerSave = false },
                title = { Text("تأیید تغییر مشخصات") },
                text = { Text("آیا می‌خواهید مشخصات مشتری را در تمام سفارش‌های او تغییر دهید؟") },
                confirmButton = { androidx.compose.material3.TextButton(onClick = {
                    database.updateCustomerDetails(customer.name, customer.phone, pendingCustomerName, pendingCustomerPhone, pendingCustomerAddress)
                    confirmCustomerSave = false
                    editCustomer = false
                    onBack()
                }) { Text("بله، ذخیره شود", color = Burgundy) } },
                dismissButton = { androidx.compose.material3.TextButton(onClick = { confirmCustomerSave = false }) { Text("انصراف") } }
            )
        }
        return
    }
    if (confirmCustomerDelete) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirmCustomerDelete = false },
            title = { Text("حذف مشتری") },
            text = { Text("آیا مطمئن هستید؟ تمام سفارش‌های این مشتری نیز حذف می‌شود و این کار قابل برگشت نیست.") },
            confirmButton = { androidx.compose.material3.TextButton(enabled = customer.activeOrderCount == 0 && customer.remaining <= 0, onClick = {
                if (customer.activeOrderCount == 0 && customer.remaining <= 0) {
                    database.deleteCustomerOrders(customer.name, customer.phone)
                    confirmCustomerDelete = false
                    onBack()
                } else {
                    confirmCustomerDelete = false
                }
            }) { Text("بله، حذف شود", color = Color(0xFFB33A32)) } },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { confirmCustomerDelete = false }) { Text("انصراف") } }
        )
    }
    if (pendingMeasurements != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { pendingMeasurements = null },
            title = { Text("تأیید ذخیره سایزها") },
            text = { Text("تغییرات سایزهای این سفارش ذخیره شود؟") },
            confirmButton = { androidx.compose.material3.TextButton(onClick = {
                database.updateOrderMeasurements(selectedOrder!!.id, pendingMeasurements!!)
                pendingMeasurements = null
                refreshKey++
                selectedOrder = null
            }) { Text("ذخیره", color = Burgundy) } },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { pendingMeasurements = null }) { Text("انصراف") } }
        )
    }
    if (statusOrder != null && pendingStatus != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { statusOrder = null; pendingStatus = null },
            title = { Text("تغییر وضعیت سفارش") },
            text = { Text("وضعیت سفارش #${statusOrder!!.id} به «${statusLabel(pendingStatus!!)}» تغییر کند؟") },
            confirmButton = { androidx.compose.material3.TextButton(onClick = {
                database.updateOrderStatus(statusOrder!!.id, pendingStatus!!)
                statusOrder = null
                pendingStatus = null
                refreshKey++
            }) { Text("تأیید تغییر", color = Burgundy) } },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { statusOrder = null; pendingStatus = null }) { Text("انصراف") } }
        )
    }
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.material3.TextButton(onClick = onBack) { Text("برگشت", color = Burgundy) }
            Column(Modifier.weight(1f)) {
                Text(if (activeOnly) "سفارش‌های فعال" else "تاریخچه کل سفارشات", color = BurgundyDark, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Text(customer.name + " • " + customer.phone, color = TextMuted, fontSize = 11.sp)
            }
        }
        Column(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 8.dp).background(BurgundySoft.copy(alpha = 0.6f), RoundedCornerShape(12.dp)).padding(12.dp)) {
            Text("مشخصات مشتری", color = BurgundyDark, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text("نام مشتری: ${customer.name}", color = TextDark, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
            Text("شماره تلفن: ${customer.phone.ifBlank { "ثبت نشده" }}", color = TextDark, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
            Text("آدرس: ${customer.address.ifBlank { "ثبت نشده" }}", color = TextDark, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
            if (customer.activeOrderCount > 0 || customer.remaining > 0) {
                Text("تا زمانی که مشتری سفارش فعال یا بدهی داشته باشد، حذف مشتری غیرفعال است.", color = Color(0xFFB33A32), fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
            }
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                androidx.compose.material3.OutlinedButton(onClick = { pendingCustomerName = customer.name; pendingCustomerPhone = customer.phone; editCustomer = true }, modifier = Modifier.weight(1f)) { Text("ویرایش مشخصات", color = Burgundy, fontSize = 11.sp) }
                androidx.compose.material3.OutlinedButton(enabled = customer.activeOrderCount == 0 && customer.remaining <= 0, onClick = { confirmCustomerDelete = true }, modifier = Modifier.weight(1f)) { Text("حذف مشتری", color = if (customer.activeOrderCount == 0 && customer.remaining <= 0) Color(0xFFB33A32) else TextMuted, fontSize = 11.sp) }
            }
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Column(Modifier.weight(1f).background(Color.White, RoundedCornerShape(8.dp)).padding(8.dp)) {
                    Text("کل سفارشات", color = TextMuted, fontSize = 10.sp)
                    Text(customer.orderCount.toString().toPersianDigits(), color = BurgundyDark, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
                Column(Modifier.weight(1f).background(Color.White, RoundedCornerShape(8.dp)).padding(8.dp)) {
                    Text("سفارش فعال", color = TextMuted, fontSize = 10.sp)
                    Text(customer.activeOrderCount.toString().toPersianDigits(), color = BurgundyDark, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
                Column(Modifier.weight(1f).background(Color.White, RoundedCornerShape(8.dp)).padding(8.dp)) {
                    Text("باقی‌مانده", color = TextMuted, fontSize = 10.sp)
                    Text(formatReceiptAmount(customer.remaining), color = if (customer.remaining > 0) Color(0xFFB33A32) else Color(0xFF237342), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        if (measurementHistory.isNotEmpty()) {
            Column(Modifier.fillMaxWidth().padding(top = 8.dp).background(Color.White, RoundedCornerShape(12.dp)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("تاریخچه اندازه‌گیری", color = BurgundyDark, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                measurementHistory.take(20).forEach { item ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(item.fieldName + " · " + item.orderType, color = TextDark, fontSize = 12.sp)
                            Text(item.recordedAt, color = TextMuted, fontSize = 10.sp)
                        }
                        Text(item.value, color = BurgundyDark, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        Text("مدل لباس‌های سفارش‌داده‌شده", color = BurgundyDark, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp, bottom = 2.dp))
        Text("برای دیدن یا تغییر سایزها، مدل لباس را انتخاب کنید.", color = TextMuted, fontSize = 11.sp, modifier = Modifier.padding(vertical = 5.dp))
        if (orders.isEmpty()) Text("سفارشی برای نمایش وجود ندارد.", color = TextMuted, modifier = Modifier.padding(top = 10.dp))
        else {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                orders.chunked(2).forEach { rowOrders ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowOrders.forEach { order ->
                            val measurementItems = remember(order.id, order.measurementsJson) {
                                runCatching {
                                    val json = org.json.JSONObject(order.measurementsJson)
                                    json.keys().asSequence().map { key -> key to json.optString(key) }
                                        .filter { it.second.isNotBlank() }.toList()
                                }.getOrDefault(emptyList())
                            }
                            Column(
                                Modifier.weight(1f).padding(bottom = 8.dp)
                                    .background(Color.White, RoundedCornerShape(10.dp))
                                    .clickable { selectedOrder = order }
                                    .padding(12.dp)
                            ) {
                                Text(order.garmentModel.ifBlank { "مدل لباس نامشخص" }, color = BurgundyDark, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("سفارش #${order.id.toString().toPersianDigits()}", color = TextMuted, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
                                Text("وضعیت: ${statusLabel(order.status)}", color = if (order.status == "ready") Color(0xFF237342) else Burgundy, fontSize = 10.sp, modifier = Modifier.padding(top = 3.dp))
                                Row(Modifier.fillMaxWidth().padding(top = 5.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    listOf("incomplete" to "در حال انجام", "ready" to "آماده", "delivered" to "تحویل‌شده").forEach { (status, label) ->
                                        androidx.compose.material3.TextButton(onClick = { statusOrder = order; pendingStatus = status }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 2.dp, vertical = 0.dp)) {
                                            Text(label, fontSize = 8.sp, color = if (order.status == status) BurgundyDark else TextMuted)
                                        }
                                    }
                                }
                                androidx.compose.material3.HorizontalDivider(color = BorderSoft, modifier = Modifier.padding(vertical = 7.dp))
                                if (measurementItems.isEmpty()) {
                                    Text("سایزی ثبت نشده", color = TextMuted, fontSize = 10.sp)
                                } else {
                                    measurementItems.forEach { (key, value) ->
                                        Text("$key: $value", color = TextDark, fontSize = 11.sp, modifier = Modifier.padding(top = 3.dp))
                                    }
                                }
                                Text("ویرایش سایزها  ✎", color = Burgundy, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
                            }
                        }
                        if (rowOrders.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

private fun statusLabel(status: String): String = when (status) {
    "ready" -> "آماده"
    "delivered" -> "تحویل‌شده"
    else -> "در حال انجام"
}

@Composable
private fun CustomerDetailsEditor(
    customer: CustomerSummary,
    onBack: () -> Unit,
    onSaveRequest: (String, String, String) -> Unit
) {
    var name by remember(customer.name) { mutableStateOf(customer.name) }
    var phone by remember(customer.phone) { mutableStateOf(customer.phone) }
    var address by remember(customer.address) { mutableStateOf(customer.address) }
    Column(Modifier.fillMaxSize().background(PageBg).padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.material3.TextButton(onClick = onBack) { Text("برگشت", color = Burgundy) }
            Text("ویرایش مشخصات مشتری", color = BurgundyDark, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        OutlinedTextField(value = name, onValueChange = { name = it }, modifier = Modifier.fillMaxWidth().padding(top = 16.dp), label = { Text("نام مشتری") }, singleLine = true, colors = appFieldColors(),
                        shape = RoundedCornerShape(16.dp),)
        OutlinedTextField(value = phone, onValueChange = { phone = it }, modifier = Modifier.fillMaxWidth().padding(top = 10.dp), label = { Text("شماره تلفن") }, singleLine = true, colors = appFieldColors(),
                        shape = RoundedCornerShape(16.dp),)
        OutlinedTextField(value = address, onValueChange = { address = it }, modifier = Modifier.fillMaxWidth().padding(top = 10.dp), label = { Text("آدرس") }, singleLine = true, colors = appFieldColors(), shape = RoundedCornerShape(16.dp))
        Button(onClick = { if (name.isNotBlank()) onSaveRequest(name.trim(), phone.trim(), address.trim()) }, modifier = Modifier.fillMaxWidth().padding(top = 18.dp), colors = ButtonDefaults.buttonColors(containerColor = Burgundy)) {
            Text("ذخیره تغییرات", color = Color.White)
        }
    }
}

@Composable
private fun MeasurementEditorScreen(
    order: CustomerOrderHistory,
    onBack: () -> Unit,
    onSave: (Map<String, String>) -> Unit
) {
    val initialMeasurements = remember(order.id) {
        runCatching {
            val json = org.json.JSONObject(order.measurementsJson)
            json.keys().asSequence().associateWith { key -> json.optString(key) }
        }.getOrDefault(emptyMap())
    }
    val values = remember(order.id) { mutableStateMapOf<String, String>().apply { putAll(initialMeasurements) } }
    Column(Modifier.fillMaxSize().background(PageBg).padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.material3.TextButton(onClick = onBack) { Text("برگشت", color = Burgundy) }
            Column(Modifier.weight(1f)) {
                Text("ویرایش سایزهای لباس", color = BurgundyDark, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(order.garmentModel, color = TextMuted, fontSize = 13.sp)
            }
        }
        Text("فقط سایزهای ثبت‌شده برای این سفارش نمایش داده می‌شود. تغییرات را ذخیره کنید.", color = TextMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp, bottom = 12.dp))
        if (values.isEmpty()) {
            Text("برای این سفارش هنوز سایزی ثبت نشده است.", color = TextMuted, fontSize = 14.sp, modifier = Modifier.padding(vertical = 16.dp))
        } else {
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
                values.keys.toList().chunked(3).forEach { rowKeys ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowKeys.forEach { key ->
                            OutlinedTextField(
                                value = values[key].orEmpty(),
                                onValueChange = { values[key] = it },
                                modifier = Modifier.weight(1f).padding(bottom = 10.dp),
                                label = { Text(key, maxLines = 1) },
                                singleLine = true,
                                colors = appFieldColors(),
                                shape = RoundedCornerShape(16.dp)
                            )
                        }
                        repeat(3 - rowKeys.size) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        Button(
            onClick = { onSave(values.toMap()) },
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Burgundy),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("ذخیره سایزها", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}
@Composable
private fun SimpleSection(title: String, description: String) {
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text(title, color = BurgundyDark, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text(description, color = TextMuted, fontSize = 14.sp)
    }
}


@Composable
internal fun appFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Burgundy,
    unfocusedBorderColor = BorderSoft,
    focusedLabelColor = Burgundy,
    unfocusedLabelColor = TextMuted,
    focusedTextColor = TextDark,
    unfocusedTextColor = TextDark,
    cursorColor = Burgundy,
    focusedContainerColor = CardBg,
    unfocusedContainerColor = CardBg,
    focusedLeadingIconColor = Burgundy,
    focusedTrailingIconColor = Burgundy
)

@Composable
private fun WorkerMenuScreen(
    onBack: () -> Unit,
    onRegister: () -> Unit,
    onList: () -> Unit,
    onPayroll: () -> Unit,
    onAttendance: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text("مدیریت کارمندان", color = BurgundyDark, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                Text("ثبت، فهرست، معاش و حاضری کارمندان", color = TextMuted, fontSize = 12.sp)
            }
            TextButton(onClick = onBack) { Text("برگشت", color = Burgundy) }
        }
        val items = listOf(
            Triple("ثبت کارمند جدید", "♙", onRegister),
            Triple("فهرست کارمندان", "☷", onList),
            Triple("معاش کارمندان", "؋", onPayroll),
            Triple("حاضری کارمندان", "✓", onAttendance)
        )
        items.chunked(2).forEach { rowItems ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                rowItems.forEach { item ->
                    SettingsMenuItem(item.first, item.second, Color(0xFFF1E9E4), Modifier.weight(1f)) { item.third() }
                }
            }
        }
    }
}
