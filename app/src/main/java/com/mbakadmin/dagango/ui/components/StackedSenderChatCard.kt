package com.mbakadmin.dagango.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mbakadmin.dagango.data.local.entity.ChatLogEntity
import com.mbakadmin.dagango.ui.theme.AppDnaTokens
import java.text.SimpleDateFormat
import java.util.*

data class ChatSenderGroup(
    val senderName: String,
    val messages: List<ChatLogEntity>,
    val latestMessage: ChatLogEntity,
    val hasPendingVerification: Boolean,
    val pendingVerificationLog: ChatLogEntity?,
    val customerPhone: String
)

/**
 * StackedSenderChatCard styled with Productivity Focus App DNA.
 * Patterns: `grouped-feed`, `tinted-card-white-floating` (24dp radius, hairline border).
 */
@Composable
fun StackedSenderChatCard(
    group: ChatSenderGroup,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onVerifyClick: (ChatLogEntity) -> Unit,
    onShippingClick: (ChatLogEntity) -> Unit,
    onCancelVerify: (Long) -> Unit,
    onDirectSendWa: (String, String) -> Unit,
    dateFormat: SimpleDateFormat,
    timeFormat: SimpleDateFormat
) {
    val totalCount = group.messages.size

    Column(modifier = Modifier.fillMaxWidth()) {
        if (!isExpanded) {
            // === STATE COLLAPSED: WALLET-STYLE STACKED CARDS ===
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() }
                    .padding(bottom = if (totalCount > 2) 20.dp else if (totalCount == 2) 12.dp else 4.dp)
            ) {
                // Layer 3 (paling bawah/belakang)
                if (totalCount >= 3) {
                    Surface(
                        shape = RoundedCornerShape(AppDnaTokens.RadiusCard),
                        color = AppDnaTokens.SurfaceMuted.copy(alpha = 0.5f),
                        border = BorderStroke(0.5.dp, AppDnaTokens.BorderCard),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .offset(y = 16.dp)
                            .height(115.dp)
                    ) {}
                }

                // Layer 2 (tengah)
                if (totalCount >= 2) {
                    Surface(
                        shape = RoundedCornerShape(AppDnaTokens.RadiusCard),
                        color = if (group.hasPendingVerification) AppDnaTokens.RoseAlert.copy(alpha = 0.15f)
                        else AppDnaTokens.SurfaceMuted.copy(alpha = 0.85f),
                        border = BorderStroke(0.5.dp, AppDnaTokens.BorderCard),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                            .offset(y = 8.dp)
                            .height(115.dp)
                    ) {}
                }

                // Layer 1: Front Card (Pesan Terbaru)
                Surface(
                    shape = RoundedCornerShape(AppDnaTokens.RadiusCard),
                    color = AppDnaTokens.SurfaceCard,
                    border = if (group.hasPendingVerification) BorderStroke(1.5.dp, AppDnaTokens.RoseAlert)
                    else BorderStroke(0.5.dp, AppDnaTokens.BorderCard),
                    shadowElevation = AppDnaTokens.ElevationLevel1,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        "👤 ${group.senderName}",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = AppDnaTokens.Foreground
                                        )
                                    )
                                    if (totalCount > 1) {
                                        Surface(
                                            shape = RoundedCornerShape(AppDnaTokens.RadiusPill),
                                            color = AppDnaTokens.SurfaceMuted
                                        ) {
                                            Text(
                                                "$totalCount pesan",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontSize = AppDnaTokens.TextMicro,
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                color = AppDnaTokens.ForegroundMuted,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                if (group.customerPhone.isNotBlank()) {
                                    Text(
                                        "📱 ${group.customerPhone}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = AppDnaTokens.ForegroundMuted
                                        )
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    dateFormat.format(Date(group.latestMessage.timestamp)),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = AppDnaTokens.TextMicro),
                                    color = AppDnaTokens.ForegroundMuted
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    Icons.Default.Layers,
                                    contentDescription = "Stacked Chat",
                                    tint = AppDnaTokens.PrimaryHero,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Banner verifikasi langsung di front card jika butuh verifikasi
                        if (group.hasPendingVerification) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Surface(
                                    color = AppDnaTokens.RoseAlert.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(AppDnaTokens.RadiusPill)
                                ) {
                                    Text(
                                        "⏳ Perlu Verifikasi Pembayaran",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = AppDnaTokens.TextMicro,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = AppDnaTokens.RoseAlert,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                group.pendingVerificationLog?.let { pLog ->
                                    Button(
                                        onClick = { onVerifyClick(pLog) },
                                        colors = ButtonDefaults.buttonColors(containerColor = AppDnaTokens.PrimaryHero),
                                        shape = RoundedCornerShape(AppDnaTokens.RadiusPill),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Verifikasi", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "💬 \"${group.latestMessage.incomingMessage}\"",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = AppDnaTokens.Foreground
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (group.latestMessage.aiReply.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "🤖 ${group.latestMessage.aiReply}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = AppDnaTokens.ForegroundMuted
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        } else {
            // === STATE EXPANDED: SINGLE OPEN THREAD ===
            Surface(
                shape = RoundedCornerShape(AppDnaTokens.RadiusCard),
                color = AppDnaTokens.SurfaceCard,
                border = BorderStroke(1.dp, AppDnaTokens.PrimaryHero.copy(alpha = 0.5f)),
                shadowElevation = AppDnaTokens.ElevationLevel2,
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Top Bar Navigasi thread yang sedang dibuka
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(onClick = onToggleExpand, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Kembali ke Stack", tint = AppDnaTokens.PrimaryHero)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    "👤 ${group.senderName}",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = AppDnaTokens.Foreground
                                    )
                                )
                                Surface(
                                    shape = RoundedCornerShape(AppDnaTokens.RadiusPill),
                                    color = AppDnaTokens.SurfaceMuted
                                ) {
                                    Text(
                                        "$totalCount pesan",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = AppDnaTokens.TextMicro),
                                        color = AppDnaTokens.ForegroundMuted,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            if (group.customerPhone.isNotBlank()) {
                                Text("📱 ${group.customerPhone}", style = MaterialTheme.typography.labelSmall, color = AppDnaTokens.ForegroundMuted)
                            }
                        }
                        TextButton(onClick = onToggleExpand, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) {
                            Text("Tutup", style = MaterialTheme.typography.labelSmall, color = AppDnaTokens.PrimaryHero)
                        }
                    }

                    if (group.hasPendingVerification) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(AppDnaTokens.RadiusMicro),
                            color = AppDnaTokens.RoseAlert.copy(alpha = 0.15f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    "⏳ Pesanan menunggu verifikasi pembayaran",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = AppDnaTokens.TextMicro,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = AppDnaTokens.RoseAlert
                                )
                                group.pendingVerificationLog?.let { pLog ->
                                    Button(
                                        onClick = { onVerifyClick(pLog) },
                                        colors = ButtonDefaults.buttonColors(containerColor = AppDnaTokens.PrimaryHero),
                                        shape = RoundedCornerShape(AppDnaTokens.RadiusPill),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("Verifikasi Sekarang", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(thickness = 0.5.dp, color = AppDnaTokens.BorderCard)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Riwayat Pesan (Paling Baru di Atas):",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = AppDnaTokens.TextMicro,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        ),
                        color = AppDnaTokens.ForegroundMuted
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // List pesan: group.messages sudah diurutkan dari terbaru ke terlama
                    Column(verticalArrangement = Arrangement.spacedBy(AppDnaTokens.GapFeed)) {
                        group.messages.forEachIndexed { idx, msg ->
                            val isLatest = idx == 0
                            SingleMessageCard(
                                index = totalCount - idx,
                                msg = msg,
                                isLatest = isLatest,
                                customerPhone = group.customerPhone,
                                senderName = group.senderName,
                                onVerifyClick = onVerifyClick,
                                onShippingClick = onShippingClick,
                                onCancelVerify = onCancelVerify,
                                onDirectSendWa = onDirectSendWa,
                                dateFormat = dateFormat
                            )
                        }
                    }
                }
            }
        }
    }
}
