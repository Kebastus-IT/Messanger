package org.messanger.project.protocol

import kotlinx.serialization.*

@Serializable
sealed class ClientEvent

@Serializable
@SerialName("join")
data class Join(
    val chatId: String
) : ClientEvent()

@Serializable
@SerialName("message")
data class SendMessage(
    val chatId: String,
    val text: String
) : ClientEvent()
