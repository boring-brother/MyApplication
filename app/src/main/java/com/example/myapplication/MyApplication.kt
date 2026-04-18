package com.example.myapplication

import android.app.Application
import com.example.myapplication.service.TouchAccessibilityService

class MyApplication : Application() {

    companion object {
        var accessibilityService: TouchAccessibilityService? = null
    }
}
