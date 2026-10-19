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
    val messages: List<ChatLogEntity>,
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        // Visual efek stacked card lapis belakang saat status ciut (stacked)
        if (!isExpanded && group.messages.size > 1) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = if (group.hasPendingVerification) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .offset(y = 6.dp)
                    .height(68.dp)
            ) {}
        }

        Card(
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(
                containerColor = if (group.hasPendingVerification) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f)
                else MaterialTheme.colorScheme.surface
            ),
            border = if (group.hasPendingVerification) BorderStroke(1.5.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.65f))
            else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Header (Nama, Jumlah Pesan, Waktu, Toggle)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleExpand() }
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("👤 ${group.senderName}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            if (group.messages.size > 1) {
                                Surface(shape = MaterialTheme.shapes.extraSmall, color = MaterialTheme.colorScheme.secondaryContainer) {
                                    Text("${group.messages.size} pesan", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                        }
                        if (group.customerPhone.isNotBlank()) {
                            Text("📱 ${group.customerPhone}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(dateFormat.format(Date(group.latestMessage.timestamp)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        IconButton(onClick = onToggleExpand, modifier = Modifier.size(32.dp)) {
                            Icon(if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                        }
                    }
                }

                // Banner Verifikasi (tetap di paling atas dalam state stacked maupun expand)
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
                            Button(onClick = { onVerifyClick(pLog) }, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Verifikasi & Proses", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                if (!isExpanded) {
                    // Preview pesan terakhir (state tertutup)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("💬 \"${group.latestMessage.incomingMessage}\"", style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    if (group.latestMessage.aiReply.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("🤖 ${group.latestMessage.aiReply}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                } else {
                    // Riwayat pesan terurut waktu dari lama ke baru
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Riwayat Percakapan (Urut Waktu):", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        group.messages.forEachIndexed { index, msg ->
                            SingleMessageCard(
                                index = index,
                                msg = msg,
                                customerPhone = group.customerPhone,
                                senderName = group.senderName,
                                onVerifyClick = onVerifyClick,
                                onShippingClick = onShippingClick,
                                onCancelVerify = onCancelVerify,
                                onDirectSendWa = onDirectSendWa,
                                timeFormat = timeFormat
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SingleMessageCard(
    index: Int,
    msg: ChatLogEntity,
    customerPhone: String,
    senderName: String,
    onVerifyClick: (ChatLogEntity) -> Unit,
    onShippingClick: (ChatLogEntity) -> Unit,
    onCancelVerify: (Long) -> Unit,
    onDirectSendWa: (String, String) -> Unit,
    timeFormat: SimpleDateFormat
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("#${index + 1}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(timeFormat.format(Date(msg.timestamp)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text("💬 \"${msg.incomingMessage}\"", style = MaterialTheme.typography.bodyMedium)

            if (msg.aiReply.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(shape = MaterialTheme.shapes.extraSmall, color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f), modifier = Modifier.fillMaxWidth()) {
                    Text("🤖 MbakAdmin: ${msg.aiReply}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(6.dp))
                }
            }

            if (msg.isPaymentClaimed) {
                Spacer(modifier = Modifier.height(8.dp))
                if (!msg.isVerifiedByHuman) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Surface(color = MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.extraSmall) {
                            Text("⏳ Perlu Verifikasi", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                        Button(onClick = { onVerifyClick(msg) }, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)) {
                            Text("Verifikasi & Proses", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                } else {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.extraSmall) {
                                Text("✅ Pembayaran Terverifikasi", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                            TextButton(onClick = { onCancelVerify(msg.id) }, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)) {
                                Text("Batal Verifikasi", style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        if (msg.trackingNumber.isBlank()) {
                            OutlinedButton(onClick = { onShippingClick(msg) }, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)) {
                                Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("🚚 Input Nomor Resi", style = MaterialTheme.typography.labelSmall)
                            }
                        } else {
                            Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.extraSmall, modifier = Modifier.fillMaxWidth()) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.padding(6.dp)) {
                                    Text("📦 Resi: ${msg.courierName} - ${msg.trackingNumber}", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                                    IconButton(
                                        onClick = {
                                            val phone = customerPhone.ifBlank { msg.customerPhone }
                                            val waMsg = "Halo kak $senderName! Pesanan kakak sudah dikirim via ${msg.courierName} dengan no. resi: ${msg.trackingNumber}. Terima kasih! 🙏"
                                            onDirectSendWa(phone, waMsg)
                                        },
                                        modifier = Modifier.size(26.dp)
                                    ) {
                                        Icon(Icons.Default.Send, contentDescription = "Kirim Resi ke WA", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
