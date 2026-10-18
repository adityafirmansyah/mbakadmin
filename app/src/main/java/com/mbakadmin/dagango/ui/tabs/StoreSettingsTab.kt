package com.mbakadmin.dagango.ui.tabs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import com.mbakadmin.dagango.data.local.entity.StoreSettingsEntity

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

    // Advanced Settings State (Collapsible)
    var showAdvancedSettings by remember { mutableStateOf(false) }
    var botTone by remember(currentSettings) { mutableStateOf(currentSettings.botTone) }
    var fallbackReplyMessage by remember(currentSettings) { mutableStateOf(currentSettings.fallbackReplyMessage) }
    var orderFormatTemplate by remember(currentSettings) { mutableStateOf(currentSettings.orderFormatTemplate) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 86.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // --- SECTION 1: PROFIL TOKO & ADMIN ---
            Text("Profil Toko & Admin", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            OutlinedTextField(
                value = storeName,
                onValueChange = { storeName = it },
                label = { Text("Nama Toko") },
                placeholder = { Text("Nama Toko Anda") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = adminName,
                onValueChange = { adminName = it },
                label = { Text("Panggilan Admin Toko") },
                placeholder = { Text("Contoh: Mbak Admin, Mas Budi, Kak Sarah") },
                supportingText = { Text("Nama ini yang akan dipakai saat membalas dan menyapa pembeli.") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // --- SECTION 2: PEMBAYARAN & PENGIRIMAN ---
            Text("Pembayaran & Pengiriman", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            OutlinedTextField(
                value = paymentInfo,
                onValueChange = { paymentInfo = it },
                label = { Text("Info Rekening & Pembayaran") },
                placeholder = { Text("BCA: 1234567890 a/n Toko Saya\nMandiri: 9876543210 a/n Toko Saya") },
                supportingText = { Text("Otomatis dikirimkan setelah pembeli melengkapi format pemesanan.") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = shippingInfo,
                onValueChange = { shippingInfo = it },
                label = { Text("Kota Asal & Ekspedisi Pengiriman") },
                placeholder = { Text("Kirim dari Jakarta Pusat via JNE, J&T, SiCepat") },
                minLines = 2,
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
                modifier = Modifier.fillMaxWidth()
            )

            // --- SECTION 3: TARGET WHATSAPP & PRIVASI KONTAK ---
            Text("Target WhatsApp & Privasi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Aplikasi WhatsApp Toko:", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = targetWhatsAppPackage == "ALL",
                        onClick = { targetWhatsAppPackage = "ALL" },
                        label = { Text("Semua WhatsApp") }
                    )
                    FilterChip(
                        selected = targetWhatsAppPackage == "WHATSAPP_BUSINESS",
                        onClick = { targetWhatsAppPackage = "WHATSAPP_BUSINESS" },
                        label = { Text("WA Business Saja") }
                    )
                    FilterChip(
                        selected = targetWhatsAppPackage == "WHATSAPP_STANDARD",
                        onClick = { targetWhatsAppPackage = "WHATSAPP_STANDARD" },
                        label = { Text("WA Standar Saja") }
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Hanya Balas Nomor Tidak Dikenal", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "Khusus 1 nomor WA: Hanya merespon calon pembeli baru (belum ada di kontak HP). Chat keluarga dan teman tidak akan dibalas bot.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Switch(
                        checked = onlyReplyUnknownContacts,
                        onCheckedChange = { onlyReplyUnknownContacts = it }
                    )
                }
            }

            // --- SECTION 4: PENGATURAN LANJUTAN (COLLAPSIBLE / ACCORDION) ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAdvancedSettings = !showAdvancedSettings }
                            .padding(vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(
                                if (showAdvancedSettings) Icons.Default.Tune else Icons.Default.Settings,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text("Pengaturan Lanjutan", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("Kustomisasi format order, gaya bahasa & fallback", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                        Icon(
                            if (showAdvancedSettings) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }

                    if (showAdvancedSettings) {
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = orderFormatTemplate,
                            onValueChange = { orderFormatTemplate = it },
                            label = { Text("Template Format Pemesanan") },
                            placeholder = { Text("Nama Penerima:\nNo. HP/WA:\nPesanan & Varian:\nJumlah:\nAlamat Lengkap:\nKecamatan & Kota:\nKode Pos:") },
                            supportingText = { Text("Format yang dikirimkan bot saat pembeli berniat order.") },
                            minLines = 4,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = botTone,
                            onValueChange = { botTone = it },
                            label = { Text("Gaya Bicara / Tone Persona") },
                            placeholder = { Text("ramah, sopan, persuasif ala admin olshop") },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = fallbackReplyMessage,
                            onValueChange = { fallbackReplyMessage = it },
                            label = { Text("Pesan Cadangan (Fallback)") },
                            placeholder = { Text("Halo kak! Pesan sudah {admin} terima ya...") },
                            supportingText = { Text("Dipakai jika koneksi internet offline. Gunakan {admin} untuk nama admin.") },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // --- TOMBOL SIMPAN PENGATURAN (FLOATING DI ATAS MENU) ---
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            color = Color.Transparent
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
                    .height(52.dp),
                shape = MaterialTheme.shapes.medium,
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp, pressedElevation = 2.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Simpan Pengaturan", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
        }
    }
}
