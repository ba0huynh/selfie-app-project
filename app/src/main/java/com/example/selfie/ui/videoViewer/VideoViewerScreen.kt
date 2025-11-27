package com.example.selfie.ui.videoViewer

import android.net.Uri
import android.widget.Toast
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.selfie.data.PhotoRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoViewerScreen(
    videoPath: String,
    onBack: () -> Unit,
    onVideoDeleted: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { PhotoRepository(context) }
    val scope = rememberCoroutineScope()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }
    var showPlayButton by remember { mutableStateOf(true) }
    
    val videoFile = remember(videoPath) { File(videoPath) }
    val fileExists = remember(videoPath) { videoFile.exists() }
    
    // Load existing note and emoji
    var noteText by remember(videoPath) { mutableStateOf(repository.getVideoNote(videoFile)) }
    var selectedEmoji by remember(videoPath) { mutableStateOf(repository.getVideoEmoji(videoFile)) }
    var showEmojiPicker by remember { mutableStateOf(false) }
    var saveJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    
    // Check if file exists, if not show error and go back
    LaunchedEffect(videoPath) {
        if (!fileExists) {
            Toast.makeText(context, "Không tìm thấy video", Toast.LENGTH_SHORT).show()
            onBack()
        }
    }
    
    if (!fileExists) {
        return
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Xem video") },
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
            // Video player
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                AndroidView(
                    factory = { ctx ->
                        VideoView(ctx).apply {
                            videoViewRef = this
                            setVideoURI(Uri.fromFile(videoFile))
                            setOnPreparedListener { mediaPlayer ->
                                mediaPlayer.isLooping = false
                            }
                            setOnCompletionListener {
                                showPlayButton = true
                            }
                            setOnClickListener {
                                if (!showPlayButton) {
                                    pause()
                                    showPlayButton = true
                                } else {
                                    start()
                                    showPlayButton = false
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    update = { videoView ->
                        // Update video view if needed
                    }
                )
                
                // Play button overlay
                if (showPlayButton) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable {
                                videoViewRef?.start()
                                showPlayButton = false
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                            modifier = Modifier.size(64.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
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
                                repository.saveVideoNote(videoFile, newText)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Thêm ghi chú cho video này...") },
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
                        repository.saveVideoEmoji(videoFile, emoji)
                        showEmojiPicker = false
                    },
                    onDismiss = { showEmojiPicker = false }
                )
            }
            
            if (showDeleteDialog) {
                DeleteConfirmationDialog(
                    onConfirm = {
                        repository.deleteVideo(videoFile)
                        onVideoDeleted()
                    },
                    onDismiss = { showDeleteDialog = false }
                )
            }
        }
    }
}

@Composable
fun DeleteConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Xóa video?") },
        text = { Text("Bạn có chắc chắn muốn xóa video này không?") },
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

