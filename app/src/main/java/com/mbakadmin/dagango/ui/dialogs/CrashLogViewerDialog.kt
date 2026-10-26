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
import androidx.compose.runtime.*
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * CrashLogViewerDialog
 * Menampilkan log crash fatal atau exception yang tertangkap di aplikasi.
 * Log dibaca dari disk di Dispatchers.IO (bukan main thread) dan dibatasi
 * hanya tail terakhir file (CrashLogger.getAllCrashLogs).
 */
@Composable
fun CrashLogViewerDialog(
    onDismiss: () -> Unit,
    onLogsCleared: () -> Unit
) {
    val context = LocalContext.current
    var crashLogs by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val logs = withContext(Dispatchers.IO) {
            CrashLogger.getAllCrashLogs(context)
                ?: "Tidak ada log crash tercatat. Sistem dalam kondisi sehat."
        }
        crashLogs = logs
    }

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
                        .heightIn(min = 120.dp, max = 380.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        val content = crashLogs
                        if (content == null) {
                            CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.Center),
                                color = AppDnaTokens.AccentGreen,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = content,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp
                                ),
                                color = AppDnaTokens.OnPrimaryHero
                            )
                        }
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
                            val logs = crashLogs ?: return@Button
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("MbakAdmin Crash Log", logs))
                            Toast.makeText(
                                context,
                                "Log disalin ke clipboard (berisi data teknikal, bagikan hanya ke developer)",
                                Toast.LENGTH_LONG
                            ).show()
                        },
                        enabled = crashLogs != null,
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
