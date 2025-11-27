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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import com.example.selfie.data.PreferencesManager
import com.example.selfie.work.ReminderScheduler
import java.util.*

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
            
            // Other Settings
            Text(
                text = "Khác",
                style = MaterialTheme.typography.titleLarge
            )
            
            SettingsItem(
                icon = Icons.Default.AccountBox,
                title = "Sao lưu & Đồng bộ",
                subtitle = "Đang phát triển",
                onClick = { }
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

