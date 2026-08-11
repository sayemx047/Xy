package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.NoticeEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.Screen
import com.example.ui.viewmodel.UiAlert

@Composable
fun GamerTopAppBar(
    walletBalance: Double,
    isAdminMode: Boolean,
    onWalletClick: () -> Unit,
    onChatClick: () -> Unit,
    onAdminToggle: () -> Unit,
    onProfileClick: () -> Unit
) {
    Surface(
        color = GamingSurface,
        border = BorderStroke(1.dp, GamingGlassBorder),
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // App Brand Logo & Title
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(GamingPrimaryGold),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SportsEsports,
                        contentDescription = "Esports Logo",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "GAME ARENA",
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = GamingTextPrimary,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "Tournament Hub",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = GamingTextSecondary
                    )
                }
            }

            // Right Actions: Wallet Chip + Admin Mode Toggle + Chat + Profile
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Wallet Chip
                Surface(
                    color = GamingCardSurface,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, GamingGlassBorder),
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onWalletClick() }
                        .testTag("wallet_balance_chip")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = "Wallet Balance",
                            tint = GamingPrimaryGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "৳${String.format("%.0f", walletBalance)}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = GamingTextPrimary
                        )
                    }
                }

                // Live Support Chat Icon
                IconButton(
                    onClick = onChatClick,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(GamingCardSurface)
                        .border(1.dp, GamingGlassBorder, CircleShape)
                        .testTag("chat_top_bar_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = "Live Support",
                        tint = GamingAccentCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Profile Avatar Icon
                IconButton(
                    onClick = onProfileClick,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(GamingCardSurface)
                        .border(1.dp, GamingGlassBorder, CircleShape)
                        .testTag("profile_top_bar_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Gamer Profile",
                        tint = GamingTextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CustomBottomNavBar(
    currentScreen: Screen,
    onNavigate: (Screen) -> Unit
) {
    Surface(
        color = GamingSurface,
        border = BorderStroke(1.dp, GamingGlassBorder),
        tonalElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavItem(
                icon = Icons.Default.Home,
                label = "Home",
                isSelected = currentScreen is Screen.Home,
                onClick = { onNavigate(Screen.Home) },
                testTag = "nav_home"
            )
            NavItem(
                icon = Icons.Default.AccountBalanceWallet,
                label = "Wallet",
                isSelected = currentScreen is Screen.Wallet,
                onClick = { onNavigate(Screen.Wallet) },
                testTag = "nav_wallet"
            )
            NavItem(
                icon = Icons.Default.Chat,
                label = "Live Chat",
                isSelected = currentScreen is Screen.LiveChat,
                onClick = { onNavigate(Screen.LiveChat("GENERAL")) },
                testTag = "nav_live_chat"
            )
            NavItem(
                icon = Icons.Default.Person,
                label = "Profile",
                isSelected = currentScreen is Screen.Profile,
                onClick = { onNavigate(Screen.Profile) },
                testTag = "nav_profile"
            )
            NavItem(
                icon = Icons.Default.AdminPanelSettings,
                label = "Admin",
                isSelected = currentScreen is Screen.AdminPanel,
                onClick = { onNavigate(Screen.AdminPanel) },
                testTag = "nav_admin"
            )
        }
    }
}

@Composable
private fun NavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val iconColor = if (isSelected) GamingPrimaryGold else GamingTextSecondary
    val textColor = if (isSelected) GamingPrimaryGold else GamingTextSecondary
    val bgModifier = if (isSelected) {
        Modifier
            .background(GamingPrimaryGold.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
            .border(1.dp, GamingGlassBorder, RoundedCornerShape(16.dp))
    } else Modifier

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .then(bgModifier)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag(testTag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = iconColor,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = textColor
        )
    }
}

@Composable
fun ProfessionalAlertCard(
    alert: UiAlert,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (alert.isSuccess) GamingSuccessGreen else GamingErrorRed
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("alert_dismiss_btn")
            ) {
                Text(
                    text = "OK",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        },
        icon = {
            Icon(
                imageVector = if (alert.isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                contentDescription = alert.title,
                tint = if (alert.isSuccess) GamingSuccessGreen else GamingErrorRed,
                modifier = Modifier.size(44.dp)
            )
        },
        title = {
            Text(
                text = alert.title,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = GamingTextPrimary,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Text(
                text = alert.message,
                fontSize = 14.sp,
                color = GamingTextSecondary,
                textAlign = TextAlign.Center
            )
        },
        containerColor = GamingSurface,
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun InsufficientCreditDialog(
    onDismiss: () -> Unit,
    onRechargeConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    onDismiss()
                    onRechargeConfirm()
                },
                colors = ButtonDefaults.buttonColors(containerColor = GamingBkashPink),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("recharge_now_confirm_btn")
            ) {
                Text("Recharge Now", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("recharge_cancel_btn")
            ) {
                Text("Cancel", color = GamingTextSecondary)
            }
        },
        icon = {
            Icon(
                imageVector = Icons.Default.AccountBalanceWallet,
                contentDescription = "Insufficient Wallet Credit",
                tint = GamingBkashPink,
                modifier = Modifier.size(44.dp)
            )
        },
        title = {
            Text(
                text = "Not enough credit. Wanna recharge?",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = GamingTextPrimary,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Text(
                text = "Your current wallet balance is insufficient to join this tournament. Recharge instantly via bKash 01789495251.",
                fontSize = 14.sp,
                color = GamingTextSecondary,
                textAlign = TextAlign.Center
            )
        },
        containerColor = GamingSurface,
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun NoticeBannerCard(notice: NoticeEntity) {
    Card(
        colors = CardDefaults.cardColors(containerColor = GamingCardSurface),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, GamingGlassBorder),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(GamingPrimaryGold.copy(alpha = 0.15f))
                    .border(1.dp, GamingGlassBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Campaign,
                    contentDescription = "Notice Icon",
                    tint = GamingPrimaryGold,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = notice.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = GamingTextPrimary
                    )
                    Text(
                        text = notice.date,
                        fontSize = 11.sp,
                        color = GamingTextSecondary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = notice.content,
                    fontSize = 13.sp,
                    color = GamingTextSecondary,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "$label Copied: $text", Toast.LENGTH_SHORT).show()
}

