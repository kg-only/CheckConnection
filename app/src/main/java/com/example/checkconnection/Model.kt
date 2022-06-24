package com.example.checkconnection

data class Model(
    var url: String,
    var timeoutRequest: Long? = 3000,
    var lastTimeRequest: Long = 0,
    var responseCode: Int = 200,
    var timeoutError: Int? = null
) {
    fun requestTime(): Boolean {
        val currentTime = System.currentTimeMillis()
        return currentTime - lastTimeRequest > timeoutRequest!!
    }

    fun updateLastTime() {
        lastTimeRequest = System.currentTimeMillis()
    }
}
