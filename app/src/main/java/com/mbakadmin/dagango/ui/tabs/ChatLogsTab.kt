package com.mbakadmin.dagango.ui.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MarkChatRead
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mbakadmin.dagango.data.local.entity.ChatLogEntity
import com.mbakadmin.dagango.ui.components.ChatSenderGroup
import com.mbakadmin.dagango.ui.components.StackedSenderChatCard
import com.mbakadmin.dagango.ui.theme.AppDnaTokens
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ChatLogsTab(
    chatLogs: List<ChatLogEntity>,
    onVerifyClick: (ChatLogEntity) -> Unit = {},
    onShippingClick: (ChatLogEntity) -> Unit = {},
    onCancelVerify: (Long) -> Unit = {},
    onDirectSendWa: (String, String) -> Unit = { _, _ -> }
) {
    if (chatLogs.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.MarkChatRead, contentDescription = null, modifier = Modifier.size(48.dp), tint = AppDnaTokens.ForegroundSubtle)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Belum ada riwayat chat otomatis.", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = AppDnaTokens.Foreground)
                Text("Chat WhatsApp yang dibalas MbakAdmin akan muncul di sini.", style = MaterialTheme.typography.bodySmall, color = AppDnaTokens.ForegroundMuted)
            }
        }
    } else {
        val dateFormat = remember { SimpleDateFormat("HH:mm - dd MMM", Locale("id", "ID")) }
        val timeFormat = remember { SimpleDateFormat("HH:mm", Locale("id", "ID")) }

        val senderGroups = remember(chatLogs) {
            chatLogs
                .groupBy { it.senderName }
                .map { (sender, logs) ->
                    val sortedDesc = logs.sortedByDescending { it.timestamp }
                    val latest = sortedDesc.first()
                    val pendingLog = logs.firstOrNull { it.isPaymentClaimed && !it.isVerifiedByHuman }
                    val phone = logs.firstOrNull { it.customerPhone.isNotBlank() }?.customerPhone ?: ""
                    ChatSenderGroup(
                        senderName = sender,
                        messages = sortedDesc,
                        latestMessage = latest,
                        hasPendingVerification = pendingLog != null,
                        pendingVerificationLog = pendingLog,
                        customerPhone = phone
                    )
                }
                .sortedWith(
                    compareByDescending<ChatSenderGroup> { it.hasPendingVerification }
                        .thenByDescending { it.latestMessage.timestamp }
                )
        }

        var openedSenderName by remember { mutableStateOf<String?>(null) }

        val displayedGroups = remember(senderGroups, openedSenderName) {
            if (openedSenderName == null) {
                senderGroups
            } else {
                senderGroups.filter { it.senderName == openedSenderName }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(AppDnaTokens.GapFeed)
        ) {
            items(displayedGroups, key = { it.senderName }) { group ->
                val isExpanded = openedSenderName == group.senderName

                StackedSenderChatCard(
                    group = group,
                    isExpanded = isExpanded,
                    onToggleExpand = {
                        openedSenderName = if (isExpanded) null else group.senderName
                    },
                    onVerifyClick = onVerifyClick,
                    onShippingClick = onShippingClick,
                    onCancelVerify = onCancelVerify,
                    onDirectSendWa = onDirectSendWa,
                    dateFormat = dateFormat,
                    timeFormat = timeFormat
                )
            }
        }
    }
}
