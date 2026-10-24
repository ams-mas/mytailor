package com.amsmas.mytailor

import android.os.Bundle
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

// MyTailor unified theme: burgundy, warm ivory, and muted rose.
private val Burgundy = Color(0xFF702027)
private val BurgundyDark = Color(0xFF55191E)
private val BurgundySoft = Color(0xFFF4E7E8)
private val PageBg = Color(0xFFFAF7F4)
private val CardBg = Color(0xFFFFFFFF)
private val BorderSoft = Color(0xFFD9C6C5)
private val TextDark = Color(0xFF302627)
private val TextMuted = Color(0xFF7D7070)
internal data class TailorRecord(val name: String, val phone: String, val address: String, val skills: List<String>)
private data class ShopSkill(val name: String)
internal data class GarmentModel(val name: String, val measurements: List<String>)
private enum class MainTab(val title: String, val symbol: String) { HOME("خانه", "⌂"), CUSTOMERS("مشتریان", "♙"), ORDERS("سفارشات", "▤"), DELIVERY("تحویل", "▣"), SERVICES("خدمات", "✂"), SETTINGS("تنظیمات", "⚙") }
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

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        // Capture the available height in this scope to avoid the implicit receiver error.
        val screenHeight = maxHeight

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
                shape = RoundedCornerShape(14.dp)
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
                shape = RoundedCornerShape(14.dp)
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
private fun TailorHome(database: LocalDatabase, onBackup: () -> Unit, onRestore: () -> Unit, onLogout: () -> Unit, onAddOrder: () -> Unit) {
    var selected by remember { mutableStateOf(MainTab.HOME) }
    var showTailorForm by remember { mutableStateOf(false) }
    var showSkillsForm by remember { mutableStateOf(false) }
    var showGarmentModels by remember { mutableStateOf(false) }
    var showWorkshopSettings by remember { mutableStateOf(false) }
    var showBackupRestore by remember { mutableStateOf(false) }
    var selectedSettingsCategory by remember { mutableStateOf<String?>(null) }
    var shopSkills by remember { mutableStateOf(listOf<ShopSkill>()) }
    var registeredTailors by remember { mutableStateOf(listOf<TailorRecord>()) }
    LaunchedEffect(database) {
        shopSkills = database.getSkills().map { ShopSkill(it) }
        registeredTailors = database.getTailors()
    }
    Column(Modifier.fillMaxSize().background(PageBg).windowInsetsPadding(WindowInsets.statusBars)) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (selected) {
                MainTab.HOME -> DashboardContent(onOpenDeliveries = { selected = MainTab.DELIVERY })
                MainTab.ORDERS -> SimpleSection("سفارشات", "مدیریت تمام سفارش‌های خیاطی")
                MainTab.DELIVERY -> DeliveryOrdersScreen(onAddOrder = onAddOrder)
                MainTab.CUSTOMERS -> SimpleSection("مشتریان", "فهرست مشتریان و معلومات تماس")
                MainTab.SERVICES -> SimpleSection("خدمات", "مدیریت خدمات و کارهای خیاطی")
                MainTab.SETTINGS -> {
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
            if (selected == MainTab.HOME) {
                Button(
                    onClick = onAddOrder,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(18.dp).size(56.dp),
                    shape = CircleShape, colors = ButtonDefaults.buttonColors(containerColor = Burgundy)
                ) { Text("+", fontSize = 30.sp, color = Color.White) }
            }
        }
        Row(Modifier.fillMaxWidth().background(Color.White).windowInsetsPadding(WindowInsets.navigationBars).padding(vertical = 7.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            MainTab.entries.forEach { tab ->
                Column(Modifier.weight(1f).clickable { selected = tab }.padding(vertical = 2.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(tab.symbol, color = if (selected == tab) BurgundyDark else Color.Gray, fontSize = 22.sp)
                    Text(tab.title, color = if (selected == tab) BurgundyDark else Color.Gray, fontSize = 10.sp, fontWeight = if (selected == tab) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
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
            OutlinedTextField(value = editName, onValueChange = { editName = it; message = "" }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), singleLine = true, label = { Text("نام مدل لباس") }, colors = appFieldColors())
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
                        colors = appFieldColors()
                    )
                    Text("حذف", color = Color(0xFFB3261E), fontSize = 12.sp, modifier = Modifier.clickable { editMeasurements = editMeasurements.filterIndexed { itemIndex, _ -> itemIndex != index }; message = "" }.padding(start = 8.dp))
                }
            }
            OutlinedTextField(value = newMeasurementName, onValueChange = { newMeasurementName = it }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), singleLine = true, label = { Text("افزودن اندازه جدید") }, colors = appFieldColors())
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
        OutlinedTextField(value = newModelName, onValueChange = { newModelName = it; message = "" }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), singleLine = true, label = { Text("نام مدل لباس") }, colors = appFieldColors())
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
        OutlinedTextField(value = newSkill, onValueChange = { newSkill = it; error = false }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("نام مهارت یا نوع کار") }, colors = appFieldColors())
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
        OutlinedTextField(value = name, onValueChange = { name = it; error = false }, modifier = Modifier.fillMaxWidth(), label = { Text("نام خیاط") }, singleLine = true, colors = appFieldColors())
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(value = phone, onValueChange = { phone = it; error = false }, modifier = Modifier.fillMaxWidth(), label = { Text("شماره تماس") }, singleLine = true, colors = appFieldColors())
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(value = address, onValueChange = { address = it }, modifier = Modifier.fillMaxWidth(), label = { Text("آدرس") }, minLines = 2, colors = appFieldColors())
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
private fun DashboardContent(onOpenDeliveries: () -> Unit) {
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
            Text("گزارش کوتاه", color = BurgundyDark, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("برنامه تحویل لباس‌ها", color = TextMuted, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp, bottom = 16.dp))
            DeliveryReportCard("امروز", "سفارش‌هایی که امروز باید تحویل بدهی", "۰", Color(0xFF702027), Modifier.fillMaxWidth(), onOpenDeliveries)
            Spacer(Modifier.height(10.dp))
            DeliveryReportCard("فردا", "سفارش‌هایی که فردا موعد تحویل دارند", "۰", Color(0xFFB7791F), Modifier.fillMaxWidth(), onOpenDeliveries)
            Spacer(Modifier.height(10.dp))
            DeliveryReportCard("تا آخر هفته", "مجموع سفارش‌های موعد تحویل این هفته", "۰", Color(0xFF2870D8), Modifier.fillMaxWidth(), onOpenDeliveries)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onOpenDeliveries, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Burgundy)) {
                Text("مشاهده برنامه تحویل", fontWeight = FontWeight.Bold)
            }
            Text("تعدادها فعلاً نمونهٔ خالی هستند؛ پس از اتصال سفارش‌ها به دیتابیس، خودکار محاسبه می‌شوند.", color = TextMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 12.dp))
            Spacer(Modifier.height(24.dp))
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
private fun SimpleSection(title: String, description: String) {
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text(title, color = BurgundyDark, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text(description, color = TextMuted, fontSize = 14.sp)
    }
}

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

@Composable
private fun appFieldColors() = OutlinedTextFieldDefaults.colors(
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
