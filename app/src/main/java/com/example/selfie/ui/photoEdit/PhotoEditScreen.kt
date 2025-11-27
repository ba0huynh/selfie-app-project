package com.example.selfie.ui.photoEdit

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class PhotoFilter {
    NONE, GRAYSCALE, SEPIA, VINTAGE, BRIGHT
}

enum class CropAspectRatio(val label: String, val ratio: Float?) {
    FREE("Tự do", null),
    SQUARE("1:1", 1f),
    FOUR_THREE("4:3", 4f / 3f),
    SIXTEEN_NINE("16:9", 16f / 9f)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoEditScreen(
    photoBytes: ByteArray,
    onSave: (ByteArray) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Load original bitmap
    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var editedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var currentFilter by remember { mutableStateOf(PhotoFilter.NONE) }
    var rotationDegrees by remember { mutableStateOf(0f) }
    var cropAspectRatio by remember { mutableStateOf<CropAspectRatio?>(null) }
    
    // Manual crop state
    var isManualCropMode by remember { mutableStateOf(false) }
    var cropStart by remember { mutableStateOf<Offset?>(null) }
    var cropEnd by remember { mutableStateOf<Offset?>(null) }
    var imageBoxSize by remember { mutableStateOf<Size?>(null) }
    var imageBoxOffset by remember { mutableStateOf<Offset?>(null) }
    var manualCropRect by remember { mutableStateOf<android.graphics.Rect?>(null) }
    
    // Load bitmap on first composition
    LaunchedEffect(photoBytes) {
        withContext(Dispatchers.IO) {
            originalBitmap = BitmapFactory.decodeByteArray(photoBytes, 0, photoBytes.size)
            editedBitmap = originalBitmap
        }
    }
    
    // Apply filter, rotation, and crop when they change
    LaunchedEffect(currentFilter, rotationDegrees, cropAspectRatio, manualCropRect, originalBitmap) {
        originalBitmap?.let { bitmap ->
            withContext(Dispatchers.Default) {
                var processed = bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true)
                
                // Apply filter first
                processed = applyFilter(processed, currentFilter)
                
                // Apply rotation
                if (rotationDegrees != 0f) {
                    processed = rotateBitmap(processed, rotationDegrees)
                }
                
                // Apply manual crop if active
                manualCropRect?.let { rect ->
                    processed = cropBitmapRect(processed, rect)
                } ?: run {
                    // Apply aspect ratio crop if no manual crop
                    cropAspectRatio?.let { aspectRatio ->
                        if (aspectRatio != CropAspectRatio.FREE) {
                            processed = cropBitmap(processed, aspectRatio.ratio!!)
                        }
                    }
                }
                
                editedBitmap = processed
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chỉnh sửa ảnh") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, "Cancel")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            editedBitmap?.let { bitmap ->
                                scope.launch(Dispatchers.IO) {
                                    val outputStream = java.io.ByteArrayOutputStream()
                                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                                    val finalBytes = outputStream.toByteArray()
                                    withContext(Dispatchers.Main) {
                                        onSave(finalBytes)
                                    }
                                }
                            }
                        }
                    ) {
                        Icon(Icons.Default.Check, "Save")
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
            // Image preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.Black)
                    .onGloballyPositioned { coordinates ->
                        imageBoxSize = Size(coordinates.size.width.toFloat(), coordinates.size.height.toFloat())
                        imageBoxOffset = coordinates.positionInRoot()
                    },
                contentAlignment = Alignment.Center
            ) {
                editedBitmap?.let { bitmap ->
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                    
                    // Manual crop overlay
                    if (isManualCropMode) {
                        ManualCropOverlay(
                            cropStart = cropStart,
                            cropEnd = cropEnd,
                            imageBoxSize = imageBoxSize,
                            bitmapSize = editedBitmap?.let { 
                                androidx.compose.ui.geometry.Size(it.width.toFloat(), it.height.toFloat())
                            },
                            onCropStart = { offset ->
                                cropStart = offset
                                cropEnd = offset
                            },
                            onCropDrag = { offset ->
                                cropEnd = offset
                            },
                            onCropEnd = { start, end ->
                                // Calculate crop rectangle in bitmap coordinates
                                // Use the processed bitmap (after filter and rotation) for coordinates
                                imageBoxSize?.let { boxSize ->
                                    editedBitmap?.let { bmp ->
                                        val imageSize = androidx.compose.ui.geometry.Size(
                                            bmp.width.toFloat(),
                                            bmp.height.toFloat()
                                        )
                                        
                                        // Calculate scale and offset for image within box
                                        val scale = min(
                                            boxSize.width / imageSize.width,
                                            boxSize.height / imageSize.height
                                        )
                                        val scaledImageWidth = imageSize.width * scale
                                        val scaledImageHeight = imageSize.height * scale
                                        val imageOffsetX = (boxSize.width - scaledImageWidth) / 2
                                        val imageOffsetY = (boxSize.height - scaledImageHeight) / 2
                                        
                                        // Convert screen coordinates to image coordinates
                                        val startX = ((start.x - imageOffsetX) / scale).coerceIn(0f, imageSize.width)
                                        val startY = ((start.y - imageOffsetY) / scale).coerceIn(0f, imageSize.height)
                                        val endX = ((end.x - imageOffsetX) / scale).coerceIn(0f, imageSize.width)
                                        val endY = ((end.y - imageOffsetY) / scale).coerceIn(0f, imageSize.height)
                                        
                                        val left = min(startX, endX).toInt()
                                        val top = min(startY, endY).toInt()
                                        val right = max(startX, endX).toInt()
                                        val bottom = max(startY, endY).toInt()
                                        
                                        // Check if crop area is valid (at least 10x10 pixels)
                                        if (right > left + 10 && bottom > top + 10) {
                                            // Apply crop immediately
                                            manualCropRect = android.graphics.Rect(left, top, right, bottom)
                                            cropAspectRatio = null // Clear aspect ratio crop
                                            isManualCropMode = false
                                            cropStart = null
                                            cropEnd = null
                                        } else {
                                            // If crop is too small, clear it
                                            cropStart = null
                                            cropEnd = null
                                        }
                                    }
                                }
                            }
                        )
                    }
                    
                }
            }
            
            // Edit controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Filter section
                Text(
                    text = "Bộ lọc",
                    style = MaterialTheme.typography.titleMedium
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterButton(
                        label = "Gốc",
                        isSelected = currentFilter == PhotoFilter.NONE,
                        onClick = { currentFilter = PhotoFilter.NONE },
                        modifier = Modifier.weight(1f)
                    )
                    FilterButton(
                        label = "Đen trắng",
                        isSelected = currentFilter == PhotoFilter.GRAYSCALE,
                        onClick = { currentFilter = PhotoFilter.GRAYSCALE },
                        modifier = Modifier.weight(1f)
                    )
                    FilterButton(
                        label = "Sepia",
                        isSelected = currentFilter == PhotoFilter.SEPIA,
                        onClick = { currentFilter = PhotoFilter.SEPIA },
                        modifier = Modifier.weight(1f)
                    )
                    FilterButton(
                        label = "Cổ điển",
                        isSelected = currentFilter == PhotoFilter.VINTAGE,
                        onClick = { currentFilter = PhotoFilter.VINTAGE },
                        modifier = Modifier.weight(1f)
                    )
                    FilterButton(
                        label = "Sáng",
                        isSelected = currentFilter == PhotoFilter.BRIGHT,
                        onClick = { currentFilter = PhotoFilter.BRIGHT },
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Divider()
                
                // Crop section
                Text(
                    text = "Cắt ảnh",
                    style = MaterialTheme.typography.titleMedium
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CropButton(
                        label = "Gốc",
                        isSelected = cropAspectRatio == null && !isManualCropMode && manualCropRect == null,
                        onClick = { 
                            cropAspectRatio = null
                            manualCropRect = null
                            isManualCropMode = false
                            cropStart = null
                            cropEnd = null
                        },
                        modifier = Modifier.weight(1f)
                    )
                    CropButton(
                        label = "1:1",
                        isSelected = cropAspectRatio == CropAspectRatio.SQUARE,
                        onClick = { 
                            cropAspectRatio = CropAspectRatio.SQUARE
                            manualCropRect = null
                            isManualCropMode = false
                            cropStart = null
                            cropEnd = null
                        },
                        modifier = Modifier.weight(1f)
                    )
                    CropButton(
                        label = "4:3",
                        isSelected = cropAspectRatio == CropAspectRatio.FOUR_THREE,
                        onClick = { 
                            cropAspectRatio = CropAspectRatio.FOUR_THREE
                            manualCropRect = null
                            isManualCropMode = false
                            cropStart = null
                            cropEnd = null
                        },
                        modifier = Modifier.weight(1f)
                    )
                    CropButton(
                        label = "16:9",
                        isSelected = cropAspectRatio == CropAspectRatio.SIXTEEN_NINE,
                        onClick = { 
                            cropAspectRatio = CropAspectRatio.SIXTEEN_NINE
                            manualCropRect = null
                            isManualCropMode = false
                            cropStart = null
                            cropEnd = null
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                
                // Manual crop button
                Button(
                    onClick = {
                        isManualCropMode = true
                        cropAspectRatio = null
                        manualCropRect = null
                        cropStart = null
                        cropEnd = null
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cắt thủ công")
                }
                
                Divider()
                
                // Rotate section
                Text(
                    text = "Xoay",
                    style = MaterialTheme.typography.titleMedium
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            rotationDegrees = (rotationDegrees + 90f) % 360f
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Refresh, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Xoay 90°")
                    }
                    Text(
                        text = "${rotationDegrees.toInt()}°",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun FilterButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(vertical = 12.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun CropButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(vertical = 12.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

fun applyFilter(bitmap: Bitmap, filter: PhotoFilter): Bitmap {
    val result = bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(result)
    val paint = Paint()
    
    val colorMatrix = ColorMatrix().apply {
        when (filter) {
            PhotoFilter.NONE -> {
                // Identity matrix - no change
            }
            PhotoFilter.GRAYSCALE -> {
                setSaturation(0f)
            }
            PhotoFilter.SEPIA -> {
                val sepiaMatrix = floatArrayOf(
                    0.393f, 0.769f, 0.189f, 0f, 0f,
                    0.349f, 0.686f, 0.168f, 0f, 0f,
                    0.272f, 0.534f, 0.131f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
                set(sepiaMatrix)
            }
            PhotoFilter.VINTAGE -> {
                setSaturation(0.5f)
                setScale(1.1f, 1.0f, 0.9f, 1f)
            }
            PhotoFilter.BRIGHT -> {
                setScale(1.2f, 1.2f, 1.2f, 1f)
            }
        }
    }
    
    paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
    canvas.drawBitmap(bitmap, 0f, 0f, paint)
    
    return result
}

fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
    val matrix = Matrix().apply {
        postRotate(degrees)
    }
    return Bitmap.createBitmap(
        bitmap,
        0,
        0,
        bitmap.width,
        bitmap.height,
        matrix,
        true
    )
}

fun cropBitmap(bitmap: Bitmap, aspectRatio: Float): Bitmap {
    val width = bitmap.width
    val height = bitmap.height
    val bitmapAspectRatio = width.toFloat() / height.toFloat()
    
    val cropWidth: Int
    val cropHeight: Int
    val x: Int
    val y: Int
    
    if (bitmapAspectRatio > aspectRatio) {
        // Bitmap is wider than desired aspect ratio, crop width
        cropHeight = height
        cropWidth = (height * aspectRatio).toInt()
        x = (width - cropWidth) / 2
        y = 0
    } else {
        // Bitmap is taller than desired aspect ratio, crop height
        cropWidth = width
        cropHeight = (width / aspectRatio).toInt()
        x = 0
        y = (height - cropHeight) / 2
    }
    
    return Bitmap.createBitmap(bitmap, x, y, cropWidth, cropHeight)
}

fun cropBitmapRect(bitmap: Bitmap, rect: android.graphics.Rect): Bitmap {
    val x = rect.left.coerceIn(0, bitmap.width)
    val y = rect.top.coerceIn(0, bitmap.height)
    val width = (rect.right - rect.left).coerceIn(1, bitmap.width - x)
    val height = (rect.bottom - rect.top).coerceIn(1, bitmap.height - y)
    
    return Bitmap.createBitmap(bitmap, x, y, width, height)
}

@Composable
fun ManualCropOverlay(
    cropStart: Offset?,
    cropEnd: Offset?,
    imageBoxSize: Size?,
    bitmapSize: Size?,
    onCropStart: (Offset) -> Unit,
    onCropDrag: (Offset) -> Unit,
    onCropEnd: (Offset, Offset) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        onCropStart(offset)
                    },
                    onDrag = { change, _ ->
                        onCropDrag(change.position)
                    },
                    onDragEnd = {
                        cropStart?.let { start ->
                            cropEnd?.let { end ->
                                onCropEnd(start, end)
                            }
                        }
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val overlayColor = Color.Black.copy(alpha = 0.5f)
            
            // Show drag selection
            cropStart?.let { start ->
                cropEnd?.let { end ->
                    val left = min(start.x, end.x)
                    val top = min(start.y, end.y)
                    val right = max(start.x, end.x)
                    val bottom = max(start.y, end.y)
                    
                    // Draw overlay on all sides
                    // Top
                    drawRect(overlayColor, topLeft = Offset(0f, 0f), size = Size(size.width, top))
                    // Bottom
                    drawRect(overlayColor, topLeft = Offset(0f, bottom), size = Size(size.width, size.height - bottom))
                    // Left
                    drawRect(overlayColor, topLeft = Offset(0f, top), size = Size(left, bottom - top))
                    // Right
                    drawRect(overlayColor, topLeft = Offset(right, top), size = Size(size.width - right, bottom - top))
                    
                    // Draw crop rectangle border
                    drawRect(
                        color = Color.White,
                        topLeft = Offset(left, top),
                        size = Size(right - left, bottom - top),
                        style = Stroke(width = 3.dp.toPx())
                    )
                } ?: run {
                    // Full overlay when no end point
                    drawRect(overlayColor)
                }
            } ?: run {
                // Full overlay when no crop started
                drawRect(overlayColor.copy(alpha = 0.3f))
            }
        }
    }
}

