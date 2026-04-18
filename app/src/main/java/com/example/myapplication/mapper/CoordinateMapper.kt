package com.example.myapplication.mapper

import com.example.myapplication.model.ScreenSize

class CoordinateMapper(
    private val clientScreen: ScreenSize,
    private var serverScreen: ScreenSize? = null
) {
    
    fun setServerScreenSize(size: ScreenSize) {
        serverScreen = size
    }
    
    fun mapToServer(clientX: Float, clientY: Float): Pair<Float, Float>? {
        val server = serverScreen ?: return null
        
        if (clientScreen.isEmpty() || server.isEmpty()) {
            return null
        }
        
        val serverX = (clientX / clientScreen.width) * server.width
        val serverY = (clientY / clientScreen.height) * server.height
        
        return Pair(serverX.coerceIn(0f, server.width.toFloat()), serverY.coerceIn(0f, server.height.toFloat()))
    }
    
    fun mapToClient(serverX: Float, serverY: Float): Pair<Float, Float>? {
        val client = clientScreen
        
        if (client.isEmpty() || serverScreen?.isEmpty() == true) {
            return null
        }
        
        val clientX = (serverX / (serverScreen?.width ?: 1)) * client.width
        val clientY = (serverY / (serverScreen?.height ?: 1)) * client.height
        
        return Pair(clientX.coerceIn(0f, client.width.toFloat()), clientY.coerceIn(0f, client.height.toFloat()))
    }
    
    fun isReady(): Boolean = serverScreen != null
}
