package com.example.selfie.ui.photoGrid

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.delay
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import com.example.selfie.util.selectable
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.selfie.data.PhotoMetadata
import com.example.selfie.data.PhotoRepository
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoGridScreen(
    onNavigateToCamera: () -> Unit,
    onNavigateToPhotoViewer: (String) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { PhotoRepository(context) }
    var photos by remember { mutableStateOf<List<PhotoMetadata>>(emptyList()) }
    var isMultiSelectMode by remember { mutableStateOf(false) }
    var selectedPhotos by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDatePickerDialog by remember { mutableStateOf(false) }
    var photosToEditDate by remember { mutableStateOf<List<PhotoMetadata>>(emptyList()) }
    var showMemoryPhotos by remember { mutableStateOf<Pair<Int, List<PhotoMetadata>>?>(null) }
    
    // Load photos whenever this composable is created or recomposed
    LaunchedEffect(Unit) {
        photos = repository.getAllPhotos()
    }
    
    fun refreshPhotos() {
        photos = repository.getAllPhotos()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nhật ký Selfie", style = MaterialTheme.typography.titleLarge) },
                actions = {
                    if (isMultiSelectMode && selectedPhotos.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                // Show date picker for selected photos
                                val selectedPhotoList = selectedPhotos.mapNotNull { path ->
                                        photos.firstOrNull { it.file.absolutePath == path }
                                }
                                if (selectedPhotoList.isNotEmpty()) {
                                    photosToEditDate = selectedPhotoList
                                }
                            }
                        ) {
                            Icon(Icons.Default.Edit, "Edit Date")
                        }
                        IconButton(
                            onClick = {
                                // Show confirmation dialog
                                showDeleteDialog = true
                            }
                        ) {
                            Icon(Icons.Default.Delete, "Delete")
                        }
                    } else {
                        IconButton(
                            onClick = {
                                // Show photo selection dialog for date editing
                                showDatePickerDialog = true
                            }
                        ) {
                            Icon(Icons.Default.Edit, "Edit Date")
                        }
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Default.Settings, "Settings")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (!isMultiSelectMode) {
                FloatingActionButton(onClick = onNavigateToCamera) {
                    Icon(Icons.Default.Add, "Take Photo")
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when {
                photos.isEmpty() -> EmptyState(onTakePhoto = onNavigateToCamera)
                else -> {
                    val groupedPhotos = photos.groupBy { dateToString(it.dateTaken) }
                    val memories = remember(photos) { findMemories(photos) }
                    
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(8.dp)
                    ) {
                        // Memories section
                        if (memories.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Kỷ niệm",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                                )
                            }
                            memories.forEach { (year, memoryPhotos) ->
                                item {
                                    MemoryCard(
                                        year = year,
                                        photoCount = memoryPhotos.size,
                                        previewPhotos = memoryPhotos.take(3),
                                        onClick = {
                                            showMemoryPhotos = Pair(year, memoryPhotos)
                                        }
                                    )
                                }
                            }
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                                Divider()
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                        
                        groupedPhotos.forEach { (dateLabel, datePhotos) ->
                            item {
                                DateHeader(label = dateLabel)
                            }
                            items(datePhotos.chunked(3)) { rowPhotos ->
                                PhotoRow(
                                    photos = rowPhotos,
                                    isMultiSelectMode = isMultiSelectMode,
                                    selectedPhotos = selectedPhotos,
                                    onPhotoClick = { photo ->
                                        if (isMultiSelectMode) {
                                            val path = photo.file.absolutePath
                                            selectedPhotos = if (selectedPhotos.contains(path)) {
                                                selectedPhotos - path
                                            } else {
                                                selectedPhotos + path
                                            }
                                        } else {
                                            onNavigateToPhotoViewer(photo.file.absolutePath)
                                        }
                                    },
                                    onPhotoLongPress = {
                                        isMultiSelectMode = true
                                        selectedPhotos = setOf(it.file.absolutePath)
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
            
            // Delete confirmation dialog
            if (showDeleteDialog) {
                DeleteConfirmationDialog(
                    photoCount = selectedPhotos.size,
                    onConfirm = {
                        selectedPhotos.mapNotNull { path ->
                            photos.firstOrNull { it.file.absolutePath == path }
                        }.forEach { photo ->
                            repository.deletePhoto(photo.file)
                        }
                        isMultiSelectMode = false
                        selectedPhotos = emptySet()
                        refreshPhotos()
                        showDeleteDialog = false
                    },
                    onDismiss = {
                        showDeleteDialog = false
                    }
                )
            }
            
            // Date picker dialog
            if (showDatePickerDialog) {
                PhotoDatePickerDialog(
                    photos = photos,
                    onPhotoSelected = { photo ->
                        photosToEditDate = listOf(photo)
                        showDatePickerDialog = false
                    },
                    onDismiss = {
                        showDatePickerDialog = false
                    }
                )
            }
            
            // Date selection dialog
            if (photosToEditDate.isNotEmpty()) {
                DateSelectionDialog(
                    photos = photosToEditDate,
                    currentDate = photosToEditDate.first().dateTaken,
                    onDateSelected = { newDate ->
                        photosToEditDate.forEach { photo ->
                            repository.savePhotoDate(photo.file, newDate)
                        }
                        refreshPhotos()
                        photosToEditDate = emptyList()
                        if (isMultiSelectMode) {
                            isMultiSelectMode = false
                            selectedPhotos = emptySet()
                        }
                    },
                    onDismiss = {
                        photosToEditDate = emptyList()
                    }
                )
            }
            
            // Memory photos dialog
            showMemoryPhotos?.let { (year, memoryPhotos) ->
                MemoryPhotosDialog(
                    year = year,
                    photos = memoryPhotos,
                    onPhotoClick = { photo ->
                        showMemoryPhotos = null
                        onNavigateToPhotoViewer(photo.file.absolutePath)
                    },
                    onDismiss = {
                        showMemoryPhotos = null
                    }
                )
            }
        }
    }
}

@Composable
fun PhotoRow(
    photos: List<PhotoMetadata>,
    isMultiSelectMode: Boolean,
    selectedPhotos: Set<String>,
    onPhotoClick: (PhotoMetadata) -> Unit,
    onPhotoLongPress: (PhotoMetadata) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        photos.forEach { photo ->
            PhotoItem(
                photo = photo,
                isSelected = selectedPhotos.contains(photo.file.absolutePath),
                isMultiSelectMode = isMultiSelectMode,
                onPhotoClick = { onPhotoClick(photo) },
                onPhotoLongPress = { onPhotoLongPress(photo) },
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
            )
        }
        // Fill remaining space if less than 3 photos
        repeat(3 - photos.size) {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun PhotoItem(
    photo: PhotoMetadata,
    isSelected: Boolean,
    isMultiSelectMode: Boolean,
    onPhotoClick: () -> Unit,
    onPhotoLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .pointerInput(photo) {
                detectTapGestures(
                    onTap = { onPhotoClick() },
                    onLongPress = { onPhotoLongPress() }
                )
            }
    ) {
        Image(
            painter = rememberAsyncImagePainter(File(photo.file.absolutePath)),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        
        if (isMultiSelectMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .selectable(isSelected)
                    .padding(8.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                    modifier = Modifier.size(24.dp)
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            modifier = Modifier.fillMaxSize(),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
        
        if (!photo.emoji.isEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = photo.emoji,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun DateHeader(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

@Composable
fun EmptyState(onTakePhoto: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Chưa có ảnh nào",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onTakePhoto) {
            Text("Chụp ảnh đầu tiên")
        }
    }
}

fun dateToString(date: Date): String {
    val today = Calendar.getInstance()
    val target = Calendar.getInstance().apply { time = date }
    
    return when {
        today.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
        today.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR) -> "Hôm nay"
        else -> SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date)
    }
}

fun findMemories(photos: List<PhotoMetadata>): List<Pair<Int, List<PhotoMetadata>>> {
    val today = Calendar.getInstance()
    val todayMonth = today.get(Calendar.MONTH)
    val todayDay = today.get(Calendar.DAY_OF_MONTH)
    val currentYear = today.get(Calendar.YEAR)
    
    // Find photos taken on the same month/day in previous years
    val memoriesMap = mutableMapOf<Int, MutableList<PhotoMetadata>>()
    
    photos.forEach { photo ->
        val photoCalendar = Calendar.getInstance().apply { time = photo.dateTaken }
        val photoYear = photoCalendar.get(Calendar.YEAR)
        val photoMonth = photoCalendar.get(Calendar.MONTH)
        val photoDay = photoCalendar.get(Calendar.DAY_OF_MONTH)
        
        // Check if it's the same month and day, but different (earlier) year
        if (photoMonth == todayMonth && photoDay == todayDay && photoYear < currentYear) {
            val yearsAgo = currentYear - photoYear
            memoriesMap.getOrPut(yearsAgo) { mutableListOf() }.add(photo)
        }
    }
    
    // Sort by years ago (most recent first)
    return memoriesMap.toList().sortedByDescending { it.first }
}

@Composable
fun DeleteConfirmationDialog(
    photoCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Xóa ảnh")
        },
        text = {
            Text(
                if (photoCount == 1) {
                    "Bạn có chắc chắn muốn xóa ảnh này không?"
                } else {
                    "Bạn có chắc chắn muốn xóa $photoCount ảnh này không?"
                }
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Xóa")
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
fun PhotoDatePickerDialog(
    photos: List<PhotoMetadata>,
    onPhotoSelected: (PhotoMetadata) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Chọn ảnh để chỉnh sửa ngày") },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(photos) { photo ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPhotoSelected(photo) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter(File(photo.file.absolutePath)),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = photo.file.name,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(photo.dateTaken),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}

@Composable
fun DateSelectionDialog(
    photos: List<PhotoMetadata>,
    currentDate: Date,
    onDateSelected: (Date) -> Unit,
    onDismiss: () -> Unit
) {
    val calendar = remember { Calendar.getInstance().apply { time = currentDate } }
    var selectedYear by remember { mutableStateOf(calendar.get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableStateOf(calendar.get(Calendar.MONTH)) }
    var selectedDay by remember { mutableStateOf(calendar.get(Calendar.DAY_OF_MONTH)) }
    var selectedHour by remember { mutableStateOf(calendar.get(Calendar.HOUR_OF_DAY)) }
    var selectedMinute by remember { mutableStateOf(calendar.get(Calendar.MINUTE)) }
    
    var showDayPicker by remember { mutableStateOf(false) }
    var showMonthPicker by remember { mutableStateOf(false) }
    var showYearPicker by remember { mutableStateOf(false) }
    var showHourPicker by remember { mutableStateOf(false) }
    var showMinutePicker by remember { mutableStateOf(false) }
    
    val yearOptions = (2020..2030).toList()
    val monthOptions = (0..11).toList()
    val monthNames = listOf("T1", "T2", "T3", "T4", "T5", "T6", "T7", "T8", "T9", "T10", "T11", "T12")
    val dayOptions = remember(selectedYear, selectedMonth) {
        val daysInMonth = Calendar.getInstance().apply {
            set(Calendar.YEAR, selectedYear)
            set(Calendar.MONTH, selectedMonth)
        }.getActualMaximum(Calendar.DAY_OF_MONTH)
        (1..daysInMonth).toList()
    }
    val hourOptions = (0..23).toList()
    val minuteOptions = (0..59).toList()
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                if (photos.size == 1) {
                    "Chọn ngày chụp ảnh"
                } else {
                    "Chọn ngày chụp ảnh (${photos.size} ảnh)"
                }
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Date selection
                Text("Ngày", style = MaterialTheme.typography.titleSmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Day
                    Box(modifier = Modifier.weight(1f)) {
                        Column {
                            Text("Ngày", style = MaterialTheme.typography.bodySmall)
                            Button(
                                onClick = { showDayPicker = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("$selectedDay")
                            }
                        }
                        DropdownMenu(
                            expanded = showDayPicker,
                            onDismissRequest = { showDayPicker = false },
                            modifier = Modifier.heightIn(max = 300.dp)
                        ) {
                            dayOptions.forEach { day ->
                                DropdownMenuItem(
                                    text = { Text("$day") },
                                    onClick = {
                                        selectedDay = day
                                        showDayPicker = false
                                    }
                                )
                            }
                        }
                    }
                    
                    // Month
                    Box(modifier = Modifier.weight(1f)) {
                        Column {
                            Text("Tháng", style = MaterialTheme.typography.bodySmall)
                            Button(
                                onClick = { showMonthPicker = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(monthNames[selectedMonth])
                            }
                        }
                        DropdownMenu(
                            expanded = showMonthPicker,
                            onDismissRequest = { showMonthPicker = false },
                            modifier = Modifier.heightIn(max = 300.dp)
                        ) {
                            monthOptions.forEach { month ->
                                DropdownMenuItem(
                                    text = { Text(monthNames[month]) },
                                    onClick = {
                                        selectedMonth = month
                                        selectedDay = minOf(selectedDay, dayOptions.maxOrNull() ?: 1)
                                        showMonthPicker = false
                                    }
                                )
                            }
                        }
                    }
                    
                    // Year
                    Box(modifier = Modifier.weight(1f)) {
                        Column {
                            Text("Năm", style = MaterialTheme.typography.bodySmall)
                            Button(
                                onClick = { showYearPicker = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("$selectedYear")
                            }
                        }
                        DropdownMenu(
                            expanded = showYearPicker,
                            onDismissRequest = { showYearPicker = false },
                            modifier = Modifier.heightIn(max = 300.dp)
                        ) {
                            yearOptions.forEach { year ->
                                DropdownMenuItem(
                                    text = { Text("$year") },
                                    onClick = {
                                        selectedYear = year
                                        showYearPicker = false
                                    }
                                )
                            }
                        }
                    }
                }
                
                Divider()
                
                // Time selection
                Text("Giờ", style = MaterialTheme.typography.titleSmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Hour
                    Box(modifier = Modifier.weight(1f)) {
                        Column {
                            Text("Giờ", style = MaterialTheme.typography.bodySmall)
                            Button(
                                onClick = { showHourPicker = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(String.format("%02d", selectedHour))
                            }
                        }
                        DropdownMenu(
                            expanded = showHourPicker,
                            onDismissRequest = { showHourPicker = false },
                            modifier = Modifier.heightIn(max = 300.dp)
                        ) {
                            hourOptions.forEach { hour ->
                                DropdownMenuItem(
                                    text = { Text(String.format("%02d", hour)) },
                                    onClick = {
                                        selectedHour = hour
                                        showHourPicker = false
                                    }
                                )
                            }
                        }
                    }
                    
                    // Minute
                    Box(modifier = Modifier.weight(1f)) {
                        Column {
                            Text("Phút", style = MaterialTheme.typography.bodySmall)
                            Button(
                                onClick = { showMinutePicker = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(String.format("%02d", selectedMinute))
                            }
                        }
                        DropdownMenu(
                            expanded = showMinutePicker,
                            onDismissRequest = { showMinutePicker = false },
                            modifier = Modifier.heightIn(max = 300.dp)
                        ) {
                            minuteOptions.forEach { minute ->
                                DropdownMenuItem(
                                    text = { Text(String.format("%02d", minute)) },
                                    onClick = {
                                        selectedMinute = minute
                                        showMinutePicker = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val newDate = Calendar.getInstance().apply {
                        set(Calendar.YEAR, selectedYear)
                        set(Calendar.MONTH, selectedMonth)
                        set(Calendar.DAY_OF_MONTH, selectedDay)
                        set(Calendar.HOUR_OF_DAY, selectedHour)
                        set(Calendar.MINUTE, selectedMinute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.time
                    onDateSelected(newDate)
                }
            ) {
                Text("Xác nhận")
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
fun MemoryCard(
    year: Int,
    photoCount: Int,
    previewPhotos: List<PhotoMetadata>,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Preview photos
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                previewPhotos.forEach { photo ->
                    Image(
                        painter = rememberAsyncImagePainter(File(photo.file.absolutePath)),
                        contentDescription = null,
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
                if (photoCount > 3) {
                    Surface(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+${photoCount - 3}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            
            // Year and count info
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = if (year == 1) {
                        "Năm ngoái"
                    } else {
                        "$year năm trước"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$photoCount ảnh",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun MemoryPhotosDialog(
    year: Int,
    photos: List<PhotoMetadata>,
    onPhotoClick: (PhotoMetadata) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (year == 1) {
                    "Năm ngoái - ${SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date())}"
                } else {
                    "$year năm trước - ${SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date())}"
                }
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 500.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(photos.chunked(3)) { rowPhotos ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowPhotos.forEach { photo ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onPhotoClick(photo) }
                            ) {
                                Image(
                                    painter = rememberAsyncImagePainter(File(photo.file.absolutePath)),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                        // Fill remaining space
                        repeat(3 - rowPhotos.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
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
