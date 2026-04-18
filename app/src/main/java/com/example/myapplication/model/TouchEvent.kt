package com.example.myapplication.model

data class TouchEvent(
    val action: Int,
    val x: Float,
    val y: Float,
    val pointerId: Int = 0,
    val pressure: Float = 1f,
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        const val ACTION_DOWN = 0
        const val ACTION_UP = 1
        const val ACTION_MOVE = 2
        const val ACTION_POINTER_DOWN = 5
        const val ACTION_POINTER_UP = 6
        const val ACTION_CANCEL = 3
    }
}
