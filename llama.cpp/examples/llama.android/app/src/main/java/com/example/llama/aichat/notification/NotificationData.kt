package com.example.llama.aichat.notification

data class NotificationData(
    val packageName: String,
    val appName: String,
    val title: String? = null,
    val text: String? = null,
    val subText: String? = null,
    val sender: String? = null,
    val category: String? = null,
    val notificationKey: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isOngoing: Boolean = false,
    val isIncomingCall: Boolean = false
)
