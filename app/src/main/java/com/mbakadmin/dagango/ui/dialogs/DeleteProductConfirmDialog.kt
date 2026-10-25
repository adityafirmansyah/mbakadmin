package com.mbakadmin.dagango.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mbakadmin.dagango.data.local.entity.ProductEntity
import com.mbakadmin.dagango.ui.theme.AppDnaTokens

/**
 * Delete confirmation dialog styled with Productivity Focus App DNA.
 * Features 32dp Hero card radius, high-contrast action buttons, and hairline borders.
 */
@Composable
fun DeleteProductConfirmDialog(
    product: ProductEntity,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(AppDnaTokens.RadiusHero),
            color = AppDnaTokens.SurfaceCard,
            border = BorderStroke(0.5.dp, AppDnaTokens.BorderCard),
            shadowElevation = AppDnaTokens.ElevationLevel2,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppDnaTokens.MarginHorizontal)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Eyebrow Micro-Label
                Text(
                    text = "KONFIRMASI TINDAKAN",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = AppDnaTokens.TextMicro,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    ),
                    color = AppDnaTokens.RoseAlert
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Hapus Produk?",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = AppDnaTokens.Foreground
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Produk \"${product.name}\" akan dihapus permanen dari katalog toko dan tidak dapat dikembalikan.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = AppDnaTokens.ForegroundMuted,
                        lineHeight = 20.sp
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(thickness = 0.5.dp, color = AppDnaTokens.BorderCard)
                Spacer(modifier = Modifier.height(14.dp))

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
                            .height(44.dp)
                    ) {
                        Text(
                            text = "Batal",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = AppDnaTokens.ForegroundMuted
                        )
                    }

                    Button(
                        onClick = onConfirm,
                        shape = RoundedCornerShape(AppDnaTokens.RadiusPill),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppDnaTokens.RoseAlert,
                            contentColor = AppDnaTokens.OnPrimaryHero
                        ),
                        modifier = Modifier
                            .weight(1.3f)
                            .height(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Hapus",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}
