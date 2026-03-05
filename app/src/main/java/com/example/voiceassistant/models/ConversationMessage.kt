package com.example.voiceassistant.models

data class ConversationMessage(
    val message: String,
    val timestamp: String,
    val isUser: Boolean
)