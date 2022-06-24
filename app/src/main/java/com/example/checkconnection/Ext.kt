package com.example.checkconnection

import java.text.SimpleDateFormat
import java.util.*

fun getDateTime(): String {
    val format = "HH:mm:ss" // you can add the format you need
//        val format = "dd MMM yyyy HH:mm:ss" // you can add the format you need
    val sdf = SimpleDateFormat(format, Locale.getDefault()) // default local
//        sdf.timeZone = TimeZone.getDefault() // set anytime zone you need
    return sdf.format(Date())
}
