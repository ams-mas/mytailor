package com.amsmas.mytailor

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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

private val Burgundy = Color(0xFF702027)
private val BurgundyDark = Color(0xFF55191E)
private val PageBg = Color(0xFFF8F6F3)
private val TextDark = Color(0xFF272424)
private val TextMuted = Color(0xFF817B78)
private enum class MainTab(val title: String, val symbol: String) { HOME("خانه", "⌂"), CUSTOMERS("مشتریان", "♙"), ORDERS("سفارشات", "▤"), SERVICES("خدمات", "✂"), SETTINGS("تنظیمات", "⚙") }
private const val WORKSHOP_IMAGE_URL =
    "https://raw.githubusercontent.com/ams-mas/mytailor/main/app/src/main/res/drawable/images/login_background.png"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                var loggedIn by remember { mutableStateOf(false) }
                if (loggedIn) {
                    TailorHome(
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
                colors = fieldColors(),
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
                colors = fieldColors(),
                shape = RoundedCornerShape(14.dp)
            )
            Text(
                "رمز عبور خود را فراموش کرده‌اید؟",
                color = Color.White,
                fontSize = 13.sp,
                modifier = Modifier.align(Alignment.Start).padding(top = 10.dp, bottom = 18.dp)
            )
            Button(
                onClick = { onLogin(email.trim(), password) },
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
    }
}


@Composable
private fun TailorHome(onLogout: () -> Unit, onAddOrder: () -> Unit) {
    var selected by remember { mutableStateOf(MainTab.HOME) }
    Column(Modifier.fillMaxSize().background(PageBg).windowInsetsPadding(WindowInsets.statusBars)) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (selected) {
                MainTab.HOME -> DashboardContent()
                MainTab.ORDERS -> SimpleSection("سفارشات", "مدیریت تمام سفارش‌های خیاطی")
                MainTab.CUSTOMERS -> SimpleSection("مشتریان", "فهرست مشتریان و معلومات تماس")
                MainTab.SERVICES -> SimpleSection("خدمات", "مدیریت خدمات و کارهای خیاطی")
                MainTab.SETTINGS -> Column {
                    SimpleSection("تنظیمات", "تنظیمات کارگاه و حساب کاربری")
                    Button(onClick = onLogout, modifier = Modifier.fillMaxWidth().padding(20.dp)) { Text("خروج از حساب") }
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
private fun DashboardContent() {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Burgundy, BurgundyDark))).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("☰", color = Color.White, fontSize = 22.sp)
            Text("خیاطی من", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            Text("♧", color = Color.White, fontSize = 22.sp)
        }
        Column(Modifier.padding(horizontal = 12.dp, vertical = 12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                StatCard("۱۲", "سفارش جدید", "▣", Color(0xFFEAF1FC), Color(0xFF2870D8), Modifier.weight(1f))
                StatCard("۲۵", "کل سفارشات", "▤", Color(0xFFFFF0EE), Color(0xFFD85B5B), Modifier.weight(1f))
            }
            Spacer(Modifier.height(9.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                StatCard("۱۸", "در حال کار", "✂", Color(0xFFFFF5DF), Color(0xFFE3A01A), Modifier.weight(1f))
                StatCard("۷", "تحویل شد", "✓", Color(0xFFEAF6EB), Color(0xFF21824B), Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth().padding(top = 21.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("سفارشات اخیر", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextDark, modifier = Modifier.weight(1f))
                Text("مشاهده همه ←", color = Color(0xFF3973D7), fontSize = 12.sp)
            }
            RecentOrder("علی احمدی", "کت و شلوار", "۱۴۰۵/۰۷/۲۸", "در حال کار", "👔", Color(0xFFFFE7A8))
            RecentOrder("فاطمه حسینی", "مانتو", "۱۴۰۵/۰۷/۲۷", "جدید", "🧥", Color(0xFFDCEAFF))
            RecentOrder("محمد کریمی", "پیراهن", "۱۴۰۵/۰۷/۲۶", "تحویل شد", "👔", Color(0xFFD8F0D9))
            RecentOrder("زهرا محمدی", "لباس مجلسی", "۱۴۰۵/۰۷/۲۵", "در حال کار", "👗", Color(0xFFFFE7A8))
            Spacer(Modifier.height(75.dp))
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
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Color.White,
    unfocusedBorderColor = Color(0xCCFFFFFF),
    focusedLabelColor = Color.White,
    unfocusedLabelColor = Color(0xFFECECEC),
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    cursorColor = Color.White
)
