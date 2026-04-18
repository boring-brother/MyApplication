package com.example.myapplication.util

import android.content.Context
import android.graphics.Point
import android.view.WindowManager
import com.example.myapplication.model.ScreenSize

object ScreenUtils {
    
    fun getScreenSize(context: Context): ScreenSize {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        val point = Point()
        windowManager?.defaultDisplay?.getSize(point)
        return ScreenSize(width = point.x, height = point.y)
    }
    
    fun getRealScreenSize(context: Context): ScreenSize {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        val point = Point()
        windowManager?.defaultDisplay?.getRealSize(point)
        return ScreenSize(width = point.x, height = point.y)
    }
}
