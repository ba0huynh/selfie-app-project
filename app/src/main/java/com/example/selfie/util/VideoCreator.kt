package com.example.selfie.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Build
import android.util.Log
import android.view.Surface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

class VideoCreator {
    companion object {
        private const val TAG = "VideoCreator"
        private const val MIME_TYPE = "video/avc"
        private const val FRAME_RATE = 30
        private const val I_FRAME_INTERVAL = 1
        private const val BIT_RATE = 2000000 // 2 Mbps
    }
    
    suspend fun createVideoFromImages(
        imageFiles: List<File>,
        outputFile: File,
        secondsPerImage: Float,
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            if (imageFiles.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("No images provided"))
            }
            
            // Load first image to get dimensions
            val firstBitmap = BitmapFactory.decodeFile(imageFiles[0].absolutePath)
            if (firstBitmap == null) {
                return@withContext Result.failure(IllegalArgumentException("Could not decode first image"))
            }
            
            val width = firstBitmap.width
            val height = firstBitmap.height
            firstBitmap.recycle()
            
            // Ensure dimensions are even and reasonable
            var videoWidth = if (width % 2 == 0) width else width - 1
            var videoHeight = if (height % 2 == 0) height else height - 1
            
            // Ensure minimum dimensions
            videoWidth = videoWidth.coerceAtLeast(64)
            videoHeight = videoHeight.coerceAtLeast(64)
            
            // Limit maximum dimensions to avoid issues (most devices support up to 1080p)
            val maxDimension = 1080
            if (videoWidth > maxDimension || videoHeight > maxDimension) {
                val scale = minOf(maxDimension.toFloat() / videoWidth, maxDimension.toFloat() / videoHeight)
                videoWidth = (videoWidth * scale).toInt()
                videoHeight = (videoHeight * scale).toInt()
                // Ensure still even
                videoWidth = if (videoWidth % 2 == 0) videoWidth else videoWidth - 1
                videoHeight = if (videoHeight % 2 == 0) videoHeight else videoHeight - 1
            }
            
            createVideoWithDimensions(imageFiles, outputFile, secondsPerImage, videoWidth, videoHeight, onProgress)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating video", e)
            Result.failure(e)
        }
    }
    
    private suspend fun createVideoWithDimensions(
        imageFiles: List<File>,
        outputFile: File,
        secondsPerImage: Float,
        videoWidth: Int,
        videoHeight: Int,
        onProgress: (Int, Int) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val frameCount = imageFiles.size
            val framesPerImage = (secondsPerImage * FRAME_RATE).toInt().coerceAtLeast(1)
            
            Log.d(TAG, "Creating video: ${videoWidth}x${videoHeight}, $frameCount images, ${secondsPerImage}s per image, $framesPerImage frames per image")
            
            // Create MediaFormat with all parameters
            val format = MediaFormat.createVideoFormat(MIME_TYPE, videoWidth, videoHeight).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)
                setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL)
            }
            
            // Find a supported encoder
            val codecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)
            val codecName = codecList.findEncoderForFormat(format)
            
            if (codecName == null) {
                Log.e(TAG, "No encoder found for format ${videoWidth}x${videoHeight}")
                // Try with smaller dimensions
                if (videoWidth > 640 || videoHeight > 640) {
                    Log.d(TAG, "Trying with smaller dimensions (640x480)...")
                    val smallerWidth = 640
                    val smallerHeight = (videoHeight * 640f / videoWidth).toInt().let { 
                        val h = if (it % 2 == 0) it else it - 1
                        h.coerceAtMost(480).coerceAtLeast(64)
                    }
                    return createVideoWithDimensions(imageFiles, outputFile, secondsPerImage, smallerWidth, smallerHeight, onProgress)
                }
                return@withContext Result.failure(Exception("Không tìm thấy encoder phù hợp cho video ${videoWidth}x${videoHeight}"))
            }
            
            Log.d(TAG, "Using encoder: $codecName")
            
            // Create encoder
            val encoder = try {
                MediaCodec.createByCodecName(codecName)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create encoder", e)
                return@withContext Result.failure(Exception("Không thể tạo encoder video: ${e.message}"))
            }
            
            try {
                encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                Log.d(TAG, "Encoder configured successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to configure encoder", e)
                Log.e(TAG, "Format: $format")
                Log.e(TAG, "Codec: $codecName")
                Log.e(TAG, "Dimensions: ${videoWidth}x${videoHeight}")
                encoder.release()
                
                // Try with smaller dimensions as fallback
                if (videoWidth > 640 || videoHeight > 640) {
                    Log.d(TAG, "Trying with smaller dimensions (640x480)...")
                    val smallerWidth = 640
                    val smallerHeight = (videoHeight * 640f / videoWidth).toInt().let { 
                        val h = if (it % 2 == 0) it else it - 1
                        h.coerceAtMost(480).coerceAtLeast(64)
                    }
                    return createVideoWithDimensions(imageFiles, outputFile, secondsPerImage, smallerWidth, smallerHeight, onProgress)
                }
                
                return@withContext Result.failure(Exception("Không thể cấu hình encoder: ${e.message}. Vui lòng thử với ít ảnh hơn hoặc ảnh có kích thước nhỏ hơn."))
            }
            
            val inputSurface = try {
                encoder.createInputSurface()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create input surface", e)
                encoder.release()
                return@withContext Result.failure(Exception("Không thể tạo surface: ${e.message}"))
            }
            
            encoder.start()
            
            // Create muxer
            val muxer = try {
                MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create muxer", e)
                encoder.stop()
                encoder.release()
                return@withContext Result.failure(Exception("Không thể tạo muxer: ${e.message}"))
            }
            var muxerStarted = false
            var videoTrackIndex = -1
            
            // Create EGL context for rendering
            val eglHelper = try {
                val helper = EGLHelper(inputSurface)
                helper.makeCurrent()
                helper
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize EGL", e)
                encoder.stop()
                encoder.release()
                muxer.release()
                return@withContext Result.failure(Exception("Không thể khởi tạo EGL: ${e.message}"))
            }
            
            try {
                var frameIndex = 0
                var presentationTimeUs: Long = 0
                val frameDurationUs = (1_000_000 / FRAME_RATE).toLong()
                
                for (imageIndex in 0 until frameCount) {
                    val imageFile = imageFiles[imageIndex]
                    val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
                    
                    if (bitmap != null) {
                        // Scale bitmap to match video dimensions if needed
                        val scaledBitmap = if (bitmap.width != videoWidth || bitmap.height != videoHeight) {
                            Bitmap.createScaledBitmap(bitmap, videoWidth, videoHeight, true)
                        } else {
                            bitmap
                        }
                        
                        // Render the same image for multiple frames
                        for (repeatFrame in 0 until framesPerImage) {
                            eglHelper.drawFrame(scaledBitmap)
                            
                            // Set presentation time and signal frame ready
                            // The encoder will process this frame
                            
                            // Drain encoder to get encoded frames
                            drainEncoder(encoder, muxer, muxerStarted, videoTrackIndex, presentationTimeUs) { started, trackIndex ->
                                muxerStarted = started
                                videoTrackIndex = trackIndex
                            }
                            
                            presentationTimeUs += frameDurationUs
                            frameIndex++
                            
                            // Small delay to ensure frame is processed
                            delay(10)
                        }
                        
                        if (scaledBitmap != bitmap) {
                            scaledBitmap.recycle()
                        }
                        bitmap.recycle()
                        
                        onProgress(imageIndex + 1, frameCount)
                    }
                }
                
                // Signal end of stream
                encoder.signalEndOfInputStream()
                
                // Drain remaining frames
                var drained = false
                while (!drained) {
                    val result = drainEncoder(encoder, muxer, muxerStarted, videoTrackIndex, presentationTimeUs) { started, trackIndex ->
                        muxerStarted = started
                        videoTrackIndex = trackIndex
                    }
                    drained = result
                }
                
            } finally {
                eglHelper.release()
                if (muxerStarted) {
                    muxer.stop()
                }
                muxer.release()
                encoder.stop()
                encoder.release()
            }
            
            Log.d(TAG, "Video created successfully: ${outputFile.absolutePath}")
            Result.success(outputFile)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error creating video", e)
            Result.failure(e)
        }
    }
    
    private fun drainEncoder(
        encoder: MediaCodec,
        muxer: MediaMuxer,
        muxerStarted: Boolean,
        videoTrackIndex: Int,
        presentationTimeUs: Long,
        onMuxerUpdate: (Boolean, Int) -> Unit
    ): Boolean {
        val bufferInfo = MediaCodec.BufferInfo()
        var currentMuxerStarted = muxerStarted
        var currentVideoTrackIndex = videoTrackIndex
        var endOfStream = false
        
        while (true) {
            val encoderStatus = encoder.dequeueOutputBuffer(bufferInfo, 10000)
            
            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                break
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                if (currentMuxerStarted) {
                    throw RuntimeException("format changed twice")
                }
                val newFormat = encoder.outputFormat
                currentVideoTrackIndex = muxer.addTrack(newFormat)
                muxer.start()
                currentMuxerStarted = true
                onMuxerUpdate(currentMuxerStarted, currentVideoTrackIndex)
            } else if (encoderStatus < 0) {
                Log.w(TAG, "Unexpected encoder status: $encoderStatus")
            } else {
                val encodedData = encoder.getOutputBuffer(encoderStatus)
                    ?: throw RuntimeException("encoderOutputBuffer $encoderStatus was null")
                
                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                    bufferInfo.size = 0
                }
                
                if (bufferInfo.size != 0 && currentMuxerStarted) {
                    bufferInfo.presentationTimeUs = presentationTimeUs
                    try {
                        muxer.writeSampleData(currentVideoTrackIndex, encodedData, bufferInfo)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error writing sample data", e)
                        throw e
                    }
                }
                
                encoder.releaseOutputBuffer(encoderStatus, false)
                
                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                    endOfStream = true
                    break
                }
            }
        }
        
        return endOfStream
    }
}

