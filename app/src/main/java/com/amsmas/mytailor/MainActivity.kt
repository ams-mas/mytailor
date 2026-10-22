package com.amsmas.mytailor

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Burgundy = Color(0xFF702027)
private val BurgundyDark = Color(0xFF51171D)
private val Cream = Color(0xFFF7EEE6)
private val Ink = Color(0xFF292323)
private val Muted = Color(0xFF776F6C)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                LoginScreen(
                    onLogin = { email, _ ->
                        Toast.makeText(this, "صفحه ورود آماده است؛ اتصال حساب هنوز فعال نشده است", Toast.LENGTH_LONG).show()
                    },
                    onRegister = {
                        Toast.makeText(this, "صفحه ثبت نام در مرحله بعد اضافه می‌شود", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

@Composable
private fun LoginScreen(
    onLogin: (String, String) -> Unit,
    onRegister: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF2B2019), Color(0xFF6E4B39), Cream, Color(0xFFEAD8CA))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 28.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .background(Burgundy, CircleShape)
                        .padding(5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color.Transparent, CircleShape)
                            .padding(19.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ContentCut,
                            contentDescription = "نشان خیاطی",
                            tint = Color.White,
                            modifier = Modifier.height(62.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "خیاطی من",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = BurgundyDark,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "مدیریت سفارشات خیاطی",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Ink,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "به صورت ساده و حرفه‌ای",
                    fontSize = 15.sp,
                    color = Ink,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 38.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("ایمیل یا شماره تماس") },
                    leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                    colors = fieldColors(),
                    shape = RoundedCornerShape(14.dp)
                )
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("رمز عبور") },
                    leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                    trailingIcon = {
                        androidx.compose.material3.TextButton(onClick = { passwordVisible = !passwordVisible }) {
                            Text(if (passwordVisible) "پنهان" else "نمایش", color = Burgundy)
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    colors = fieldColors(),
                    shape = RoundedCornerShape(14.dp)
                )
                Text(
                    text = "رمز عبور خود را فراموش کرده‌اید؟",
                    color = Burgundy,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(top = 10.dp, bottom = 18.dp)
                )

                Button(
                    onClick = { onLogin(email.trim(), password) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Burgundy)
                ) {
                    Text("ورود به حساب", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onRegister,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = Burgundy
                    ),
                    border = null
                ) {
                    Text("ثبت نام", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
            }

            Text(
                text = "مدیریت آسان مشتریان، سفارشات و اندازه‌ها",
                fontSize = 13.sp,
                color = Ink,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 28.dp, bottom = 12.dp)
            )
        }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Burgundy,
    unfocusedBorderColor = Color(0xFFD6C9C1),
    focusedLabelColor = Burgundy,
    unfocusedLabelColor = Muted,
    focusedLeadingIconColor = Burgundy,
    unfocusedLeadingIconColor = Muted,
    focusedTextColor = Ink,
    unfocusedTextColor = Ink,
    cursorColor = Burgundy
)
