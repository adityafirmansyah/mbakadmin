package com.mbakadmin.dagango.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.mbakadmin.dagango.MbakAdminApp
import com.mbakadmin.dagango.data.local.entity.ChatLogEntity
import com.mbakadmin.dagango.data.local.entity.ProductEntity
import com.mbakadmin.dagango.data.local.entity.StoreSettingsEntity
import com.mbakadmin.dagango.service.MbakAdminGuardianService
import com.mbakadmin.dagango.util.BatteryOptimizationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.*

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
                    label = { Text("Toko & AI") }
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
                        openWhatsAppMessage(context, phone, msg)
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
        FulfillmentDialog(
            log = log,
            products = products,
            onDismiss = { fulfillmentLog = null },
            onConfirmFulfillment = { selectedProduct, qty, sendWa ->
                scope.launch {
                    if (selectedProduct != null && qty > 0) {
                        app.database.productDao().deductStock(selectedProduct.id, qty)
                    }
                    app.database.chatLogDao().updateVerificationStatus(log.id, true, "VERIFIED")
                    fulfillmentLog = null
                    snackbarHostState.showSnackbar("Pembayaran diverifikasi & stok diperbarui! ✅")

                    if (sendWa) {
                        val adminName = settings?.adminName?.ifBlank { "Mbak Admin" } ?: "Mbak Admin"
                        val waMsg = "Halo kak ${log.senderName}! Pembayaran untuk pesanan kakak sudah diverifikasi lunas ya oleh $adminName. Pesanan sedang disiapkan untuk packing & pengiriman. Terima kasih banyak! 🙏"
                        openWhatsAppMessage(context, log.customerPhone, waMsg)
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
                        openWhatsAppMessage(context, log.customerPhone, waMsg)
                    }
                }
            }
        )
    }
}

@Composable
fun CatalogTab(
    products: List<ProductEntity>,
    onEditProduct: (ProductEntity) -> Unit,
    onDeleteProduct: (ProductEntity) -> Unit,
    onToggleActive: (ProductEntity) -> Unit
) {
    if (products.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Belum ada produk. Tambahkan produk dengan tombol '+'")
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(products) { product ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (product.isActive) MaterialTheme.colorScheme.surface
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = product.name,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Rp ${product.price.toInt()} | Stok: ${product.stock} pcs",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Switch(
                                checked = product.isActive,
                                onCheckedChange = { onToggleActive(product) }
                            )
                        }

                        if (product.variants.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Varian: ${product.variants}", style = MaterialTheme.typography.bodySmall)
                        }
                        if (product.description.isNotBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(product.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TextButton(onClick = { onEditProduct(product) }) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Edit")
                            }
                            TextButton(
                                onClick = { onDeleteProduct(product) },
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Hapus")
                            }
                        }
                    }
                }
            }
        }
    }
}

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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(chatLogs) { log ->
                val needsVerification = log.isPaymentClaimed && !log.isVerifiedByHuman
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (needsVerification) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f)
                        else MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "👤 ${log.senderName}",
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = dateFormat.format(Date(log.timestamp)),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "💬 \"${log.incomingMessage}\"",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "🤖 MbakAdmin: ${log.aiReply}",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(8.dp)
                            )
                        }

                        // Human Verification & Fulfillment Section
                        if (log.isPaymentClaimed) {
                            Spacer(modifier = Modifier.height(10.dp))
                            if (!log.isVerifiedByHuman) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.errorContainer,
                                        shape = MaterialTheme.shapes.extraSmall
                                    ) {
                                        Text(
                                            text = "⏳ Perlu Verifikasi Pembayaran",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    Button(
                                        onClick = { onVerifyClick(log) },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Verifikasi & Proses", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            } else {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            shape = MaterialTheme.shapes.extraSmall
                                        ) {
                                            Text(
                                                text = "✅ Pembayaran Terverifikasi",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                        TextButton(onClick = { onCancelVerify(log.id) }) {
                                            Text("Batal Verifikasi", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Shipping / Resi Info
                                    if (log.trackingNumber.isBlank()) {
                                        OutlinedButton(
                                            onClick = { onShippingClick(log) },
                                            modifier = Modifier.fillMaxWidth(),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                        ) {
                                            Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("🚚 Input Nomor Resi Pengiriman", style = MaterialTheme.typography.labelSmall)
                                        }
                                    } else {
                                        Surface(
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                            shape = MaterialTheme.shapes.small,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier.padding(8.dp)
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "📦 Resi: ${log.courierName} - ${log.trackingNumber}",
                                                        style = MaterialTheme.typography.labelMedium
                                                    )
                                                }
                                                IconButton(
                                                    onClick = {
                                                        val msg = "Halo kak ${log.senderName}! Pesanan kakak sudah dikirim via ${log.courierName} dengan no. resi: ${log.trackingNumber}. Terima kasih sudah belanja di toko kami! 🙏"
                                                        onDirectSendWa(log.customerPhone, msg)
                                                    },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(Icons.Default.Send, contentDescription = "Kirim Resi ke WA", tint = MaterialTheme.colorScheme.primary)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StoreSettingsTab(
    currentSettings: StoreSettingsEntity,
    onSaveSettings: (StoreSettingsEntity) -> Unit
) {
    val scope = rememberCoroutineScope()
    var storeName by remember(currentSettings) { mutableStateOf(currentSettings.storeName) }
    var adminName by remember(currentSettings) { mutableStateOf(currentSettings.adminName) }
    var paymentInfo by remember(currentSettings) { mutableStateOf(currentSettings.paymentInfo) }
    var shippingInfo by remember(currentSettings) { mutableStateOf(currentSettings.shippingInfo) }
    var defaultShippingFee by remember(currentSettings) { mutableStateOf(currentSettings.defaultShippingFee.toInt().toString()) }

    // Advanced Settings State
    var showAdvancedSettings by remember { mutableStateOf(false) }
    var botTone by remember(currentSettings) { mutableStateOf(currentSettings.botTone) }
    var fallbackReplyMessage by remember(currentSettings) { mutableStateOf(currentSettings.fallbackReplyMessage) }
    var orderFormatTemplate by remember(currentSettings) { mutableStateOf(currentSettings.orderFormatTemplate) }
    var aiTimeoutSeconds by remember(currentSettings) { mutableIntStateOf(currentSettings.aiTimeoutSeconds) }

    var isTestingAi by remember { mutableStateOf(false) }
    var testResultMsg by remember { mutableStateOf<String?>(null) }
    var isTestSuccess by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // --- SECTION 1: PROFIL TOKO & PERSONA ---
        Text("Profil Toko & Admin", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        OutlinedTextField(
            value = storeName,
            onValueChange = { storeName = it },
            label = { Text("Nama Toko") },
            placeholder = { Text("Nama Toko Anda") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = adminName,
            onValueChange = { adminName = it },
            label = { Text("Panggilan Admin Toko") },
            placeholder = { Text("Contoh: Mbak Admin, Mas Budi, Kak Sarah") },
            supportingText = { Text("Nama ini yang akan dipakai saat membalas dan menyapa pembeli.") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // --- SECTION 2: PEMBAYARAN & ONGKIR ---
        Text("Pembayaran & Pengiriman", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        OutlinedTextField(
            value = paymentInfo,
            onValueChange = { paymentInfo = it },
            label = { Text("Info Rekening & Pembayaran") },
            placeholder = { Text("BCA: 1234567890 a/n Toko Saya\nMandiri: 9876543210 a/n Toko Saya") },
            supportingText = { Text("Otomatis dikirimkan setelah pembeli melengkapi format pemesanan.") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = shippingInfo,
            onValueChange = { shippingInfo = it },
            label = { Text("Kota Asal & Ekspedisi Pengiriman") },
            placeholder = { Text("Kirim dari Jakarta Pusat via JNE, J&T, SiCepat") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = defaultShippingFee,
            onValueChange = { str ->
                defaultShippingFee = str.filter { it.isDigit() }
            },
            label = { Text("Estimasi Ongkir Standar (Rp)") },
            placeholder = { Text("15000") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // --- SECTION 3: STATUS ENGINE AI TERINTEGRASI ---
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("✨ AI Engine Aktif", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Text(
                            text = "Hermes Combo",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = "AI bawaan MbakAdmin terhubung otomatis ke server berkecepatan tinggi tanpa perlu setting API Key rumit.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedButton(
                    onClick = {
                        isTestingAi = true
                        testResultMsg = null
                        scope.launch(Dispatchers.IO) {
                            val result = testAiConnection(
                                baseUrl = StoreSettingsEntity.DEFAULT_AI_BASE_URL,
                                apiKey = StoreSettingsEntity.DEFAULT_AI_API_KEY,
                                model = StoreSettingsEntity.DEFAULT_AI_MODEL,
                                timeoutSec = aiTimeoutSeconds.coerceIn(10, 180).toLong()
                            )
                            isTestingAi = false
                            isTestSuccess = result.first
                            testResultMsg = result.second
                        }
                    },
                    enabled = !isTestingAi,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isTestingAi) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Memeriksa Koneksi AI...")
                    } else {
                        Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Test Koneksi AI")
                    }
                }

                testResultMsg?.let { msg ->
                    Surface(
                        color = if (isTestSuccess) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isTestSuccess) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        }

        // --- SECTION 4: PENGATURAN LANJUTAN (COLLAPSIBLE / ACCORDION) ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAdvancedSettings = !showAdvancedSettings }
                        .padding(vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            if (showAdvancedSettings) Icons.Default.Tune else Icons.Default.Settings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text("Pengaturan Lanjutan", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("Kustomisasi format order, gaya bahasa & fallback", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                    Icon(
                        if (showAdvancedSettings) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline
                    )
                }

                if (showAdvancedSettings) {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = orderFormatTemplate,
                        onValueChange = { orderFormatTemplate = it },
                        label = { Text("Template Format Pemesanan") },
                        placeholder = { Text("Nama Penerima:\nNo. HP/WA:\nAlamat Lengkap:\nKecamatan & Kota:\nPesanan & Varian:\nJumlah:") },
                        supportingText = { Text("Format yang dikirimkan bot saat pembeli berniat order.") },
                        minLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = botTone,
                        onValueChange = { botTone = it },
                        label = { Text("Gaya Bicara / Tone Persona") },
                        placeholder = { Text("ramah, sopan, persuasif ala admin olshop") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = fallbackReplyMessage,
                        onValueChange = { fallbackReplyMessage = it },
                        label = { Text("Pesan Cadangan (Fallback)") },
                        placeholder = { Text("Halo kak! Pesan sudah {admin} terima ya...") },
                        supportingText = { Text("Dipakai jika koneksi internet offline. Gunakan {admin} untuk nama admin.") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = aiTimeoutSeconds.toString(),
                        onValueChange = { str ->
                            val filtered = str.filter { it.isDigit() }
                            aiTimeoutSeconds = filtered.toIntOrNull() ?: 60
                        },
                        label = { Text("Batas Waktu Respon AI (Detik)") },
                        placeholder = { Text("60") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // --- SECTION 5: TOMBOL SIMPAN PENGATURAN ---
        Button(
            onClick = {
                onSaveSettings(
                    currentSettings.copy(
                        storeName = storeName,
                        adminName = adminName.ifBlank { "Mbak Admin" },
                        paymentInfo = paymentInfo,
                        shippingInfo = shippingInfo,
                        defaultShippingFee = defaultShippingFee.toDoubleOrNull() ?: 15000.0,
                        orderFormatTemplate = orderFormatTemplate.ifBlank {
                            "Nama Penerima:\nNo. HP/WA:\nAlamat Lengkap:\nKecamatan & Kota:\nPesanan & Varian:\nJumlah:"
                        },
                        botTone = botTone,
                        fallbackReplyMessage = fallbackReplyMessage.ifBlank {
                            "Halo kak! Pesan sudah {admin} terima ya, mohon ditunggu sebentar biar segera dicek manual oleh tim kami 🙏"
                        },
                        aiApiBaseUrl = StoreSettingsEntity.DEFAULT_AI_BASE_URL,
                        aiApiKey = StoreSettingsEntity.DEFAULT_AI_API_KEY,
                        aiModel = StoreSettingsEntity.DEFAULT_AI_MODEL,
                        aiTimeoutSeconds = aiTimeoutSeconds.coerceIn(15, 180)
                    )
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Save, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Simpan Pengaturan")
        }
    }
}

@Composable
fun SystemReliabilityTab(
    context: Context,
    isListenerEnabled: Boolean,
    isBatteryOptimized: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Status Kesiapan Sistem", style = MaterialTheme.typography.titleLarge)

        StatusCard(
            title = if (isListenerEnabled) "Izin Notifikasi WhatsApp Aktif" else "Izin Notifikasi Belum Aktif",
            subtitle = if (isListenerEnabled) "MbakAdmin dapat mendeteksi chat masuk" else "Wajib diaktifkan agar bot bisa merespons chat",
            isOk = isListenerEnabled,
            actionLabel = "Buka Izin",
            onActionClick = {
                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
        )

        StatusCard(
            title = if (!isBatteryOptimized) "Baterai Bebas Batasan (Aman)" else "Optimasi Baterai Masih Aktif",
            subtitle = if (!isBatteryOptimized) "Android tidak akan mematikan bot saat layar mati" else "HP dapat mematikan bot sewaktu-waktu demi hemat baterai",
            isOk = !isBatteryOptimized,
            actionLabel = "Matikan Batasan",
            onActionClick = {
                BatteryOptimizationHelper.requestIgnoreBatteryOptimizations(context)
            }
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Auto-Start Khusus HP (${Build.MANUFACTURER.uppercase()})", style = MaterialTheme.typography.titleSmall)
                    Text("Izinkan app berjalan otomatis saat HP dinyalakan", style = MaterialTheme.typography.bodySmall)
                }
                TextButton(onClick = {
                    BatteryOptimizationHelper.openOemAutoStartSettings(context)
                }) {
                    Text("Buka")
                }
            }
        }
    }
}

@Composable
fun StatusCard(
    title: String,
    subtitle: String,
    isOk: Boolean,
    actionLabel: String,
    onActionClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isOk) MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.errorContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isOk) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (isOk) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleSmall)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall)
            }
            if (!isOk) {
                Button(onClick = onActionClick, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
                    Text(actionLabel, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialProduct == null) "Tambah Produk Baru" else "Edit Produk") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Produk") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Harga (Rp)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = stock,
                    onValueChange = { stock = it },
                    label = { Text("Stok") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = variants,
                    onValueChange = { variants = it },
                    label = { Text("Varian (contoh: Merah, Navy, Hitam)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Deskripsi Singkat") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val p = price.toDoubleOrNull() ?: 0.0
                    val s = stock.toIntOrNull() ?: 0
                    if (name.isNotBlank() && p > 0) {
                        onConfirm(
                            initialProduct?.copy(
                                name = name,
                                price = p,
                                stock = s,
                                variants = variants,
                                description = description
                            ) ?: ProductEntity(
                                name = name,
                                price = p,
                                stock = s,
                                variants = variants,
                                description = description
                            )
                        )
                    }
                }
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

fun isNotificationServiceEnabled(context: Context): Boolean {
    val pkgName = context.packageName
    val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
    if (flat != null && flat.isNotEmpty()) {
        val names = flat.split(":")
        for (name in names) {
            val cn = ComponentName.unflattenFromString(name)
            if (cn != null && cn.packageName == pkgName) {
                return true
            }
        }
    }
    return false
}

private fun testAiConnection(baseUrl: String, apiKey: String, model: String, timeoutSec: Long = 60L): Pair<Boolean, String> {
    try {
        val client = okhttp3.OkHttpClient.Builder()
            .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(timeoutSec, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        val endpoint = if (baseUrl.endsWith("/chat/completions")) {
            baseUrl
        } else {
            "${baseUrl.trimEnd('/')}/chat/completions"
        }

        val payload = org.json.JSONObject().apply {
            put("model", model)
            put("messages", org.json.JSONArray().apply {
                put(org.json.JSONObject().apply {
                    put("role", "user")
                    put("content", "ping")
                })
            })
            put("max_tokens", 10)
            put("stream", false)
        }

        val requestBuilder = okhttp3.Request.Builder()
            .url(endpoint)
            .addHeader("Content-Type", "application/json")
            .post(payload.toString().toRequestBody("application/json".toMediaType()))

        if (apiKey.isNotBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer $apiKey")
        }

        client.newCall(requestBuilder.build()).execute().use { response ->
            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                val text = extractAiResponseContent(body)
                if (text.isNotBlank()) {
                    return Pair(true, "✅ Koneksi Berhasil! Respon ($model): \"$text\"")
                } else {
                    return Pair(false, "❌ Respon kosong dari model $model")
                }
            } else {
                val errBody = response.body?.string() ?: ""
                return Pair(false, "❌ Gagal HTTP ${response.code}: $errBody")
            }
        }
    } catch (e: Exception) {
        return Pair(false, "❌ Error: ${e.javaClass.simpleName} - ${e.message}")
    }
}

private fun extractAiResponseContent(rawBody: String): String {
    val body = rawBody.trim()
    if (body.isBlank()) return ""

    // 1. Coba parse standard JSON object
    if (body.startsWith("{")) {
        try {
            val json = org.json.JSONObject(body)
            val choices = json.optJSONArray("choices")
            if (choices != null && choices.length() > 0) {
                val choice = choices.getJSONObject(0)
                val message = choice.optJSONObject("message")
                val content = message?.optString("content")
                if (!content.isNullOrBlank()) return content.trim()

                val delta = choice.optJSONObject("delta")
                val deltaContent = delta?.optString("content")
                if (!deltaContent.isNullOrBlank()) return deltaContent.trim()
            }
        } catch (_: Exception) {}
    }

    // 2. Fallback parse SSE stream (data: {...}\n\ndata: {...})
    if (body.contains("data:")) {
        val contentBuilder = StringBuilder()
        body.lines().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.startsWith("data:") && !trimmed.contains("[DONE]")) {
                val jsonStr = trimmed.removePrefix("data:").trim()
                try {
                    val json = org.json.JSONObject(jsonStr)
                    val choices = json.optJSONArray("choices")
                    if (choices != null && choices.length() > 0) {
                        val choice = choices.getJSONObject(0)
                        val delta = choice.optJSONObject("delta")
                        val part = delta?.optString("content")
                        if (!part.isNullOrEmpty()) {
                            contentBuilder.append(part)
                        }
                    }
                } catch (_: Exception) {}
            }
        }
        val sseResult = contentBuilder.toString().trim()
        if (sseResult.isNotBlank()) return sseResult
    }

    return ""
}

fun openWhatsAppMessage(context: Context, phone: String, message: String) {
    try {
        val encoded = java.net.URLEncoder.encode(message, "UTF-8")
        val cleanPhone = phone.filter { it.isDigit() }
        val finalPhone = if (cleanPhone.startsWith("0")) "62" + cleanPhone.substring(1) else cleanPhone
        val uri = if (finalPhone.isNotBlank()) {
            android.net.Uri.parse("https://api.whatsapp.com/send?phone=$finalPhone&text=$encoded")
        } else {
            android.net.Uri.parse("https://api.whatsapp.com/send?text=$encoded")
        }
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.whatsapp")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        try {
            val encoded = java.net.URLEncoder.encode(message, "UTF-8")
            val fallbackUri = android.net.Uri.parse("https://api.whatsapp.com/send?text=$encoded")
            context.startActivity(
                Intent(Intent.ACTION_VIEW, fallbackUri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        } catch (_: Exception) {
            android.widget.Toast.makeText(context, "Aplikasi WhatsApp tidak ditemukan", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
fun FulfillmentDialog(
    log: ChatLogEntity,
    products: List<ProductEntity>,
    onDismiss: () -> Unit,
    onConfirmFulfillment: (ProductEntity?, Int, Boolean) -> Unit
) {
    var selectedProduct by remember { mutableStateOf<ProductEntity?>(products.firstOrNull()) }
    var qtyText by remember { mutableStateOf("1") }
    var sendWhatsAppConfirmation by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Verifikasi & Proses Pesanan", style = MaterialTheme.typography.titleMedium)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Pembeli: ${log.senderName}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                if (log.customerPhone.isNotBlank()) {
                    Text(
                        text = "📱 No. HP/WA: ${log.customerPhone}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Pesan Pembeli: \"${log.incomingMessage}\"",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(8.dp)
                    )
                }

                HorizontalDivider()

                Text("Potong Stok Otomatis (Opsional):", style = MaterialTheme.typography.labelMedium)

                if (products.isEmpty()) {
                    Text("Belum ada produk di katalog.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                } else {
                    products.forEach { prod ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedProduct = if (selectedProduct?.id == prod.id) null else prod }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedProduct?.id == prod.id,
                                onClick = { selectedProduct = if (selectedProduct?.id == prod.id) null else prod }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(prod.name, style = MaterialTheme.typography.bodyMedium)
                                Text("Sisa Stok: ${prod.stock} | Rp${prod.price.toInt()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }

                    if (selectedProduct != null) {
                        OutlinedTextField(
                            value = qtyText,
                            onValueChange = { qtyText = it.filter { c -> c.isDigit() } },
                            label = { Text("Jumlah Terjual (Qty)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                HorizontalDivider()

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { sendWhatsAppConfirmation = !sendWhatsAppConfirmation }
                ) {
                    Checkbox(
                        checked = sendWhatsAppConfirmation,
                        onCheckedChange = { sendWhatsAppConfirmation = it }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Kirim pesan konfirmasi lunas ke WhatsApp pembeli",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = qtyText.toIntOrNull() ?: 1
                    onConfirmFulfillment(selectedProduct, qty, sendWhatsAppConfirmation)
                }
            ) {
                Text("Verifikasi Pembayaran")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

@Composable
fun ShippingDialog(
    log: ChatLogEntity,
    onDismiss: () -> Unit,
    onConfirmShipping: (String, String, Boolean) -> Unit
) {
    var courierName by remember { mutableStateOf(if (log.courierName.isNotBlank()) log.courierName else "J&T Express") }
    var trackingNumber by remember { mutableStateOf(log.trackingNumber) }
    var sendWhatsAppResi by remember { mutableStateOf(true) }

    val commonCouriers = listOf("J&T Express", "JNE", "SiCepat", "Shopee Xpress", "Pos Indonesia", "GoSend/Grab")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Input Nomor Resi Pengiriman", style = MaterialTheme.typography.titleMedium)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Penerima: ${log.senderName}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )

                Text("Pilih Ekspedisi / Kurir:", style = MaterialTheme.typography.labelMedium)
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    commonCouriers.forEach { courier ->
                        FilterChip(
                            selected = courierName == courier,
                            onClick = { courierName = courier },
                            label = { Text(courier, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                OutlinedTextField(
                    value = courierName,
                    onValueChange = { courierName = it },
                    label = { Text("Nama Ekspedisi") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = trackingNumber,
                    onValueChange = { trackingNumber = it },
                    label = { Text("Nomor Resi") },
                    placeholder = { Text("Contoh: JT1234567890") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { sendWhatsAppResi = !sendWhatsAppResi }
                ) {
                    Checkbox(
                        checked = sendWhatsAppResi,
                        onCheckedChange = { sendWhatsAppResi = it }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Kirim info resi ke WhatsApp pembeli",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (trackingNumber.isNotBlank()) {
                        onConfirmShipping(courierName.ifBlank { "Kurir" }, trackingNumber, sendWhatsAppResi)
                    }
                },
                enabled = trackingNumber.isNotBlank()
            ) {
                Text("Simpan & Kirim Resi")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
