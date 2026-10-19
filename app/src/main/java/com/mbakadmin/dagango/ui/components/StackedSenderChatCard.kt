package com.mbakadmin.dagango.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mbakadmin.dagango.data.local.entity.ChatLogEntity
import java.text.SimpleDateFormat
import java.util.*

data class ChatSenderGroup(
    val senderName: String,
    val messages: List<ChatLogEntity>, // Diurutkan dari yang terbaru (index 0) ke yang terlama
    val latestMessage: ChatLogEntity,
    val hasPendingVerification: Boolean,
    val pendingVerificationLog: ChatLogEntity?,
    val customerPhone: String
)

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
            // Kartu teratas adalah pesan paling baru, di bawahnya mengintip kartu-kartu sebelumnya
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() }
                    .padding(bottom = if (totalCount > 2) 20.dp else if (totalCount == 2) 12.dp else 4.dp)
            ) {
                // Layer 3 (paling bawah/belakang, mengintip di paling bawah tumpukan)
                if (totalCount >= 3) {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .offset(y = 16.dp)
                            .height(115.dp)
                    ) {}
                }

                // Layer 2 (tengah, mengintip di antara layer 1 & 3)
                if (totalCount >= 2) {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = if (group.hasPendingVerification) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                            .offset(y = 8.dp)
                            .height(115.dp)
                    ) {}
                }

                // Layer 1: Front Card (Pesan Terbaru)
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = if (group.hasPendingVerification) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f)
                        else MaterialTheme.colorScheme.surface
                    ),
                    border = if (group.hasPendingVerification) BorderStroke(1.5.dp, MaterialTheme.colorScheme.error)
                    else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("👤 ${group.senderName}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    if (totalCount > 1) {
                                        Surface(shape = MaterialTheme.shapes.extraSmall, color = MaterialTheme.colorScheme.secondaryContainer) {
                                            Text("$totalCount pesan", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                        }
                                    }
                                }
                                if (group.customerPhone.isNotBlank()) {
                                    Text("📱 ${group.customerPhone}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(dateFormat.format(Date(group.latestMessage.timestamp)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.Layers, contentDescription = "Stacked Chat", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            }
                        }

                        // Banner verifikasi langsung di front card jika butuh verifikasi
                        if (group.hasPendingVerification) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Surface(color = MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.extraSmall) {
                                    Text("⏳ Perlu Verifikasi Pembayaran", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontWeight = FontWeight.SemiBold)
                                }
                                group.pendingVerificationLog?.let { pLog ->
                                    Button(onClick = { onVerifyClick(pLog) }, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Verifikasi", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "💬 \"${group.latestMessage.incomingMessage}\"",
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (group.latestMessage.aiReply.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "🤖 ${group.latestMessage.aiReply}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        } else {
            // === STATE EXPANDED: SINGLE OPEN THREAD, URUT DARI TERBARU (TOP) KE TERLAMA (BOTTOM) ===
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Top Bar Navigasi thread yang sedang dibuka
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(onClick = onToggleExpand, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Kembali ke Stack", tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("👤 ${group.senderName}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Surface(shape = MaterialTheme.shapes.extraSmall, color = MaterialTheme.colorScheme.secondaryContainer) {
                                    Text("$totalCount pesan", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                            if (group.customerPhone.isNotBlank()) {
                                Text("📱 ${group.customerPhone}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        TextButton(onClick = onToggleExpand, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) {
                            Text("Tutup", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    if (group.hasPendingVerification) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("⏳ Pesanan menunggu verifikasi pembayaran", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onErrorContainer, fontWeight = FontWeight.Bold)
                                group.pendingVerificationLog?.let { pLog ->
                                    Button(onClick = { onVerifyClick(pLog) }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) {
                                        Text("Verifikasi Sekarang", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Riwayat Pesan (Paling Baru di Atas):",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // List pesan: group.messages sudah diurutkan dari terbaru ke terlama
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
