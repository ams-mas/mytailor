package com.amsmas.mytailor

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
private val BurgundyDark = Color(0xFF51171D)
private val Cream = Color(0xFFF7EEE6)
private val Ink = Color(0xFF292323)
private val Muted = Color(0xFF776F6C)
private const val WORKSHOP_IMAGE_URL =
    "https://raw.githubusercontent.com/ams-mas/mytailor/main/app/src/main/res/drawable/images/My%20Tailoring%20Workshop.png"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                LoginScreen(
                    onLogin = { _, _ ->
                        Toast.makeText(this, "ورود حساب هنوز به سیستم آنلاین وصل نشده است", Toast.LENGTH_LONG).show()
                    },
                    onRegister = {
                        Toast.makeText(this, "ثبت نام در نسخه بعدی فعال می‌شود", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

@Composable
private fun LoginScreen(onLogin: (String, String) -> Unit, onRegister: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().background(Cream)
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.navigationBars),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(330.dp)) {
            AsyncImage(
                model = WORKSHOP_IMAGE_URL,
                contentDescription = "تصویر کارگاه خیاطی",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier.fillMaxSize().background(
                    Brush.verticalGradient(listOf(Color(0x33201010), Color(0xCC321316)))
                )
            )
            Column(
                modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 20.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("خیاطی من", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold)
                Text("مدیریت سفارشات خیاطی", color = Color.White, fontSize = 17.sp, modifier = Modifier.padding(top = 6.dp))
                Text("به صورت ساده و حرفه‌ای", color = Color.White, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 26.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("ورود به حساب", color = BurgundyDark, fontSize = 25.sp, fontWeight = FontWeight.Bold)
            Text(
                "برای ادامه، معلومات حساب خود را وارد کنید",
                color = Muted, fontSize = 13.sp, textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 7.dp, bottom = 20.dp)
            )
            OutlinedTextField(
                value = email, onValueChange = { email = it }, modifier = Modifier.fillMaxWidth(),
                singleLine = true, label = { Text("ایمیل یا شماره تماس") },
                colors = fieldColors(), shape = RoundedCornerShape(14.dp)
            )
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = password, onValueChange = { password = it }, modifier = Modifier.fillMaxWidth(),
                singleLine = true, label = { Text("رمز عبور") },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    androidx.compose.material3.TextButton(onClick = { passwordVisible = !passwordVisible }) {
                        Text(if (passwordVisible) "پنهان" else "نمایش", color = Burgundy)
                    }
                },
                colors = fieldColors(), shape = RoundedCornerShape(14.dp)
            )
            Text(
                "رمز عبور خود را فراموش کرده‌اید؟", color = Burgundy, fontSize = 13.sp,
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
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Burgundy)
            ) {
                Text("ثبت نام", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
            Text(
                "مدیریت آسان مشتریان، سفارشات و اندازه‌ها", fontSize = 12.sp, color = Ink,
                textAlign = TextAlign.Center, modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
            )
        }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Burgundy, unfocusedBorderColor = Color(0xFFD6C9C1),
    focusedLabelColor = Burgundy, unfocusedLabelColor = Muted,
    focusedTextColor = Ink, unfocusedTextColor = Ink, cursorColor = Burgundy
)
