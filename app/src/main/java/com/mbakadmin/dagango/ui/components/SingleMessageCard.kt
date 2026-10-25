package com.mbakadmin.dagango.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mbakadmin.dagango.data.local.entity.ChatLogEntity
import com.mbakadmin.dagango.ui.theme.AppDnaTokens
import java.text.SimpleDateFormat
import java.util.Date

/**
 * SingleMessageCard styled with Productivity Focus App DNA.
 */
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
        shape = RoundedCornerShape(AppDnaTokens.RadiusMicro),
        color = if (isLatest) AppDnaTokens.SurfaceCard else AppDnaTokens.SurfaceMuted.copy(alpha = 0.5f),
        border = BorderStroke(
            0.5.dp,
            if (isLatest) AppDnaTokens.PrimaryHero.copy(alpha = 0.3f) else AppDnaTokens.BorderCard
        ),
        shadowElevation = if (isLatest) AppDnaTokens.ElevationLevel1 else 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "#$index",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = AppDnaTokens.TextMicro,
                            fontWeight = FontWeight.Bold
                        ),
                        color = AppDnaTokens.PrimaryHero
                    )
                    if (isLatest) {
                        Surface(
                            shape = RoundedCornerShape(AppDnaTokens.RadiusPill),
                            color = AppDnaTokens.PrimaryHero
                        ) {
                            Text(
                                "TERBARU",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp
                                ),
                                color = AppDnaTokens.OnPrimaryHero,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    dateFormat.format(Date(msg.timestamp)),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = AppDnaTokens.TextMicro),
                    color = AppDnaTokens.ForegroundMuted
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "💬 \"${msg.incomingMessage}\"",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = AppDnaTokens.TextBody,
                    color = AppDnaTokens.Foreground
                )
            )

            if (msg.aiReply.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(AppDnaTokens.RadiusPill),
                    color = AppDnaTokens.SurfaceMuted,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "🤖 MbakAdmin: ${msg.aiReply}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = AppDnaTokens.TextCaption,
                            color = AppDnaTokens.Foreground
                        ),
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            if (msg.isPaymentClaimed) {
                Spacer(modifier = Modifier.height(10.dp))
                if (!msg.isVerifiedByHuman) {
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
                                "⏳ Perlu Verifikasi",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = AppDnaTokens.TextMicro,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = AppDnaTokens.RoseAlert,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Button(
                            onClick = { onVerifyClick(msg) },
                            colors = ButtonDefaults.buttonColors(containerColor = AppDnaTokens.PrimaryHero),
                            shape = RoundedCornerShape(AppDnaTokens.RadiusPill),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text("Verifikasi & Proses", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
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
                                color = AppDnaTokens.SuccessGreen.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(AppDnaTokens.RadiusPill)
                            ) {
                                Text(
                                    "✅ Pembayaran Terverifikasi",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = AppDnaTokens.TextMicro,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = AppDnaTokens.SuccessGreen,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            TextButton(onClick = { onCancelVerify(msg.id) }, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)) {
                                Text("Batal Verifikasi", style = MaterialTheme.typography.labelSmall, color = AppDnaTokens.ForegroundMuted)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        if (msg.trackingNumber.isBlank()) {
                            OutlinedButton(
                                onClick = { onShippingClick(msg) },
                                shape = RoundedCornerShape(AppDnaTokens.RadiusPill),
                                border = BorderStroke(0.5.dp, AppDnaTokens.BorderCard),
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(16.dp), tint = AppDnaTokens.PrimaryHero)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("🚚 Input Nomor Resi", style = MaterialTheme.typography.labelSmall, color = AppDnaTokens.PrimaryHero)
                            }
                        } else {
                            Surface(
                                color = AppDnaTokens.SurfaceMuted,
                                shape = RoundedCornerShape(AppDnaTokens.RadiusPill),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                    Text(
                                        "📦 Resi: ${msg.courierName} - ${msg.trackingNumber}",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = {
                                            val phone = customerPhone.ifBlank { msg.customerPhone }
                                            val waMsg = "Halo kak $senderName! Pesanan kakak sudah dikirim via ${msg.courierName} dengan no. resi: ${msg.trackingNumber}. Terima kasih! 🙏"
                                            onDirectSendWa(phone, waMsg)
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Send, contentDescription = "Kirim Resi ke WA", tint = AppDnaTokens.PrimaryHero)
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
