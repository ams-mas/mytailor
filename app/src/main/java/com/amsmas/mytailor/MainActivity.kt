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
private val Ink = Color(0xFF292323)
private val Muted = Color(0xFF776F6C)
private const val WORKSHOP_IMAGE_URL =
    "https://raw.githubusercontent.com/ams-mas/mytailor/main/app/src/main/res/drawable/images/login_background.png"

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

    BoxWithConstraints(
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
            // Start the form around the middle of the screen.
            Spacer(Modifier.height(maxHeight * 0.45f))
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
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Color.White,
    unfocusedBorderColor = Color(0xCCFFFFFF),
    focusedLabelColor = Color.White,
    unfocusedLabelColor = Color(0xFFECECEC),
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    cursorColor = Color.White
)
