package com.mbakadmin.dagango.ui.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mbakadmin.dagango.data.local.entity.ChatLogEntity

@Composable
fun ShippingDialog(
    log: ChatLogEntity,
    onDismiss: () -> Unit,
    onConfirmShipping: (String, String, Boolean) -> Unit
) {
    var courierName by remember { mutableStateOf(if (log.courierName.isNotBlank()) log.courierName else "J&T Express") }
    var trackingNumber by remember { mutableStateOf(log.trackingNumber) }
    var sendWhatsAppResi by remember { mutableStateOf(true) }

    val commonCouriers = listOf("J&T Express", "JNE", "SiCepat", "Shopee Xpress", "Pos Indonesia", "GoSend/Grab")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Input Nomor Resi Pengiriman", style = MaterialTheme.typography.titleMedium)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Penerima: ${log.senderName}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )

                Text("Pilih Ekspedisi / Kurir:", style = MaterialTheme.typography.labelMedium)
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    commonCouriers.forEach { courier ->
                        FilterChip(
                            selected = courierName == courier,
                            onClick = { courierName = courier },
                            label = { Text(courier, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                OutlinedTextField(
                    value = courierName,
                    onValueChange = { courierName = it },
                    label = { Text("Nama Ekspedisi") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = trackingNumber,
                    onValueChange = { trackingNumber = it },
                    label = { Text("Nomor Resi") },
                    placeholder = { Text("Contoh: JT1234567890") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { sendWhatsAppResi = !sendWhatsAppResi }
                ) {
                    Checkbox(
                        checked = sendWhatsAppResi,
                        onCheckedChange = { sendWhatsAppResi = it }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Kirim info resi ke WhatsApp pembeli",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (trackingNumber.isNotBlank()) {
                        onConfirmShipping(courierName.ifBlank { "Kurir" }, trackingNumber, sendWhatsAppResi)
                    }
                },
                enabled = trackingNumber.isNotBlank()
            ) {
                Text("Simpan & Kirim Resi")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
