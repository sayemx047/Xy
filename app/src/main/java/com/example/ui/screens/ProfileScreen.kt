package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.db.DeviceLogEntity
import com.example.data.db.UserEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel

@Composable
fun ProfileScreen(
    viewModel: MainViewModel,
    currentUser: UserEntity?,
    deviceLog: DeviceLogEntity?
) {
    var fname by remember(currentUser) { mutableStateOf(currentUser?.firstName ?: "") }
    var lname by remember(currentUser) { mutableStateOf(currentUser?.lastName ?: "") }
    var ffUid by remember(currentUser) { mutableStateOf(currentUser?.ffUid ?: "") }
    var whatsapp by remember(currentUser) { mutableStateOf(currentUser?.whatsapp ?: "") }

    // Gallery Profile Picture Picker
    var profileImageUri by remember { mutableStateOf<Uri?>(null) }
    val galleryPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            profileImageUri = uri
        }
    }

    // Password Change Dialog State
    var showPasswordDialog by remember { mutableStateOf(false) }
    var currentPasswordInput by remember { mutableStateOf("") }
    var newPasswordInput by remember { mutableStateOf("") }

    // Account Delete Dialog State
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 30.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "GAMER PROFILE & SECURITY",
                fontWeight = FontWeight.Black,
                fontSize = 20.sp,
                color = GamingPrimaryGold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Profile Avatar Header
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = GamingCardSurface),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, GamingGlassBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(GamingPrimaryGold)
                            .border(2.dp, GamingPrimaryGold, CircleShape)
                            .clickable { galleryPhotoLauncher.launch("image/*") }
                            .testTag("avatar_gallery_picker"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (profileImageUri != null) {
                            AsyncImage(
                                model = profileImageUri,
                                contentDescription = "Profile Photo from Gallery",
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(
                                text = (currentUser?.firstName?.take(1) ?: "G").uppercase(),
                                fontWeight = FontWeight.Black,
                                fontSize = 28.sp,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = "${currentUser?.firstName ?: "Gamer"} ${currentUser?.lastName ?: ""}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = GamingTextPrimary
                        )
                        Text(
                            text = currentUser?.email ?: "",
                            fontSize = 12.sp,
                            color = GamingTextSecondary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "FF UID: ${currentUser?.ffUid?.ifEmpty { "Not set" }}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = GamingAccentCyan
                        )
                        OutlinedButton(
                            onClick = { galleryPhotoLauncher.launch("image/*") },
                            modifier = Modifier.padding(top = 4.dp).testTag("select_gallery_photo_btn"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = GamingPrimaryGold),
                            border = BorderStroke(1.dp, GamingGlassBorder),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = "Gallery", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Gallery Avatar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Editable Details Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = GamingCardSurface),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, GamingGlassBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Edit Gamer Details",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = GamingPrimaryGold
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = fname,
                            onValueChange = { fname = it },
                            label = { Text("First Name") },
                            modifier = Modifier.weight(1f).testTag("profile_fname")
                        )
                        OutlinedTextField(
                            value = lname,
                            onValueChange = { lname = it },
                            label = { Text("Last Name") },
                            modifier = Modifier.weight(1f).testTag("profile_lname")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = ffUid,
                        onValueChange = { ffUid = it },
                        label = { Text("Free Fire UID") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("profile_ffuid")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = whatsapp,
                        onValueChange = { whatsapp = it },
                        label = { Text("WhatsApp Number") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("profile_whatsapp")
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { viewModel.updateProfile(fname, lname, ffUid, whatsapp) },
                        colors = ButtonDefaults.buttonColors(containerColor = GamingPrimaryGold),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("save_profile_btn")
                    ) {
                        Text("SAVE PROFILE DETAILS", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Device Monitoring Card (Logged Device Details Visible to Admin)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = GamingSurface),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, GamingAccentCyan),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DeveloperMode, contentDescription = "Device Details", tint = GamingAccentCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SYSTEM DEVICE MONITORING",
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            color = GamingAccentCyan
                        )
                    }
                    Text(
                        text = "Device telemetry logged automatically for tournament fair-play.",
                        fontSize = 11.sp,
                        color = GamingTextSecondary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    DeviceMetricRow(
                        icon = Icons.Default.PhoneAndroid,
                        label = "Device Model",
                        value = deviceLog?.model ?: "Android Device"
                    )
                    DeviceMetricRow(
                        icon = Icons.Default.BatteryChargingFull,
                        label = "Battery Status",
                        value = "${deviceLog?.batteryLevel ?: 85}% Charged"
                    )
                    DeviceMetricRow(
                        icon = Icons.Default.Wifi,
                        label = "Network Type",
                        value = deviceLog?.networkType ?: "Wi-Fi High Speed"
                    )
                    DeviceMetricRow(
                        icon = Icons.Default.Dns,
                        label = "IP Address",
                        value = deviceLog?.ipAddress ?: "192.168.1.100"
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Security Actions: Change Password & Delete Account
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = GamingCardSurface),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Account Security Actions", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = GamingTextPrimary)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Change Password Button
                    OutlinedButton(
                        onClick = { showPasswordDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("change_password_dialog_btn")
                    ) {
                        Icon(Icons.Default.LockReset, contentDescription = "Change Password", tint = GamingPrimaryGold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Change Password (Requires Current Pass)", color = GamingPrimaryGold, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Delete Account Button
                    OutlinedButton(
                        onClick = { showDeleteConfirmDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = GamingErrorRed),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GamingErrorRed),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("delete_account_dialog_btn")
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = "Delete Account", tint = GamingErrorRed)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete Account Permanently", color = GamingErrorRed, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Logout Button
                    Button(
                        onClick = { viewModel.logout() },
                        colors = ButtonDefaults.buttonColors(containerColor = GamingSurface),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("logout_btn")
                    ) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Logout", tint = GamingTextSecondary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sign Out", color = GamingTextSecondary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Change Password Dialog (Requires Current Password input per requirement)
    if (showPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showPasswordDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.changePassword(currentPasswordInput, newPasswordInput)
                        showPasswordDialog = false
                        currentPasswordInput = ""
                        newPasswordInput = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GamingPrimaryGold)
                ) {
                    Text("Confirm Change", color = GamingDarkBackground, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasswordDialog = false }) {
                    Text("Cancel", color = GamingTextSecondary)
                }
            },
            title = { Text("Secure Password Change", fontWeight = FontWeight.Bold, color = GamingTextPrimary) },
            text = {
                Column {
                    Text("Enter your current password to authorize change:", fontSize = 12.sp, color = GamingTextSecondary)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = currentPasswordInput,
                        onValueChange = { currentPasswordInput = it },
                        label = { Text("Current Password *") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_current_password")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newPasswordInput,
                        onValueChange = { newPasswordInput = it },
                        label = { Text("New Password *") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_new_password")
                    )
                }
            },
            containerColor = GamingCardSurface,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Delete Account Confirmation Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAccount()
                        showDeleteConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GamingErrorRed)
                ) {
                    Text("Yes, Delete Permanently", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel", color = GamingTextSecondary)
                }
            },
            title = { Text("Confirm Account Deletion", fontWeight = FontWeight.Bold, color = GamingErrorRed) },
            text = {
                Text("This action cannot be undone. Your gamer profile, wallet records, and registrations will be permanently deleted.", fontSize = 13.sp, color = GamingTextPrimary)
            },
            containerColor = GamingCardSurface,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
private fun DeviceMetricRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = label, tint = GamingAccentCyan, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, fontSize = 12.sp, color = GamingTextSecondary)
        }
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GamingTextPrimary)
    }
}
