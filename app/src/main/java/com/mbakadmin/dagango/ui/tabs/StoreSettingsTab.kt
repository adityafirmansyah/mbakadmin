package com.mbakadmin.dagango.ui.tabs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mbakadmin.dagango.data.local.entity.StoreSettingsEntity
import com.mbakadmin.dagango.ui.theme.AppDnaTokens

/**
 * StoreSettingsTab styled with Productivity Focus App DNA.
 * Patterns: `grouped-feed`, `tinted-card-white-floating`.
 */
@Composable
fun StoreSettingsTab(
    currentSettings: StoreSettingsEntity,
    onSaveSettings: (StoreSettingsEntity) -> Unit
) {
    var storeName by remember(currentSettings) { mutableStateOf(currentSettings.storeName) }
    var adminName by remember(currentSettings) { mutableStateOf(currentSettings.adminName) }
    var paymentInfo by remember(currentSettings) { mutableStateOf(currentSettings.paymentInfo) }
    var shippingInfo by remember(currentSettings) { mutableStateOf(currentSettings.shippingInfo) }
    var defaultShippingFee by remember(currentSettings) { mutableStateOf(currentSettings.defaultShippingFee.toInt().toString()) }
    var targetWhatsAppPackage by remember(currentSettings) { mutableStateOf(currentSettings.targetWhatsAppPackage) }
    var onlyReplyUnknownContacts by remember(currentSettings) { mutableStateOf(currentSettings.onlyReplyUnknownContacts) }

    var showAdvancedSettings by remember { mutableStateOf(false) }
    var botTone by remember(currentSettings) { mutableStateOf(currentSettings.botTone) }
    var fallbackReplyMessage by remember(currentSettings) { mutableStateOf(currentSettings.fallbackReplyMessage) }
    var orderFormatTemplate by remember(currentSettings) { mutableStateOf(currentSettings.orderFormatTemplate) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(AppDnaTokens.GapFeed)
        ) {
            // --- SECTION 1: PROFIL TOKO & ADMIN ---
            Surface(
                shape = RoundedCornerShape(AppDnaTokens.RadiusCard),
                color = AppDnaTokens.SurfaceCard,
                border = BorderStroke(0.5.dp, AppDnaTokens.BorderCard),
                shadowElevation = AppDnaTokens.ElevationLevel1,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "PROFIL TOKO & ADMIN",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = AppDnaTokens.TextEyebrow,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = AppDnaTokens.ForegroundMuted
                    )

                    OutlinedTextField(
                        value = storeName,
                        onValueChange = { storeName = it },
                        label = { Text("Nama Toko") },
                        placeholder = { Text("Nama Toko Anda") },
                        singleLine = true,
                        shape = RoundedCornerShape(AppDnaTokens.RadiusMicro),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = adminName,
                        onValueChange = { adminName = it },
                        label = { Text("Panggilan Admin Toko") },
                        placeholder = { Text("Contoh: Mbak Admin, Mas Budi, Kak Sarah") },
                        supportingText = { Text("Nama ini yang akan dipakai saat membalas dan menyapa pembeli.") },
                        singleLine = true,
                        shape = RoundedCornerShape(AppDnaTokens.RadiusMicro),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // --- SECTION 2: PEMBAYARAN & PENGIRIMAN ---
            Surface(
                shape = RoundedCornerShape(AppDnaTokens.RadiusCard),
                color = AppDnaTokens.SurfaceCard,
                border = BorderStroke(0.5.dp, AppDnaTokens.BorderCard),
                shadowElevation = AppDnaTokens.ElevationLevel1,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "PEMBAYARAN & PENGIRIMAN",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = AppDnaTokens.TextEyebrow,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = AppDnaTokens.ForegroundMuted
                    )

                    OutlinedTextField(
                        value = paymentInfo,
                        onValueChange = { paymentInfo = it },
                        label = { Text("Info Rekening & Pembayaran") },
                        placeholder = { Text("BCA: 1234567890 a/n Toko Saya\nMandiri: 9876543210 a/n Toko Saya") },
                        supportingText = { Text("Otomatis dikirimkan setelah pembeli melengkapi format pemesanan.") },
                        minLines = 3,
                        shape = RoundedCornerShape(AppDnaTokens.RadiusMicro),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = shippingInfo,
                        onValueChange = { shippingInfo = it },
                        label = { Text("Kota Asal & Ekspedisi Pengiriman") },
                        placeholder = { Text("Kirim dari Jakarta Pusat via JNE, J&T, SiCepat") },
                        minLines = 2,
                        shape = RoundedCornerShape(AppDnaTokens.RadiusMicro),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = defaultShippingFee,
                        onValueChange = { str ->
                            defaultShippingFee = str.filter { it.isDigit() }
                        },
                        label = { Text("Estimasi Ongkir Standar (Rp)") },
                        placeholder = { Text("15000") },
                        singleLine = true,
                        shape = RoundedCornerShape(AppDnaTokens.RadiusMicro),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // --- SECTION 3: TARGET WHATSAPP & PRIVASI KONTAK ---
            Surface(
                shape = RoundedCornerShape(AppDnaTokens.RadiusCard),
                color = AppDnaTokens.SurfaceCard,
                border = BorderStroke(0.5.dp, AppDnaTokens.BorderCard),
                shadowElevation = AppDnaTokens.ElevationLevel1,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "TARGET WHATSAPP & PRIVASI",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = AppDnaTokens.TextEyebrow,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = AppDnaTokens.ForegroundMuted
                    )

                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TargetChip("Semua WA", targetWhatsAppPackage == "ALL") { targetWhatsAppPackage = "ALL" }
                        TargetChip("WA Business", targetWhatsAppPackage == "WHATSAPP_BUSINESS") { targetWhatsAppPackage = "WHATSAPP_BUSINESS" }
                        TargetChip("WA Standar", targetWhatsAppPackage == "WHATSAPP_STANDARD") { targetWhatsAppPackage = "WHATSAPP_STANDARD" }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Hanya Balas Nomor Tidak Dikenal", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = AppDnaTokens.Foreground)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                "Khusus 1 nomor WA: Hanya merespon calon pembeli baru (belum ada di kontak). Chat keluarga/teman diabaikan.",
                                style = MaterialTheme.typography.bodySmall.copy(color = AppDnaTokens.ForegroundMuted)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Switch(
                            checked = onlyReplyUnknownContacts,
                            onCheckedChange = { onlyReplyUnknownContacts = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AppDnaTokens.SurfaceCard,
                                checkedTrackColor = AppDnaTokens.PrimaryHero
                            )
                        )
                    }
                }
            }

            // --- SECTION 4: PENGATURAN LANJUTAN ---
            Surface(
                shape = RoundedCornerShape(AppDnaTokens.RadiusCard),
                color = AppDnaTokens.SurfaceCard,
                border = BorderStroke(0.5.dp, AppDnaTokens.BorderCard),
                shadowElevation = AppDnaTokens.ElevationLevel1,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAdvancedSettings = !showAdvancedSettings }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(
                                if (showAdvancedSettings) Icons.Default.Tune else Icons.Default.Settings,
                                contentDescription = null,
                                tint = AppDnaTokens.PrimaryHero
                            )
                            Column {
                                Text("Pengaturan Lanjutan", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = AppDnaTokens.Foreground)
                                Text("Format order, gaya bahasa & fallback", style = MaterialTheme.typography.bodySmall.copy(color = AppDnaTokens.ForegroundMuted))
                            }
                        }
                        Icon(
                            if (showAdvancedSettings) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = AppDnaTokens.ForegroundMuted
                        )
                    }

                    if (showAdvancedSettings) {
                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(thickness = 0.5.dp, color = AppDnaTokens.BorderCard)
                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = orderFormatTemplate,
                            onValueChange = { orderFormatTemplate = it },
                            label = { Text("Template Format Pemesanan") },
                            placeholder = { Text("Nama Penerima:\nNo. HP/WA:\nPesanan & Varian:\nJumlah:\nAlamat Lengkap:\nKecamatan & Kota:\nKode Pos:") },
                            supportingText = { Text("Format yang dikirimkan bot saat pembeli berniat order.") },
                            minLines = 4,
                            shape = RoundedCornerShape(AppDnaTokens.RadiusMicro),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = botTone,
                            onValueChange = { botTone = it },
                            label = { Text("Gaya Bicara / Tone Persona") },
                            placeholder = { Text("ramah, sopan, persuasif ala admin olshop") },
                            minLines = 2,
                            shape = RoundedCornerShape(AppDnaTokens.RadiusMicro),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = fallbackReplyMessage,
                            onValueChange = { fallbackReplyMessage = it },
                            label = { Text("Pesan Cadangan (Fallback)") },
                            placeholder = { Text("Halo kak! Pesan sudah {admin} terima ya...") },
                            supportingText = { Text("Dipakai jika koneksi internet offline. Gunakan {admin} untuk nama admin.") },
                            minLines = 2,
                            shape = RoundedCornerShape(AppDnaTokens.RadiusMicro),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // --- TOMBOL SIMPAN PENGATURAN (STICKY DENGAN BACKING PERMUKAAN BERSIH) ---
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            shape = RoundedCornerShape(AppDnaTokens.RadiusPill),
            color = AppDnaTokens.SurfaceCanvas.copy(alpha = 0.95f),
            shadowElevation = AppDnaTokens.ElevationLevel2
        ) {
            Button(
                onClick = {
                    onSaveSettings(
                        currentSettings.copy(
                            storeName = storeName,
                            adminName = adminName.ifBlank { "Mbak Admin" },
                            paymentInfo = paymentInfo,
                            shippingInfo = shippingInfo,
                            defaultShippingFee = defaultShippingFee.toDoubleOrNull() ?: 15000.0,
                            orderFormatTemplate = orderFormatTemplate.ifBlank {
                                "Nama Penerima:\nNo. HP/WA:\nPesanan & Varian:\nJumlah:\nAlamat Lengkap:\nKecamatan & Kota:\nKode Pos:"
                            },
                            botTone = botTone,
                            fallbackReplyMessage = fallbackReplyMessage.ifBlank {
                                "Halo kak! Pesan sudah {admin} terima ya, mohon ditunggu sebentar biar segera dicek manual oleh tim kami 🙏"
                            },
                            aiApiBaseUrl = StoreSettingsEntity.DEFAULT_AI_BASE_URL,
                            aiApiKey = StoreSettingsEntity.DEFAULT_AI_API_KEY,
                            aiModel = StoreSettingsEntity.DEFAULT_AI_MODEL,
                            targetWhatsAppPackage = targetWhatsAppPackage,
                            onlyReplyUnknownContacts = onlyReplyUnknownContacts
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(AppDnaTokens.RadiusPill),
                colors = ButtonDefaults.buttonColors(containerColor = AppDnaTokens.PrimaryHero)
            ) {
                Icon(Icons.Default.Save, contentDescription = null, tint = AppDnaTokens.OnPrimaryHero, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Simpan Pengaturan", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = AppDnaTokens.OnPrimaryHero)
            }
        }
    }
}

@Composable
private fun TargetChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(AppDnaTokens.RadiusPill),
        color = if (isSelected) AppDnaTokens.PrimaryHero else AppDnaTokens.SurfaceMuted,
        modifier = Modifier.height(32.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxHeight()
                .clickable { onClick() }
                .padding(horizontal = 14.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = AppDnaTokens.TextCaption,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (isSelected) AppDnaTokens.OnPrimaryHero else AppDnaTokens.ForegroundMuted
            )
        }
    }
}
