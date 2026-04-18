package com.example.myapplication.network

import com.example.myapplication.model.ScreenSize
import com.example.myapplication.model.TouchEvent
import com.example.myapplication.util.Constants
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetSocketAddress
import java.net.Socket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ClientSocketManager {

    private var socket: Socket? = null
    private var dataOutputStream: DataOutputStream? = null
    private var dataInputStream: DataInputStream? = null

    private val connectionScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val eventChannel = Channel<TouchEvent>(capacity = Channel.UNLIMITED)
    private var heartbeatJob: Job? = null

    private val _connectionState = MutableStateFlow(false)
    val connectionState: StateFlow<Boolean> = _connectionState

    private var listener: ConnectionListener? = null

    init {
        connectionScope.launch {
            for (event in eventChannel) {
                try {
                    dataOutputStream?.writeInt(Constants.MESSAGE_TYPE_TOUCH_EVENT)
                    dataOutputStream?.writeInt(event.action)
                    dataOutputStream?.writeFloat(event.x)
                    dataOutputStream?.writeFloat(event.y)
                    dataOutputStream?.writeInt(event.pointerId)
                    dataOutputStream?.writeFloat(event.pressure)
                    dataOutputStream?.flush()
                } catch (e: Exception) {
                    e.printStackTrace()
                    _connectionState.value = false
                    listener?.onError("Send failed: ${e.message}")
                    break
                }
            }
        }
    }

    fun connectToServer(ip: String, port: Int) {
        connectionScope.launch {
            try {
                _connectionState.value = false

                socket = Socket()
                socket?.connect(InetSocketAddress(ip, port), Constants.CONNECTION_TIMEOUT.toInt())

                dataOutputStream = DataOutputStream(socket?.getOutputStream())
                dataInputStream = DataInputStream(socket?.getInputStream())

                _connectionState.value = true
                listener?.onConnected()

                startHeartbeat()
                receiveData()
            } catch (e: Exception) {
                println("Connection error: ${e.message}")
                _connectionState.value = false
                listener?.onError("Connection failed: ${e.message}")
            }
        }
    }

    fun disconnect() {
        connectionScope.launch {
            heartbeatJob?.cancel()
            stopHeartbeat()
            try {
                dataInputStream?.close()
                dataOutputStream?.close()
                socket?.close()
                _connectionState.value = false
                listener?.onDisconnected()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun sendTouchEvent(event: TouchEvent) {
        eventChannel.trySend(event)
    }

    private fun startHeartbeat() {
        heartbeatJob = connectionScope.launch {
            while (isActive) {
                delay(Constants.HEARTBEAT_INTERVAL)
                try {
                    dataOutputStream?.writeInt(Constants.MESSAGE_TYPE_HEARTBEAT)
                    dataOutputStream?.flush()
                } catch (e: Exception) {
                    e.printStackTrace()
                    break
                }
            }
        }
    }

    private fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }

    private fun receiveData() {
        connectionScope.launch {
            try {
                while (socket?.isConnected == true && dataInputStream != null) {
                    val messageType = dataInputStream?.readInt() ?: continue

                    when (messageType) {
                        Constants.MESSAGE_TYPE_SCREEN_SIZE -> {
                            val width = dataInputStream?.readInt() ?: 0
                            val height = dataInputStream?.readInt() ?: 0
                            val screenSize = ScreenSize(width, height)
                            listener?.onScreenSizeReceived(screenSize)
                        }
                    }
                }
            } catch (e: Exception) {
                println("Receive error: ${e.message}")
                _connectionState.value = false
                listener?.onDisconnected()
            }
        }
    }

    fun setConnectionListener(listener: ConnectionListener?) {
        this.listener = listener
    }

    fun cleanup() {
        heartbeatJob?.cancel()
        try {
            dataInputStream?.close()
            dataOutputStream?.close()
            socket?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        eventChannel.close()
        _connectionState.value = false
        listener = null
        connectionScope.cancel()
    }
}
