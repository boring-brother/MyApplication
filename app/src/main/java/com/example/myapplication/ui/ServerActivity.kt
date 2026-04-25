package com.example.myapplication.ui

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.MainActivity
import com.example.myapplication.MyApplication
import com.example.myapplication.model.TouchEvent
import com.example.myapplication.service.ServerService
import com.example.myapplication.service.TouchAccessibilityService
import com.example.myapplication.util.NetworkUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ServerActivity : ComponentActivity(), ServerService.ServerEventListener {
    
    private var serverService: ServerService? = null
    private var serviceBound = false
    
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected
    
    private val _ipAddress = MutableStateFlow("")
    val ipAddress: StateFlow<String> = _ipAddress
    
    private val _isAccessibilityEnabled = MutableStateFlow(false)
    val isAccessibilityEnabled: StateFlow<Boolean> = _isAccessibilityEnabled
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        _ipAddress.value = NetworkUtils.getWifiIpAddress(this) ?: "Unknown"
        
        lifecycleScope.launch {
            _isAccessibilityEnabled.value = isAccessibilityServiceEnabled()
        }
        
        setContent {
            ServerScreen(
                ipAddress = _ipAddress.collectAsState().value,
                isConnected = _isConnected.collectAsState().value,
                isAccessibilityEnabled = _isAccessibilityEnabled.collectAsState().value,
                onToggleService = { startOrStopService() },
                onOpenAccessibilitySettings = { openAccessibilitySettings() },
                onSwitchMode = { switchToModeSelect() },
                onRefreshAccessibility = {
                    lifecycleScope.launch {
                        _isAccessibilityEnabled.value = isAccessibilityServiceEnabled()
                    }
                }
            )
        }
    }
    
    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            _isAccessibilityEnabled.value = isAccessibilityServiceEnabled()
        }
    }
    
    private fun startOrStopService() {
        if (_isConnected.value) {
            stopService(Intent(this, ServerService::class.java))
            _isConnected.value = false
            finish()
        } else {
            if (!isAccessibilityServiceEnabled()) {
                openAccessibilitySettings()
                return
            }
            
            val ip = _ipAddress.value
            if (ip == "Unknown" || ip.isBlank() || !NetworkUtils.isValidIpAddress(ip)) {
                return
            }
            
            val intent = Intent(this, ServerService::class.java)
            startService(intent)
            finish()
        }
    }
    
    private fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
    }
    
    private fun switchToModeSelect() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
    }
    
    private fun isAccessibilityServiceEnabled(): Boolean {
        val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        
        for (service in enabledServices) {
            if (service.resolveInfo.serviceInfo.packageName == packageName &&
                service.resolveInfo.serviceInfo.name == TouchAccessibilityService::class.java.name
            ) {
                return true
            }
        }
        return false
    }
    
    override fun onClientConnected() {
        lifecycleScope.launch {
            _isConnected.value = true
        }
    }
    
    override fun onClientDisconnected() {
        lifecycleScope.launch {
            _isConnected.value = false
        }
    }
    
    override fun onError(message: String) {
        lifecycleScope.launch {
            _isConnected.value = false
        }
    }
    
    override fun onReceiveTouchEvent(event: TouchEvent) {
        when (event.action) {
            android.view.MotionEvent.ACTION_DOWN, android.view.MotionEvent.ACTION_MOVE -> {
                MyApplication.accessibilityService?.executeTouch(event.x, event.y, event.action)
            }
        }
    }
}

@Composable
fun ServerScreen(
    ipAddress: String,
    isConnected: Boolean,
    isAccessibilityEnabled: Boolean,
    onToggleService: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onSwitchMode: () -> Unit,
    onRefreshAccessibility: () -> Unit
) {
    val hasValidIp = ipAddress != "Unknown" && ipAddress.isNotBlank() && ipAddress.matches(
        "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$".toRegex()
    )
    
    val canStart = hasValidIp && isAccessibilityEnabled
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = onSwitchMode) {
                Text("Switch Mode")
            }
            
            Text(
                text = "Server Mode",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.width(80.dp))
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (!isAccessibilityEnabled) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFF3E0)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "⚠️ Accessibility Service Required",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE65100)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Please enable Accessibility Service to allow remote touch execution",
                        fontSize = 14.sp,
                        color = Color(0xFFE65100)
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onOpenAccessibilitySettings,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Enable Now")
                        }
                        
                        Button(
                            onClick = onRefreshAccessibility,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50)
                            )
                        ) {
                            Text("Refresh")
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        if (!hasValidIp) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFEBEE)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "❌ No Valid IP Address",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD32F2F)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Cannot start server without a valid WiFi IP address. Please connect to WiFi network.",
                        fontSize = 14.sp,
                        color = Color(0xFFD32F2F)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Your IP Address:",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = ipAddress,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (hasValidIp) MaterialTheme.colorScheme.primary else Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Status:",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(
                                color = if (isConnected) Color.Green else Color.Gray,
                                shape = MaterialTheme.shapes.small
                            )
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = if (isConnected) "Connected" else "Waiting for connection",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onToggleService,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = canStart && !isConnected,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isConnected) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = if (isConnected) "Stop Server" else "Start Server",
                fontSize = 18.sp
            )
        }
        
        if (!canStart) {
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = when {
                    !isAccessibilityEnabled -> "Enable Accessibility Service first"
                    !hasValidIp -> "Connect to WiFi to get IP address"
                    else -> ""
                },
                fontSize = 14.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }
        
        Spacer(modifier = Modifier.weight(1f))
    }
}
