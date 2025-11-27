package com.example.selfie.data

import java.io.File
import java.util.Date

data class VideoMetadata(
    val file: File,
    val dateCreated: Date,
    val note: String = "",
    val emoji: String = ""
)

