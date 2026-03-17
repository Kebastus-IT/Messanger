package org.messanger.project

data class UserSession(
    val token: String,
    val userId: String,
    val login: String,
    val displayName: String
)

data class UiChatMessage(
    val serverMsgId: Long?,
    val fromUserId: String,
    val text: String,
    val createdAt: String?,
    val isMine: Boolean
)