package com.example.myapplication.model

data class ScreenSize(
    val width: Int,
    val height: Int
) {
    fun aspectRatio(): Float = width.toFloat() / height.toFloat()
    
    fun isEmpty(): Boolean = width <= 0 || height <= 0
}
