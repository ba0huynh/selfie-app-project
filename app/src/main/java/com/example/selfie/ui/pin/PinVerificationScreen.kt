package com.example.selfie.ui.pin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.selfie.data.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PinVerificationScreen(
    onPinVerified: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var attempts by remember { mutableStateOf(0) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Lock icon
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Title
        Text(
            text = "Nhập mã PIN",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Error message
        if (errorMessage != null) {
            Text(
                text = errorMessage ?: "",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // PIN input display
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(vertical = 24.dp)
        ) {
            repeat(4) { index ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (index < enteredPin.length) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    modifier = Modifier.size(56.dp),
                    border = if (index < enteredPin.length) null else {
                        BorderStroke(
                            2.dp,
                            MaterialTheme.colorScheme.outline
                        )
                    }
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (index < enteredPin.length) {
                            Text(
                                text = "●",
                                fontSize = 28.sp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Number pad
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Rows 1-3
            (1..3).forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ((row - 1) * 3 + 1..row * 3).forEach { num ->
                        NumberButton(
                            number = num.toString(),
                            onClick = {
                                errorMessage = null
                                if (enteredPin.length < 4) {
                                    enteredPin += num
                                    if (enteredPin.length == 4) {
                                        // Verify PIN
                                        if (prefs.verifyPin(enteredPin)) {
                                            onPinVerified()
                                        } else {
                                            attempts++
                                            errorMessage = if (attempts >= 3) {
                                                "Đã nhập sai quá nhiều lần. Vui lòng thử lại sau."
                                            } else {
                                                "Mã PIN không đúng"
                                            }
                                            enteredPin = ""
                                            if (attempts >= 3) {
                                                // Reset attempts after a delay
                                                CoroutineScope(Dispatchers.Main).launch {
                                                    delay(3000)
                                                    attempts = 0
                                                    errorMessage = null
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
            
            // Row 4: 0 and backspace
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Spacer(modifier = Modifier.weight(1f))
                NumberButton(
                    number = "0",
                    onClick = {
                        errorMessage = null
                        if (enteredPin.length < 4) {
                            enteredPin += "0"
                            if (enteredPin.length == 4) {
                                // Verify PIN
                                if (prefs.verifyPin(enteredPin)) {
                                    onPinVerified()
                                } else {
                                    attempts++
                                    errorMessage = if (attempts >= 3) {
                                        "Đã nhập sai quá nhiều lần. Vui lòng thử lại sau."
                                    } else {
                                        "Mã PIN không đúng"
                                    }
                                    enteredPin = ""
                                    if (attempts >= 3) {
                                        CoroutineScope(Dispatchers.Main).launch {
                                            delay(3000)
                                            attempts = 0
                                            errorMessage = null
                                        }
                                    }
                                }
                            }
                        }
                    }
                )
                IconButton(
                    onClick = {
                        errorMessage = null
                        if (enteredPin.isNotEmpty()) {
                            enteredPin = enteredPin.dropLast(1)
                        }
                    },
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        "Xóa",
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun NumberButton(
    number: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.size(72.dp),
        shape = RoundedCornerShape(36.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Text(
            text = number,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

