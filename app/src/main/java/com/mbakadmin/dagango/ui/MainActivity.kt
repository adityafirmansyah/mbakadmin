package com.mbakadmin.dagango.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.mbakadmin.dagango.MbakAdminApp
import com.mbakadmin.dagango.data.local.entity.ChatLogEntity
import com.mbakadmin.dagango.data.local.entity.ProductEntity
import com.mbakadmin.dagango.data.local.entity.StoreSettingsEntity
import com.mbakadmin.dagango.service.MbakAdminGuardianService
import com.mbakadmin.dagango.util.BatteryOptimizationHelper
import kotlinx.coroutines.launch
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

    val products by app.database.productDao().getAllProducts().collectAsState(initial = emptyList())
    val chatLogs by app.database.chatLogDao().getRecentChatLogs().collectAsState(initial = emptyList())
    val settings by app.database.storeSettingsDao().getSettingsFlow().collectAsState(initial = null)

    var showAddEditProductDialog by remember { mutableStateOf<ProductEntity?>(null) }
    var isCreatingNewProduct by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("MbakAdmin 🤖", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = if (isListenerEnabled) "Aktif Berjaga" else "Perlu Izin Sistem",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isListenerEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
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
                        scope.launch { app.database.productDao().deleteProduct(product) }
                    },
                    onToggleActive = { product ->
                        scope.launch {
                            app.database.productDao().updateProduct(product.copy(isActive = !product.isActive))
                        }
                    }
                )
                1 -> ChatLogsTab(chatLogs = chatLogs)
                2 -> StoreSettingsTab(
                    currentSettings = settings ?: StoreSettingsEntity(),
                    onSaveSettings = { updated ->
                        scope.launch { app.database.storeSettingsDao().saveSettings(updated) }
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
fun ChatLogsTab(chatLogs: List<ChatLogEntity>) {
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
                Card(modifier = Modifier.fillMaxWidth()) {
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
    var storeName by remember(currentSettings) { mutableStateOf(currentSettings.storeName) }
    var adminName by remember(currentSettings) { mutableStateOf(currentSettings.adminName) }
    var paymentInfo by remember(currentSettings) { mutableStateOf(currentSettings.paymentInfo) }
    var shippingInfo by remember(currentSettings) { mutableStateOf(currentSettings.shippingInfo) }
    var botTone by remember(currentSettings) { mutableStateOf(currentSettings.botTone) }
    var isAutoReplyActive by remember(currentSettings) { mutableStateOf(currentSettings.isAutoReplyActive) }
    var aiApiBaseUrl by remember(currentSettings) { mutableStateOf(currentSettings.aiApiBaseUrl) }
    var aiApiKey by remember(currentSettings) { mutableStateOf(currentSettings.aiApiKey) }
    var aiModel by remember(currentSettings) { mutableStateOf(currentSettings.aiModel) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Auto-Reply WhatsApp", style = MaterialTheme.typography.titleMedium)
                    Text("Aktifkan agar AI otomatis membalas notifikasi chat pembeli", style = MaterialTheme.typography.bodySmall)
                }
                Switch(
                    checked = isAutoReplyActive,
                    onCheckedChange = { isAutoReplyActive = it }
                )
            }
        }

        Text("Informasi Toko & Admin", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = storeName,
            onValueChange = { storeName = it },
            label = { Text("Nama Toko") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = adminName,
            onValueChange = { adminName = it },
            label = { Text("Nama / Panggilan Admin (Persona)") },
            placeholder = { Text("Contoh: Mbak Admin, Mas Budi, Kak Sarah") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = paymentInfo,
            onValueChange = { paymentInfo = it },
            label = { Text("Info Rekening & Pembayaran") },
            placeholder = { Text("BCA: 1234567890 a/n Toko\nQRIS tersedia") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = shippingInfo,
            onValueChange = { shippingInfo = it },
            label = { Text("Info Pengiriman & Ekspedisi") },
            placeholder = { Text("Kirim dari Bandung via J&T, SiCepat, Gosend") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth()
        )

        Text("Konfigurasi AI Engine & Proxy", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = botTone,
            onValueChange = { botTone = it },
            label = { Text("Gaya Bicara / Tone Persona") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = aiApiBaseUrl,
            onValueChange = { aiApiBaseUrl = it },
            label = { Text("API Base URL (OpenAI / OpenRouter / 9Router / Custom)") },
            placeholder = { Text("https://api.openai.com/v1") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = aiApiKey,
            onValueChange = { aiApiKey = it },
            label = { Text("API Key") },
            placeholder = { Text("sk-...") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = aiModel,
            onValueChange = { aiModel = it },
            label = { Text("Model AI (Default: gpt-4o-mini)") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                onSaveSettings(
                    currentSettings.copy(
                        storeName = storeName,
                        adminName = adminName.ifBlank { "Mbak Admin" },
                        paymentInfo = paymentInfo,
                        shippingInfo = shippingInfo,
                        botTone = botTone,
                        isAutoReplyActive = isAutoReplyActive,
                        aiApiBaseUrl = aiApiBaseUrl.ifBlank { "https://api.openai.com/v1" },
                        aiApiKey = aiApiKey,
                        aiModel = aiModel.ifBlank { "gpt-4o-mini" }
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
