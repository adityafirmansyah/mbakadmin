package com.mbakadmin.dagango.ui.tabs

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mbakadmin.dagango.ui.components.StatusCard
import com.mbakadmin.dagango.util.BatteryOptimizationHelper

@Composable
fun SystemReliabilityTab(
    context: Context,
    isListenerEnabled: Boolean,
    isBatteryOptimized: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Status Kesiapan Sistem", style = MaterialTheme.typography.titleLarge)

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

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Auto-Start Khusus HP (${Build.MANUFACTURER.uppercase()})", style = MaterialTheme.typography.titleSmall)
                    Text("Izinkan app berjalan otomatis saat HP dinyalakan", style = MaterialTheme.typography.bodySmall)
                }
                TextButton(onClick = {
                    BatteryOptimizationHelper.openOemAutoStartSettings(context)
                }) {
                    Text("Buka")
                }
            }
        }
    }
}
