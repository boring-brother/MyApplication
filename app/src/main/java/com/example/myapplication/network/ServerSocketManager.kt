package com.example.myapplication.network

import com.example.myapplication.model.ScreenSize
import com.example.myapplication.model.TouchEvent
import com.example.myapplication.util.Constants
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.ServerSocket
import java.net.Socket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

interface ConnectionListener {
    fun onConnected()
    fun onDisconnected()
    fun onError(message: String)
    fun onTouchEvent(event: TouchEvent)
    fun onScreenSizeReceived(screenSize: ScreenSize)
    fun onClientConnected(clientInfo: String)
}

class ServerSocketManager(private val serverScreenSize: ScreenSize) {

    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null
    private var dataOutputStream: DataOutputStream? = null
    private var dataInputStream: DataInputStream? = null

    private val serverScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var clientJob: Job? = null
    private var heartbeatJob: Job? = null

    private val _connectionState = MutableStateFlow(false)
    val connectionState: StateFlow<Boolean> = _connectionState

    private var listener: ConnectionListener? = null
    private var lastHeartbeatTime: Long = 0

    fun startServer(port: Int) {
        serverScope.launch {
            try {
                serverSocket = ServerSocket(port)
                println("Server started on port $port")

                while (serverSocket?.isClosed == false) {
                    try {
                        clientSocket = serverSocket?.accept()
                        println("Client connected")

                        // Close previous client connection
                        clientJob?.cancel()
                        closeClientConnection()

                        dataOutputStream = DataOutputStream(clientSocket?.getOutputStream())
                        dataInputStream = DataInputStream(clientSocket?.getInputStream())

                        _connectionState.value = true
                        lastHeartbeatTime = System.currentTimeMillis()
                        listener?.onConnected()

                        sendScreenSize(serverScreenSize)
                        startHeartbeatChecker()

                        clientJob = launch {
                            handleClient()
                        }
                    } catch (e: Exception) {
                        println("Accept error: ${e.message}")
                        break
                    }
                }
            } catch (e: Exception) {
                println("Server start error: ${e.message}")
                _connectionState.value = false
                listener?.onError("Server error: ${e.message}")
            }
        }
    }

    private suspend fun handleClient() {
        try {
            while (clientSocket?.isConnected == true && dataInputStream != null) {
                val messageType = dataInputStream?.readInt() ?: break

                when (messageType) {
                    Constants.MESSAGE_TYPE_TOUCH_EVENT -> {
                        val event = readTouchEvent()
                        listener?.onTouchEvent(event)
                    }
                    Constants.MESSAGE_TYPE_HEARTBEAT -> {
                        println("Received heartbeat")
                        lastHeartbeatTime = System.currentTimeMillis()
                    }
                }
            }
        } catch (e: Exception) {
            println("Client handle error: ${e.message}")
        } finally {
            _connectionState.value = false
            listener?.onDisconnected()
        }
    }

    private fun readTouchEvent(): TouchEvent {
        val action = dataInputStream?.readInt() ?: 0
        val x = dataInputStream?.readFloat() ?: 0f
        val y = dataInputStream?.readFloat() ?: 0f
        val pointerId = dataInputStream?.readInt() ?: 0
        val pressure = dataInputStream?.readFloat() ?: 1f

        return TouchEvent(action, x, y, pointerId, pressure)
    }

    fun sendScreenSize(screenSize: ScreenSize) {
        serverScope.launch {
            try {
                dataOutputStream?.writeInt(Constants.MESSAGE_TYPE_SCREEN_SIZE)
                dataOutputStream?.writeInt(screenSize.width)
                dataOutputStream?.writeInt(screenSize.height)
                dataOutputStream?.flush()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun stopServer() {
        serverScope.launch {
            clientJob?.cancel()
            closeClientConnection()
            try {
                serverSocket?.close()
                _connectionState.value = false
                listener?.onDisconnected()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun closeClientConnection() {
        heartbeatJob?.cancel()
        heartbeatJob = null
        try {
            dataInputStream?.close()
            dataOutputStream?.close()
            clientSocket?.close()
        } catch (e: Exception) {
            // Ignore
        }
        dataInputStream = null
        dataOutputStream = null
        clientSocket = null
    }

    private fun startHeartbeatChecker() {
        heartbeatJob = serverScope.launch {
            while (isActive) {
                delay(Constants.HEARTBEAT_INTERVAL)
                val now = System.currentTimeMillis()
                if (now - lastHeartbeatTime > Constants.HEARTBEAT_INTERVAL * 3) {
                    println("Heartbeat timeout, disconnecting client")
                    clientJob?.cancel()
                    closeClientConnection()
                    _connectionState.value = false
                    listener?.onDisconnected()
                    break
                }
            }
        }
    }

    fun setConnectionListener(listener: ConnectionListener?) {
        this.listener = listener
    }

    fun cleanup() {
        clientJob?.cancel()
        closeClientConnection()
        try {
            serverSocket?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        serverSocket = null
        _connectionState.value = false
        listener = null
        serverScope.cancel()
    }
}
