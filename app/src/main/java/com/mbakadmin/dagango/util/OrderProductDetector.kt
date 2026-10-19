package com.mbakadmin.dagango.util

import com.mbakadmin.dagango.data.local.entity.ChatLogEntity
import com.mbakadmin.dagango.data.local.entity.ProductEntity

object OrderProductDetector {

    data class DetectedOrderItem(
        val product: ProductEntity,
        val quantity: Int
    )

    /**
     * Mendeteksi produk dan estimasi kuantitas masing-masing produk yang dibeli konsumen dari riwayat percakapan chat.
     * Mampu mendeteksi pembelian multi-produk (misal: "2 Cempe dan 1 Katak" atau "Cempe 2 pcs, Katak 1 pcs").
     */
    fun detectPurchasedProducts(
        messages: List<ChatLogEntity>,
        allProducts: List<ProductEntity>
    ): List<DetectedOrderItem> {
        if (allProducts.isEmpty() || messages.isEmpty()) return emptyList()

        val allIncoming = messages.map { it.incomingMessage }.filter { it.isNotBlank() }
        val allReplies = messages.map { it.aiReply }.filter { it.isNotBlank() }

        val orderLineRegex = Regex("""(?i)(?:Pesanan|Order|Barang)\s*(?:&|\+|\/)?\s*(?:Varian)?\s*:\s*(.+)""")
        val qtyLineRegex = Regex("""(?i)(?:Jumlah|Qty|Banyaknya)\s*:\s*(\d+)""")

        var templateQty: Int? = null
        val explicitOrderTexts = mutableListOf<String>()

        for (msg in allIncoming) {
            val orderMatch = orderLineRegex.find(msg)
            if (orderMatch != null) {
                explicitOrderTexts.add(orderMatch.groupValues[1])
            }
            val qtyMatch = qtyLineRegex.find(msg)
            if (qtyMatch != null) {
                templateQty = qtyMatch.groupValues[1].toIntOrNull()
            }
        }

        val detectedMap = mutableMapOf<Long, DetectedOrderItem>()

        for (product in allProducts) {
            val pName = product.name.trim()
            if (pName.length < 2) continue

            val pattern = Regex("""(?i)(?:\b|(?<=\W))${Regex.escape(pName)}(?:\b|(?=\W))""")

            // 1. Prioritas utama: Nama produk disebut pada baris format pemesanan
            val matchedOrderLine = explicitOrderTexts.firstOrNull { pattern.containsMatchIn(it) }
            if (matchedOrderLine != null) {
                val specificQty = extractQuantityForProduct(matchedOrderLine, pName)
                val qty = specificQty ?: templateQty ?: 1
                detectedMap[product.id] = DetectedOrderItem(product, qty.coerceAtLeast(1))
                continue
            }

            // 2. Disebut dalam pesan masuk pembeli
            var matchedInIncoming = false
            for (msg in allIncoming) {
                if (pattern.containsMatchIn(msg)) {
                    matchedInIncoming = true
                    val specificQty = extractQuantityForProduct(msg, pName)
                    val qty = specificQty ?: templateQty ?: 1
                    detectedMap[product.id] = DetectedOrderItem(product, qty.coerceAtLeast(1))
                    break
                }
            }
            if (matchedInIncoming) continue

            // 3. Disebut dalam rangkuman/respon AI ke pembeli
            for (reply in allReplies) {
                if (pattern.containsMatchIn(reply)) {
                    val specificQty = extractQuantityForProduct(reply, pName)
                    val qty = specificQty ?: templateQty ?: 1
                    detectedMap[product.id] = DetectedOrderItem(product, qty.coerceAtLeast(1))
                    break
                }
            }
        }

        return detectedMap.values.toList()
    }

    /**
     * Mengekstrak angka kuantitas yang berasosiasi langsung dengan suatu nama produk tertentu.
     * Mendukung format sebelum ("2 Cempe", "2 pcs Cempe", "2x Cempe") maupun
     * sesudah ("Cempe 2", "Cempe x2", "Cempe: 2", "Cempe (2)").
     */
    private fun extractQuantityForProduct(text: String, productName: String): Int? {
        val escaped = Regex.escape(productName)

        // Pola sesudah: "Cempe 2 pcs", "Cempe x 2", "Cempe: 2", "Cempe (2)"
        val afterPattern = Regex("""(?i)$escaped[\s:=(-]*(?:sebanyak|x)?\s*(\d+)\s*(?:pcs|ekor|buah|biji|box|pack|pasang|item|\))?""")
        val matchAfter = afterPattern.find(text)?.groupValues?.get(1)?.toIntOrNull()
        if (matchAfter != null && matchAfter in 1..9999) {
            return matchAfter
        }

        // Pola sebelum: "2 Cempe", "2 pcs Cempe", "2x Cempe"
        val beforePattern = Regex("""(?i)(\d+)\s*(?:pcs|ekor|buah|biji|box|pack|pasang|item|x)?\s*(?:sebanyak|x)?\s*$escaped""")
        val matchBefore = beforePattern.find(text)?.groupValues?.get(1)?.toIntOrNull()
        if (matchBefore != null && matchBefore in 1..9999) {
            return matchBefore
        }

        return null
    }
}
