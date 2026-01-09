package com.neuralquark.hackernewsclient.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object TimeUtils {
    
    /**
     * Convert Unix timestamp to relative time string (e.g., "2 hours ago")
     */
    fun getRelativeTimeSpan(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - (timestamp * 1000) // HN uses seconds, we need milliseconds
        
        return when {
            diff < TimeUnit.MINUTES.toMillis(1) -> "just now"
            diff < TimeUnit.HOURS.toMillis(1) -> {
                val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
                "$minutes ${if (minutes == 1L) "minute" else "minutes"} ago"
            }
            diff < TimeUnit.DAYS.toMillis(1) -> {
                val hours = TimeUnit.MILLISECONDS.toHours(diff)
                "$hours ${if (hours == 1L) "hour" else "hours"} ago"
            }
            diff < TimeUnit.DAYS.toMillis(30) -> {
                val days = TimeUnit.MILLISECONDS.toDays(diff)
                "$days ${if (days == 1L) "day" else "days"} ago"
            }
            diff < TimeUnit.DAYS.toMillis(365) -> {
                val months = TimeUnit.MILLISECONDS.toDays(diff) / 30
                "$months ${if (months == 1L) "month" else "months"} ago"
            }
            else -> {
                val years = TimeUnit.MILLISECONDS.toDays(diff) / 365
                "$years ${if (years == 1L) "year" else "years"} ago"
            }
        }
    }
    
    /**
     * Format timestamp to readable date
     */
    fun formatDate(timestamp: Long): String {
        val date = Date(timestamp * 1000)
        val format = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        return format.format(date)
    }
    
    /**
     * Check if current time is within notification hours
     * @param startHour Start hour (inclusive) in 24-hour format
     * @param endHour End hour (exclusive) in 24-hour format
     */
    fun isWithinNotificationHours(startHour: Int = 10, endHour: Int = 18): Boolean {
        val calendar = java.util.Calendar.getInstance()
        val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        return hour in startHour until endHour
    }
    
    /**
     * Get delay until next notification window
     * @param startHour Start hour of notification window in 24-hour format
     */
    fun getDelayUntilNotificationWindow(startHour: Int = 10): Long {
        val calendar = java.util.Calendar.getInstance()
        val currentHour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        
        if (currentHour < startHour) {
            // Schedule for startHour today
            calendar.set(java.util.Calendar.HOUR_OF_DAY, startHour)
            calendar.set(java.util.Calendar.MINUTE, 0)
            calendar.set(java.util.Calendar.SECOND, 0)
        } else {
            // Schedule for startHour tomorrow
            calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
            calendar.set(java.util.Calendar.HOUR_OF_DAY, startHour)
            calendar.set(java.util.Calendar.MINUTE, 0)
            calendar.set(java.util.Calendar.SECOND, 0)
        }
        
        return calendar.timeInMillis - System.currentTimeMillis()
    }
    
    /**
     * Format hour to display string (e.g., 10 -> "10:00 AM", 18 -> "6:00 PM")
     */
    fun formatHour(hour: Int): String {
        return when {
            hour == 0 -> "12:00 AM"
            hour < 12 -> "$hour:00 AM"
            hour == 12 -> "12:00 PM"
            else -> "${hour - 12}:00 PM"
        }
    }
}
