package com.mbakadmin.dagango.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.mbakadmin.dagango.MbakAdminApp
import com.mbakadmin.dagango.data.local.entity.ChatLogEntity
import com.mbakadmin.dagango.data.local.entity.ProductEntity
import com.mbakadmin.dagango.data.local.entity.StoreSettingsEntity
import com.mbakadmin.dagango.service.MbakAdminGuardianService
import com.mbakadmin.dagango.ui.dialogs.FulfillmentDialog
import com.mbakadmin.dagango.ui.dialogs.ProductDialog
import com.mbakadmin.dagango.ui.dialogs.ShippingDialog
import com.mbakadmin.dagango.ui.tabs.CatalogTab
import com.mbakadmin.dagango.ui.tabs.ChatLogsTab
import com.mbakadmin.dagango.ui.tabs.StoreSettingsTab
import com.mbakadmin.dagango.ui.tabs.SystemReliabilityTab
import com.mbakadmin.dagango.util.BatteryOptimizationHelper
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as MbakAdminApp
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Katalog, 1: Riwayat Chat, 2: Toko & AI, 3: Sistem

    var isListenerEnabled by remember { mutableStateOf(isNotificationServiceEnabled(context)) }
    var isBatteryOptimized by remember { mutableStateOf(!BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isListenerEnabled = isNotificationServiceEnabled(context)
                isBatteryOptimized = !BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)
                if (isListenerEnabled) {
                    MbakAdminGuardianService.startService(context)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val products by app.database.productDao().getAllProducts().collectAsState(initial = emptyList())
    val chatLogs by app.database.chatLogDao().getRecentChatLogs().collectAsState(initial = emptyList())
    val settings by app.database.storeSettingsDao().getSettingsFlow().collectAsState(initial = null)

    var showAddEditProductDialog by remember { mutableStateOf<ProductEntity?>(null) }
    var isCreatingNewProduct by remember { mutableStateOf(false) }
    var fulfillmentLog by remember { mutableStateOf<ChatLogEntity?>(null) }
    var shippingLog by remember { mutableStateOf<ChatLogEntity?>(null) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            val isAutoReply = settings?.isAutoReplyActive ?: true
            TopAppBar(
                title = {
                    Column {
                        Text("MbakAdmin 🤖", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = if (!isListenerEnabled) "Perlu Izin Sistem"
                            else if (isAutoReply) "Auto-Reply Aktif"
                            else "Auto-Reply Nonaktif",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (!isListenerEnabled) MaterialTheme.colorScheme.error
                            else if (isAutoReply) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Switch(
                            checked = isAutoReply,
                            onCheckedChange = { checked ->
                                scope.launch {
                                    val current = settings ?: StoreSettingsEntity()
                                    app.database.storeSettingsDao().saveSettings(current.copy(isAutoReplyActive = checked))
                                    val msg = if (checked) "Auto-Reply WhatsApp diaktifkan 🤖" else "Auto-Reply WhatsApp dinonaktifkan ⏸️"
                                    snackbarHostState.showSnackbar(msg)
                                }
                            },
                            thumbContent = {
                                Icon(
                                    imageVector = if (isAutoReply) Icons.Default.Check else Icons.Default.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    }
                    if (selectedTab == 1 && chatLogs.isNotEmpty()) {
                        IconButton(onClick = {
                            scope.launch { app.database.chatLogDao().clearChatLogs() }
                        }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Bersihkan Log")
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Inventory2, contentDescription = null) },
                    label = { Text("Katalog") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Forum, contentDescription = null) },
                    label = { Text("Chat Log") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Storefront, contentDescription = null) },
                    label = { Text("Toko") }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Security, contentDescription = null) },
                    label = { Text("Sistem") }
                )
            }
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(onClick = { isCreatingNewProduct = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Tambah Produk")
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            when (selectedTab) {
                0 -> CatalogTab(
                    products = products,
                    onEditProduct = { showAddEditProductDialog = it },
                    onDeleteProduct = { product ->
                        scope.launch {
                            app.database.productDao().deleteProduct(product)
                            snackbarHostState.showSnackbar("Produk '${product.name}' berhasil dihapus")
                        }
                    },
                    onToggleActive = { product ->
                        scope.launch {
                            val newStatus = !product.isActive
                            app.database.productDao().updateProduct(product.copy(isActive = newStatus))
                            val statusText = if (newStatus) "diaktifkan" else "dinonaktifkan"
                            snackbarHostState.showSnackbar("Stok '${product.name}' $statusText")
                        }
                    }
                )
                1 -> ChatLogsTab(
                    chatLogs = chatLogs,
                    onVerifyClick = { log -> fulfillmentLog = log },
                    onShippingClick = { log -> shippingLog = log },
                    onCancelVerify = { logId ->
                        scope.launch {
                            app.database.chatLogDao().updateVerificationStatus(logId, false, "PENDING")
                            snackbarHostState.showSnackbar("Status verifikasi dibatalkan")
                        }
                    },
                    onDirectSendWa = { phone, msg ->
                        openWhatsAppMessage(context, phone, msg, settings?.targetWhatsAppPackage ?: "ALL")
                    }
                )
                2 -> StoreSettingsTab(
                    currentSettings = settings ?: StoreSettingsEntity(),
                    onSaveSettings = { updated ->
                        scope.launch {
                            app.database.storeSettingsDao().saveSettings(updated)
                            snackbarHostState.showSnackbar("Pengaturan toko & AI berhasil disimpan!")
                        }
                    }
                )
                3 -> SystemReliabilityTab(
                    context = context,
                    isListenerEnabled = isListenerEnabled,
                    isBatteryOptimized = isBatteryOptimized
                )
            }
        }
    }

    if (isCreatingNewProduct) {
        ProductDialog(
            initialProduct = null,
            onDismiss = { isCreatingNewProduct = false },
            onConfirm = { product ->
                scope.launch {
                    app.database.productDao().insertProduct(product)
                    isCreatingNewProduct = false
                    snackbarHostState.showSnackbar("Produk '${product.name}' berhasil ditambahkan!")
                }
            }
        )
    }

    showAddEditProductDialog?.let { productToEdit ->
        ProductDialog(
            initialProduct = productToEdit,
            onDismiss = { showAddEditProductDialog = null },
            onConfirm = { updated ->
                scope.launch {
                    app.database.productDao().updateProduct(updated)
                    showAddEditProductDialog = null
                    snackbarHostState.showSnackbar("Perubahan '${updated.name}' tersimpan!")
                }
            }
        )
    }

    fulfillmentLog?.let { log ->
        val senderLogs = remember(log, chatLogs) {
            chatLogs.filter { it.senderName.equals(log.senderName, ignoreCase = true) }
        }
        FulfillmentDialog(
            log = log,
            products = products,
            recentLogs = senderLogs,
            onDismiss = { fulfillmentLog = null },
            onConfirmFulfillment = { itemsToDeduct, sendWa ->
                scope.launch {
                    itemsToDeduct.forEach { (selectedProduct, qty) ->
                        if (qty > 0) {
                            app.database.productDao().deductStock(selectedProduct.id, qty)
                        }
                    }
                    app.database.chatLogDao().updateVerificationStatus(log.id, true, "VERIFIED")
                    fulfillmentLog = null
                    val count = itemsToDeduct.size
                    val msg = if (count > 0) {
                        "Pembayaran diverifikasi & stok $count produk diperbarui! ✅"
                    } else {
                        "Pembayaran diverifikasi! ✅"
                    }
                    snackbarHostState.showSnackbar(msg)

                    if (sendWa) {
                        val adminName = settings?.adminName?.ifBlank { "Mbak Admin" } ?: "Mbak Admin"
                        val waMsg = "Halo kak ${log.senderName}! Pembayaran untuk pesanan kakak sudah diverifikasi lunas ya oleh $adminName. Pesanan sedang disiapkan untuk packing & pengiriman. Terima kasih banyak! 🙏"
                        val targetPhone = log.customerPhone.ifBlank {
                            senderLogs.firstOrNull { it.customerPhone.isNotBlank() }?.customerPhone ?: ""
                        }
                        openWhatsAppMessage(context, targetPhone, waMsg, settings?.targetWhatsAppPackage ?: "ALL")
                    }
                }
            }
        )
    }

    shippingLog?.let { log ->
        ShippingDialog(
            log = log,
            onDismiss = { shippingLog = null },
            onConfirmShipping = { courier, trackingNum, sendWa ->
                scope.launch {
                    app.database.chatLogDao().updateShippingInfo(log.id, trackingNum, courier)
                    shippingLog = null
                    snackbarHostState.showSnackbar("Resi berhasil disimpan! 📦")

                    if (sendWa) {
                        val adminName = settings?.adminName?.ifBlank { "Mbak Admin" } ?: "Mbak Admin"
                        val waMsg = "Halo kak ${log.senderName}! Pesanan kakak sudah dikirim oleh $adminName via $courier dengan nomor resi: $trackingNum. Bisa dicek berkala ya kak, terima kasih banyak! 🙏"
                        openWhatsAppMessage(context, log.customerPhone, waMsg, settings?.targetWhatsAppPackage ?: "ALL")
                    }
                }
            }
        )
    }
}
