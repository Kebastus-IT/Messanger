package org.messanger.project.models

import kotlinx.serialization.Serializable

@Serializable
data class ChatSummary(
    val id: String,
    val displayTitle: String,
    val type: String
)
@Serializable
data class UserSummary(
    val id: String,
    val login: String,
    val displayName: String
)

@Serializable
data class CreateDmRequest(
    val otherUserId: String
)

@Serializable
data class RawChat(
    val id: String,
    val displayTitle: String,
    val type: String
){
}