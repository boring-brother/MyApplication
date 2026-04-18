package com.example.myapplication.ui

import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.MainActivity
import com.example.myapplication.mapper.CoordinateMapper
import com.example.myapplication.model.ScreenSize
import com.example.myapplication.model.TouchEvent
import com.example.myapplication.network.ClientSocketManager
import com.example.myapplication.network.ConnectionListener
import com.example.myapplication.repository.PreferencesRepository
import com.example.myapplication.util.Constants
import com.example.myapplication.util.ScreenUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ClientActivity : ComponentActivity(), ConnectionListener {
    
    private lateinit var preferencesRepository: PreferencesRepository
    private var clientSocketManager: ClientSocketManager? = null
    private var coordinateMapper: CoordinateMapper? = null
    
    private val _connectionState = MutableStateFlow(false)
    val connectionState: StateFlow<Boolean> = _connectionState
    
    private val _serverIp = MutableStateFlow("")
    val serverIp: StateFlow<String> = _serverIp
    
    private val _connectionError = MutableStateFlow<String?>(null)
    val connectionError: StateFlow<String?> = _connectionError
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableFullScreen()
        
        preferencesRepository = PreferencesRepository(this)
        
        lifecycleScope.launch {
            _serverIp.value = preferencesRepository.getServerIp() ?: ""
        }
        
        val clientScreen = ScreenUtils.getRealScreenSize(this)
        coordinateMapper = CoordinateMapper(clientScreen)
        
        setContent {
            ClientContent(
                connectionState = _connectionState.collectAsState().value,
                serverIp = _serverIp.collectAsState().value,
                connectionError = _connectionError.collectAsState().value,
                onConnect = { ip -> connectToServer(ip) },
                onDisconnect = { disconnect() },
                onSendTouchEvent = { event -> sendTouchEvent(event) },
                onSwitchMode = { switchToModeSelect() },
                onChangeServer = { ip -> changeServer(ip) }
            )
        }
    }
    
    @Suppress("DEPRECATION")
    private fun enableFullScreen() {
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_FULLSCREEN
        )
    }
    
    private fun connectToServer(ip: String) {
        lifecycleScope.launch {
            preferencesRepository.saveServerIp(ip)
            _serverIp.value = ip
            _connectionError.value = null
            
            clientSocketManager = ClientSocketManager()
            clientSocketManager?.setConnectionListener(this@ClientActivity)
            clientSocketManager?.connectToServer(ip, Constants.DEFAULT_PORT)
        }
    }
    
    private fun changeServer(ip: String) {
        disconnect()
        connectToServer(ip)
    }
    
    private fun disconnect() {
        clientSocketManager?.disconnect()
        _connectionState.value = false
    }
    
    private fun switchToModeSelect() {
        disconnect()
        val intent = android.content.Intent(this, MainActivity::class.java)
        intent.flags = android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
        finish()
    }
    
    override fun onDestroy() {
        clientSocketManager?.cleanup()
        super.onDestroy()
    }
    
    override fun onConnected() {
        lifecycleScope.launch {
            _connectionState.value = true
            _connectionError.value = null
        }
    }
    
    override fun onDisconnected() {
        lifecycleScope.launch {
            _connectionState.value = false
        }
    }
    
    override fun onError(message: String) {
        lifecycleScope.launch {
            _connectionState.value = false
            _connectionError.value = message
        }
    }
    
    override fun onTouchEvent(event: TouchEvent) {
    }
    
    override fun onScreenSizeReceived(screenSize: ScreenSize) {
        coordinateMapper?.setServerScreenSize(screenSize)
    }
    
    override fun onClientConnected(clientInfo: String) {
    }
    
    private fun sendTouchEvent(event: TouchEvent) {
        clientSocketManager?.sendTouchEvent(event)
    }
}

@Composable
fun ClientContent(
    connectionState: Boolean,
    serverIp: String,
    connectionError: String?,
    onConnect: (String) -> Unit,
    onDisconnect: () -> Unit,
    onSendTouchEvent: (TouchEvent) -> Unit,
    onSwitchMode: () -> Unit,
    onChangeServer: (String) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (!connectionState) {
            ClientScreen(
                serverIp = serverIp,
                connectionError = connectionError,
                onConnect = onConnect,
                onSwitchMode = onSwitchMode
            )
        }
        
        if (connectionState) {
            TouchOverlay(
                onSendTouchEvent = onSendTouchEvent,
                onChangeServer = onChangeServer,
                modifier = Modifier.matchParentSize()
            )
            
            ConnectionStatusBar(
                serverIp = serverIp,
                onDisconnect = onDisconnect,
                onSwitchMode = onSwitchMode
            )
        }
    }
}

@Composable
fun TouchOverlay(
    onSendTouchEvent: (TouchEvent) -> Unit,
    onChangeServer: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showServerDialog by remember { mutableStateOf(false) }
    
    if (showServerDialog) {
        var newServerIp by remember { mutableStateOf("") }
        
        AlertDialog(
            onDismissRequest = { showServerDialog = false },
            title = { Text("Change Server") },
            text = {
                Column {
                    Text("Enter new server IP:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newServerIp,
                        onValueChange = { newServerIp = it },
                        label = { Text("Server IP") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newServerIp.isNotBlank()) {
                            onChangeServer(newServerIp)
                            showServerDialog = false
                        }
                    },
                    enabled = newServerIp.isNotBlank()
                ) {
                    Text("Connect")
                }
            },
            dismissButton = {
                TextButton(onClick = { showServerDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    AndroidView(
        factory = { context ->
            View(context).apply {
                setOnTouchListener { _, event ->
                    onSendTouchEvent(
                        TouchEvent(
                            action = event.actionMasked,
                            x = event.x,
                            y = event.y,
                            pointerId = event.getPointerId(event.actionIndex),
                            pressure = event.getPressure(event.actionIndex)
                        )
                    )
                    true
                }
            }
        },
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF2C3E50))
    )
}

@Composable
fun ConnectionStatusBar(
    serverIp: String,
    onDisconnect: () -> Unit,
    onSwitchMode: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Text(
            text = "Connected to: $serverIp",
            color = Color.White.copy(alpha = 0.7f),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp)
        )
        
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            TextButton(
                onClick = onSwitchMode
            ) {
                Text(
                    text = "Switch Mode",
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            TextButton(
                onClick = onDisconnect
            ) {
                Text(
                    text = "Disconnect",
                    color = Color.Red
                )
            }
        }
    }
}

@Composable
fun ClientScreen(
    serverIp: String,
    connectionError: String?,
    onConnect: (String) -> Unit,
    onSwitchMode: () -> Unit
) {
    var ipInput by remember { mutableStateOf(serverIp) }
    
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
                Text("Switch Mode", color = Color.White)
            }
            
            Text(
                text = "Client Mode",
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.width(80.dp))
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        if (connectionError != null) {
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
                        text = "❌ Connection Failed",
                        color = Color(0xFFD32F2F),
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = connectionError,
                        color = Color(0xFFD32F2F),
                        fontSize = 14.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF34495E)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Server IP Address:",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = ipInput,
                    onValueChange = { ipInput = it },
                    label = { Text("Enter IP", color = Color.White) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = { onConnect(ipInput) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = ipInput.isNotBlank()
                ) {
                    Text("Connect")
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
    }
}
