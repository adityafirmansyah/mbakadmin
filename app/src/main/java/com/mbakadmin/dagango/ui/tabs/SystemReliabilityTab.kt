package com.mbakadmin.dagango.ui.tabs

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mbakadmin.dagango.ui.components.StatusCard
import com.mbakadmin.dagango.ui.dialogs.CrashLogViewerDialog
import com.mbakadmin.dagango.ui.theme.AppDnaTokens
import com.mbakadmin.dagango.util.BatteryOptimizationHelper
import com.mbakadmin.dagango.util.CrashLogger

@Composable
fun SystemReliabilityTab(
    context: Context,
    isListenerEnabled: Boolean,
    isBatteryOptimized: Boolean
) {
    var showCrashLogDialog by remember { mutableStateOf(false) }
    var hasCrashLogs by remember { mutableStateOf(CrashLogger.getAllCrashLogs(context) != null) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(AppDnaTokens.GapFeed)
    ) {
        Text(
            "STATUS KESIAPAN SISTEM",
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = AppDnaTokens.TextEyebrow,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            ),
            color = AppDnaTokens.ForegroundMuted
        )

        StatusCard(
            title = if (isListenerEnabled) "Izin Notifikasi WhatsApp Aktif" else "Izin Notifikasi Belum Aktif",
            subtitle = if (isListenerEnabled) "MbakAdmin dapat mendeteksi chat masuk" else "Wajib diaktifkan agar bot bisa merespons chat",
            isOk = isListenerEnabled,
            actionLabel = "Buka Izin",
            onActionClick = {
                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
        )

        StatusCard(
            title = if (!isBatteryOptimized) "Baterai Bebas Batasan (Aman)" else "Optimasi Baterai Masih Aktif",
            subtitle = if (!isBatteryOptimized) "Android tidak akan mematikan bot saat layar mati" else "HP dapat mematikan bot sewaktu-waktu demi hemat baterai",
            isOk = !isBatteryOptimized,
            actionLabel = "Matikan Batasan",
            onActionClick = {
                BatteryOptimizationHelper.requestIgnoreBatteryOptimizations(context)
            }
        )

        Surface(
            shape = RoundedCornerShape(AppDnaTokens.RadiusCard),
            color = AppDnaTokens.SurfaceCard,
            border = BorderStroke(0.5.dp, AppDnaTokens.BorderCard),
            shadowElevation = AppDnaTokens.ElevationLevel1,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(AppDnaTokens.RadiusMicro),
                    color = AppDnaTokens.SurfaceMuted,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = null,
                            tint = AppDnaTokens.PrimaryHero,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Auto-Start Khusus HP (${Build.MANUFACTURER.uppercase()})",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = AppDnaTokens.Foreground
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        "Izinkan app berjalan otomatis saat HP dinyalakan",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = AppDnaTokens.ForegroundMuted
                        )
                    )
                }

                TextButton(
                    onClick = {
                        BatteryOptimizationHelper.openOemAutoStartSettings(context)
                    }
                ) {
                    Text(
                        "Buka",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = AppDnaTokens.PrimaryHero
                    )
                }
            }
        }

        // --- SECTION DIAGNOSTIK & LOG CRASH ---
        Surface(
            shape = RoundedCornerShape(AppDnaTokens.RadiusCard),
            color = AppDnaTokens.SurfaceCard,
            border = BorderStroke(
                0.5.dp,
                if (hasCrashLogs) AppDnaTokens.RoseAlert.copy(alpha = 0.5f) else AppDnaTokens.BorderCard
            ),
            shadowElevation = AppDnaTokens.ElevationLevel1,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(AppDnaTokens.RadiusMicro),
                    color = if (hasCrashLogs) AppDnaTokens.RoseAlert.copy(alpha = 0.12f) else AppDnaTokens.SurfaceMuted,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.BugReport,
                            contentDescription = null,
                            tint = if (hasCrashLogs) AppDnaTokens.RoseAlert else AppDnaTokens.PrimaryHero,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (hasCrashLogs) "Ditemukan Log Crash / Masalah" else "Log Diagnostik & Crash",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = AppDnaTokens.Foreground
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        if (hasCrashLogs) "Ada catatan crash sistem. Buka untuk menyalin log."
                        else "Tidak ada crash fatal. Sistem dalam keadaan optimal.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = AppDnaTokens.ForegroundMuted
                        )
                    )
                }

                Button(
                    onClick = { showCrashLogDialog = true },
                    shape = RoundedCornerShape(AppDnaTokens.RadiusPill),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (hasCrashLogs) AppDnaTokens.RoseAlert else AppDnaTokens.PrimaryHero
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        "Lihat Log",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = AppDnaTokens.OnPrimaryHero
                    )
                }
            }
        }
    }

    if (showCrashLogDialog) {
        CrashLogViewerDialog(
            onDismiss = { showCrashLogDialog = false },
            onLogsCleared = {
                hasCrashLogs = false
            }
        )
    }
}
