package com.mbakadmin.dagango.ui.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mbakadmin.dagango.data.local.entity.ChatLogEntity
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ChatLogsTab(
    chatLogs: List<ChatLogEntity>,
    onVerifyClick: (ChatLogEntity) -> Unit = {},
    onShippingClick: (ChatLogEntity) -> Unit = {},
    onCancelVerify: (Long) -> Unit = {},
    onDirectSendWa: (String, String) -> Unit = { _, _ -> }
) {
    if (chatLogs.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.MarkChatRead, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Belum ada riwayat chat otomatis.", style = MaterialTheme.typography.bodyMedium)
                Text("Chat WhatsApp yang dibalas MbakAdmin akan muncul di sini.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
        }
    } else {
        val dateFormat = remember { SimpleDateFormat("HH:mm - dd MMM", Locale("id", "ID")) }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(chatLogs) { log ->
                val needsVerification = log.isPaymentClaimed && !log.isVerifiedByHuman
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (needsVerification) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f)
                        else MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "👤 ${log.senderName}",
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = dateFormat.format(Date(log.timestamp)),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "💬 \"${log.incomingMessage}\"",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "🤖 MbakAdmin: ${log.aiReply}",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(8.dp)
                            )
                        }

                        // Human Verification & Fulfillment Section
                        if (log.isPaymentClaimed) {
                            Spacer(modifier = Modifier.height(10.dp))
                            if (!log.isVerifiedByHuman) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.errorContainer,
                                        shape = MaterialTheme.shapes.extraSmall
                                    ) {
                                        Text(
                                            text = "⏳ Perlu Verifikasi Pembayaran",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    Button(
                                        onClick = { onVerifyClick(log) },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Verifikasi & Proses", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            } else {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            shape = MaterialTheme.shapes.extraSmall
                                        ) {
                                            Text(
                                                text = "✅ Pembayaran Terverifikasi",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                        TextButton(onClick = { onCancelVerify(log.id) }) {
                                            Text("Batal Verifikasi", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Shipping / Resi Info
                                    if (log.trackingNumber.isBlank()) {
                                        OutlinedButton(
                                            onClick = { onShippingClick(log) },
                                            modifier = Modifier.fillMaxWidth(),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                        ) {
                                            Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("🚚 Input Nomor Resi Pengiriman", style = MaterialTheme.typography.labelSmall)
                                        }
                                    } else {
                                        Surface(
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                            shape = MaterialTheme.shapes.small,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier.padding(8.dp)
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "📦 Resi: ${log.courierName} - ${log.trackingNumber}",
                                                        style = MaterialTheme.typography.labelMedium
                                                    )
                                                }
                                                IconButton(
                                                    onClick = {
                                                        val msg = "Halo kak ${log.senderName}! Pesanan kakak sudah dikirim via ${log.courierName} dengan no. resi: ${log.trackingNumber}. Terima kasih sudah belanja di toko kami! 🙏"
                                                        onDirectSendWa(log.customerPhone, msg)
                                                    },
                                                    modifier = Modifier.size(28.dp)
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
        }
    }
}
