package com.example.selfie.ui.camera

import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.ViewGroup
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.Executor

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(
    onPhotoCaptured: (ByteArray) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var cameraSelector by remember { mutableStateOf(CameraSelector.DEFAULT_FRONT_CAMERA) }
    
    if (!cameraPermissionState.status.isGranted) {
        CameraPermissionRequest(cameraPermissionState)
        return
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        CameraPreview(
            cameraSelector = cameraSelector,
            onImageCapture = { imageCapture = it },
            modifier = Modifier.fillMaxSize(),
            lifecycleOwner = lifecycleOwner
        )
        
        CameraControls(
            imageCapture = imageCapture,
            onPhotoCaptured = onPhotoCaptured,
            onCancel = onDismiss,
            onFlipCamera = {
                cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA) {
                    CameraSelector.DEFAULT_BACK_CAMERA
                } else {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(vertical = 48.dp, horizontal = 16.dp)
        )
    }
}

@Composable
fun CameraPreview(
    cameraSelector: CameraSelector,
    onImageCapture: (ImageCapture) -> Unit,
    modifier: Modifier = Modifier,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner
) {
    val context = LocalContext.current
    val currentCameraSelector = rememberUpdatedState(cameraSelector)
    
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var preview by remember { mutableStateOf<Preview?>(null) }
    var imageCaptureInstance by remember { mutableStateOf<ImageCapture?>(null) }
    
    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            
            cameraProviderFuture.addListener({
                val provider = cameraProviderFuture.get()
                cameraProvider = provider
                
                val previewInstance = Preview.Builder().build()
                previewInstance.setSurfaceProvider(previewView.surfaceProvider)
                preview = previewInstance
                
                val capture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()
                imageCaptureInstance = capture
                onImageCapture(capture)
                
                try {
                    provider.unbindAll()
                    provider.bindToLifecycle(
                        lifecycleOwner,
                        currentCameraSelector.value,
                        previewInstance,
                        capture
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(ctx))
            
            previewView
        },
        update = { previewView ->
            // Rebind camera when selector changes
            cameraProvider?.let { provider ->
                preview?.let { previewInstance ->
                    imageCaptureInstance?.let { capture ->
                        try {
                            provider.unbindAll()
                            provider.bindToLifecycle(
                                lifecycleOwner,
                                currentCameraSelector.value,
                                previewInstance,
                                capture
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        },
        modifier = modifier
    )
}

@Composable
fun CameraControls(
    imageCapture: ImageCapture?,
    onPhotoCaptured: (ByteArray) -> Unit,
    onCancel: () -> Unit,
    onFlipCamera: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onCancel) {
            Icon(Icons.Default.Close, "Cancel", tint = MaterialTheme.colorScheme.onBackground)
        }
        
        FloatingActionButton(
            onClick = {
                imageCapture?.let { capture ->
                    val tempFile = java.io.File(context.cacheDir, "temp_capture_${System.currentTimeMillis()}.jpg")
                    val outputOptions = ImageCapture.OutputFileOptions.Builder(tempFile).build()
                    
                    capture.takePicture(outputOptions, ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                try {
                                    if (tempFile.exists()) {
                                        val bitmap = BitmapFactory.decodeFile(tempFile.absolutePath)
                                        val byteArray = java.io.ByteArrayOutputStream().apply {
                                            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, this)
                                        }.toByteArray()
                                        
                                        // Call on main thread
                                        scope.launch(Dispatchers.Main) {
                                            onPhotoCaptured(byteArray)
                                            tempFile.delete()
                                        }
                                    } else {
                                        tempFile.delete()
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                            
                            override fun onError(exception: ImageCaptureException) {
                                exception.printStackTrace()
                            }
                        })
                }
            },
            containerColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp)
        ) {
            Icon(Icons.Default.Add, "Capture", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(32.dp))
        }
        
        IconButton(onClick = onFlipCamera) {
            Icon(Icons.Default.Refresh, "Flip Camera", tint = MaterialTheme.colorScheme.onBackground)
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraPermissionRequest(permissionState: com.google.accompanist.permissions.PermissionState) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Camera permission is required",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { permissionState.launchPermissionRequest() }
        ) {
            Text("Grant Permission")
        }
    }
}

