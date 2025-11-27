package com.example.selfie.ui.settings

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import com.example.selfie.data.PreferencesManager
import com.example.selfie.data.GoogleDriveSettings
import com.example.selfie.data.GoogleDriveManager
import com.example.selfie.work.ReminderScheduler
import java.util.*
import kotlinx.coroutines.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.LocalActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    var reminderEnabled by remember { mutableStateOf(prefs.getReminderSettings().isEnabled) }
    var reminderHour by remember { mutableStateOf(prefs.getReminderSettings().reminderHour) }
    var reminderMinute by remember { mutableStateOf(prefs.getReminderSettings().reminderMinute) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showPermissionRationale by remember { mutableStateOf(false) }
    var pendingEnableReminder by remember { mutableStateOf(false) }
    var showGoogleDriveDialog by remember { mutableStateOf(false) }
    
    // Google Drive settings
    var googleDriveSettings by remember { mutableStateOf(prefs.getGoogleDriveSettings()) }
    
    // For Android 13+ (API 33+), we need to request notification permission
    val notificationPermissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    } else {
        null
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cài đặt") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Reminder Section
            Text(
                text = "Nhắc nhở",
                style = MaterialTheme.typography.titleLarge
            )
            
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Notifications, "Reminder")
                        Column {
                            Text("Nhắc nhở chụp ảnh", style = MaterialTheme.typography.bodyLarge)
                            Text("Nhận thông báo hàng ngày", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    
                    Switch(
                        checked = reminderEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                // Check notification permission before enabling
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    if (notificationPermissionState?.status?.isGranted == true) {
                                        // Permission granted, enable reminder
                                        reminderEnabled = true
                                        pendingEnableReminder = false
                                        prefs.saveReminderSettings(
                                            prefs.getReminderSettings().copy(isEnabled = true)
                                        )
                                        ReminderScheduler.scheduleDailyReminder(context)
                                    } else if (notificationPermissionState?.status?.shouldShowRationale == true) {
                                        // Show rationale dialog
                                        pendingEnableReminder = true
                                        showPermissionRationale = true
                                    } else {
                                        // Request permission
                                        pendingEnableReminder = true
                                        notificationPermissionState?.launchPermissionRequest()
                                    }
                                } else {
                                    // Android < 13, no permission needed
                                    reminderEnabled = true
                                    pendingEnableReminder = false
                                    prefs.saveReminderSettings(
                                        prefs.getReminderSettings().copy(isEnabled = true)
                                    )
                                    ReminderScheduler.scheduleDailyReminder(context)
                                }
                            } else {
                                // Disable reminder
                                reminderEnabled = false
                                pendingEnableReminder = false
                                prefs.saveReminderSettings(
                                    prefs.getReminderSettings().copy(isEnabled = false)
                                )
                                ReminderScheduler.cancelReminder(context)
                            }
                        }
                    )
                }
            }
            
            if (reminderEnabled) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showTimePicker = true }
            ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Thời gian nhắc nhở")
                        Text(
                            text = String.format("%02d:%02d", reminderHour, reminderMinute),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Backup & Sync Section
            Text(
                text = "Sao lưu & Đồng bộ",
                style = MaterialTheme.typography.titleLarge
            )
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showGoogleDriveDialog = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (googleDriveSettings.isConnected) {
                            Icons.Default.Done
                        } else {
                            Icons.Default.Clear
                        },
                        contentDescription = null,
                        tint = if (googleDriveSettings.isConnected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Google Drive",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = if (googleDriveSettings.isConnected) {
                                googleDriveSettings.accountEmail.ifEmpty { "Đã kết nối" }
                            } else {
                                "Kết nối để sao lưu ảnh"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(Icons.Default.ArrowForward, null)
                }
            }
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Other Settings
            Text(
                text = "Khác",
                style = MaterialTheme.typography.titleLarge
            )
            
            SettingsItem(
                icon = Icons.Default.Lock,
                title = "Bảo mật",
                subtitle = "Đang phát triển",
                onClick = { }
            )
            
            SettingsItem(
                icon = Icons.Default.ArrowForward,
                title = "Tạo video Time-lapse",
                subtitle = "Đang phát triển",
                onClick = { }
            )
            
            if (showTimePicker) {
                TimePickerDialog(
                    initialHour = reminderHour,
                    initialMinute = reminderMinute,
                    onTimeSelected = { hour, minute ->
                        reminderHour = hour
                        reminderMinute = minute
                        showTimePicker = false
                        prefs.saveReminderSettings(
                            prefs.getReminderSettings().copy(
                                reminderHour = hour,
                                reminderMinute = minute
                            )
                        )
                        if (reminderEnabled) {
                            ReminderScheduler.scheduleDailyReminder(context)
                        }
                    },
                    onDismiss = { showTimePicker = false }
                )
            }
            
            // Permission rationale dialog
            if (showPermissionRationale) {
                AlertDialog(
                    onDismissRequest = { showPermissionRationale = false },
                    title = { Text("Quyền thông báo cần thiết") },
                    text = {
                        Text("Để gửi thông báo nhắc nhở chụp ảnh, ứng dụng cần quyền thông báo. Vui lòng cấp quyền trong cài đặt.")
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showPermissionRationale = false
                                notificationPermissionState?.launchPermissionRequest()
                            }
                        ) {
                            Text("Cấp quyền")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showPermissionRationale = false }) {
                            Text("Hủy")
                        }
                    }
                )
            }
            
            // Google Drive dialog
            if (showGoogleDriveDialog) {
                GoogleDriveDialog(
                    settings = googleDriveSettings,
                    onConnect = { email ->
                        googleDriveSettings = googleDriveSettings.copy(
                            isConnected = true,
                            accountEmail = email
                        )
                        prefs.saveGoogleDriveSettings(googleDriveSettings)
                        showGoogleDriveDialog = false
                    },
                    onDisconnect = {
                        val driveManager = GoogleDriveManager(context)
                        driveManager.clearCredentials()
                        googleDriveSettings = GoogleDriveSettings()
                        prefs.saveGoogleDriveSettings(googleDriveSettings)
                        showGoogleDriveDialog = false
                    },
                    onAutoBackupToggle = { enabled ->
                        googleDriveSettings = googleDriveSettings.copy(autoBackup = enabled)
                        prefs.saveGoogleDriveSettings(googleDriveSettings)
                    },
                    onDismiss = { showGoogleDriveDialog = false }
                )
            }
        }
    }
    
    // Handle permission result - enable reminder if permission was granted and user wanted to enable it
    LaunchedEffect(notificationPermissionState?.status) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionState?.status?.let { status ->
                if (status.isGranted && pendingEnableReminder && !reminderEnabled) {
                    // Permission was just granted, enable reminder
                    reminderEnabled = true
                    pendingEnableReminder = false
                    prefs.saveReminderSettings(
                        prefs.getReminderSettings().copy(isEnabled = true)
                    )
                    ReminderScheduler.scheduleDailyReminder(context)
                } else if (!status.isGranted && pendingEnableReminder) {
                    // Permission was denied, reset pending state
                    pendingEnableReminder = false
                }
            }
        }
    }
}

@Composable
fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null)
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(subtitle, style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.Default.ArrowForward, null)
        }
    }
}

@Composable
fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onTimeSelected: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedHour by remember { mutableStateOf(initialHour) }
    var selectedMinute by remember { mutableStateOf(initialMinute) }
    var isPickerMode by remember { mutableStateOf(true) } // true for hour, false for minute
    
    val hourOptions = (0..23).map { it.toString().padStart(2, '0') }
    val minuteOptions = (0..59).map { it.toString().padStart(2, '0') }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Chọn thời gian") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isPickerMode) {
                    Text("Chọn giờ", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        hourOptions.chunked(8).forEach { chunk ->
                            Column {
                                chunk.forEach { hour ->
                                    Button(
                                        onClick = {
                                            selectedHour = hour.toInt()
                                            isPickerMode = false
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (hour.toInt() == selectedHour)
                                                MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surface
                                        )
                                    ) {
                                        Text(hour)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Text("Chọn phút", style = MaterialTheme.typography.titleMedium)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        minuteOptions.forEach { minute ->
                            Button(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    selectedMinute = minute.toInt()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (minute.toInt() == selectedMinute)
                                        MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Text(minute)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (!isPickerMode) {
                TextButton(
                    onClick = {
                        onTimeSelected(selectedHour, selectedMinute)
                    }
                ) {
                    Text("OK")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}

@Composable
fun GoogleDriveDialog(
    settings: GoogleDriveSettings,
    onConnect: (String) -> Unit,
    onDisconnect: () -> Unit,
    onAutoBackupToggle: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val activity = LocalActivity.current
    val context = LocalContext.current
    var isConnecting by remember { mutableStateOf(false) }
    var connectionError by remember { mutableStateOf<String?>(null) }
    
    // Google Sign-In client
    val googleSignInClient = remember(activity) {
        if (activity != null) {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(
                    Scope(com.google.api.services.drive.DriveScopes.DRIVE_FILE)
                )
                .build()
            GoogleSignIn.getClient(activity, gso)
        } else {
            null
        }
    }
    
    // Check if already signed in
    LaunchedEffect(Unit) {
        if (activity != null) {
            val account = GoogleSignIn.getLastSignedInAccount(activity)
            if (account != null && !settings.isConnected) {
                // User is signed in but not connected in our app
                try {
                    val driveManager = GoogleDriveManager(context)
                    driveManager.initializeWithGoogleSignInAccount(account)
                    onConnect(account.email ?: account.displayName ?: "Unknown")
                } catch (e: Exception) {
                    // Ignore errors, user will need to sign in again
                }
            }
        }
    }
    
    // Handle Google Sign-In result
    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            // Initialize Google Drive Manager
            val driveManager = GoogleDriveManager(context)
            driveManager.initializeWithGoogleSignInAccount(account)
            
            // Update settings
            onConnect(account.email ?: account.displayName ?: "Unknown")
            isConnecting = false
        } catch (e: ApiException) {
            connectionError = when (e.statusCode) {
                12501 -> "Đăng nhập bị hủy"
                7 -> "Không thể kết nối. Vui lòng kiểm tra kết nối mạng"
                10 -> "Lỗi cấu hình. Vui lòng liên hệ nhà phát triển"
                12500 -> "Lỗi đăng nhập. Vui lòng thử lại"
                else -> {
                    // Check if it's a 403 error (access_denied)
                    val errorMessage = e.message ?: ""
                    if (errorMessage.contains("403") || errorMessage.contains("access_denied")) {
                        "Lỗi 403: Ứng dụng chưa được xác minh. Vui lòng thêm email của bạn vào danh sách Test Users trong Google Cloud Console. Xem hướng dẫn trong OAUTH_SETUP.md"
                    } else {
                        "Lỗi đăng nhập: ${e.message}"
                    }
                }
            }
            isConnecting = false
        } catch (e: Exception) {
            val errorMessage = e.message ?: ""
            connectionError = if (errorMessage.contains("403") || errorMessage.contains("access_denied")) {
                "Lỗi 403: Ứng dụng chưa được xác minh. Vui lòng thêm email của bạn vào danh sách Test Users trong Google Cloud Console."
            } else {
                "Lỗi: ${e.message}"
            }
            isConnecting = false
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (settings.isConnected) {
                        Icons.Default.Done
                    } else {
                        Icons.Default.Clear
                    },
                    contentDescription = null,
                    tint = if (settings.isConnected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Text("Google Drive")
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (settings.isConnected) {
                    // Connected state
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Đã kết nối",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = settings.accountEmail,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    
                    // Auto backup toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Sao lưu tự động",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "Tự động tải ảnh lên Google Drive",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.autoBackup,
                            onCheckedChange = onAutoBackupToggle
                        )
                    }
                    
                    if (settings.lastBackupTime > 0) {
                        val lastBackupDate = java.text.SimpleDateFormat(
                            "dd/MM/yyyy HH:mm",
                            java.util.Locale.getDefault()
                        ).format(java.util.Date(settings.lastBackupTime))
                        Text(
                            text = "Lần sao lưu cuối: $lastBackupDate",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Divider()
                    
                    Button(
                        onClick = onDisconnect,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Delete, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ngắt kết nối")
                    }
                } else {
                    // Not connected state
                    Text(
                        text = "Kết nối với Google Drive để tự động sao lưu ảnh của bạn.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    if (connectionError != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text(
                                text = connectionError ?: "",
                                modifier = Modifier.padding(12.dp),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                    
                    Button(
                        onClick = {
                            isConnecting = true
                            connectionError = null
                            googleSignInClient?.let { client ->
                                val signInIntent = client.signInIntent
                                signInLauncher.launch(signInIntent)
                            } ?: run {
                                connectionError = "Không thể khởi tạo Google Sign-In"
                                isConnecting = false
                            }
                        },
                        enabled = !isConnecting && googleSignInClient != null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isConnecting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Đang kết nối...")
                        } else {
                            Icon(Icons.Default.Add, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Kết nối với Google Drive")
                        }
                    }
                    
                    Text(
                        text = "Đăng nhập bằng tài khoản Google của bạn để sao lưu ảnh lên Google Drive.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Đóng")
            }
        }
    )
}

