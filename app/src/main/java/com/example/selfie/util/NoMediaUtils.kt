package com.example.selfie.util

import android.content.Context
import android.os.Environment
import java.io.File

object NoMediaUtils {
    
    fun createNoMediaFile(context: Context): File? {
        val photoDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            ?: return null
        
        val noMediaFile = File(photoDir, ".nomedia")
        if (!noMediaFile.exists()) {
            noMediaFile.createNewFile()
        }
        
        return noMediaFile
    }
    
    fun ensureNoMediaFile(context: Context) {
        createNoMediaFile(context)
    }
}

