package com.example.selfie.data

import android.content.Context
import android.content.SharedPreferences
import android.os.Environment
import com.example.selfie.util.NoMediaUtils
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PhotoRepository(private val context: Context) {
    
    private val photoDir: File
        get() = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) 
            ?: File(context.filesDir, "photos").also { it.mkdirs() }
    
    private val notesPrefs: SharedPreferences by lazy {
        context.getSharedPreferences("photo_notes", Context.MODE_PRIVATE)
    }
    
    private val emojiPrefs: SharedPreferences by lazy {
        context.getSharedPreferences("photo_emojis", Context.MODE_PRIVATE)
    }
    
    private val datePrefs: SharedPreferences by lazy {
        context.getSharedPreferences("photo_dates", Context.MODE_PRIVATE)
    }
    
    private val driveUploadPrefs: SharedPreferences by lazy {
        context.getSharedPreferences("photo_drive_uploads", Context.MODE_PRIVATE)
    }
    
    init {
        photoDir.mkdirs()
        NoMediaUtils.ensureNoMediaFile(context)
    }
    
    fun savePhoto(bitmapBytes: ByteArray): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "IMG_$timeStamp.jpg"
        val file = File(photoDir, fileName)
        file.writeBytes(bitmapBytes)
        return file
    }
    
    fun getAllPhotos(): List<PhotoMetadata> {
        return photoDir.listFiles()
            ?.filter { it.isFile && it.extension.lowercase() == "jpg" }
            ?.map { file ->
                PhotoMetadata(
                    file = file,
                    dateTaken = getPhotoDate(file),
                    note = getPhotoNote(file),
                    emoji = getPhotoEmoji(file),
                    filter = ""
                )
            }
            ?.sortedByDescending { it.dateTaken } ?: emptyList()
    }
    
    fun deletePhoto(file: File): Boolean {
        return file.delete()
    }
    
    fun deletePhotos(files: List<File>): List<File> {
        val deleted = mutableListOf<File>()
        files.forEach { file ->
            if (file.delete()) {
                deleted.add(file)
            }
        }
        return deleted
    }
    
    fun getPhotosForDate(targetDate: Date): List<PhotoMetadata> {
        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val targetDateStr = dateFormat.format(targetDate)
        
        return getAllPhotos().filter {
            dateFormat.format(it.dateTaken) == targetDateStr
        }
    }
    
    fun getPhotosForDayInHistory(dayOfMonth: Int, month: Int): List<PhotoMetadata> {
        return getAllPhotos().filter {
            val calendar = java.util.Calendar.getInstance()
            calendar.time = it.dateTaken
            calendar.get(java.util.Calendar.DAY_OF_MONTH) == dayOfMonth &&
            calendar.get(java.util.Calendar.MONTH) == month
        }
    }
    
    fun updatePhotoMetadata(file: File, note: String, emoji: String, filter: String) {
        savePhotoNote(file, note)
        savePhotoEmoji(file, emoji)
    }
    
    fun savePhotoNote(file: File, note: String) {
        notesPrefs.edit()
            .putString(file.absolutePath, note)
            .apply()
    }
    
    fun getPhotoNote(file: File): String {
        return notesPrefs.getString(file.absolutePath, "") ?: ""
    }
    
    fun savePhotoEmoji(file: File, emoji: String) {
        emojiPrefs.edit()
            .putString(file.absolutePath, emoji)
            .apply()
    }
    
    fun getPhotoEmoji(file: File): String {
        return emojiPrefs.getString(file.absolutePath, "") ?: ""
    }
    
    fun savePhotoDate(file: File, date: Date) {
        datePrefs.edit()
            .putLong(file.absolutePath, date.time)
            .apply()
    }
    
    fun getPhotoDate(file: File): Date {
        val customDate = datePrefs.getLong(file.absolutePath, -1)
        return if (customDate != -1L) {
            Date(customDate)
        } else {
            Date(file.lastModified())
        }
    }
    
    fun getPhotoCount(): Int {
        return photoDir.listFiles()?.count { it.isFile } ?: 0
    }
    
    fun markPhotoAsUploaded(file: File, driveFileId: String? = null) {
        driveUploadPrefs.edit()
            .putBoolean(file.absolutePath, true)
            .apply()
        if (driveFileId != null) {
            // Store the Drive file ID for potential future use
            driveUploadPrefs.edit()
                .putString("${file.absolutePath}_id", driveFileId)
                .apply()
        }
    }
    
    fun isPhotoUploaded(file: File): Boolean {
        return driveUploadPrefs.getBoolean(file.absolutePath, false)
    }
    
    fun getPhotoDriveFileId(file: File): String? {
        return driveUploadPrefs.getString("${file.absolutePath}_id", null)
    }
    
    fun markPhotoAsNotUploaded(file: File) {
        driveUploadPrefs.edit()
            .remove(file.absolutePath)
            .remove("${file.absolutePath}_id")
            .apply()
    }
    
    // Video management methods
    private val videoDir: File
        get() = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) 
            ?: File(context.filesDir, "videos").also { it.mkdirs() }
    
    private val videoNotesPrefs: SharedPreferences by lazy {
        context.getSharedPreferences("video_notes", Context.MODE_PRIVATE)
    }
    
    private val videoEmojiPrefs: SharedPreferences by lazy {
        context.getSharedPreferences("video_emojis", Context.MODE_PRIVATE)
    }
    
    private val videoDatePrefs: SharedPreferences by lazy {
        context.getSharedPreferences("video_dates", Context.MODE_PRIVATE)
    }
    
    init {
        videoDir.mkdirs()
    }
    
    fun saveVideo(videoFile: File): File {
        // Video file is already created, just ensure it's in the right directory
        if (videoFile.parentFile?.absolutePath != videoDir.absolutePath) {
            val newFile = File(videoDir, videoFile.name)
            videoFile.copyTo(newFile, overwrite = true)
            videoFile.delete()
            return newFile
        }
        return videoFile
    }
    
    fun getAllVideos(): List<VideoMetadata> {
        return videoDir.listFiles()
            ?.filter { it.isFile && (it.extension.lowercase() == "mp4" || it.extension.lowercase() == "mov") }
            ?.map { file ->
                VideoMetadata(
                    file = file,
                    dateCreated = getVideoDate(file),
                    note = getVideoNote(file),
                    emoji = getVideoEmoji(file)
                )
            }
            ?.sortedByDescending { it.dateCreated } ?: emptyList()
    }
    
    fun deleteVideo(file: File): Boolean {
        // Also clean up metadata
        videoNotesPrefs.edit().remove(file.absolutePath).apply()
        videoEmojiPrefs.edit().remove(file.absolutePath).apply()
        videoDatePrefs.edit().remove(file.absolutePath).apply()
        return file.delete()
    }
    
    fun saveVideoNote(file: File, note: String) {
        videoNotesPrefs.edit()
            .putString(file.absolutePath, note)
            .apply()
    }
    
    fun getVideoNote(file: File): String {
        return videoNotesPrefs.getString(file.absolutePath, "") ?: ""
    }
    
    fun saveVideoEmoji(file: File, emoji: String) {
        videoEmojiPrefs.edit()
            .putString(file.absolutePath, emoji)
            .apply()
    }
    
    fun getVideoEmoji(file: File): String {
        return videoEmojiPrefs.getString(file.absolutePath, "") ?: ""
    }
    
    fun saveVideoDate(file: File, date: Date) {
        videoDatePrefs.edit()
            .putLong(file.absolutePath, date.time)
            .apply()
    }
    
    fun getVideoDate(file: File): Date {
        val customDate = videoDatePrefs.getLong(file.absolutePath, -1)
        return if (customDate != -1L) {
            Date(customDate)
        } else {
            Date(file.lastModified())
        }
    }
    
    fun getVideoCount(): Int {
        return videoDir.listFiles()?.count { it.isFile } ?: 0
    }
}

