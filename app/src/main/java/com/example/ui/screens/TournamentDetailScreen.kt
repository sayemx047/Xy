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
import com.example.data.db.ChatMessageEntity
import com.example.data.db.TournamentEntity
import com.example.data.db.UserEntity
import com.example.ui.components.copyToClipboard
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.Screen

@Composable
fun TournamentDetailScreen(
    tournamentId: String,
    viewModel: MainViewModel,
    currentUser: UserEntity?
) {
    val context = LocalContext.current
    val tournament by viewModel.repository.getTournamentFlow(tournamentId)
        .collectAsState(initial = null)

    val registrations by viewModel.userRegistrations.collectAsState()
    val isRegistered = remember(registrations, tournamentId) {
        registrations.any { it.tournamentId == tournamentId }
    }

    // Chat messages specific to this tournament
    val chatMessages by viewModel.repository.getChatMessagesFlow("TOURNAMENT_$tournamentId")
        .collectAsState(initial = emptyList())

    // Form states
    var ffUid by remember(currentUser) { mutableStateOf(currentUser?.ffUid ?: "") }
    var firstName by remember(currentUser) { mutableStateOf(currentUser?.firstName ?: "") }
    var lastName by remember(currentUser) { mutableStateOf(currentUser?.lastName ?: "") }
    var squadName by remember { mutableStateOf("") }
    var p1Username by remember { mutableStateOf("") }
    var p2Username by remember { mutableStateOf("") }
    var p3Username by remember { mutableStateOf("") }
    var p4Username by remember { mutableStateOf("") }
    var whatsapp by remember(currentUser) { mutableStateOf(currentUser?.whatsapp ?: "") }

    var chatMessageInput by remember { mutableStateOf("") }

    if (tournament == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = GamingPrimaryGold)
        }
        return
    }

    val t = tournament!!

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 30.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))
            // Back Button Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.navigateTo(Screen.Home) },
                    modifier = Modifier.testTag("tournament_back_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back to Home",
                        tint = GamingPrimaryGold
                    )
                }
                Text(
                    text = t.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = GamingTextPrimary,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Tournament Summary Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = GamingCardSurface),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, GamingGlassBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = GamingPrimaryGold,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "${t.gameMode.uppercase()} • MAP: ${t.map.uppercase()}",
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Text(
                            text = t.scheduleTime,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = GamingAccentCyan
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(GamingSurface)
                            .border(1.dp, GamingGlassBorder, RoundedCornerShape(16.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("PRIZE POOL", fontSize = 10.sp, color = GamingTextSecondary)
                            Text("৳${t.prizePool.toInt()}", fontWeight = FontWeight.Black, fontSize = 16.sp, color = GamingSuccessGreen)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("PER KILL", fontSize = 10.sp, color = GamingTextSecondary)
                            Text("৳${t.perKill.toInt()}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = GamingAccentCyan)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("ENTRY FEE", fontSize = 10.sp, color = GamingTextSecondary)
                            Text("৳${t.entryFee.toInt()}", fontWeight = FontWeight.Black, fontSize = 16.sp, color = GamingPrimaryGold)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Private Tournament Info Section (If Registered)
        if (isRegistered) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = GamingSurface),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GamingSuccessGreen),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VpnKey, contentDescription = "Room Key", tint = GamingSuccessGreen)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "PRIVATE TOURNAMENT INFO",
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = GamingSuccessGreen
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Visible only to registered tournament participants.",
                            fontSize = 11.sp,
                            color = GamingTextSecondary
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Room ID & Room Password Card
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Room ID
                            Surface(
                                color = GamingCardSurface,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("ROOM ID", fontSize = 10.sp, color = GamingTextSecondary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = t.roomId,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 18.sp,
                                        color = GamingPrimaryGold
                                    )
                                    if (t.roomId != "TBA") {
                                        TextButton(
                                            onClick = { copyToClipboard(context, "Room ID", t.roomId) },
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text("COPY ID", fontSize = 11.sp, color = GamingAccentCyan)
                                        }
                                    }
                                }
                            }

                            // Room Password
                            Surface(
                                color = GamingCardSurface,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("ROOM PASSWORD", fontSize = 10.sp, color = GamingTextSecondary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = t.roomPassword,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 18.sp,
                                        color = GamingPrimaryGold
                                    )
                                    if (t.roomPassword != "TBA") {
                                        TextButton(
                                            onClick = { copyToClipboard(context, "Room Pass", t.roomPassword) },
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text("COPY PASS", fontSize = 11.sp, color = GamingAccentCyan)
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Text("Tournament Rules:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = GamingTextPrimary)
                        Text("1. Hacking, emulator bypass, or bug exploitation results in immediate ban.", fontSize = 12.sp, color = GamingTextSecondary)
                        Text("2. Join custom room 5 minutes before match schedule.", fontSize = 12.sp, color = GamingTextSecondary)
                        Text("3. Take screenshot of your match result for prize claim.", fontSize = 12.sp, color = GamingTextSecondary)
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Dedicated Tournament Live Chat with Admin
            item {
                Text(
                    text = "TOURNAMENT PARTICIPANT CHAT WITH ADMIN",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = GamingPrimaryGold,
                    letterSpacing = 0.8.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(chatMessages) { msg ->
                val isMe = msg.senderEmail == currentUser?.email
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                ) {
                    Surface(
                        color = if (msg.isAdmin) GamingAccentPink.copy(alpha = 0.85f) else if (isMe) GamingPrimaryGold else GamingCardSurface,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = if (msg.isAdmin) "👑 Admin Support" else msg.senderName,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isMe || msg.isAdmin) GamingDarkBackground else GamingAccentCyan
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = msg.message,
                                fontSize = 13.sp,
                                color = if (isMe || msg.isAdmin) GamingDarkBackground else GamingTextPrimary
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = chatMessageInput,
                        onValueChange = { chatMessageInput = it },
                        placeholder = { Text("Message Admin regarding match...") },
                        modifier = Modifier.weight(1f).testTag("tournament_chat_input")
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (chatMessageInput.isNotEmpty()) {
                                viewModel.sendChatMessage("TOURNAMENT_$tournamentId", chatMessageInput)
                                chatMessageInput = ""
                            }
                        },
                        modifier = Modifier.testTag("tournament_chat_send_btn")
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send", tint = GamingPrimaryGold)
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        } else {
            // Registration Requirement Form (If NOT registered)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = GamingCardSurface),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Automated Tournament Registration",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = GamingPrimaryGold
                        )
                        Text(
                            text = "Mandatory details for bracket entry & automated prize distribution.",
                            fontSize = 12.sp,
                            color = GamingTextSecondary
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // FF UID (Mandatory)
                        OutlinedTextField(
                            value = ffUid,
                            onValueChange = { ffUid = it },
                            label = { Text("Free Fire UID (FF UID) *") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("reg_input_ff_uid")
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // First Name & Last Name
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = firstName,
                                onValueChange = { firstName = it },
                                label = { Text("First Name *") },
                                modifier = Modifier.weight(1f).testTag("reg_input_first_name")
                            )
                            OutlinedTextField(
                                value = lastName,
                                onValueChange = { lastName = it },
                                label = { Text("Last Name *") },
                                modifier = Modifier.weight(1f).testTag("reg_input_last_name")
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        // Squad Name (if Duo/Squad)
                        if (t.gameMode.equals("Squad", true) || t.gameMode.equals("Duo", true)) {
                            OutlinedTextField(
                                value = squadName,
                                onValueChange = { squadName = it },
                                label = { Text("Squad Name *") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("reg_input_squad_name")
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        // 4 Player Usernames (if Squad)
                        if (t.gameMode.equals("Squad", true)) {
                            Text("4 Player Usernames (Squad Roster) *", fontSize = 12.sp, color = GamingAccentCyan, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedTextField(value = p1Username, onValueChange = { p1Username = it }, label = { Text("P1") }, modifier = Modifier.weight(1f))
                                OutlinedTextField(value = p2Username, onValueChange = { p2Username = it }, label = { Text("P2") }, modifier = Modifier.weight(1f))
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedTextField(value = p3Username, onValueChange = { p3Username = it }, label = { Text("P3") }, modifier = Modifier.weight(1f))
                                OutlinedTextField(value = p4Username, onValueChange = { p4Username = it }, label = { Text("P4") }, modifier = Modifier.weight(1f))
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        // WhatsApp Number (Optional)
                        OutlinedTextField(
                            value = whatsapp,
                            onValueChange = { whatsapp = it },
                            label = { Text("WhatsApp Number (Optional)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("reg_input_whatsapp")
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        val allPlayersJoined = if (t.gameMode.equals("Squad", true)) {
                            listOf(p1Username, p2Username, p3Username, p4Username).joinToString(", ")
                        } else ""

                        // Join Button (Triggers Wallet Check)
                        Button(
                            onClick = {
                                viewModel.registerTournament(
                                    tournamentId = t.id,
                                    ffUid = ffUid,
                                    fname = firstName,
                                    lname = lastName,
                                    squadName = squadName,
                                    playerUsernames = allPlayersJoined,
                                    whatsapp = whatsapp
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GamingPrimaryGold),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("submit_tournament_join_btn")
                        ) {
                            Text(
                                text = "CONFIRM & DEDUCT ৳${t.entryFee.toInt()}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
