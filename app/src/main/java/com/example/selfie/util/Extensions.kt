package com.example.selfie.util

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color

fun Modifier.selectable(selected: Boolean) = if (selected) {
    this.drawWithContent {
        drawRect(color = Color.Black.copy(alpha = 0.3f))
        drawContent()
    }
} else {
    this
}

