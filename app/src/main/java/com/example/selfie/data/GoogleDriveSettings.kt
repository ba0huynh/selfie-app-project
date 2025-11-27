package com.example.selfie.data

data class GoogleDriveSettings(
    val isConnected: Boolean = false,
    val accountEmail: String = "",
    val autoBackup: Boolean = false,
    val lastBackupTime: Long = 0L
)

