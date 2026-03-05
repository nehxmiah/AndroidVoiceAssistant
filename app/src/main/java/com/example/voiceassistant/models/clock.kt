package com.example.voiceassistant.models

import java.text.SimpleDateFormat
import java.util.*

class Clock {

    fun getCurrentTime(format12hr: Boolean = true): String {
        val now = Date()
        val format = if (format12hr) {
            SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
        } else {
            SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        }
        return format.format(now)
    }

    fun getCurrentDate(fullFormat: Boolean = true): String {
        val now = Date()
        val format = if (fullFormat) {
            SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault())
        } else {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        }
        return format.format(now)
    }

    fun getTimestamp(): String {
        val now = Date()
        val format = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return format.format(now)
    }

    fun getTimeComponents(): Map<String, Int> {
        val calendar = Calendar.getInstance()
        return mapOf(
            "hour" to calendar.get(Calendar.HOUR_OF_DAY),
            "minute" to calendar.get(Calendar.MINUTE),
            "second" to calendar.get(Calendar.SECOND),
            "day" to calendar.get(Calendar.DAY_OF_MONTH),
            "month" to calendar.get(Calendar.MONTH) + 1,
            "year" to calendar.get(Calendar.YEAR)
        )
    }
}