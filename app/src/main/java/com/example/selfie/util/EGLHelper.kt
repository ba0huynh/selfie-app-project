package com.example.selfie.util

import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLUtils
import android.view.Surface
import javax.microedition.khronos.egl.*

class EGLHelper(private val surface: Surface) {
    private var eglDisplay: EGLDisplay? = null
    private var eglContext: EGLContext? = null
    private var eglSurface: EGLSurface? = null
    private var textureId = -1
    
    private val vertexShaderCode = """
        attribute vec4 vPosition;
        attribute vec2 vTexCoord;
        varying vec2 texCoord;
        void main() {
            gl_Position = vPosition;
            texCoord = vTexCoord;
        }
    """.trimIndent()
    
    private val fragmentShaderCode = """
        precision mediump float;
        uniform sampler2D uTexture;
        varying vec2 texCoord;
        void main() {
            gl_FragColor = texture2D(uTexture, texCoord);
        }
    """.trimIndent()
    
    private var program = -1
    private var positionHandle = -1
    private var texCoordHandle = -1
    private var textureHandle = -1
    
    fun makeCurrent() {
        val egl = EGLContext.getEGL() as EGL10
        
        eglDisplay = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY)
        if (eglDisplay == EGL10.EGL_NO_DISPLAY) {
            throw RuntimeException("Unable to get EGL display")
        }
        
        val version = IntArray(2)
        if (!egl.eglInitialize(eglDisplay, version)) {
            throw RuntimeException("Unable to initialize EGL")
        }
        
        val configs = arrayOfNulls<EGLConfig>(1)
        val configSpec = intArrayOf(
            EGL10.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
            EGL10.EGL_RED_SIZE, 8,
            EGL10.EGL_GREEN_SIZE, 8,
            EGL10.EGL_BLUE_SIZE, 8,
            EGL10.EGL_ALPHA_SIZE, 8,
            EGL10.EGL_DEPTH_SIZE, 0,
            EGL10.EGL_STENCIL_SIZE, 0,
            EGL10.EGL_NONE
        )
        
        val numConfigs = IntArray(1)
        if (!egl.eglChooseConfig(eglDisplay, configSpec, configs, 1, numConfigs) || numConfigs[0] == 0) {
            throw RuntimeException("Unable to choose EGL config")
        }
        
        if (configs[0] == null) {
            throw RuntimeException("No EGL config found")
        }
        
        val contextAttribs = intArrayOf(
            EGL_CONTEXT_CLIENT_VERSION, 2,
            EGL10.EGL_NONE
        )
        
        eglContext = egl.eglCreateContext(
            eglDisplay,
            configs[0],
            EGL10.EGL_NO_CONTEXT,
            contextAttribs
        )
        
        if (eglContext == null || eglContext == EGL10.EGL_NO_CONTEXT) {
            throw RuntimeException("Unable to create EGL context")
        }
        
        eglSurface = egl.eglCreateWindowSurface(eglDisplay, configs[0], surface, null)
        if (eglSurface == null || eglSurface == EGL10.EGL_NO_SURFACE) {
            throw RuntimeException("Unable to create EGL surface")
        }
        
        if (!egl.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
            throw RuntimeException("Unable to make EGL context current")
        }
        
        // Setup shaders
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)
        
        if (vertexShader == 0 || fragmentShader == 0) {
            throw RuntimeException("Failed to compile shaders")
        }
        
        program = GLES20.glCreateProgram()
        if (program == 0) {
            throw RuntimeException("Failed to create program")
        }
        
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)
        
        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == 0) {
            val error = GLES20.glGetProgramInfoLog(program)
            GLES20.glDeleteProgram(program)
            throw RuntimeException("Failed to link program: $error")
        }
        
        positionHandle = GLES20.glGetAttribLocation(program, "vPosition")
        texCoordHandle = GLES20.glGetAttribLocation(program, "vTexCoord")
        textureHandle = GLES20.glGetUniformLocation(program, "uTexture")
        
        // Create texture
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        textureId = textures[0]
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
    }
    
    fun drawFrame(bitmap: Bitmap) {
        val egl = EGLContext.getEGL() as EGL10
        
        // Upload bitmap to texture
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
        
        // Setup viewport
        GLES20.glViewport(0, 0, bitmap.width, bitmap.height)
        
        // Clear
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        
        // Use program
        GLES20.glUseProgram(program)
        
        // Setup vertices (full screen quad)
        val vertices = floatArrayOf(
            -1.0f, -1.0f, 0.0f,
            1.0f, -1.0f, 0.0f,
            -1.0f, 1.0f, 0.0f,
            1.0f, 1.0f, 0.0f
        )
        
        val texCoords = floatArrayOf(
            0.0f, 1.0f,
            1.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 0.0f
        )
        
        val vertexBuffer = java.nio.ByteBuffer.allocateDirect(vertices.size * 4)
            .order(java.nio.ByteOrder.nativeOrder())
            .asFloatBuffer()
        vertexBuffer.put(vertices)
        vertexBuffer.position(0)
        
        val texBuffer = java.nio.ByteBuffer.allocateDirect(texCoords.size * 4)
            .order(java.nio.ByteOrder.nativeOrder())
            .asFloatBuffer()
        texBuffer.put(texCoords)
        texBuffer.position(0)
        
        // Enable attributes
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer)
        
        GLES20.glEnableVertexAttribArray(texCoordHandle)
        GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 0, texBuffer)
        
        // Set texture
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glUniform1i(textureHandle, 0)
        
        // Draw
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        
        // Disable attributes
        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(texCoordHandle)
        
        // Swap buffers
        egl.eglSwapBuffers(eglDisplay, eglSurface)
    }
    
    fun release() {
        val egl = EGLContext.getEGL() as EGL10
        
        try {
            if (textureId != -1) {
                GLES20.glDeleteTextures(1, intArrayOf(textureId), 0)
            }
            
            if (program != -1) {
                GLES20.glDeleteProgram(program)
            }
            
            if (eglDisplay != null && eglDisplay != EGL10.EGL_NO_DISPLAY) {
                egl.eglMakeCurrent(eglDisplay, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT)
                
                if (eglSurface != null && eglSurface != EGL10.EGL_NO_SURFACE) {
                    egl.eglDestroySurface(eglDisplay, eglSurface)
                }
                
                if (eglContext != null && eglContext != EGL10.EGL_NO_CONTEXT) {
                    egl.eglDestroyContext(eglDisplay, eglContext)
                }
                
                egl.eglTerminate(eglDisplay)
            }
        } catch (e: Exception) {
            android.util.Log.e("EGLHelper", "Error releasing EGL resources", e)
        }
    }
    
    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        if (shader == 0) {
            throw RuntimeException("Failed to create shader")
        }
        
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)
        
        val compileStatus = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0)
        if (compileStatus[0] == 0) {
            val error = GLES20.glGetShaderInfoLog(shader)
            GLES20.glDeleteShader(shader)
            throw RuntimeException("Failed to compile shader: $error")
        }
        
        return shader
    }
    
    companion object {
        private const val EGL_CONTEXT_CLIENT_VERSION = 0x3098
        private const val EGL_OPENGL_ES2_BIT = 4
    }
}

