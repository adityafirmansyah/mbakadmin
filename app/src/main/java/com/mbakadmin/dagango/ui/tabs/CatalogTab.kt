package com.mbakadmin.dagango.ui.tabs

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mbakadmin.dagango.data.local.entity.ProductEntity
import com.mbakadmin.dagango.ui.components.CompactProductGridCard
import com.mbakadmin.dagango.ui.components.CompactProductListItem
import com.mbakadmin.dagango.ui.dialogs.DeleteProductConfirmDialog
import com.mbakadmin.dagango.ui.theme.AppDnaTokens

enum class CatalogFilter {
    ALL,
    ACTIVE,
    INACTIVE,
    OUT_OF_STOCK
}

/**
 * Patterns: `grouped-feed`, `tinted-card-white-floating`
 */
@Composable
fun CatalogTab(
    products: List<ProductEntity>,
    onEditProduct: (ProductEntity) -> Unit,
    onDeleteProduct: (ProductEntity) -> Unit,
    onToggleActive: (ProductEntity) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(CatalogFilter.ALL) }
    var isGridView by remember { mutableStateOf(false) } // Default compact list
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
        modifier = Modifier.fillMaxSize()
    ) {
        // --- TOP TOOLBAR: SEARCH & VIEW TOGGLE ---
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Surface(
                shape = RoundedCornerShape(AppDnaTokens.RadiusMicro),
                color = AppDnaTokens.SurfaceCard,
                border = androidx.compose.foundation.BorderStroke(0.5.dp, AppDnaTokens.BorderCard),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp)
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = AppDnaTokens.ForegroundMuted,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                "Cari produk / varian...",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = AppDnaTokens.TextBody),
                                color = AppDnaTokens.ForegroundSubtle
                            )
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                            unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Clear, contentDescription = "Hapus", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // View Mode Toggle (Compact List vs 2-Column Grid)
            Surface(
                shape = RoundedCornerShape(AppDnaTokens.RadiusMicro),
                color = AppDnaTokens.SurfaceCard,
                border = androidx.compose.foundation.BorderStroke(0.5.dp, AppDnaTokens.BorderCard),
                modifier = Modifier.size(48.dp)
            ) {
                IconButton(
                    onClick = { isGridView = !isGridView },
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                        contentDescription = if (isGridView) "Mode List" else "Mode Grid",
                        tint = AppDnaTokens.PrimaryHero,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // --- FILTER CHIPS ROW ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChipItem(
                label = "Semua (${products.size})",
                isSelected = selectedFilter == CatalogFilter.ALL,
                onClick = { selectedFilter = CatalogFilter.ALL }
            )
            FilterChipItem(
                label = "Aktif ($activeCount)",
                isSelected = selectedFilter == CatalogFilter.ACTIVE,
                onClick = { selectedFilter = CatalogFilter.ACTIVE }
            )
            FilterChipItem(
                label = "Nonaktif ($inactiveCount)",
                isSelected = selectedFilter == CatalogFilter.INACTIVE,
                onClick = { selectedFilter = CatalogFilter.INACTIVE }
            )
            if (outOfStockCount > 0) {
                FilterChipItem(
                    label = "Habis ($outOfStockCount)",
                    isSelected = selectedFilter == CatalogFilter.OUT_OF_STOCK,
                    onClick = { selectedFilter = CatalogFilter.OUT_OF_STOCK },
                    isAlert = true
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- CONTENT FEED AREA ---
        if (products.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(48.dp), tint = AppDnaTokens.ForegroundSubtle)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Belum ada produk di katalog.", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = AppDnaTokens.Foreground)
                    Text("Tekan tombol '+' di bawah untuk menambah produk baru.", style = MaterialTheme.typography.bodySmall, color = AppDnaTokens.ForegroundMuted)
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
                    color = AppDnaTokens.ForegroundMuted
                )
            }
        } else {
            if (isGridView) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(AppDnaTokens.GapFeed),
                    verticalArrangement = Arrangement.spacedBy(AppDnaTokens.GapFeed),
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
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(AppDnaTokens.GapFeed),
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

@Composable
private fun FilterChipItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    isAlert: Boolean = false
) {
    Surface(
        shape = RoundedCornerShape(AppDnaTokens.RadiusPill),
        color = if (isSelected) {
            if (isAlert) AppDnaTokens.RoseAlert else AppDnaTokens.PrimaryHero
        } else AppDnaTokens.SurfaceCard,
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            if (isSelected) androidx.compose.ui.graphics.Color.Transparent else AppDnaTokens.BorderCard
        ),
        shadowElevation = if (isSelected) AppDnaTokens.ElevationLevel1 else 0.dp,
        modifier = Modifier.height(34.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 14.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = AppDnaTokens.TextCaption,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (isSelected) AppDnaTokens.OnPrimaryHero
                else if (isAlert) AppDnaTokens.RoseAlert
                else AppDnaTokens.ForegroundMuted
            )
        }
    }
}
