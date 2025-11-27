package com.example.selfie.data

import java.io.File
import java.util.Date

data class PhotoMetadata(
    val file: File,
    val dateTaken: Date,
    val note: String = "",
    val emoji: String = "",
    val filter: String = ""
)

