package com.example.myapplication.util

object Constants {
    const val DEFAULT_PORT = 8899
    const val HEARTBEAT_INTERVAL = 5000L
    const val RECONNECT_DELAY = 3000L
    const val CONNECTION_TIMEOUT = 10000L
    
    const val MESSAGE_TYPE_CONNECT_REQUEST = 0x01
    const val MESSAGE_TYPE_CONNECT_RESPONSE = 0x02
    const val MESSAGE_TYPE_TOUCH_EVENT = 0x03
    const val MESSAGE_TYPE_HEARTBEAT = 0x04
    const val MESSAGE_TYPE_SCREEN_SIZE = 0x05
    const val MESSAGE_TYPE_SCREENSHOT = 0x06
    
    const val PREF_NAME = "remotetouch_prefs"
    const val PREF_SELECTED_MODE = "selected_mode"
    const val PREF_SERVER_IP = "server_ip"
    const val PREF_SERVER_PORT = "server_port"
    const val PREF_BACKGROUND_COLOR = "background_color"
    
    const val CHANNEL_ID = "remotetouch_service_channel"
    const val SERVICE_NOTIFICATION_ID = 1001
    
    enum class AppMode {
        SERVER,
        CLIENT
    }
}
