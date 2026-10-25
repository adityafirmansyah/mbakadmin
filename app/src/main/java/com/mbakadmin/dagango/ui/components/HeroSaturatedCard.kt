package com.mbakadmin.dagango.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mbakadmin.dagango.ui.theme.AppDnaTokens

/**
 * Compact Header: `hero-saturated`
 * Ultra high-density dark charcoal header anchor (#181A1F).
 * Preserves all critical data & controls (Store Name, Live Status, Active Count, Switch)
 * while reducing vertical space consumption from ~190dp to ~56dp.
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
        shape = RoundedCornerShape(AppDnaTokens.RadiusCard),
        color = AppDnaTokens.PrimaryHero,
        shadowElevation = AppDnaTokens.ElevationLevel2,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // --- LEFT COLUMN: LIVE INDICATOR + STORE & STATUS TITLE ---
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                // Micro Eyebrow with Status Dot
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(
                                color = if (!isListenerEnabled) AppDnaTokens.RoseAlert
                                else if (isAutoReplyActive) AppDnaTokens.AccentGreenVibrant
                                else AppDnaTokens.ForegroundMuted,
                                shape = CircleShape
                            )
                    )
                    Text(
                        text = if (!isListenerEnabled) "PERLU IZIN NOTIFIKASI"
                        else "${storeName.uppercase().ifBlank { "MBAKADMIN" }} • ${if (isAutoReplyActive) "WA LIVE" else "DIJEDA"}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = if (!isListenerEnabled) AppDnaTokens.RoseAlert else AppDnaTokens.AccentGreenVibrant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Main Status Headline
                Text(
                    text = if (!isListenerEnabled) "Izin Notifikasi Belum Aktif"
                    else if (isAutoReplyActive) "Auto-Reply Aktif 🤖"
                    else "Auto-Reply Dijeda ⏸️",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppDnaTokens.OnPrimaryHero
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // --- RIGHT CONTROLS: STAT PILLS & HIGH-CONTRAST SWITCH ---
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Pending Verification Badge (High priority alert)
                if (pendingVerificationsCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(AppDnaTokens.RadiusPill),
                        color = AppDnaTokens.RoseAlert.copy(alpha = 0.25f)
                    ) {
                        Text(
                            text = "$pendingVerificationsCount Cek",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = AppDnaTokens.RoseAlert,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                } else {
                    // Active Product Count Pill
                    Surface(
                        shape = RoundedCornerShape(AppDnaTokens.RadiusPill),
                        color = AppDnaTokens.OnPrimaryHero.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "$activeProductsCount Produk",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = AppDnaTokens.OnPrimaryHero,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Switch Toggle
                Switch(
                    checked = isAutoReplyActive,
                    onCheckedChange = onToggleAutoReply,
                    modifier = Modifier.scale(0.8f),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = AppDnaTokens.PrimaryHero,
                        checkedTrackColor = AppDnaTokens.OnPrimaryHero,
                        uncheckedThumbColor = AppDnaTokens.ForegroundMuted,
                        uncheckedTrackColor = AppDnaTokens.SurfaceMuted.copy(alpha = 0.25f)
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
        }
    }
}
