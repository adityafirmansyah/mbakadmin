package com.mbakadmin.dagango.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mbakadmin.dagango.ui.theme.AppDnaTokens

/**
 * Mandatory Patterns: `full-width-nav` & `fab-elevated`
 * Fixed flush bottom navigation with hairline divider, 5 slots, and
 * a 44dp circular elevated action button raised 8dp above the top edge.
 */
@Composable
fun FullWidthNavBar(
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    onFabClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(76.dp)
    ) {
        // Nav bar container
        Surface(
            color = AppDnaTokens.SurfaceCard,
            shadowElevation = AppDnaTokens.ElevationLevel1,
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .align(Alignment.BottomCenter)
        ) {
            // Hairline top border
            Column(modifier = Modifier.fillMaxSize()) {
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = AppDnaTokens.BorderCard
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceAround,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp)
                ) {
                    // Slot 1: Katalog
                    NavItem(
                        label = "Katalog",
                        icon = Icons.Default.Inventory2,
                        isSelected = selectedTab == 0,
                        onClick = { onSelectTab(0) },
                        modifier = Modifier.weight(1f)
                    )

                    // Slot 2: Chat Log
                    NavItem(
                        label = "Chat Log",
                        icon = Icons.Default.Forum,
                        isSelected = selectedTab == 1,
                        onClick = { onSelectTab(1) },
                        modifier = Modifier.weight(1f)
                    )

                    // Slot 3: Spacer for Elevated FAB in the center
                    Spacer(modifier = Modifier.weight(1f))

                    // Slot 4: Toko
                    NavItem(
                        label = "Toko",
                        icon = Icons.Default.Storefront,
                        isSelected = selectedTab == 2,
                        onClick = { onSelectTab(2) },
                        modifier = Modifier.weight(1f)
                    )

                    // Slot 5: Sistem
                    NavItem(
                        label = "Sistem",
                        icon = Icons.Default.Security,
                        isSelected = selectedTab == 3,
                        onClick = { onSelectTab(3) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Elevated FAB: 44dp diameter circle raised 8dp above the top border
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-AppDnaTokens.FabElevationOffset))
                .size(AppDnaTokens.FabSize)
                .shadow(
                    elevation = AppDnaTokens.ElevationLevel2,
                    shape = CircleShape
                )
                .background(AppDnaTokens.PrimaryHero, CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onFabClick
                )
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Aksi Utama",
                tint = AppDnaTokens.OnPrimaryHero,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun NavItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) AppDnaTokens.Foreground else AppDnaTokens.ForegroundMuted,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            ),
            color = if (isSelected) AppDnaTokens.Foreground else AppDnaTokens.ForegroundMuted
        )
    }
}
