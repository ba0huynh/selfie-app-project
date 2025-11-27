package com.example.selfie

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import kotlinx.coroutines.delay
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.selfie.ui.Screen
import java.net.URLEncoder
import java.net.URLDecoder
import com.example.selfie.ui.camera.CameraScreen
import com.example.selfie.ui.photoEdit.PhotoEditScreen
import com.example.selfie.ui.photoGrid.PhotoGridScreen
import com.example.selfie.ui.photoViewer.PhotoViewerScreen
import com.example.selfie.ui.settings.SettingsScreen
import com.example.selfie.ui.theme.SelfieTheme
import com.example.selfie.work.ReminderScheduler

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Schedule daily reminders
        ReminderScheduler.scheduleDailyReminder(this)
        
        enableEdgeToEdge()
        setContent {
            SelfieTheme {
                val navController = rememberNavController()
                var refreshKey by remember { mutableStateOf(0) }
                var photoToEdit by remember { mutableStateOf<ByteArray?>(null) }
                val initialIntent = remember { intent }
                
                // Navigate to camera when notification is clicked
                LaunchedEffect(initialIntent) {
                    if (initialIntent?.action == "OPEN_CAMERA") {
                        // Small delay to ensure NavHost is ready
                        delay(100)
                        navController.navigate(Screen.Camera.route) {
                            // Clear back stack so back button goes to photo grid
                            popUpTo(Screen.PhotoGrid.route) { inclusive = false }
                        }
                    }
                }
                
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = Screen.PhotoGrid.route
                    ) {
                        composable(Screen.PhotoGrid.route) {
                            // Use key to force refresh when coming back from camera
                            key(refreshKey) {
                                PhotoGridScreen(
                                    onNavigateToCamera = {
                                        navController.navigate(Screen.Camera.route)
                                    },
                                    onNavigateToPhotoViewer = { photoPath ->
                                        val encodedPath = URLEncoder.encode(photoPath, "UTF-8")
                                        navController.navigate("photo_viewer/$encodedPath")
                                    },
                                    onNavigateToSettings = {
                                        navController.navigate(Screen.Settings.route)
                                    }
                                )
                            }
                        }
                        
                        composable(Screen.Camera.route) {
                            CameraScreen(
                                onPhotoCaptured = { photoBytes ->
                                    // Navigate to edit screen with photo bytes
                                    photoToEdit = photoBytes
                                    navController.navigate(Screen.PhotoEdit.route)
                                },
                                onDismiss = {
                                    navController.popBackStack()
                                }
                            )
                        }
                        
                        composable(Screen.PhotoEdit.route) {
                            photoToEdit?.let { photoBytes ->
                                PhotoEditScreen(
                                    photoBytes = photoBytes,
                                    onSave = { editedBytes ->
                                        // Save edited photo, increment refresh key, and return to grid
                                        val repo = com.example.selfie.data.PhotoRepository(this@MainActivity)
                                        repo.savePhoto(editedBytes)
                                        refreshKey = refreshKey + 1
                                        photoToEdit = null
                                        navController.popBackStack(Screen.PhotoGrid.route, inclusive = false)
                                    },
                                    onCancel = {
                                        photoToEdit = null
                                        navController.popBackStack()
                                    }
                                )
                            }
                        }
                        
                        composable("photo_viewer/{photoPath}") { backStackEntry ->
                            val encodedPath = backStackEntry.arguments?.getString("photoPath")
                            val photoPath = encodedPath?.let { URLDecoder.decode(it, "UTF-8") }
                            if (photoPath != null) {
                                PhotoViewerScreen(
                                    photoPath = photoPath,
                                    onBack = {
                                        navController.popBackStack()
                                    },
                                    onPhotoDeleted = {
                                        navController.popBackStack()
                                    }
                                )
                            }
                        }
                        
                        composable(Screen.Settings.route) {
                            SettingsScreen(
                                onBack = {
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Navigation will be handled by recomposition when intent changes
    }
}
