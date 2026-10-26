package com.mbakadmin.dagango.ui.dialogs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mbakadmin.dagango.ui.theme.AppDnaTokens
import com.mbakadmin.dagango.util.CrashLogger

/**
 * CrashLogViewerDialog
 * Menampilkan log crash fatal atau exception yang tertangkap di aplikasi
 * dengan tombol Salin Log dan Bersihkan Log.
 */
@Composable
fun CrashLogViewerDialog(
    onDismiss: () -> Unit,
    onLogsCleared: () -> Unit
) {
    val context = LocalContext.current
    val crashLogs = CrashLogger.getAllCrashLogs(context) ?: "Tidak ada log crash tercatat. Sistem dalam kondisi sehat."

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(AppDnaTokens.RadiusHero),
            color = AppDnaTokens.SurfaceCard,
            border = BorderStroke(0.5.dp, AppDnaTokens.BorderCard),
            shadowElevation = AppDnaTokens.ElevationLevel2,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppDnaTokens.MarginHorizontal, vertical = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = "DIAGNOSTIK SISTEM",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = AppDnaTokens.TextMicro,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp
                            ),
                            color = AppDnaTokens.RoseAlert
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Riwayat Crash Log",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = AppDnaTokens.Foreground
                            )
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = AppDnaTokens.ForegroundMuted)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(thickness = 0.5.dp, color = AppDnaTokens.BorderCard)
                Spacer(modifier = Modifier.height(14.dp))

                // Log Content Scrollable Box
                Surface(
                    shape = RoundedCornerShape(AppDnaTokens.RadiusMicro),
                    color = AppDnaTokens.PrimaryHero,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .heightIn(max = 380.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = crashLogs,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            ),
                            color = AppDnaTokens.OnPrimaryHero
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(thickness = 0.5.dp, color = AppDnaTokens.BorderCard)
                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons: Salin Log & Bersihkan Log
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = {
                            CrashLogger.clearLogs(context)
                            onLogsCleared()
                            Toast.makeText(context, "Log berhasil dibersihkan", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        shape = RoundedCornerShape(AppDnaTokens.RadiusPill),
                        border = BorderStroke(0.5.dp, AppDnaTokens.BorderCard),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp), tint = AppDnaTokens.RoseAlert)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Bersihkan", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = AppDnaTokens.RoseAlert)
                    }

                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("MbakAdmin Crash Log", crashLogs)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Log disalin ke clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(AppDnaTokens.RadiusPill),
                        colors = ButtonDefaults.buttonColors(containerColor = AppDnaTokens.PrimaryHero),
                        modifier = Modifier
                            .weight(1.3f)
                            .height(44.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp), tint = AppDnaTokens.OnPrimaryHero)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Salin Log", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = AppDnaTokens.OnPrimaryHero)
                    }
                }
            }
        }
    }
}
