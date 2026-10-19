package com.mbakadmin.dagango.ui.tabs

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mbakadmin.dagango.data.local.entity.ProductEntity
import com.mbakadmin.dagango.ui.components.CompactProductGridCard
import com.mbakadmin.dagango.ui.components.CompactProductListItem
import com.mbakadmin.dagango.ui.components.DeleteProductConfirmDialog

enum class CatalogFilter {
    ALL,
    ACTIVE,
    INACTIVE,
    OUT_OF_STOCK
}

@Composable
fun CatalogTab(
    products: List<ProductEntity>,
    onEditProduct: (ProductEntity) -> Unit,
    onDeleteProduct: (ProductEntity) -> Unit,
    onToggleActive: (ProductEntity) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(CatalogFilter.ALL) }
    var isGridView by remember { mutableStateOf(false) } // Default compact list (fits 8-10 items)
    var productToDelete by remember { mutableStateOf<ProductEntity?>(null) }

    val activeCount = remember(products) { products.count { it.isActive } }
    val inactiveCount = remember(products) { products.count { !it.isActive } }
    val outOfStockCount = remember(products) { products.count { it.stock <= 0 } }

    val filteredProducts = remember(products, searchQuery, selectedFilter) {
        val q = searchQuery.trim().lowercase()
        products.filter { prod ->
            val matchesQuery = q.isBlank() ||
                prod.name.lowercase().contains(q) ||
                prod.variants.lowercase().contains(q) ||
                prod.category.lowercase().contains(q)

            val matchesFilter = when (selectedFilter) {
                CatalogFilter.ALL -> true
                CatalogFilter.ACTIVE -> prod.isActive
                CatalogFilter.INACTIVE -> !prod.isActive
                CatalogFilter.OUT_OF_STOCK -> prod.stock <= 0
            }

            matchesQuery && matchesFilter
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        // --- TOP TOOLBAR: SEARCH & VIEW TOGGLE ---
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Cari produk / varian...", style = MaterialTheme.typography.bodyMedium) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Hapus", modifier = Modifier.size(18.dp))
                        }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
            )

            Spacer(modifier = Modifier.width(6.dp))

            // View Mode Toggle (Compact List vs 2-Column Grid)
            IconButton(
                onClick = { isGridView = !isGridView },
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                    contentDescription = if (isGridView) "Mode List" else "Mode Grid",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // --- FILTER CHIPS ROW ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = selectedFilter == CatalogFilter.ALL,
                onClick = { selectedFilter = CatalogFilter.ALL },
                label = { Text("Semua (${products.size})", style = MaterialTheme.typography.labelSmall) }
            )
            FilterChip(
                selected = selectedFilter == CatalogFilter.ACTIVE,
                onClick = { selectedFilter = CatalogFilter.ACTIVE },
                label = { Text("Aktif ($activeCount)", style = MaterialTheme.typography.labelSmall) }
            )
            FilterChip(
                selected = selectedFilter == CatalogFilter.INACTIVE,
                onClick = { selectedFilter = CatalogFilter.INACTIVE },
                label = { Text("Nonaktif ($inactiveCount)", style = MaterialTheme.typography.labelSmall) }
            )
            if (outOfStockCount > 0) {
                FilterChip(
                    selected = selectedFilter == CatalogFilter.OUT_OF_STOCK,
                    onClick = { selectedFilter = CatalogFilter.OUT_OF_STOCK },
                    label = { Text("Habis ($outOfStockCount)", style = MaterialTheme.typography.labelSmall) }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // --- CONTENT AREA ---
        if (products.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Belum ada produk di katalog.", style = MaterialTheme.typography.bodyMedium)
                    Text("Tekan tombol '+' di bawah untuk menambah produk baru.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
            }
        } else if (filteredProducts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Tidak ada produk yang cocok dengan pencarian / filter.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        } else {
            if (isGridView) {
                // 2-Column Grid Mode: fits 8-10 cards vertically on screen
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(filteredProducts, key = { it.id }) { product ->
                        CompactProductGridCard(
                            product = product,
                            onEdit = onEditProduct,
                            onDelete = { productToDelete = it },
                            onToggleActive = onToggleActive
                        )
                    }
                }
            } else {
                // Dense List Mode: ~60-68dp per item, fits 8-10+ products easily on phone viewport
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(filteredProducts, key = { it.id }) { product ->
                        CompactProductListItem(
                            product = product,
                            onEdit = onEditProduct,
                            onDelete = { productToDelete = it },
                            onToggleActive = onToggleActive
                        )
                    }
                }
            }
        }
    }

    productToDelete?.let { prod ->
        DeleteProductConfirmDialog(
            product = prod,
            onDismiss = { productToDelete = null },
            onConfirm = {
                onDeleteProduct(prod)
                productToDelete = null
            }
        )
    }
}
