package id.mbakadmin.app.ui

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
import id.mbakadmin.app.MbakAdminApp
import id.mbakadmin.app.data.local.entity.ProductEntity
import id.mbakadmin.app.service.MbakAdminGuardianService
import id.mbakadmin.app.util.BatteryOptimizationHelper
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

    var isListenerEnabled by remember { mutableStateOf(isNotificationServiceEnabled(context)) }
    var isBatteryOptimized by remember { mutableStateOf(!BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)) }

    // Auto-refresh permission state saat user kembali ke aplikasi
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
    var showAddProductDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MbakAdmin 🤖") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddProductDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Produk")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Section Reliability & Permissions
            Text("Status Kesiapan Sistem", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            // Card 1: Notification Listener Permission
            StatusCard(
                title = if (isListenerEnabled) "Izin Notifikasi WhatsApp Aktif" else "Izin Notifikasi WhatsApp Belum Aktif",
                subtitle = if (isListenerEnabled) "MbakAdmin dapat membaca & mendeteksi chat masuk" else "Wajib diaktifkan agar bot bisa merespons chat",
                isOk = isListenerEnabled,
                actionLabel = "Buka Izin",
                onActionClick = {
                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Card 2: Battery Optimization Whitelist
            StatusCard(
                title = if (!isBatteryOptimized) "Baterai Bebas Batasan (Aman)" else "Optimasi Baterai Masih Aktif",
                subtitle = if (!isBatteryOptimized) "Android tidak akan mematikan bot saat layar mati" else "HP dapat mematikan bot sewaktu-waktu demi hemat baterai",
                isOk = !isBatteryOptimized,
                actionLabel = "Matikan Batasan",
                onActionClick = {
                    BatteryOptimizationHelper.requestIgnoreBatteryOptimizations(context)
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Card 3: Auto-Start Vendor Manager (Xiaomi/Oppo/Vivo/Infinix)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
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

            Spacer(modifier = Modifier.height(20.dp))

            // Section Katalog Produk
            Text("Katalog Produk Lokal (${products.size})", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))

            if (products.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Belum ada produk. Tambahkan produk dengan tombol '+'")
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(products) { product ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(product.name, style = MaterialTheme.typography.titleMedium)
                                Text("Rp ${product.price.toInt()} | Stok: ${product.stock}", style = MaterialTheme.typography.bodyMedium)
                                if (product.variants.isNotBlank()) {
                                    Text("Varian: ${product.variants}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddProductDialog) {
        AddProductDialog(
            onDismiss = { showAddProductDialog = false },
            onConfirm = { name, price, stock, variants ->
                scope.launch {
                    app.database.productDao().insertProduct(
                        ProductEntity(
                            name = name,
                            price = price,
                            stock = stock,
                            variants = variants
                        )
                    )
                    showAddProductDialog = false
                }
            }
        )
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
fun AddProductDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, price: Double, stock: Int, variants: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var stock by remember { mutableStateOf("10") }
    var variants by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah Produk Baru") },
        text = {
            Column {
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
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val p = price.toDoubleOrNull() ?: 0.0
                    val s = stock.toIntOrNull() ?: 0
                    if (name.isNotBlank() && p > 0) {
                        onConfirm(name, p, s, variants)
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
