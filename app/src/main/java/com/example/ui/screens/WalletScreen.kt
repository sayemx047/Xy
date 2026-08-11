package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.WalletTransactionEntity
import com.example.data.db.UserEntity
import com.example.ui.components.copyToClipboard
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel

@Composable
fun WalletScreen(
    viewModel: MainViewModel,
    currentUser: UserEntity?,
    transactions: List<WalletTransactionEntity>
) {
    val context = LocalContext.current
    var rechargeAmountText by remember { mutableStateOf("100") }
    var transactionIdInput by remember { mutableStateOf("") }

    val bkashOfficialNumber = "01789495251"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 30.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "IN-APP WALLET & RECHARGE",
                fontWeight = FontWeight.Black,
                fontSize = 20.sp,
                color = GamingPrimaryGold,
                letterSpacing = 1.sp
            )
            Text(
                text = "Manage credits, recharge via bKash, and track verification status.",
                fontSize = 12.sp,
                color = GamingTextSecondary
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Wallet Balance Hero Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = GamingCardSurface),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, GamingGlassBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("CURRENT WALLET BALANCE", fontSize = 11.sp, color = GamingTextSecondary, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "৳${String.format("%.2f", currentUser?.walletBalance ?: 0.0)}",
                        fontWeight = FontWeight.Black,
                        fontSize = 32.sp,
                        color = GamingPrimaryGold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = GamingSurface,
                        border = BorderStroke(1.dp, GamingGlassBorder),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = "Instant Checkout", tint = GamingSuccessGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Automatic Entry Fee Deductions Enabled", fontSize = 11.sp, color = GamingSuccessGreen)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // bKash Official Recharge Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = GamingBkashPink.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, GamingBkashPink),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = GamingBkashPink,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("bKash", fontWeight = FontWeight.Black, color = Color.White, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Official Cash In / Personal", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = GamingTextPrimary)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // bKash Number & Copy Button
                    Surface(
                        color = GamingDarkBackground,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("BKASH NUMBER", fontSize = 10.sp, color = GamingTextSecondary)
                                Text(bkashOfficialNumber, fontWeight = FontWeight.Black, fontSize = 18.sp, color = GamingBkashPink)
                            }

                            Button(
                                onClick = { copyToClipboard(context, "bKash Number", bkashOfficialNumber) },
                                colors = ButtonDefaults.buttonColors(containerColor = GamingBkashPink),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("copy_bkash_number_btn")
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("COPY NUMBER", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Recharge Instructions:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = GamingTextPrimary)
                    Text("1. Send Money / Cash In your amount to $bkashOfficialNumber.", fontSize = 11.sp, color = GamingTextSecondary)
                    Text("2. Copy the Transaction ID from bKash SMS or App receipt.", fontSize = 11.sp, color = GamingTextSecondary)
                    Text("3. Enter amount and paste Transaction ID below for instant admin verification.", fontSize = 11.sp, color = GamingTextSecondary)

                    Spacer(modifier = Modifier.height(16.dp))

                    // Recharge Amount Input
                    OutlinedTextField(
                        value = rechargeAmountText,
                        onValueChange = { rechargeAmountText = it },
                        label = { Text("Recharge Amount (BDT)") },
                        leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = "Amount", tint = GamingBkashPink) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GamingBkashPink,
                            focusedLabelColor = GamingBkashPink
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("input_recharge_amount")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Transaction ID Input
                    OutlinedTextField(
                        value = transactionIdInput,
                        onValueChange = { transactionIdInput = it },
                        label = { Text("bKash Transaction ID (e.g. BAX8942KL)") },
                        leadingIcon = { Icon(Icons.Default.ReceiptLong, contentDescription = "TxID", tint = GamingBkashPink) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GamingBkashPink,
                            focusedLabelColor = GamingBkashPink
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("input_transaction_id")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Submit Button
                    Button(
                        onClick = {
                            val amt = rechargeAmountText.toDoubleOrNull() ?: 0.0
                            viewModel.submitRecharge(amt, transactionIdInput)
                            transactionIdInput = ""
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GamingBkashPink),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("submit_recharge_btn")
                    ) {
                        Text("SUBMIT TRANSACTION ID", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Transaction History Header
        item {
            Text(
                text = "TRANSACTION HISTORY",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = GamingAccentCyan,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        if (transactions.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = GamingSurface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No previous wallet recharge transactions found.",
                        fontSize = 13.sp,
                        color = GamingTextSecondary,
                        modifier = Modifier.padding(20.dp)
                    )
                }
            }
        } else {
            items(transactions) { tx ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = GamingCardSurface),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "TxID: ${tx.transactionId}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = GamingTextPrimary
                            )
                            Text(
                                text = "bKash: ${tx.bkashNumber}",
                                fontSize = 11.sp,
                                color = GamingTextSecondary
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "+ BDT ${tx.amount.toInt()}",
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                color = GamingPrimaryGold
                            )

                            val badgeColor = when (tx.status) {
                                "VERIFIED" -> GamingSuccessGreen
                                "REJECTED" -> GamingErrorRed
                                else -> GamingPrimaryGold
                            }

                            Surface(
                                color = badgeColor.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = tx.status,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = badgeColor,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
