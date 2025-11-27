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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import com.example.selfie.util.selectable
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.selfie.data.PhotoMetadata
import com.example.selfie.data.VideoMetadata
import com.example.selfie.data.PhotoRepository
import com.example.selfie.data.GoogleDriveManager
import com.example.selfie.data.PreferencesManager
import com.example.selfie.util.VideoCreator
import kotlinx.coroutines.launch
import android.os.Environment
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.Date

// Custom Cloud Download Icon
val CloudDownloadIcon: ImageVector
    get() {
        if (_cloudDownloadIcon != null) {
            return _cloudDownloadIcon!!
        }
        _cloudDownloadIcon = ImageVector.Builder(
            name = "CloudDownload",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f
        ).apply {
            path(
                fill = null,
                fillAlpha = 1.0f,
                stroke = SolidColor(Color.Black),
                strokeAlpha = 1.0f,
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                strokeLineMiter = 4.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(12.0f, 13.0f)
                verticalLineToRelative(8.0f)
                lineToRelative(-4.0f, -4.0f)
            }
            path(
                fill = null,
                fillAlpha = 1.0f,
                stroke = SolidColor(Color.Black),
                strokeAlpha = 1.0f,
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                strokeLineMiter = 4.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveToRelative(12.0f, 21.0f)
                lineToRelative(4.0f, -4.0f)
            }
            path(
                fill = null,
                fillAlpha = 1.0f,
                stroke = SolidColor(Color.Black),
                strokeAlpha = 1.0f,
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                strokeLineMiter = 4.0f,
                pathFillType = PathFillType.NonZero
            ) {
                // Cloud shape - using lines and curves
                // Start from left side of cloud
                moveTo(4.0f, 15.0f)
                // Left arc
                arcTo(7.0f, 7.0f, 0f, true, true, 16.0f, 8.0f)
                // Right side
                horizontalLineToRelative(2.0f)
                // Right arc
                arcTo(4.5f, 4.5f, 0f, false, true, 20.0f, 16.5f)
                // Close the path
                lineTo(4.0f, 15.0f)
            }
        }.build()
        return _cloudDownloadIcon!!
    }
private var _cloudDownloadIcon: ImageVector? = null

// Sealed class to represent both photos and videos
sealed class MediaItem {
    data class Photo(val metadata: PhotoMetadata) : MediaItem()
    data class Video(val metadata: VideoMetadata) : MediaItem()
    
    val date: Date
        get() = when (this) {
            is Photo -> metadata.dateTaken
            is Video -> metadata.dateCreated
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoGridScreen(
    onNavigateToCamera: () -> Unit,
    onNavigateToPhotoViewer: (String) -> Unit,
    onNavigateToVideoViewer: (String) -> Unit = {},
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { PhotoRepository(context) }
    val prefs = remember { PreferencesManager(context) }
    val scope = rememberCoroutineScope()
    var photos by remember { mutableStateOf<List<PhotoMetadata>>(emptyList()) }
    var videos by remember { mutableStateOf<List<VideoMetadata>>(emptyList()) }
    var isMultiSelectMode by remember { mutableStateOf(false) }
    var selectedPhotos by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDatePickerDialog by remember { mutableStateOf(false) }
    var photosToEditDate by remember { mutableStateOf<List<PhotoMetadata>>(emptyList()) }
    var showMemoryPhotos by remember { mutableStateOf<Pair<Int, List<PhotoMetadata>>?>(null) }
    
    // Google Drive upload state
    var isUploading by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var uploadError by remember { mutableStateOf<String?>(null) }
    var uploadSuccess by remember { mutableStateOf(false) }
    
    // Video creation state
    var showVideoDialog by remember { mutableStateOf(false) }
    var isCreatingVideo by remember { mutableStateOf(false) }
    var videoProgress by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var videoError by remember { mutableStateOf<String?>(null) }
    var videoSuccess by remember { mutableStateOf(false) }
    var createdVideoFile by remember { mutableStateOf<File?>(null) }
    
    // Check Google Drive connection - refresh when screen is recomposed
    var googleDriveSettings by remember { mutableStateOf(prefs.getGoogleDriveSettings()) }
    val isGoogleDriveConnected = googleDriveSettings.isConnected
    
    // Refresh Google Drive settings
    LaunchedEffect(Unit) {
        googleDriveSettings = prefs.getGoogleDriveSettings()
    }
    
    // Load photos and videos whenever this composable is created or recomposed
    LaunchedEffect(Unit) {
        photos = repository.getAllPhotos()
        videos = repository.getAllVideos()
    }
    
    fun refreshPhotos() {
        photos = repository.getAllPhotos()
        videos = repository.getAllVideos()
    }
    
    fun createVideo(photoList: List<PhotoMetadata>, secondsPerImage: Float) {
        scope.launch {
            isCreatingVideo = true
            videoError = null
            videoSuccess = false
            videoProgress = Pair(0, photoList.size)
            createdVideoFile = null
            
            try {
                // Create output file
                val videoDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
                    ?: File(context.filesDir, "videos").also { it.mkdirs() }
                
                if (!videoDir.exists()) {
                    videoDir.mkdirs()
                }
                
                if (!videoDir.canWrite()) {
                    throw Exception("Không có quyền ghi vào thư mục video")
                }
                
                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val videoFile = File(videoDir, "timelapse_$timeStamp.mp4")
                
                android.util.Log.d("VideoCreation", "Creating video at: ${videoFile.absolutePath}")
                
                // Create video
                val videoCreator = VideoCreator()
                val photoFiles = photoList.map { it.file }
                val result = videoCreator.createVideoFromImages(
                    imageFiles = photoFiles,
                    outputFile = videoFile,
                    secondsPerImage = secondsPerImage,
                    onProgress = { current, total ->
                        videoProgress = Pair(current, total)
                    }
                )
                
                if (result.isSuccess) {
                    // Save video to repository (app storage)
                    val savedVideoFile = repository.saveVideo(videoFile)
                    // Set creation date based on first photo's date
                    val firstPhotoDate = photoList.firstOrNull()?.dateTaken ?: Date()
                    repository.saveVideoDate(savedVideoFile, firstPhotoDate)
                    
                    createdVideoFile = savedVideoFile
                    videoSuccess = true
                    videoError = null
                    // Refresh videos list
                    refreshPhotos()
                } else {
                    val exception = result.exceptionOrNull()
                    val errorMsg = exception?.message ?: "Lỗi không xác định khi tạo video"
                    android.util.Log.e("VideoCreation", "Video creation failed", exception)
                    videoError = errorMsg
                }
            } catch (e: Exception) {
                android.util.Log.e("VideoCreation", "Exception during video creation", e)
                videoError = e.message ?: "Lỗi khi tạo video: ${e.javaClass.simpleName}"
            } finally {
                isCreatingVideo = false
            }
        }
    }
    
    fun uploadPhotosToDrive(photoList: List<PhotoMetadata>) {
        if (!isGoogleDriveConnected) {
            uploadError = "Chưa kết nối với Google Drive. Vui lòng kết nối trong Cài đặt."
            return
        }
        
        scope.launch {
            isUploading = true
            uploadError = null
            uploadSuccess = false
            uploadProgress = Pair(0, photoList.size)
            
            try {
                // Initialize Google Drive Manager
                val driveManager = GoogleDriveManager(context)
                val accountEmail = googleDriveSettings.accountEmail
                if (accountEmail.isNotEmpty()) {
                    driveManager.initializeCredential(accountEmail)
                } else {
                    throw IllegalStateException("Account email not found")
                }
                
                // Upload photos to "selfie" folder
                val photoFiles = photoList.map { it.file }
                val result = driveManager.uploadPhotosToFolder(
                    photoFiles = photoFiles,
                    folderName = "selfie",
                    onProgress = { current, total ->
                        uploadProgress = Pair(current, total)
                    }
                )
                
                if (result.isSuccess) {
                    val successCount = result.getOrNull() ?: 0
                    // Mark photos as uploaded
                    photoList.forEach { photo ->
                        repository.markPhotoAsUploaded(photo.file)
                    }
                    uploadSuccess = true
                    uploadError = null
                    // Refresh photos to update UI
                    refreshPhotos()
                    // Clear selection after successful upload
                    isMultiSelectMode = false
                    selectedPhotos = emptySet()
                } else {
                    val exception = result.exceptionOrNull()
                    uploadError = exception?.message ?: "Lỗi không xác định khi tải lên"
                }
            } catch (e: Exception) {
                uploadError = e.message ?: "Lỗi khi tải lên ảnh"
            } finally {
                isUploading = false
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isMultiSelectMode) {
                        Text(
                            text = if (selectedPhotos.isNotEmpty()) {
                                "${selectedPhotos.size} ảnh đã chọn"
                            } else {
                                "Chọn ảnh"
                            },
                            style = MaterialTheme.typography.titleLarge
                        )
                    } else {
                        Text("Nhật ký Selfie", style = MaterialTheme.typography.titleLarge)
                    }
                },
                actions = {
                    if (isMultiSelectMode) {
                        if (selectedPhotos.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    // Show video creation dialog
                                    showVideoDialog = true
                                },
                                enabled = !isCreatingVideo
                            ) {
                                Icon(Icons.Default.PlayArrow, "Create Video")
                            }
                            if (isGoogleDriveConnected) {
                                IconButton(
                                    onClick = {
                                        // Upload selected photos to Google Drive
                                        val selectedPhotoList = selectedPhotos.mapNotNull { path ->
                                            photos.firstOrNull { it.file.absolutePath == path }
                                        }
                                        if (selectedPhotoList.isNotEmpty()) {
                                            uploadPhotosToDrive(selectedPhotoList)
                                        }
                                    },
                                    enabled = !isUploading
                                ) {
                                    Icon(Icons.Default.KeyboardArrowUp, "Upload to Google Drive")
                                }
                            }
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
                        }
                        // Close button to exit multi-select mode
                        IconButton(
                            onClick = {
                                isMultiSelectMode = false
                                selectedPhotos = emptySet()
                            }
                        ) {
                            Icon(Icons.Default.Close, "Cancel selection")
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
                photos.isEmpty() && videos.isEmpty() -> EmptyState(onTakePhoto = onNavigateToCamera)
                else -> {
                    // Combine photos and videos into a unified list
                    val allMedia = remember(photos, videos) {
                        (photos.map { MediaItem.Photo(it) } + videos.map { MediaItem.Video(it) })
                            .sortedByDescending { it.date }
                    }
                    val groupedMedia = allMedia.groupBy { 
                        when (it) {
                            is MediaItem.Photo -> dateToString(it.metadata.dateTaken)
                            is MediaItem.Video -> dateToString(it.metadata.dateCreated)
                        }
                    }
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
                        
                        groupedMedia.forEach { (dateLabel, dateItems) ->
                            item {
                                DateHeader(label = dateLabel)
                            }
                            items(dateItems.chunked(3)) { rowItems ->
                                MediaRow(
                                    items = rowItems,
                                    isMultiSelectMode = isMultiSelectMode,
                                    selectedPhotos = selectedPhotos,
                                    repository = repository,
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
                                    onVideoClick = { video ->
                                        if (!isMultiSelectMode) {
                                            onNavigateToVideoViewer(video.file.absolutePath)
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
            
            // Upload progress dialog
            if (isUploading || uploadProgress != null || uploadError != null || uploadSuccess) {
                UploadProgressDialog(
                    isUploading = isUploading,
                    progress = uploadProgress,
                    error = uploadError,
                    success = uploadSuccess,
                    onDismiss = {
                        uploadProgress = null
                        uploadError = null
                        uploadSuccess = false
                    }
                )
            }
            
            // Video creation dialog
            if (showVideoDialog) {
                val selectedPhotoList = selectedPhotos.mapNotNull { path ->
                    photos.firstOrNull { it.file.absolutePath == path }
                }
                VideoCreationDialog(
                    photoCount = selectedPhotoList.size,
                    onConfirm = { secondsPerImage ->
                        showVideoDialog = false
                        createVideo(selectedPhotoList, secondsPerImage)
                    },
                    onDismiss = {
                        showVideoDialog = false
                    }
                )
            }
            
            // Video creation progress dialog
            if (isCreatingVideo || videoProgress != null || videoError != null || videoSuccess) {
                VideoProgressDialog(
                    isCreating = isCreatingVideo,
                    progress = videoProgress,
                    error = videoError,
                    success = videoSuccess,
                    videoFile = createdVideoFile,
                    onDismiss = {
                        videoProgress = null
                        videoError = null
                        videoSuccess = false
                        createdVideoFile = null
                    }
                )
            }
        }
    }
}

@Composable
fun MediaRow(
    items: List<MediaItem>,
    isMultiSelectMode: Boolean,
    selectedPhotos: Set<String>,
    repository: PhotoRepository,
    onPhotoClick: (PhotoMetadata) -> Unit,
    onVideoClick: (VideoMetadata) -> Unit,
    onPhotoLongPress: (PhotoMetadata) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { item ->
            when (item) {
                is MediaItem.Photo -> {
                    PhotoItem(
                        photo = item.metadata,
                        isSelected = selectedPhotos.contains(item.metadata.file.absolutePath),
                        isMultiSelectMode = isMultiSelectMode,
                        isUploaded = repository.isPhotoUploaded(item.metadata.file),
                        onPhotoClick = { onPhotoClick(item.metadata) },
                        onPhotoLongPress = { onPhotoLongPress(item.metadata) },
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                    )
                }
                is MediaItem.Video -> {
                    VideoItem(
                        video = item.metadata,
                        repository = repository,
                        onVideoClick = { onVideoClick(item.metadata) },
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                    )
                }
            }
        }
        // Fill remaining space if less than 3 items
        repeat(3 - items.size) {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun PhotoRow(
    photos: List<PhotoMetadata>,
    isMultiSelectMode: Boolean,
    selectedPhotos: Set<String>,
    repository: PhotoRepository,
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
                isUploaded = repository.isPhotoUploaded(photo.file),
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
    isUploaded: Boolean,
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
        
        // Cloud icon indicator in bottom right
        if (isUploaded) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = CloudDownloadIcon,
                        contentDescription = "Uploaded to Google Drive",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
        
        // Emoji in bottom left
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
fun VideoItem(
    video: VideoMetadata,
    repository: PhotoRepository,
    onVideoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onVideoClick)
    ) {
        // Try to show a thumbnail from the video file
        // For now, show a placeholder with video icon
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Video",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Video",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        // Play icon overlay
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(8.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
        
        // Emoji in bottom left
        if (!video.emoji.isEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = video.emoji,
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

@Composable
fun UploadProgressDialog(
    isUploading: Boolean,
    progress: Pair<Int, Int>?,
    error: String?,
    success: Boolean,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {
            if (!isUploading) {
                onDismiss()
            }
        },
        title = {
            Text(
                text = when {
                    success -> "Tải lên thành công"
                    error != null -> "Lỗi tải lên"
                    else -> "Đang tải lên..."
                }
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when {
                    success -> {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        progress?.let { (current, total) ->
                            Text("Đã tải lên $current/$total ảnh lên Google Drive")
                        } ?: Text("Đã tải lên ảnh lên Google Drive")
                    }
                    error != null -> {
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    isUploading -> {
                        CircularProgressIndicator()
                        progress?.let { (current, total) ->
                            Text("Đang tải lên $current/$total ảnh...")
                            LinearProgressIndicator(
                                progress = { current.toFloat() / total.toFloat() },
                                modifier = Modifier.fillMaxWidth()
                            )
                        } ?: Text("Đang tải lên...")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isUploading
            ) {
                Text("Đóng")
            }
        }
    )
}

@Composable
fun VideoCreationDialog(
    photoCount: Int,
    onConfirm: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    var secondsPerImage by remember { mutableStateOf("1.0") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val totalDuration = remember(secondsPerImage) {
        try {
            val seconds = secondsPerImage.toFloatOrNull() ?: 0f
            val total = seconds * photoCount
            String.format("%.1f", total)
        } catch (e: Exception) {
            "0.0"
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Tạo video Time-lapse")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Số ảnh đã chọn: $photoCount",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                OutlinedTextField(
                    value = secondsPerImage,
                    onValueChange = { newValue ->
                        errorMessage = null
                        secondsPerImage = newValue
                    },
                    label = { Text("Giây mỗi ảnh") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = errorMessage != null
                )
                
                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Thông tin video",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Thời lượng video: $totalDuration giây",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Số khung hình: ${photoCount * 30}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val seconds = secondsPerImage.toFloatOrNull()
                    if (seconds == null || seconds <= 0) {
                        errorMessage = "Vui lòng nhập số giây hợp lệ (lớn hơn 0)"
                    } else if (seconds > 10) {
                        errorMessage = "Số giây mỗi ảnh không được vượt quá 10"
                    } else {
                        onConfirm(seconds)
                    }
                }
            ) {
                Text("Tạo video")
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
fun VideoProgressDialog(
    isCreating: Boolean,
    progress: Pair<Int, Int>?,
    error: String?,
    success: Boolean,
    videoFile: File?,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {
            if (!isCreating) {
                onDismiss()
            }
        },
        title = {
            Text(
                text = when {
                    success -> "Tạo video thành công"
                    error != null -> "Lỗi tạo video"
                    else -> "Đang tạo video..."
                }
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when {
                    success -> {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        progress?.let { (current, total) ->
                            Text("Đã xử lý $current/$total ảnh")
                        } ?: Text("Video đã được tạo thành công")
                        if (videoFile != null) {
                            Text(
                                text = "Video: ${videoFile.name}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    error != null -> {
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    isCreating -> {
                        CircularProgressIndicator()
                        progress?.let { (current, total) ->
                            Text("Đang xử lý ảnh $current/$total...")
                            LinearProgressIndicator(
                                progress = { current.toFloat() / total.toFloat() },
                                modifier = Modifier.fillMaxWidth()
                            )
                        } ?: Text("Đang tạo video...")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isCreating
            ) {
                Text("Đóng")
            }
        }
    )
}
