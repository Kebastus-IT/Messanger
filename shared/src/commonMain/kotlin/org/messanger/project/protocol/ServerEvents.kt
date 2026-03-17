package org.messanger.project.protocol

import kotlinx.serialization.*

@Serializable
sealed class ServerEvent

@Serializable
@SerialName("connected")
data class Connected(
    val userId: String
) : ServerEvent()

@Serializable
@SerialName("joined_chat")
data class JoinedChat(
    val chatId: String
) : ServerEvent()

@Serializable
@SerialName("recent_messages")
data class RecentMessages(
    val chatId: String,
    val items: List<ChatMessage>
) : ServerEvent()


@Serializable
@SerialName("chat_message")
data class ChatMessage(
    val chatId: String,
    val fromUserId: String,
    val text: String,
    val serverMsgId: Long? = null,
    val createdAt: String? = null
) : ServerEvent()

@Serializable
@SerialName("error")
data class ErrorEvent(
    val code: String,
    val message: String
) : ServerEvent()