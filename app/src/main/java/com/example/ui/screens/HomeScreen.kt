package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.db.NoticeEntity
import com.example.data.db.TournamentEntity
import com.example.data.db.TournamentRegistrationEntity
import com.example.ui.components.NoticeBannerCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.Screen

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    tournaments: List<TournamentEntity>,
    notices: List<NoticeEntity>,
    userRegistrations: List<TournamentRegistrationEntity>
) {
    var selectedFilter by remember { mutableStateOf("All") }
    val filters = listOf("All", "Solo", "Duo", "Squad")

    val registeredTournamentIds = remember(userRegistrations) {
        userRegistrations.map { it.tournamentId }.toSet()
    }

    val pinnedImages = remember(tournaments) {
        tournaments.mapNotNull { it.pinnedImageUri }.filter { it.isNotEmpty() }
    }

    val filteredTournaments = remember(tournaments, selectedFilter) {
        if (selectedFilter == "All") tournaments
        else tournaments.filter { it.gameMode.equals(selectedFilter, ignoreCase = true) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 20.dp)
    ) {
        // Hero Banner Artwork
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = GamingCardSurface),
                border = BorderStroke(1.dp, GamingGlassBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_hero_banner),
                        contentDescription = "Esports Hero Banner",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.35f))
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Surface(
                            color = GamingPrimaryGold,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "OFFICIAL TOURNAMENTS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "DOMINATE THE ARENA & WIN CASH",
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Pinned Admin Images Banner Section
        if (pinnedImages.isNotEmpty()) {
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "Pinned Tournament Banners",
                            tint = GamingAccentPink,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "PINNED ANNOUNCEMENTS",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = GamingAccentPink,
                            letterSpacing = 1.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(pinnedImages) { uri ->
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .width(260.dp)
                                    .height(130.dp)
                            ) {
                                AsyncImage(
                                    model = uri,
                                    contentDescription = "Admin Pinned Image",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        // Global Notices
        if (notices.isNotEmpty()) {
            item {
                Text(
                    text = "LATEST NOTICES",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = GamingAccentCyan,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
            }
            items(notices) { notice ->
                NoticeBannerCard(notice = notice)
            }
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Tournament Filters
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LIVE TOURNAMENTS",
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    color = GamingTextPrimary,
                    letterSpacing = 0.5.sp
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(filters) { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = { Text(filter, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GamingPrimaryGold,
                                selectedLabelColor = Color.White,
                                containerColor = GamingCardSurface,
                                labelColor = GamingTextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selectedFilter == filter,
                                borderColor = GamingGlassBorder,
                                selectedBorderColor = GamingPrimaryGold
                            ),
                            modifier = Modifier.testTag("filter_$filter")
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Tournament List
        items(filteredTournaments) { tournament ->
            val isJoined = registeredTournamentIds.contains(tournament.id)

            Card(
                colors = CardDefaults.cardColors(containerColor = GamingCardSurface),
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, GamingGlassBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable {
                        viewModel.navigateTo(Screen.TournamentDetail(tournament.id))
                    }
                    .testTag("tournament_card_${tournament.id}")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = GamingSurface,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, GamingPrimaryGold.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "${tournament.gameMode.uppercase()} • ${tournament.map}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = GamingPrimaryGold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        if (isJoined) {
                            Surface(
                                color = GamingSuccessGreen.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, GamingSuccessGreen.copy(alpha = 0.3f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Joined",
                                        tint = GamingSuccessGreen,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "JOINED",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GamingSuccessGreen
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = tournament.scheduleTime,
                                fontSize = 12.sp,
                                color = GamingAccentCyan,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = tournament.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = GamingTextPrimary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Metrics: Prize Pool, Per Kill, Entry Fee
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
                            Text(
                                "৳${tournament.prizePool.toInt()}",
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                color = GamingSuccessGreen
                            )
                        }
                        HorizontalDivider(
                            modifier = Modifier
                                .height(28.dp)
                                .width(1.dp),
                            color = GamingGlassSubtleBorder
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("PER KILL", fontSize = 10.sp, color = GamingTextSecondary)
                            Text(
                                "৳${tournament.perKill.toInt()}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = GamingAccentCyan
                            )
                        }
                        HorizontalDivider(
                            modifier = Modifier
                                .height(28.dp)
                                .width(1.dp),
                            color = GamingGlassSubtleBorder
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("ENTRY FEE", fontSize = 10.sp, color = GamingTextSecondary)
                            Text(
                                "৳${tournament.entryFee.toInt()}",
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                color = GamingPrimaryGold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Slot Progress Bar
                    val progress = (tournament.slotsFilled.toFloat() / tournament.slotsTotal.toFloat()).coerceIn(0f, 1f)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Slots Filled: ${tournament.slotsFilled}/${tournament.slotsTotal}",
                            fontSize = 12.sp,
                            color = GamingTextSecondary
                        )
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = GamingPrimaryGold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = GamingPrimaryGold,
                        trackColor = GamingGlassSubtleBorder
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Action Button
                    Button(
                        onClick = {
                            viewModel.navigateTo(Screen.TournamentDetail(tournament.id))
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isJoined) GamingSuccessGreen else GamingPrimaryGold
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_action_${tournament.id}")
                    ) {
                        Text(
                            text = if (isJoined) "VIEW TOURNAMENT INFO & ROOM ID" else "JOIN TOURNAMENT",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
