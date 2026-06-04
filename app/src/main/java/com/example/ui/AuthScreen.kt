package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun AuthScreen(
    viewModel: BacklogViewModel
) {
    var isSignUp by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    
    val authError by viewModel.authError.collectAsStateWithLifecycle()
    val authLoading by viewModel.authLoading.collectAsStateWithLifecycle()
    val currentLang by viewModel.currentLanguageState.collectAsStateWithLifecycle()
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFEF7FF)) // M3 theme background
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F2FA)), // M3 surface
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 450.dp)
                .border(1.dp, Color(0xFFCAC4D0).copy(alpha = 0.5f), RoundedCornerShape(28.dp)),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Language Swift Selector row inside card at the top
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val fallbackLangs = listOf(
                        "en" to "EN",
                        "es" to "ES",
                        "pt" to "PT"
                    )
                    Row(
                        modifier = Modifier
                            .background(Color(0xFFEADDFF).copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        fallbackLangs.forEach { (code, label) ->
                            val isSelected = currentLang == code
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isSelected) Color(0xFF6750A4) else Color.Transparent)
                                    .clickable { viewModel.setLanguage(code) }
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else Color(0xFF49454F)
                                )
                            }
                        }
                    }
                }

                // High-fidelity graphic container representing Workspace Hub
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEADDFF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isSignUp) Icons.Default.Face else Icons.Default.Lock,
                        contentDescription = "Authentication Icon",
                        tint = Color(0xFF21005D),
                        modifier = Modifier.size(30.dp)
                    )
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isSignUp) {
                            Localization.get("auth_welcome_signup", currentLang)
                        } else {
                            Localization.get("auth_welcome_login", currentLang)
                        },
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1D1B20),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isSignUp) {
                            Localization.get("auth_desc_signup", currentLang)
                        } else {
                            Localization.get("auth_desc_login", currentLang)
                        },
                        fontSize = 11.sp,
                        color = Color(0xFF49454F),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }

                // Dual-mode inputs
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (isSignUp) {
                        OutlinedTextField(
                            value = displayName,
                            onValueChange = { displayName = it },
                            label = { Text(Localization.get("label_display_name", currentLang)) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF6750A4),
                                focusedLabelColor = Color(0xFF6750A4)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("displayName_input"),
                            placeholder = { Text(Localization.get("placeholder_display_name", currentLang)) }
                        )
                    }

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text(Localization.get("label_email_address", currentLang)) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF6750A4),
                            focusedLabelColor = Color(0xFF6750A4)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("email_input"),
                        placeholder = { Text(Localization.get("placeholder_email", currentLang)) }
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(Localization.get("label_password", currentLang)) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF6750A4),
                            focusedLabelColor = Color(0xFF6750A4)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("password_input"),
                        placeholder = { Text(Localization.get("placeholder_password", currentLang)) }
                    )
                }

                if (authError != null) {
                    val rawError = authError ?: ""
                    // Translate standard validation errors if matches found
                    val translatedError = when {
                        rawError.contains("all fields", ignoreCase = true) -> Localization.get("All fields are required", currentLang)
                        rawError.contains("invalid email", ignoreCase = true) -> Localization.get("Invalid email format", currentLang)
                        rawError.contains("must be at least 6", ignoreCase = true) -> Localization.get("Password must be at least 6 characters", currentLang)
                        rawError.contains("already exists", ignoreCase = true) -> Localization.get("Account with this email already exists", currentLang)
                        rawError.contains("incorrect", ignoreCase = true) -> Localization.get("Incorrect email or password", currentLang)
                        else -> rawError
                    }
                    Text(
                        text = translatedError,
                        color = Color(0xFFBA1A1A),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }

                Button(
                    onClick = {
                        if (isSignUp) {
                            if (displayName.isBlank() || email.isBlank() || password.isBlank()) {
                                viewModel.authError.value = "All fields are required"
                            } else {
                                viewModel.signUp(email, password, displayName, onSuccess = {})
                            }
                        } else {
                            if (email.isBlank() || password.isBlank()) {
                                viewModel.authError.value = "All fields are required"
                            } else {
                                viewModel.logIn(email, password, onSuccess = {})
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("auth_submit_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4)),
                    shape = RoundedCornerShape(100.dp),
                    enabled = !authLoading
                ) {
                    if (authLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = if (isSignUp) {
                                Localization.get("btn_register", currentLang)
                            } else {
                                Localization.get("btn_login", currentLang)
                            },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))
                
                Text(
                    text = if (currentLang == "es") "— o —" else if (currentLang == "pt") "— ou —" else "— or —",
                    fontSize = 11.sp,
                    color = Color(0xFF49454F).copy(alpha = 0.6f)
                )

                OutlinedButton(
                    onClick = {
                        viewModel.signUpWithGoogle("milercenizario56@gmail.com", onSuccess = {})
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("google_auth_button"),
                    shape = RoundedCornerShape(100.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF1D1B20)),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFCAC4D0)))
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(0.5.dp)) {
                            Text("G", fontWeight = FontWeight.Black, color = Color(0xFF4285F4), fontSize = 14.sp)
                            Text("o", fontWeight = FontWeight.Black, color = Color(0xFFEA4335), fontSize = 14.sp)
                            Text("o", fontWeight = FontWeight.Black, color = Color(0xFFFBBC05), fontSize = 14.sp)
                            Text("g", fontWeight = FontWeight.Black, color = Color(0xFF4285F4), fontSize = 14.sp)
                            Text("l", fontWeight = FontWeight.Black, color = Color(0xFF34A853), fontSize = 14.sp)
                            Text("e", fontWeight = FontWeight.Black, color = Color(0xFFEA4335), fontSize = 14.sp)
                        }
                        Text(
                            text = if (isSignUp) {
                                if (currentLang == "es") "Registrarse con Google" else if (currentLang == "pt") "Registrar com Google" else "Sign up with Google"
                            } else {
                                if (currentLang == "es") "Iniciar con Google" else if (currentLang == "pt") "Entrar com Google" else "Sign in with Google"
                            },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF1D1B20)
                        )
                    }
                }

                TextButton(
                    onClick = {
                        isSignUp = !isSignUp
                        viewModel.authError.value = null
                    },
                    modifier = Modifier.testTag("auth_switch_mode_button")
                ) {
                    Text(
                        text = if (isSignUp) {
                            Localization.get("switch_to_login", currentLang)
                        } else {
                            Localization.get("switch_to_signup", currentLang)
                        },
                        color = Color(0xFF6750A4),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
