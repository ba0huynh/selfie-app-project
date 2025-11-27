package com.example.selfie.ui

sealed class Screen(val route: String) {
    object PinVerification : Screen("pin_verification")
    object PhotoGrid : Screen("photo_grid")
    object Camera : Screen("camera")
    object PhotoEdit : Screen("photo_edit") {
        fun createRoute() = "photo_edit"
    }
    object PhotoViewer : Screen("photo_viewer/{photoPath}") {
        fun createRoute(photoPath: String) = "photo_viewer/${photoPath}"
    }
    object VideoViewer : Screen("video_viewer/{videoPath}") {
        fun createRoute(videoPath: String) = "video_viewer/${videoPath}"
    }
    object Settings : Screen("settings")
}

