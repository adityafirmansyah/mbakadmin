package com.mbakadmin.dagango.ui.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mbakadmin.dagango.data.local.entity.ChatLogEntity
import com.mbakadmin.dagango.data.local.entity.ProductEntity
import com.mbakadmin.dagango.util.OrderProductDetector

data class OrderFulfillmentItemState(
    val product: ProductEntity,
    val qtyText: String,
    val isChecked: Boolean = true
)

@Composable
fun FulfillmentDialog(
    log: ChatLogEntity,
    products: List<ProductEntity>,
    recentLogs: List<ChatLogEntity>,
    onDismiss: () -> Unit,
    onConfirmFulfillment: (List<Pair<ProductEntity, Int>>, Boolean) -> Unit
) {
    // Deteksi produk yang dipesan pembeli dari riwayat chat
    val detectedOrders = remember(log, recentLogs, products) {
        OrderProductDetector.detectPurchasedProducts(recentLogs, products)
    }

    var selectedItems by remember(detectedOrders) {
        mutableStateOf<List<OrderFulfillmentItemState>>(
            if (detectedOrders.isNotEmpty()) {
                detectedOrders.map { OrderFulfillmentItemState(it.product, it.quantity.toString(), isChecked = true) }
            } else {
                emptyList()
            }
        )
    }

    var isSearchCatalogOpen by remember { mutableStateOf(detectedOrders.isEmpty()) }
    var catalogSearchQuery by remember { mutableStateOf("") }
    var sendWhatsAppConfirmation by remember { mutableStateOf(true) }

    val customerPhone = remember(log, recentLogs) {
        log.customerPhone.ifBlank {
            recentLogs.firstOrNull { it.customerPhone.isNotBlank() }?.customerPhone ?: ""
        }
    }

    val filteredCatalog = remember(catalogSearchQuery, products, selectedItems) {
        val selectedIds = selectedItems.map { it.product.id }.toSet()
        val query = catalogSearchQuery.trim().lowercase()
        products.filter { prod ->
            !selectedIds.contains(prod.id) &&
                (query.isBlank() || prod.name.lowercase().contains(query) || prod.variants.lowercase().contains(query))
        }.take(8)
    }

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
                if (customerPhone.isNotBlank()) {
                    Text(
                        text = "📱 No. HP/WA: $customerPhone",
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

                Text("Potong Stok Produk Terjual:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)

                if (selectedItems.isNotEmpty()) {
                    selectedItems.forEachIndexed { index, item ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (item.isChecked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Checkbox(
                                        checked = item.isChecked,
                                        onCheckedChange = { checked ->
                                            selectedItems = selectedItems.toMutableList().also {
                                                it[index] = item.copy(isChecked = checked)
                                            }
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.product.name, style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            "Sisa Stok: ${item.product.stock} | Rp${item.product.price.toInt()}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            selectedItems = selectedItems.filterIndexed { i, _ -> i != index }
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Hapus", modifier = Modifier.size(16.dp))
                                    }
                                }

                                if (item.isChecked) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        OutlinedTextField(
                                            value = item.qtyText,
                                            onValueChange = { newQty ->
                                                val filtered = newQty.filter { it.isDigit() }
                                                selectedItems = selectedItems.toMutableList().also {
                                                    it[index] = item.copy(qtyText = filtered)
                                                }
                                            },
                                            label = { Text("Jumlah Terjual (Qty)") },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                        val qty = item.qtyText.toIntOrNull() ?: 0
                                        if (qty > 0) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                "Rp${(item.product.price * qty).toInt()}",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else if (!isSearchCatalogOpen) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "ℹ️ Tidak ada produk terdeteksi dari chat pembeli.",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(10.dp),
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                // Tombol toggle buka katalog manual jika penjual ingin menambah produk lain
                if (products.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextButton(
                            onClick = { isSearchCatalogOpen = !isSearchCatalogOpen },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(
                                if (isSearchCatalogOpen) Icons.Default.Close else Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                if (isSearchCatalogOpen) "Tutup Pencarian Katalog"
                                else "+ Pilih Produk Lain dari Katalog",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }

                    if (isSearchCatalogOpen) {
                        OutlinedTextField(
                            value = catalogSearchQuery,
                            onValueChange = { catalogSearchQuery = it },
                            placeholder = { Text("Cari dari ${products.size} produk...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (filteredCatalog.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                filteredCatalog.forEach { prod ->
                                    Surface(
                                        shape = MaterialTheme.shapes.extraSmall,
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedItems = selectedItems + OrderFulfillmentItemState(prod, "1", isChecked = true)
                                                catalogSearchQuery = ""
                                            }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(8.dp)
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(prod.name, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                                Text("Stok: ${prod.stock} | Rp${prod.price.toInt()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                            }
                                            Icon(Icons.Default.Add, contentDescription = "Tambah", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                            }
                        } else if (catalogSearchQuery.isNotBlank()) {
                            Text("Tidak ada produk yang cocok dengan pencarian.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
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
                    val toDeduct = selectedItems
                        .filter { it.isChecked && (it.qtyText.toIntOrNull() ?: 0) > 0 }
                        .map { it.product to (it.qtyText.toIntOrNull() ?: 1) }
                    onConfirmFulfillment(toDeduct, sendWhatsAppConfirmation)
                }
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
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
