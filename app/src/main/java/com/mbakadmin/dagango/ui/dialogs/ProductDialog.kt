package com.mbakadmin.dagango.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mbakadmin.dagango.data.local.entity.ProductEntity
import com.mbakadmin.dagango.ui.theme.AppDnaTokens

/**
 * ProductDialog styled with Productivity Focus App DNA.
 * Features:
 * - 32dp Hero corner radius with hairline border
 * - Keyboard-safe layout with imePadding() and sticky footer action buttons
 * - Form fields with AppDnaTokens styling and numeric keyboard options
 */
@Composable
fun ProductDialog(
    initialProduct: ProductEntity?,
    onDismiss: () -> Unit,
    onConfirm: (ProductEntity) -> Unit
) {
    var name by remember { mutableStateOf(initialProduct?.name ?: "") }
    var price by remember { mutableStateOf(initialProduct?.price?.toInt()?.toString() ?: "") }
    var stock by remember { mutableStateOf(initialProduct?.stock?.toString() ?: "10") }
    var variants by remember { mutableStateOf(initialProduct?.variants ?: "") }
    var description by remember { mutableStateOf(initialProduct?.description ?: "") }

    val parsedPrice = price.filter { it.isDigit() }.toDoubleOrNull() ?: 0.0
    val parsedStock = stock.filter { it.isDigit() }.toIntOrNull() ?: 0
    val isFormValid = name.trim().isNotBlank() && parsedPrice > 0

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            shape = RoundedCornerShape(AppDnaTokens.RadiusHero),
            color = AppDnaTokens.SurfaceCard,
            border = BorderStroke(0.5.dp, AppDnaTokens.BorderCard),
            shadowElevation = AppDnaTokens.ElevationLevel2,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppDnaTokens.MarginHorizontal, vertical = 20.dp)
                .imePadding()
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // --- HEADER ---
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = "KATALOG PRODUK",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = AppDnaTokens.TextMicro,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp
                            ),
                            color = AppDnaTokens.AccentGreen
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (initialProduct == null) "Tambah Produk Baru" else "Edit Produk",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = AppDnaTokens.Foreground
                            )
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup",
                            tint = AppDnaTokens.ForegroundMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(thickness = 0.5.dp, color = AppDnaTokens.BorderCard)
                Spacer(modifier = Modifier.height(14.dp))

                // --- SCROLLABLE INPUT FORM ---
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Nama Produk
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nama Produk *") },
                        placeholder = { Text("Contoh: Kaos Polos Combed 30s") },
                        singleLine = true,
                        shape = RoundedCornerShape(AppDnaTokens.RadiusMicro),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AppDnaTokens.PrimaryHero,
                            unfocusedBorderColor = AppDnaTokens.BorderCard,
                            focusedLabelColor = AppDnaTokens.PrimaryHero
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Harga & Stok Row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = price,
                            onValueChange = { str -> price = str.filter { it.isDigit() } },
                            label = { Text("Harga (Rp) *") },
                            placeholder = { Text("50000") },
                            supportingText = if (parsedPrice > 0) {
                                {
                                    Text(
                                        text = "Rp %,d".format(java.util.Locale("id", "ID"), parsedPrice.toLong()),
                                        fontSize = 11.sp,
                                        color = AppDnaTokens.AccentGreen
                                    )
                                }
                            } else null,
                            singleLine = true,
                            shape = RoundedCornerShape(AppDnaTokens.RadiusMicro),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Next
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AppDnaTokens.PrimaryHero,
                                unfocusedBorderColor = AppDnaTokens.BorderCard,
                                focusedLabelColor = AppDnaTokens.PrimaryHero
                            ),
                            modifier = Modifier.weight(1.3f)
                        )

                        OutlinedTextField(
                            value = stock,
                            onValueChange = { str -> stock = str.filter { it.isDigit() } },
                            label = { Text("Stok") },
                            placeholder = { Text("10") },
                            singleLine = true,
                            shape = RoundedCornerShape(AppDnaTokens.RadiusMicro),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Next
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AppDnaTokens.PrimaryHero,
                                unfocusedBorderColor = AppDnaTokens.BorderCard,
                                focusedLabelColor = AppDnaTokens.PrimaryHero
                            ),
                            modifier = Modifier.weight(0.9f)
                        )
                    }

                    // Varian
                    OutlinedTextField(
                        value = variants,
                        onValueChange = { variants = it },
                        label = { Text("Varian Produk") },
                        placeholder = { Text("Contoh: Merah, Navy, Hitam / S, M, L, XL") },
                        supportingText = { Text("Pisahkan varian dengan tanda koma.", fontSize = 11.sp, color = AppDnaTokens.ForegroundMuted) },
                        singleLine = true,
                        shape = RoundedCornerShape(AppDnaTokens.RadiusMicro),
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Next
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AppDnaTokens.PrimaryHero,
                            unfocusedBorderColor = AppDnaTokens.BorderCard,
                            focusedLabelColor = AppDnaTokens.PrimaryHero
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Deskripsi Singkat
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Deskripsi Singkat (Opsional)") },
                        placeholder = { Text("Keterangan bahan, spesifikasi, atau catatan...") },
                        minLines = 3,
                        singleLine = false,
                        shape = RoundedCornerShape(AppDnaTokens.RadiusMicro),
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Default
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AppDnaTokens.PrimaryHero,
                            unfocusedBorderColor = AppDnaTokens.BorderCard,
                            focusedLabelColor = AppDnaTokens.PrimaryHero
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(thickness = 0.5.dp, color = AppDnaTokens.BorderCard)
                Spacer(modifier = Modifier.height(14.dp))

                // --- STICKY FOOTER ACTIONS (ALWAYS VISIBLE ABOVE KEYBOARD) ---
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(AppDnaTokens.RadiusPill),
                        border = BorderStroke(0.5.dp, AppDnaTokens.BorderCard),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                    ) {
                        Text(
                            text = "Batal",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = AppDnaTokens.ForegroundMuted
                        )
                    }

                    Button(
                        onClick = {
                            if (isFormValid) {
                                onConfirm(
                                    initialProduct?.copy(
                                        name = name.trim(),
                                        price = parsedPrice,
                                        stock = parsedStock,
                                        variants = variants.trim(),
                                        description = description.trim()
                                    ) ?: ProductEntity(
                                        name = name.trim(),
                                        price = parsedPrice,
                                        stock = parsedStock,
                                        variants = variants.trim(),
                                        description = description.trim()
                                    )
                                )
                            }
                        },
                        enabled = isFormValid,
                        shape = RoundedCornerShape(AppDnaTokens.RadiusPill),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppDnaTokens.PrimaryHero,
                            contentColor = AppDnaTokens.OnPrimaryHero,
                            disabledContainerColor = AppDnaTokens.SurfaceMuted,
                            disabledContentColor = AppDnaTokens.ForegroundSubtle
                        ),
                        modifier = Modifier
                            .weight(1.5f)
                            .height(46.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Simpan Produk",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    }
}
