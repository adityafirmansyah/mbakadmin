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
import androidx.compose.ui.unit.dp
import com.mbakadmin.dagango.data.local.entity.ChatLogEntity
import com.mbakadmin.dagango.ui.components.ChatSenderGroup
import com.mbakadmin.dagango.ui.components.StackedSenderChatCard
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
                Icon(Icons.Default.MarkChatRead, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Belum ada riwayat chat otomatis.", style = MaterialTheme.typography.bodyMedium)
                Text("Chat WhatsApp yang dibalas MbakAdmin akan muncul di sini.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
        }
    } else {
        val dateFormat = remember { SimpleDateFormat("HH:mm - dd MMM", Locale("id", "ID")) }
        val timeFormat = remember { SimpleDateFormat("HH:mm", Locale("id", "ID")) }

        // Kelompokkan chat log berdasarkan pengirim (senderName), prioritaskan yang butuh verifikasi di paling atas
        val senderGroups = remember(chatLogs) {
            chatLogs
                .groupBy { it.senderName }
                .map { (sender, logs) ->
                    val timeSorted = logs.sortedBy { it.timestamp }
                    val latest = logs.maxByOrNull { it.timestamp } ?: logs.first()
                    val pendingLog = logs.firstOrNull { it.isPaymentClaimed && !it.isVerifiedByHuman }
                    val phone = logs.firstOrNull { it.customerPhone.isNotBlank() }?.customerPhone ?: ""
                    ChatSenderGroup(
                        senderName = sender,
                        messages = timeSorted,
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

        var expandedSenderNames by remember { mutableStateOf(setOf<String>()) }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(senderGroups, key = { it.senderName }) { group ->
                val isExpanded = expandedSenderNames.contains(group.senderName)

                StackedSenderChatCard(
                    group = group,
                    isExpanded = isExpanded,
                    onToggleExpand = {
                        expandedSenderNames = if (isExpanded) {
                            expandedSenderNames - group.senderName
                        } else {
                            expandedSenderNames + group.senderName
                        }
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
