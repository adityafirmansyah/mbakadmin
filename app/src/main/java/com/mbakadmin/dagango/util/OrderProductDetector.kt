package com.mbakadmin.dagango.util

import com.mbakadmin.dagango.data.local.entity.ChatLogEntity
import com.mbakadmin.dagango.data.local.entity.ProductEntity

object OrderProductDetector {

    data class DetectedOrderItem(
        val product: ProductEntity,
        val quantity: Int
    )

    /**
     * Mendeteksi produk dan estimasi kuantitas yang dipesan konsumen dari riwayat percakapan chat.
     * Mengutamakan baris format order (Pesanan & Varian / Jumlah), teks pesan pembeli, dan rincian respon bot.
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
            val matchInOrderLine = explicitOrderTexts.any { pattern.containsMatchIn(it) }
            if (matchInOrderLine) {
                val qty = templateQty ?: 1
                detectedMap[product.id] = DetectedOrderItem(product, qty.coerceAtLeast(1))
                continue
            }

            // 2. Disebut dalam pesan masuk pembeli
            var matchedInIncoming = false
            for (msg in allIncoming) {
                if (pattern.containsMatchIn(msg)) {
                    matchedInIncoming = true
                    // Cek kuantitas yang berdekatan dengan nama produk (misal: "Cempe 2 pcs" atau "2 cempe")
                    val nearbyQtyAfter = Regex("""(?i)${Regex.escape(pName)}\s*(?:sebanyak|x)?\s*(\d+)\s*(?:pcs|ekor|buah|biji|box|pack|pasang|item)?""")
                        .find(msg)?.groupValues?.get(1)?.toIntOrNull()
                    val nearbyQtyBefore = Regex("""(?i)(\d+)\s*(?:pcs|ekor|buah|biji|box|pack|pasang|item)?\s*${Regex.escape(pName)}""")
                        .find(msg)?.groupValues?.get(1)?.toIntOrNull()

                    val qty = nearbyQtyAfter ?: nearbyQtyBefore ?: templateQty ?: 1
                    detectedMap[product.id] = DetectedOrderItem(product, qty.coerceAtLeast(1))
                    break
                }
            }
            if (matchedInIncoming) continue

            // 3. Disebut dalam rangkuman/respon AI ke pembeli
            for (reply in allReplies) {
                if (pattern.containsMatchIn(reply)) {
                    val qty = templateQty ?: 1
                    detectedMap[product.id] = DetectedOrderItem(product, qty.coerceAtLeast(1))
                    break
                }
            }
        }

        return detectedMap.values.toList()
    }
}
