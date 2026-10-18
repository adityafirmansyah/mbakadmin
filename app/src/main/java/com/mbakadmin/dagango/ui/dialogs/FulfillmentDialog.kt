package com.mbakadmin.dagango.ui.dialogs

import androidx.compose.foundation.clickable
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
import com.mbakadmin.dagango.data.local.entity.ProductEntity

@Composable
fun FulfillmentDialog(
    log: ChatLogEntity,
    products: List<ProductEntity>,
    onDismiss: () -> Unit,
    onConfirmFulfillment: (ProductEntity?, Int, Boolean) -> Unit
) {
    var selectedProduct by remember { mutableStateOf<ProductEntity?>(products.firstOrNull()) }
    var qtyText by remember { mutableStateOf("1") }
    var sendWhatsAppConfirmation by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Verifikasi & Proses Pesanan", style = MaterialTheme.typography.titleMedium)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Pembeli: ${log.senderName}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                if (log.customerPhone.isNotBlank()) {
                    Text(
                        text = "📱 No. HP/WA: ${log.customerPhone}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Pesan Pembeli: \"${log.incomingMessage}\"",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(8.dp)
                    )
                }

                HorizontalDivider()

                Text("Potong Stok Otomatis (Opsional):", style = MaterialTheme.typography.labelMedium)

                if (products.isEmpty()) {
                    Text("Belum ada produk di katalog.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                } else {
                    products.forEach { prod ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedProduct = if (selectedProduct?.id == prod.id) null else prod }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedProduct?.id == prod.id,
                                onClick = { selectedProduct = if (selectedProduct?.id == prod.id) null else prod }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(prod.name, style = MaterialTheme.typography.bodyMedium)
                                Text("Sisa Stok: ${prod.stock} | Rp${prod.price.toInt()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }

                    if (selectedProduct != null) {
                        OutlinedTextField(
                            value = qtyText,
                            onValueChange = { qtyText = it.filter { c -> c.isDigit() } },
                            label = { Text("Jumlah Terjual (Qty)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                HorizontalDivider()

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { sendWhatsAppConfirmation = !sendWhatsAppConfirmation }
                ) {
                    Checkbox(
                        checked = sendWhatsAppConfirmation,
                        onCheckedChange = { sendWhatsAppConfirmation = it }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Kirim pesan konfirmasi lunas ke WhatsApp pembeli",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = qtyText.toIntOrNull() ?: 1
                    onConfirmFulfillment(selectedProduct, qty, sendWhatsAppConfirmation)
                }
            ) {
                Text("Verifikasi Pembayaran")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
