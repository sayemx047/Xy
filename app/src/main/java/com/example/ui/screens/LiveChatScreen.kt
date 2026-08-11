package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.db.ChatMessageEntity
import com.example.data.db.UserEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel

@Composable
fun LiveChatScreen(
    contextKey: String,
    viewModel: MainViewModel,
    currentUser: UserEntity?,
    isAdminMode: Boolean
) {
    var activeContext by remember(contextKey) { mutableStateOf(contextKey) }

    val chatMessages by viewModel.repository.getChatMessagesFlow(activeContext)
        .collectAsState(initial = emptyList())

    var messageInput by remember { mutableStateOf("") }
    var attachedImageUri by remember { mutableStateOf<String?>(null) }

    // Message edit state
    var editingMessageId by remember { mutableStateOf<Long?>(null) }
    var editMessageText by remember { mutableStateOf("") }

    // Predefined gallery image samples for easy simulation
    val sampleGalleryImages = listOf(
        "https://images.unsplash.com/photo-1542751371-adc38448a05e?w=500", // Gaming setup
        "https://images.unsplash.com/photo-1560253023-3ec5d502959f?w=500", // Esports match screenshot
        "https://images.unsplash.com/photo-1538481199705-c710c4e965fc?w=500"  // Tournament trophy
    )
    var showGalleryPicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Chat Header with Context Switcher
        Text(
            text = "LIVE SUPPORT & COMMUNITY CHAT",
            fontWeight = FontWeight.Black,
            fontSize = 18.sp,
            color = GamingPrimaryGold,
            letterSpacing = 0.8.sp
        )
        Text(
            text = "24/48-Hour auto deletion active • 3 Message limit per user until admin replies",
            fontSize = 11.sp,
            color = GamingTextSecondary
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Context Switcher Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = activeContext == "GENERAL",
                onClick = { activeContext = "GENERAL" },
                label = { Text("General Help") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = GamingAccentCyan,
                    selectedLabelColor = GamingDarkBackground
                ),
                modifier = Modifier.testTag("chat_tab_general")
            )
            FilterChip(
                selected = activeContext == "FORGOT_PASSWORD",
                onClick = { activeContext = "FORGOT_PASSWORD" },
                label = { Text("Password Reset") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = GamingAccentPink,
                    selectedLabelColor = Color.White
                ),
                modifier = Modifier.testTag("chat_tab_forgot_password")
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Auto Deletion Notice Ticker
        Surface(
            color = GamingCardSurface,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, GamingGlassBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Timer, contentDescription = "Timer", tint = GamingAccentCyan, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Auto-Chat Clean: Messages automatically auto-expire after 24 hours.",
                    fontSize = 11.sp,
                    color = GamingTextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Message List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 12.dp)
        ) {
            items(chatMessages) { msg ->
                val isMe = msg.senderEmail == (if (isAdminMode) "admin@gaming.com" else currentUser?.email)

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
                ) {
                    Surface(
                        color = when {
                            msg.isAdmin -> GamingAccentPink.copy(alpha = 0.9f)
                            isMe -> GamingPrimaryGold
                            else -> GamingCardSurface
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.widthIn(max = 280.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (msg.isAdmin) "👑 Admin Support" else msg.senderName,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isMe || msg.isAdmin) GamingDarkBackground else GamingAccentCyan
                                )

                                // Edit button available for user's own text messages (no delete option per requirement)
                                if (isMe && !msg.isAdmin) {
                                    IconButton(
                                        onClick = {
                                            editingMessageId = msg.id
                                            editMessageText = msg.message
                                        },
                                        modifier = Modifier.size(20.dp).testTag("edit_msg_btn_${msg.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit Message",
                                            tint = GamingDarkBackground,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }

                            if (msg.imageUri != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Card(
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(140.dp)
                                ) {
                                    AsyncImage(
                                        model = msg.imageUri,
                                        contentDescription = "Attached Chat Image",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = msg.message,
                                fontSize = 14.sp,
                                color = if (isMe || msg.isAdmin) GamingDarkBackground else GamingTextPrimary,
                                fontWeight = FontWeight.Normal
                            )

                            if (msg.isEdited) {
                                Text(
                                    text = "(edited)",
                                    fontSize = 9.sp,
                                    color = if (isMe || msg.isAdmin) GamingDarkBackground.copy(alpha = 0.7f) else GamingTextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Attached Image Preview
        if (attachedImageUri != null) {
            Surface(
                color = GamingCardSurface,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = attachedImageUri,
                            contentDescription = "Preview Attached Image",
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Image Attached", fontSize = 12.sp, color = GamingTextPrimary)
                    }

                    IconButton(onClick = { attachedImageUri = null }) {
                        Icon(Icons.Default.Close, contentDescription = "Remove Image", tint = GamingErrorRed)
                    }
                }
            }
        }

        // Gallery Image Attachment Selector Modal/Sheet
        if (showGalleryPicker) {
            Surface(
                color = GamingCardSurface,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Select Image from Gallery", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = GamingPrimaryGold)
                        IconButton(onClick = { showGalleryPicker = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = GamingTextSecondary)
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        sampleGalleryImages.forEachIndexed { idx, url ->
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .size(80.dp)
                                    .clickable {
                                        attachedImageUri = url
                                        showGalleryPicker = false
                                    }
                            ) {
                                AsyncImage(
                                    model = url,
                                    contentDescription = "Gallery item $idx",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }
        }

        // Edit Message Dialog
        if (editingMessageId != null) {
            AlertDialog(
                onDismissRequest = { editingMessageId = null },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.editChatMessage(editingMessageId!!, editMessageText)
                            editingMessageId = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GamingPrimaryGold)
                    ) {
                        Text("Save Edit", color = GamingDarkBackground)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { editingMessageId = null }) {
                        Text("Cancel", color = GamingTextSecondary)
                    }
                },
                title = { Text("Edit Message", fontWeight = FontWeight.Bold, color = GamingTextPrimary) },
                text = {
                    OutlinedTextField(
                        value = editMessageText,
                        onValueChange = { editMessageText = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                containerColor = GamingCardSurface
            )
        }

        // Bottom Chat Input Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { showGalleryPicker = true },
                modifier = Modifier.testTag("chat_gallery_picker_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = "Select Image",
                    tint = GamingAccentCyan
                )
            }

            OutlinedTextField(
                value = messageInput,
                onValueChange = { messageInput = it },
                placeholder = {
                    Text(if (isAdminMode) "Reply as Admin..." else "Type support / chat message...")
                },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("input_live_chat_msg")
            )

            Spacer(modifier = Modifier.width(6.dp))

            IconButton(
                onClick = {
                    viewModel.sendChatMessage(
                        chatContext = activeContext,
                        message = messageInput,
                        imageUri = attachedImageUri
                    )
                    messageInput = ""
                    attachedImageUri = null
                },
                modifier = Modifier.testTag("send_live_chat_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send Message",
                    tint = GamingPrimaryGold
                )
            }
        }
    }
}
