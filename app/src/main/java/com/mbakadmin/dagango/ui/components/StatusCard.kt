package com.mbakadmin.dagango.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mbakadmin.dagango.ui.theme.AppDnaTokens

/**
 * StatusCard styled with Productivity Focus App DNA.
 * Pattern: `tinted-card-white-floating` (24dp radius, hairline border, crisp status pills).
 */
@Composable
fun StatusCard(
    title: String,
    subtitle: String,
    isOk: Boolean,
    actionLabel: String,
    onActionClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(AppDnaTokens.RadiusCard),
        color = AppDnaTokens.SurfaceCard,
        border = BorderStroke(
            0.5.dp,
            if (isOk) AppDnaTokens.BorderCard else AppDnaTokens.RoseAlert.copy(alpha = 0.5f)
        ),
        shadowElevation = AppDnaTokens.ElevationLevel1,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(AppDnaTokens.RadiusMicro),
                color = if (isOk) AppDnaTokens.SuccessGreen.copy(alpha = 0.12f) else AppDnaTokens.RoseAlert.copy(alpha = 0.12f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isOk) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (isOk) AppDnaTokens.SuccessGreen else AppDnaTokens.RoseAlert,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = AppDnaTokens.Foreground
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = AppDnaTokens.ForegroundMuted
                    )
                )
            }

            if (!isOk) {
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onActionClick,
                    shape = RoundedCornerShape(AppDnaTokens.RadiusPill),
                    colors = ButtonDefaults.buttonColors(containerColor = AppDnaTokens.RoseAlert),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        actionLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = AppDnaTokens.OnPrimaryHero
                    )
                }
            }
        }
    }
}
