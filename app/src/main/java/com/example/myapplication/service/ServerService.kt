package com.example.myapplication.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.myapplication.R
import com.example.myapplication.model.ScreenSize
import com.example.myapplication.model.TouchEvent
import com.example.myapplication.network.ConnectionListener
import com.example.myapplication.network.ServerSocketManager
import com.example.myapplication.ui.ServerActivity
import com.example.myapplication.util.Constants
import com.example.myapplication.util.ScreenUtils

class ServerService : Service(), ConnectionListener {
    
    private val binder = LocalBinder()
    private var serverSocketManager: ServerSocketManager? = null
    private var serverScreenSize: ScreenSize? = null
    
    private var eventListener: ServerEventListener? = null
    
    interface ServerEventListener {
        fun onClientConnected()
        fun onClientDisconnected()
        fun onReceiveTouchEvent(event: TouchEvent)
        fun onError(message: String)
    }
    
    inner class LocalBinder : Binder() {
        fun getService(): ServerService = this@ServerService
    }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        serverScreenSize = ScreenUtils.getRealScreenSize(this)
        startForegroundService()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "ACTION_STOP") {
            stopServer()
            return START_NOT_STICKY
        }
        startServer()
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder {
        return binder
    }
    
    override fun onDestroy() {
        serverSocketManager?.cleanup()
        serverSocketManager = null
        eventListener = null
        super.onDestroy()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                Constants.CHANNEL_ID,
                "RemoteTouch Server Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "RemoteTouch Server is running"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun startForegroundService() {
        val stopIntent = Intent(this, ServerService::class.java).apply {
            action = "ACTION_STOP"
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val openIntent = Intent(this, ServerActivity::class.java)
        val openPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val notification = NotificationCompat.Builder(this, Constants.CHANNEL_ID)
            .setContentTitle("RemoteTouch Server")
            .setContentText("Running and waiting for connections")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setContentIntent(openPendingIntent)
            .addAction(
                android.R.drawable.ic_delete,
                "Stop",
                stopPendingIntent
            )
            .build()
        
        startForeground(Constants.SERVICE_NOTIFICATION_ID, notification)
    }
    
    private fun startServer() {
        serverScreenSize?.let { screenSize ->
            serverSocketManager = ServerSocketManager(screenSize)
            serverSocketManager?.setConnectionListener(this)
            serverSocketManager?.startServer(Constants.DEFAULT_PORT)
        }
    }
    
    fun stopServer() {
        serverSocketManager?.cleanup()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }
    
    fun setEventListener(listener: ServerEventListener?) {
        eventListener = listener
    }
    
    override fun onConnected() {
        eventListener?.onClientConnected()
    }
    
    override fun onDisconnected() {
        eventListener?.onClientDisconnected()
    }
    
    override fun onError(message: String) {
        eventListener?.onError(message)
    }
    
    override fun onTouchEvent(event: TouchEvent) {
        eventListener?.onReceiveTouchEvent(event)
    }
    
    override fun onScreenSizeReceived(screenSize: ScreenSize) {
    }
    
    override fun onClientConnected(clientInfo: String) {
    }
}
