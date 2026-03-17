package org.messanger.project.models

import kotlinx.serialization.Serializable

@Serializable
data class ChatSummary(
    val id: String,
    val title: String
)