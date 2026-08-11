package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.db.NoticeEntity
import com.example.ui.components.NoticeBannerCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.Screen

@Composable
fun LoginScreen(
    viewModel: MainViewModel,
    loginNotices: List<NoticeEntity>
) {
    var isRegisterTab by remember { mutableStateOf(false) }

    // Form inputs
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var savePassword by remember { mutableStateOf(true) }

    // Forgot Password & Firebase Dialogs
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var showFirebaseDialog by remember { mutableStateOf(false) }
    var resetEmailInput by remember { mutableStateOf("") }
    var resetNewPassInput by remember { mutableStateOf("") }

    // Registration extra inputs
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var ffUid by remember { mutableStateOf("") }
    var whatsapp by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        GamingDarkBackground,
                        GamingSurface,
                        GamingCardSurface
                    )
                )
            )
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Logo & Branding
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(GamingCardSurface)
                        .border(1.dp, GamingGlassBorder, RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_app_icon),
                        contentDescription = "Tournament App Icon",
                        modifier = Modifier.size(72.dp),
                        contentScale = ContentScale.Fit
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "TOURNAMENT GAMING",
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp,
                    color = GamingTextPrimary,
                    letterSpacing = 1.2.sp
                )
                Text(
                    text = "Esports Battles & In-App Wallet Platform",
                    fontSize = 12.sp,
                    color = GamingTextSecondary
                )
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Tab Switcher: Login / Register
            item {
                Surface(
                    color = GamingCardSurface,
                    shape = RoundedCornerShape(25.dp),
                    border = BorderStroke(1.dp, GamingGlassBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp)
                    ) {
                        Button(
                            onClick = { isRegisterTab = false },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!isRegisterTab) GamingPrimaryGold else Color.Transparent,
                                contentColor = if (!isRegisterTab) Color.White else GamingTextSecondary
                            ),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("tab_login")
                        ) {
                            Text("LOGIN", fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { isRegisterTab = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isRegisterTab) GamingPrimaryGold else Color.Transparent,
                                contentColor = if (isRegisterTab) Color.White else GamingTextSecondary
                            ),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("tab_register")
                        ) {
                            Text("REGISTER", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Form Fields
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = GamingCardSurface),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, GamingGlassBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = if (isRegisterTab) "Create New Account" else "Account Sign In",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = GamingTextPrimary
                        )
                        Text(
                            text = if (isRegisterTab) "One account per email policy strictly enforced." else "Sign in to access tournaments and wallet balance.",
                            fontSize = 12.sp,
                            color = GamingTextSecondary
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Email Field
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email Address") },
                            leadingIcon = {
                                Icon(Icons.Default.Email, contentDescription = "Email", tint = GamingPrimaryGold)
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GamingPrimaryGold,
                                unfocusedBorderColor = GamingGlassSubtleBorder,
                                focusedLabelColor = GamingPrimaryGold
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_email")
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Password Field
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = "Password", tint = GamingPrimaryGold)
                            },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Toggle Password Visibility",
                                        tint = GamingTextSecondary
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GamingPrimaryGold,
                                unfocusedBorderColor = GamingGlassSubtleBorder,
                                focusedLabelColor = GamingPrimaryGold
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_password")
                        )

                        if (!isRegisterTab) {
                            // Save Password Checkbox + Forgot Password
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = savePassword,
                                        onCheckedChange = { savePassword = it },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = GamingPrimaryGold,
                                            uncheckedColor = GamingTextSecondary
                                        ),
                                        modifier = Modifier.testTag("checkbox_save_password")
                                    )
                                    Text("Save Password", fontSize = 13.sp, color = GamingTextPrimary)
                                }

                                TextButton(
                                    onClick = {
                                        resetEmailInput = email
                                        showForgotPasswordDialog = true
                                    },
                                    modifier = Modifier.testTag("forgot_password_btn")
                                ) {
                                    Text("Forgot Password?", fontSize = 12.sp, color = GamingAccentCyan)
                                }
                            }

                            // Temporary Bypass Login Option (User: x, Pass: y)
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                color = GamingPrimaryGold.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, GamingPrimaryGold.copy(alpha = 0.35f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        email = "x"
                                        password = "y"
                                        viewModel.login("x", "y", savePassword)
                                    }
                                    .testTag("quick_bypass_login_chip")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Bolt,
                                        contentDescription = "Bypass Login",
                                        tint = GamingPrimaryGold,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "⚡ Quick Test Bypass: User 'x' | Pass 'y'",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GamingPrimaryGold
                                    )
                                }
                            }
                        } else {
                            // Registration Extra Fields
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = firstName,
                                    onValueChange = { firstName = it },
                                    label = { Text("First Name") },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("input_first_name")
                                )
                                OutlinedTextField(
                                    value = lastName,
                                    onValueChange = { lastName = it },
                                    label = { Text("Last Name") },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("input_last_name")
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = ffUid,
                                onValueChange = { ffUid = it },
                                label = { Text("Free Fire UID (FF UID)") },
                                leadingIcon = {
                                    Icon(Icons.Default.Badge, contentDescription = "FF UID", tint = GamingAccentCyan)
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_ff_uid")
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = whatsapp,
                                onValueChange = { whatsapp = it },
                                label = { Text("WhatsApp Number (Optional)") },
                                leadingIcon = {
                                    Icon(Icons.Default.Phone, contentDescription = "WhatsApp", tint = GamingSuccessGreen)
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_whatsapp")
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Submit Button
                        Button(
                            onClick = {
                                if (isRegisterTab) {
                                    viewModel.register(
                                        email = email,
                                        pass = password,
                                        fname = firstName,
                                        lname = lastName,
                                        ffUid = ffUid,
                                        whatsapp = whatsapp,
                                        savePass = savePassword
                                    )
                                } else {
                                    viewModel.login(
                                        email = email,
                                        pass = password,
                                        savePass = savePassword
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GamingPrimaryGold),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("auth_submit_btn")
                        ) {
                            Text(
                                text = if (isRegisterTab) "REGISTER ACCOUNT" else "SIGN IN",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.White
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Live Chat Quick Support launcher integrated into login page
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = GamingSurface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(GamingAccentCyan.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SupportAgent,
                                    contentDescription = "Live Chat Support",
                                    tint = GamingAccentCyan
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Need Assistance / Reset?",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = GamingTextPrimary
                                )
                                Text(
                                    text = "Integrated 24/7 Live Support Chat",
                                    fontSize = 11.sp,
                                    color = GamingTextSecondary
                                )
                            }
                        }

                        Button(
                            onClick = { viewModel.navigateTo(Screen.LiveChat("GENERAL")) },
                            colors = ButtonDefaults.buttonColors(containerColor = GamingAccentCyan),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.testTag("login_live_chat_btn")
                        ) {
                            Text("LIVE CHAT", fontWeight = FontWeight.Bold, color = GamingDarkBackground)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                // Firebase Sync Options Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = GamingSurface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, GamingGlassBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(GamingPrimaryGold.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudSync,
                                    contentDescription = "Firebase Sync",
                                    tint = GamingPrimaryGold
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Firebase Realtime Sync",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = GamingTextPrimary
                                )
                                Text(
                                    text = "Setup & google-services.json guide",
                                    fontSize = 11.sp,
                                    color = GamingTextSecondary
                                )
                            }
                        }

                        Button(
                            onClick = { showFirebaseDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = GamingPrimaryGold),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.testTag("firebase_info_btn")
                        ) {
                            Text("FIREBASE", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 11.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Notices Ticker / List
            if (loginNotices.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Platform Notices",
                                tint = GamingAccentPink,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "PLATFORM NOTICES",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = GamingAccentPink,
                                letterSpacing = 1.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                items(loginNotices) { notice ->
                    NoticeBannerCard(notice = notice)
                }
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }

        // Forgot Password Modal Dialog (Keeps user on Login Screen)
        if (showForgotPasswordDialog) {
            AlertDialog(
                onDismissRequest = { showForgotPasswordDialog = false },
                containerColor = GamingCardSurface,
                title = {
                    Text("RESET ACCOUNT PASSWORD", fontWeight = FontWeight.Black, fontSize = 16.sp, color = GamingPrimaryGold)
                },
                text = {
                    Column {
                        Text("Enter your account email and your new password to reset instantly.", fontSize = 12.sp, color = GamingTextSecondary)
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = resetEmailInput,
                            onValueChange = { resetEmailInput = it },
                            label = { Text("Account Email") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GamingPrimaryGold,
                                unfocusedBorderColor = GamingGlassSubtleBorder
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("reset_email_input")
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = resetNewPassInput,
                            onValueChange = { resetNewPassInput = it },
                            label = { Text("New Password") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GamingPrimaryGold,
                                unfocusedBorderColor = GamingGlassSubtleBorder
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("reset_password_input")
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.resetPasswordDirectly(resetEmailInput, resetNewPassInput)
                            showForgotPasswordDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GamingPrimaryGold),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("confirm_reset_btn")
                    ) {
                        Text("RESET PASSWORD", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showForgotPasswordDialog = false }) {
                        Text("CANCEL", color = GamingTextSecondary)
                    }
                }
            )
        }

        // Firebase Setup & Status Modal Dialog
        if (showFirebaseDialog) {
            AlertDialog(
                onDismissRequest = { showFirebaseDialog = false },
                containerColor = GamingCardSurface,
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Cloud, contentDescription = null, tint = GamingPrimaryGold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("FIREBASE INTEGRATION", fontWeight = FontWeight.Black, fontSize = 16.sp, color = GamingTextPrimary)
                    }
                },
                text = {
                    Column {
                        Text(
                            text = "This platform is fully equipped with Room Local Persistence and ready for Firebase Cloud Sync.\n\n" +
                                   "🔥 To bind your Firebase Console project:\n" +
                                   "1. Download your 'google-services.json'.\n" +
                                   "2. Place it in the /app project directory.\n" +
                                   "3. Enable Firebase Auth and Realtime Database in your Firebase Console.\n\n" +
                                   "Local database is active and performing at peak speed.",
                            fontSize = 12.sp,
                            color = GamingTextSecondary
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showFirebaseDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = GamingPrimaryGold),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("OK", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            )
        }
    }
}
