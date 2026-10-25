package com.mbakadmin.dagango.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mbakadmin.dagango.ui.theme.AppDnaTokens

/**
 * Mandatory Pattern: `hero-saturated`
 * One saturated stage per screen. 32dp corner radius, 20dp internal padding,
 * deep charcoal background with high-contrast content.
 */
@Composable
fun HeroSaturatedCard(
    storeName: String,
    isAutoReplyActive: Boolean,
    isListenerEnabled: Boolean,
    onToggleAutoReply: (Boolean) -> Unit,
    activeProductsCount: Int,
    pendingVerificationsCount: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(AppDnaTokens.RadiusHero),
        color = AppDnaTokens.PrimaryHero,
        shadowElevation = AppDnaTokens.ElevationLevel2,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Eyebrow Micro-Label
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "${storeName.uppercase().ifBlank { "MbakAdmin Toko" }} • WHATSAPP LIVE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = AppDnaTokens.TextMicro,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    ),
                    color = AppDnaTokens.AccentCyan
                )

                if (!isListenerEnabled) {
                    Surface(
                        shape = RoundedCornerShape(AppDnaTokens.RadiusPill),
                        color = AppDnaTokens.RoseAlert.copy(alpha = 0.25f)
                    ) {
                        Text(
                            text = "PERLU IZIN",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = AppDnaTokens.RoseAlert,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main Status Title & High-Contrast Switch
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (!isListenerEnabled) "Perlu Izin Notifikasi"
                        else if (isAutoReplyActive) "MbakAdmin Aktif 🤖"
                        else "Auto-Reply Dijeda ⏸️",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = AppDnaTokens.OnPrimaryHero
                        )
                    )
                    Text(
                        text = if (!isListenerEnabled) "Aktifkan izin notifikasi di tab Sistem"
                        else if (isAutoReplyActive) "Memantau & membalas chat pembeli"
                        else "Aktifkan agar bot membalas otomatis",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = AppDnaTokens.ForegroundSubtle
                        )
                    )
                }

                Switch(
                    checked = isAutoReplyActive,
                    onCheckedChange = onToggleAutoReply,
                    modifier = Modifier.scale(0.85f),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = AppDnaTokens.PrimaryHero,
                        checkedTrackColor = AppDnaTokens.OnPrimaryHero,
                        uncheckedThumbColor = AppDnaTokens.ForegroundMuted,
                        uncheckedTrackColor = AppDnaTokens.SurfaceMuted.copy(alpha = 0.2f)
                    ),
                    thumbContent = {
                        Icon(
                            imageVector = if (isAutoReplyActive) Icons.Default.Check else Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(SwitchDefaults.IconSize)
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stat pills inside Hero stage
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Product count pill
                Surface(
                    shape = RoundedCornerShape(AppDnaTokens.RadiusMicro),
                    color = AppDnaTokens.OnPrimaryHero.copy(alpha = 0.1f),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "$activeProductsCount",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = AppDnaTokens.OnPrimaryHero
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Produk Aktif",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = AppDnaTokens.ForegroundSubtle
                            )
                        )
                    }
                }

                // Pending Verification count pill
                Surface(
                    shape = RoundedCornerShape(AppDnaTokens.RadiusMicro),
                    color = if (pendingVerificationsCount > 0) AppDnaTokens.RoseAlert.copy(alpha = 0.25f)
                    else AppDnaTokens.OnPrimaryHero.copy(alpha = 0.1f),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "$pendingVerificationsCount",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (pendingVerificationsCount > 0) AppDnaTokens.RoseAlert else AppDnaTokens.OnPrimaryHero
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (pendingVerificationsCount > 0) "Perlu Cek" else "Verifikasi",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (pendingVerificationsCount > 0) AppDnaTokens.RoseAlert else AppDnaTokens.ForegroundSubtle
                            )
                        )
                    }
                }
            }
        }
    }
}
