package com.mbakadmin.dagango.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mbakadmin.dagango.data.local.entity.ChatLogEntity
import java.text.SimpleDateFormat
import java.util.Date

@Composable
fun SingleMessageCard(
    index: Int,
    msg: ChatLogEntity,
    isLatest: Boolean,
    customerPhone: String,
    senderName: String,
    onVerifyClick: (ChatLogEntity) -> Unit,
    onShippingClick: (ChatLogEntity) -> Unit,
    onCancelVerify: (Long) -> Unit,
    onDirectSendWa: (String, String) -> Unit,
    dateFormat: SimpleDateFormat
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = if (isLatest) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("#$index", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    if (isLatest) {
                        Surface(shape = MaterialTheme.shapes.extraSmall, color = MaterialTheme.colorScheme.primary) {
                            Text("Terbaru", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                        }
                    }
                }
                Text(dateFormat.format(Date(msg.timestamp)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text("💬 \"${msg.incomingMessage}\"", style = MaterialTheme.typography.bodyMedium)

            if (msg.aiReply.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = MaterialTheme.shapes.extraSmall,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("🤖 MbakAdmin: ${msg.aiReply}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(6.dp))
                }
            }

            if (msg.isPaymentClaimed) {
                Spacer(modifier = Modifier.height(8.dp))
                if (!msg.isVerifiedByHuman) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(color = MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.extraSmall) {
                            Text("⏳ Perlu Verifikasi", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                        Button(onClick = { onVerifyClick(msg) }, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)) {
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
