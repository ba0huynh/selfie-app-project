package com.example.selfie.data

import android.content.Context
import android.util.Log
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class GoogleDriveManager(private val context: Context) {
    
    companion object {
        private const val TAG = "GoogleDriveManager"
        private val SCOPES = listOf(DriveScopes.DRIVE_FILE)
        private const val APP_FOLDER_NAME = "Selfie Diary"
    }
    
    private var driveService: Drive? = null
    private var credential: GoogleAccountCredential? = null
    
    fun initializeCredential(accountName: String): GoogleAccountCredential {
        credential = GoogleAccountCredential.usingOAuth2(context, SCOPES)
            .setBackOff(com.google.api.client.util.ExponentialBackOff())
            .setSelectedAccountName(accountName)
        
        val transport = NetHttpTransport()
        val jsonFactory = JacksonFactory.getDefaultInstance()
        
        driveService = Drive.Builder(transport, jsonFactory, credential)
            .setApplicationName("Selfie Diary")
            .build()
        
        return credential!!
    }
    
    fun initializeWithGoogleSignInAccount(account: GoogleSignInAccount): GoogleAccountCredential {
        val accountName = account.email ?: throw IllegalArgumentException("Account email is null")
        return initializeCredential(accountName)
    }
    
    suspend fun uploadPhoto(photoFile: File, folderName: String = APP_FOLDER_NAME): Result<String> = withContext(Dispatchers.IO) {
        try {
            val drive = driveService ?: return@withContext Result.failure(
                IllegalStateException("Drive service not initialized. Please sign in first.")
            )
            
            // Ensure folder exists
            val folderId = getOrCreateFolder(drive, folderName)
            
            // Create file metadata
            val fileMetadata = com.google.api.services.drive.model.File().apply {
                name = photoFile.name
                parents = listOf(folderId)
            }
            
            // Upload file
            val mediaContent = FileContent("image/jpeg", photoFile)
            val uploadedFile = drive.files().create(fileMetadata, mediaContent)
                .setFields("id, name, webViewLink")
                .execute()
            
            Log.d(TAG, "Photo uploaded successfully: ${uploadedFile.name} (ID: ${uploadedFile.id})")
            Result.success(uploadedFile.id ?: "")
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading photo", e)
            Result.failure(e)
        }
    }
    
    private suspend fun getOrCreateAppFolder(drive: Drive): String = withContext(Dispatchers.IO) {
        getOrCreateFolder(drive, APP_FOLDER_NAME)
    }
    
    suspend fun getOrCreateFolder(drive: Drive, folderName: String): String = withContext(Dispatchers.IO) {
        try {
            // Try to find existing folder
            val query = "name='$folderName' and mimeType='application/vnd.google-apps.folder' and trashed=false"
            val result = drive.files().list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("files(id, name)")
                .execute()
            
            if (result.files.isNotEmpty()) {
                return@withContext result.files[0].id
            }
            
            // Create folder if it doesn't exist
            val folderMetadata = com.google.api.services.drive.model.File().apply {
                name = folderName
                mimeType = "application/vnd.google-apps.folder"
            }
            
            val folder = drive.files().create(folderMetadata)
                .setFields("id, name")
                .execute()
            
            Log.d(TAG, "Created folder: $folderName (ID: ${folder.id})")
            folder.id
        } catch (e: Exception) {
            Log.e(TAG, "Error getting/creating folder: $folderName", e)
            throw e
        }
    }
    
    suspend fun uploadAllPhotos(photoFiles: List<File>, onProgress: (Int, Int) -> Unit = { _, _ -> }): Result<Int> = withContext(Dispatchers.IO) {
        try {
            var successCount = 0
            val total = photoFiles.size
            
            photoFiles.forEachIndexed { index, file ->
                val result = uploadPhoto(file)
                if (result.isSuccess) {
                    successCount++
                }
                onProgress(index + 1, total)
            }
            
            Result.success(successCount)
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading photos", e)
            Result.failure(e)
        }
    }
    
    suspend fun uploadPhotosToFolder(photoFiles: List<File>, folderName: String, onProgress: (Int, Int) -> Unit = { _, _ -> }): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val drive = driveService ?: return@withContext Result.failure(
                IllegalStateException("Drive service not initialized. Please sign in first.")
            )
            
            // Ensure folder exists
            val folderId = getOrCreateFolder(drive, folderName)
            
            var successCount = 0
            val total = photoFiles.size
            
            photoFiles.forEachIndexed { index, file ->
                val result = uploadPhoto(file, folderName)
                if (result.isSuccess) {
                    successCount++
                }
                onProgress(index + 1, total)
            }
            
            Result.success(successCount)
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading photos to folder: $folderName", e)
            Result.failure(e)
        }
    }
    
    fun clearCredentials() {
        driveService = null
        credential = null
    }
    
    fun isInitialized(): Boolean = driveService != null && credential != null
}

