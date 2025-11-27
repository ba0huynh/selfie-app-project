package com.example.selfie.ui.photoViewer

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.selfie.data.PhotoRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoViewerScreen(
    photoPath: String,
    onBack: () -> Unit,
    onPhotoDeleted: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { PhotoRepository(context) }
    val scope = rememberCoroutineScope()
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    val photoFile = remember(photoPath) { File(photoPath) }
    val fileExists = remember(photoPath) { photoFile.exists() }
    
    // Load existing note and emoji
    var noteText by remember(photoPath) { mutableStateOf(repository.getPhotoNote(photoFile)) }
    var selectedEmoji by remember(photoPath) { mutableStateOf(repository.getPhotoEmoji(photoFile)) }
    var showEmojiPicker by remember { mutableStateOf(false) }
    var saveJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    
    // Check if file exists, if not show error and go back
    LaunchedEffect(photoPath) {
        if (!fileExists) {
            Toast.makeText(context, "Không tìm thấy ảnh", Toast.LENGTH_SHORT).show()
            onBack()
        }
    }
    
    if (!fileExists) {
        return
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Xem ảnh") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showDeleteDialog = true }
                    ) {
                        Icon(Icons.Default.Delete, "Delete")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Image viewer
            Box(modifier = Modifier.weight(1f)) {
                ZoomableImage(
                    file = photoFile,
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            // Emoji and Note section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Emoji selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Cảm xúc:",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .clickable { showEmojiPicker = true }
                                .background(
                                    if (selectedEmoji.isNotEmpty()) 
                                        MaterialTheme.colorScheme.primaryContainer 
                                    else 
                                        MaterialTheme.colorScheme.surfaceVariant
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = selectedEmoji.ifEmpty { "😊" },
                                fontSize = 24.sp
                            )
                        }
                    }
                    
                    Divider()
                    
                    // Note input
                    Text(
                        text = "Ghi chú",
                        style = MaterialTheme.typography.titleSmall
                    )
                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { newText ->
                            noteText = newText
                            // Cancel previous save job
                            saveJob?.cancel()
                            // Save after 500ms delay (debounce)
                            saveJob = scope.launch {
                                delay(500)
                                repository.savePhotoNote(photoFile, newText)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Thêm ghi chú cho ảnh này...") },
                        maxLines = 3,
                        singleLine = false
                    )
                }
            }
            
            // Emoji picker dialog
            if (showEmojiPicker) {
                EmojiPickerDialog(
                    currentEmoji = selectedEmoji,
                    onEmojiSelected = { emoji ->
                        selectedEmoji = emoji
                        repository.savePhotoEmoji(photoFile, emoji)
                        showEmojiPicker = false
                    },
                    onDismiss = { showEmojiPicker = false }
                )
            }
            
            if (showDeleteDialog) {
                DeleteConfirmationDialog(
                    onConfirm = {
                        repository.deletePhoto(photoFile)
                        onPhotoDeleted()
                    },
                    onDismiss = { showDeleteDialog = false }
                )
            }
        }
    }
}

@Composable
fun ZoomableImage(
    file: File,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    
    val state = rememberTransformableState { zoomChange, panChange, rotationChange ->
        scale = (scale * zoomChange).coerceIn(1f, 3f)
        
        val extraWidth = (scale - 1f) / 2f
        val maxOffsetX = extraWidth
        val minOffsetX = -extraWidth
        
        val extraHeight = (scale - 1f) / 2f
        val maxOffsetY = extraHeight
        val minOffsetY = -extraHeight
        
        offsetX = (offsetX + panChange.x / scale).coerceIn(minOffsetX, maxOffsetX)
        offsetY = (offsetY + panChange.y / scale).coerceIn(minOffsetY, maxOffsetY)
    }
    
    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        scale = if (scale > 1f) 1f else 2f
                        offsetX = 0f
                        offsetY = 0f
                    }
                )
            }
            .transformable(state = state)
    ) {
        Image(
            painter = rememberAsyncImagePainter(file),
            contentDescription = "Photo",
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY
                ),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
fun DeleteConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Xóa ảnh?") },
        text = { Text("Bạn có chắc chắn muốn xóa ảnh này không?") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
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
fun EmojiPickerDialog(
    currentEmoji: String,
    onEmojiSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val emojis = listOf(
        "😊" to "Vui",
        "😢" to "Buồn",
        "😠" to "Giận",
        "😭" to "Khóc"
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Chọn cảm xúc") },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                emojis.forEach { (emoji, label) ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .padding(8.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .size(56.dp)
                                .clickable { onEmojiSelected(emoji) },
                            shape = CircleShape,
                            color = if (currentEmoji == emoji) 
                                MaterialTheme.colorScheme.primaryContainer 
                            else 
                                MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Text(
                                    text = emoji,
                                    fontSize = 32.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall
                        )
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

