package com.example.myapplication.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.MotionEvent
import android.view.accessibility.AccessibilityEvent
import com.example.myapplication.MyApplication

class TouchAccessibilityService : AccessibilityService() {
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        MyApplication.accessibilityService = this
        
        val info = serviceInfo.apply {
            eventTypes = android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK
            feedbackType = android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK
            flags = (android.accessibilityservice.AccessibilityServiceInfo.DEFAULT or
                    android.accessibilityservice.AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION_MODE or
                    android.accessibilityservice.AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS)
        }
        serviceInfo = info
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    }
    
    override fun onInterrupt() {
    }
    
    override fun onDestroy() {
        super.onDestroy()
        MyApplication.accessibilityService = null
    }
    
    fun executeTouch(x: Float, y: Float, action: Int) {
        val path = Path().apply { moveTo(x, y) }
        
        val stroke = GestureDescription.StrokeDescription(path, 0, 10)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                dispatchGesture(gesture, object : GestureResultCallback() {
                    override fun onCompleted(gestureDescription: GestureDescription) {
                        super.onCompleted(gestureDescription)
                    }
                    
                    override fun onCancelled(gestureDescription: GestureDescription) {
                        super.onCancelled(gestureDescription)
                    }
                }, null)
            }
            MotionEvent.ACTION_MOVE -> {
                dispatchGesture(gesture, null, null)
            }
        }
    }
    
    fun executeSwipe(fromX: Float, fromY: Float, toX: Float, toY: Float) {
        val path = Path().apply {
            moveTo(fromX, fromY)
            lineTo(toX, toY)
        }
        
        val stroke = GestureDescription.StrokeDescription(path, 0, 200)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        
        dispatchGesture(gesture, null, null)
    }
}
