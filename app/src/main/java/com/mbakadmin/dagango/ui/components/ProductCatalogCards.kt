package com.mbakadmin.dagango.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mbakadmin.dagango.data.local.entity.ProductEntity
import com.mbakadmin.dagango.ui.theme.AppDnaTokens

/**
 * Pattern: `tinted-card-white-floating`
 * Floating white card with 24dp radius, hairline border, 16dp internal padding.
 */
@Composable
fun CompactProductListItem(
    product: ProductEntity,
    onEdit: (ProductEntity) -> Unit,
    onDelete: (ProductEntity) -> Unit,
    onToggleActive: (ProductEntity) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val isOutOfStock = product.stock <= 0

    Surface(
        shape = RoundedCornerShape(AppDnaTokens.RadiusCard),
        color = if (!product.isActive) AppDnaTokens.SurfaceMuted.copy(alpha = 0.5f)
        else AppDnaTokens.SurfaceCard,
        border = BorderStroke(
            0.5.dp,
            if (!product.isActive) AppDnaTokens.BorderCard.copy(alpha = 0.5f)
            else AppDnaTokens.BorderCard
        ),
        shadowElevation = if (product.isActive) AppDnaTokens.ElevationLevel1 else 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit(product) }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Leading Icon Avatar (SurfaceMuted container)
            Surface(
                shape = RoundedCornerShape(AppDnaTokens.RadiusMicro),
                color = if (isOutOfStock) AppDnaTokens.RoseAlert.copy(alpha = 0.12f)
                else if (product.isActive) AppDnaTokens.SurfaceMuted
                else AppDnaTokens.SurfaceMuted.copy(alpha = 0.5f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isOutOfStock) Icons.Default.Warning else Icons.Default.Inventory2,
                        contentDescription = null,
                        tint = if (isOutOfStock) AppDnaTokens.RoseAlert
                        else if (product.isActive) AppDnaTokens.PrimaryHero
                        else AppDnaTokens.ForegroundMuted,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Center Details
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (product.isActive) AppDnaTokens.Foreground else AppDnaTokens.ForegroundMuted
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (isOutOfStock) {
                        Surface(
                            shape = RoundedCornerShape(AppDnaTokens.RadiusPill),
                            color = AppDnaTokens.RoseAlert.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "HABIS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.8.sp
                                ),
                                color = AppDnaTokens.RoseAlert,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Oversized numeral style price with Indonesian thousand separator
                    Text(
                        text = "Rp %,d".format(java.util.Locale("id", "ID"), product.price.toLong()),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = AppDnaTokens.Foreground
                        )
                    )
                    Text("•", style = MaterialTheme.typography.bodySmall, color = AppDnaTokens.ForegroundSubtle)

                    // Micro-label stock
                    Surface(
                        shape = RoundedCornerShape(AppDnaTokens.RadiusPill),
                        color = if (isOutOfStock) AppDnaTokens.RoseAlert.copy(alpha = 0.1f) else AppDnaTokens.SurfaceMuted
                    ) {
                        Text(
                            text = "STOK: ${product.stock}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = AppDnaTokens.TextMicro,
                                fontWeight = FontWeight.Bold,
                                color = if (isOutOfStock) AppDnaTokens.RoseAlert else AppDnaTokens.ForegroundMuted
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                if (product.variants.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Varian: ${product.variants}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = AppDnaTokens.ForegroundMuted
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Switch (DNA Saturated Accent)
            Switch(
                checked = product.isActive,
                onCheckedChange = { onToggleActive(product) },
                modifier = Modifier.scale(0.8f),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = AppDnaTokens.SurfaceCard,
                    checkedTrackColor = AppDnaTokens.PrimaryHero,
                    uncheckedThumbColor = AppDnaTokens.ForegroundMuted,
                    uncheckedTrackColor = AppDnaTokens.SurfaceMuted
                )
            )

            // 3-dots overflow menu
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Opsi",
                        modifier = Modifier.size(18.dp),
                        tint = AppDnaTokens.ForegroundMuted
                    )
                }

                ProductActionDropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    onEdit = { onEdit(product) },
                    onDelete = { onDelete(product) }
                )
            }
        }
    }
}

@Composable
fun CompactProductGridCard(
    product: ProductEntity,
    onEdit: (ProductEntity) -> Unit,
    onDelete: (ProductEntity) -> Unit,
    onToggleActive: (ProductEntity) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val isOutOfStock = product.stock <= 0

    Surface(
        shape = RoundedCornerShape(AppDnaTokens.RadiusCard),
        color = if (!product.isActive) AppDnaTokens.SurfaceMuted.copy(alpha = 0.5f)
        else AppDnaTokens.SurfaceCard,
        border = BorderStroke(
            0.5.dp,
            if (!product.isActive) AppDnaTokens.BorderCard.copy(alpha = 0.5f)
            else AppDnaTokens.BorderCard
        ),
        shadowElevation = if (product.isActive) AppDnaTokens.ElevationLevel1 else 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit(product) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(AppDnaTokens.RadiusPill),
                    color = if (isOutOfStock) AppDnaTokens.RoseAlert.copy(alpha = 0.15f)
                    else AppDnaTokens.SurfaceMuted
                ) {
                    Text(
                        text = if (isOutOfStock) "HABIS" else "STOK: ${product.stock}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = AppDnaTokens.TextMicro,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        ),
                        color = if (isOutOfStock) AppDnaTokens.RoseAlert
                        else AppDnaTokens.ForegroundMuted,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Switch(
                    checked = product.isActive,
                    onCheckedChange = { onToggleActive(product) },
                    modifier = Modifier.scale(0.72f),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = AppDnaTokens.SurfaceCard,
                        checkedTrackColor = AppDnaTokens.PrimaryHero
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = product.name,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = AppDnaTokens.Foreground
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.heightIn(min = 36.dp)
            )

            if (product.variants.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = product.variants,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = AppDnaTokens.ForegroundMuted
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Rp %,d".format(java.util.Locale("id", "ID"), product.price.toLong()),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = AppDnaTokens.Foreground
                    )
                )

                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(Icons.Default.MoreVert, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                    ProductActionDropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        onEdit = { onEdit(product) },
                        onDelete = { onDelete(product) }
                    )
                }
            }
        }
    }
}

/**
 * Dropdown Menu styled with Productivity Focus App DNA:
 * - 12dp micro rounded corners with 0.5dp hairline border
 * - Pure white SurfaceCard with elevation level 2
 * - Crisp icons with theme colors and hairline dividers
 */
@Composable
fun ProductActionDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(AppDnaTokens.RadiusMicro),
        containerColor = AppDnaTokens.SurfaceCard,
        tonalElevation = 0.dp,
        shadowElevation = AppDnaTokens.ElevationLevel2,
        border = BorderStroke(0.5.dp, AppDnaTokens.BorderCard),
        modifier = Modifier.widthIn(min = 160.dp)
    ) {
        DropdownMenuItem(
            text = {
                Text(
                    text = "Edit Produk",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = AppDnaTokens.TextBody
                    ),
                    color = AppDnaTokens.Foreground
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = AppDnaTokens.PrimaryHero,
                    modifier = Modifier.size(18.dp)
                )
            },
            colors = MenuDefaults.itemColors(
                textColor = AppDnaTokens.Foreground,
                leadingIconColor = AppDnaTokens.PrimaryHero
            ),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            onClick = {
                onDismissRequest()
                onEdit()
            }
        )

        HorizontalDivider(thickness = 0.5.dp, color = AppDnaTokens.BorderCard)

        DropdownMenuItem(
            text = {
                Text(
                    text = "Hapus Produk",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = AppDnaTokens.TextBody
                    ),
                    color = AppDnaTokens.RoseAlert
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = AppDnaTokens.RoseAlert,
                    modifier = Modifier.size(18.dp)
                )
            },
            colors = MenuDefaults.itemColors(
                textColor = AppDnaTokens.RoseAlert,
                leadingIconColor = AppDnaTokens.RoseAlert
            ),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            onClick = {
                onDismissRequest()
                onDelete()
            }
        )
    }
}
