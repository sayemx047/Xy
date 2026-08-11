package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.DeviceLogEntity
import com.example.data.db.TournamentEntity
import com.example.data.db.WalletTransactionEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel

@Composable
fun AdminPanelScreen(
    viewModel: MainViewModel,
    allTransactions: List<WalletTransactionEntity>,
    tournaments: List<TournamentEntity>,
    allDeviceLogs: List<DeviceLogEntity>
) {
    // Notice broadcast form inputs
    var noticeTitle by remember { mutableStateOf("") }
    var noticeContent by remember { mutableStateOf("") }

    // Room ID & Password distributor inputs
    var selectedTournamentId by remember { mutableStateOf(tournaments.firstOrNull()?.id ?: "") }
    var inputRoomId by remember { mutableStateOf("") }
    var inputRoomPass by remember { mutableStateOf("") }

    // Pinned Banner Image URL input
    var bannerImageUrl by remember { mutableStateOf("https://images.unsplash.com/photo-1542751371-adc38448a05e?w=800") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 30.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AdminPanelSettings, contentDescription = "Admin", tint = GamingAccentPink, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ADMIN VERIFICATION PANEL",
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    color = GamingAccentPink,
                    letterSpacing = 1.sp
                )
            }
            Text(
                text = "Verify bKash recharges, publish notices, set room IDs, and inspect device logs.",
                fontSize = 12.sp,
                color = GamingTextSecondary
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Section 1: Pending bKash Transaction Verification
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = GamingCardSurface),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, GamingGlassBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Payments, contentDescription = "Payments", tint = GamingBkashPink)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "bKash Recharge Verifications",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = GamingPrimaryGold
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    val pendingTxs = allTransactions.filter { it.status == "PENDING" }
                    if (pendingTxs.isEmpty()) {
                        Text("No pending recharge requests awaiting verification.", fontSize = 12.sp, color = GamingTextSecondary)
                    } else {
                        pendingTxs.forEach { tx ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = GamingSurface),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, GamingGlassBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("User: ${tx.userEmail}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = GamingAccentCyan)
                                    Text("TxID: ${tx.transactionId} | Amount: ৳${tx.amount.toInt()}", fontWeight = FontWeight.Black, fontSize = 14.sp, color = GamingPrimaryGold)

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = { viewModel.adminApproveTransaction(tx.id, true) },
                                            colors = ButtonDefaults.buttonColors(containerColor = GamingSuccessGreen),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("admin_approve_${tx.id}")
                                        ) {
                                            Text("APPROVE & CREDIT", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                                        }

                                        Button(
                                            onClick = { viewModel.adminApproveTransaction(tx.id, false) },
                                            colors = ButtonDefaults.buttonColors(containerColor = GamingErrorRed),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("admin_reject_${tx.id}")
                                        ) {
                                            Text("REJECT", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Section 2: Distribute Tournament Room ID & Password
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = GamingCardSurface),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Distribute Custom Room ID & Pass", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = GamingPrimaryGold)
                    Text("Credentials will instantly appear on joined players' private Tournament Info page.", fontSize = 11.sp, color = GamingTextSecondary)

                    Spacer(modifier = Modifier.height(12.dp))

                    if (tournaments.isNotEmpty()) {
                        Text("Select Tournament:", fontSize = 11.sp, color = GamingTextSecondary)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(tournaments) { tourney ->
                                FilterChip(
                                    selected = selectedTournamentId == tourney.id,
                                    onClick = { selectedTournamentId = tourney.id },
                                    label = { Text(tourney.id) }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = inputRoomId,
                            onValueChange = { inputRoomId = it },
                            label = { Text("Room ID") },
                            modifier = Modifier.weight(1f).testTag("input_admin_room_id")
                        )
                        OutlinedTextField(
                            value = inputRoomPass,
                            onValueChange = { inputRoomPass = it },
                            label = { Text("Room Password") },
                            modifier = Modifier.weight(1f).testTag("input_admin_room_pass")
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (selectedTournamentId.isNotEmpty() && inputRoomId.isNotEmpty()) {
                                viewModel.adminUpdateRoomInfo(selectedTournamentId, inputRoomId, inputRoomPass)
                                inputRoomId = ""
                                inputRoomPass = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GamingPrimaryGold),
                        modifier = Modifier.fillMaxWidth().testTag("admin_publish_room_btn")
                    ) {
                        Text("RELEASE ROOM CREDENTIALS", fontWeight = FontWeight.Bold, color = GamingDarkBackground)
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Section 3: Pin Banner Image / Broadcast Notice
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = GamingCardSurface),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Pin Home Banner Image", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = GamingPrimaryGold)
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = bannerImageUrl,
                        onValueChange = { bannerImageUrl = it },
                        label = { Text("Banner Image URL") },
                        modifier = Modifier.fillMaxWidth().testTag("input_admin_banner_url")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            if (selectedTournamentId.isNotEmpty()) {
                                viewModel.adminPinTournamentImage(selectedTournamentId, bannerImageUrl)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GamingAccentCyan),
                        modifier = Modifier.fillMaxWidth().testTag("admin_pin_banner_btn")
                    ) {
                        Text("PIN BANNER TO HOME SCREEN", fontWeight = FontWeight.Bold, color = GamingDarkBackground)
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                    Divider(color = GamingSurface)
                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Publish Platform Notice", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = GamingPrimaryGold)
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = noticeTitle,
                        onValueChange = { noticeTitle = it },
                        label = { Text("Notice Title") },
                        modifier = Modifier.fillMaxWidth().testTag("input_notice_title")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = noticeContent,
                        onValueChange = { noticeContent = it },
                        label = { Text("Notice Description") },
                        modifier = Modifier.fillMaxWidth().testTag("input_notice_content")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            if (noticeTitle.isNotEmpty()) {
                                viewModel.adminPostNotice(noticeTitle, noticeContent, isLoginScreen = true)
                                noticeTitle = ""
                                noticeContent = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GamingAccentPink),
                        modifier = Modifier.fillMaxWidth().testTag("admin_publish_notice_btn")
                    ) {
                        Text("BROADCAST GLOBAL NOTICE", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Section 4: Device Logs Monitor View
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = GamingSurface),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("User Device Logs Monitor", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = GamingAccentCyan)
                    Text("Track active user devices, IP addresses, battery level, and network details.", fontSize = 11.sp, color = GamingTextSecondary)

                    Spacer(modifier = Modifier.height(12.dp))

                    if (allDeviceLogs.isEmpty()) {
                        Text("No device logs recorded yet.", fontSize = 12.sp, color = GamingTextSecondary)
                    } else {
                        allDeviceLogs.forEach { log ->
                            Surface(
                                color = GamingCardSurface,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("User: ${log.userEmail}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = GamingPrimaryGold)
                                    Text("Model: ${log.model} | Battery: ${log.batteryLevel}%", fontSize = 11.sp, color = GamingTextPrimary)
                                    Text("Network: ${log.networkType} | IP: ${log.ipAddress}", fontSize = 11.sp, color = GamingTextSecondary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
