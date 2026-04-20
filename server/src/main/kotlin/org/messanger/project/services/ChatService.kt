package org.messanger.project.services

import org.messanger.project.database.ChatRepository
import org.messanger.project.models.ChatSummary
import org.messanger.project.models.UserSummary

sealed interface CreateDmResult {
    data class Success(val chat: ChatSummary) : CreateDmResult
    data object OtherUserNotFound : CreateDmResult
    data object CannotDmSelf : CreateDmResult
}

class ChatService(private val chatRepo: ChatRepository) {
    suspend fun getOrCreateDm(currentUserId: String, otherUserId: String): CreateDmResult {
        if(otherUserId.isBlank()) return CreateDmResult.OtherUserNotFound
        if (otherUserId == currentUserId) return CreateDmResult.CannotDmSelf
        if (!chatRepo.userExists(otherUserId)) return CreateDmResult.OtherUserNotFound
        val chatId = buildChatIdForDM(currentUserId, otherUserId)
        val chat = chatRepo.ensureDmAndGet(chatId, currentUserId, otherUserId) ?: error("ensureDmAndGet returned right after insert -- invariant broken")
        return CreateDmResult.Success(chat)
     }
    private fun buildChatIdForDM(userA : String, userB: String): String{
        val sorted = listOf(userA,userB).sorted()
        return "dm:${sorted[0]}:${sorted[1]}"
    }
    suspend fun searchUsers(query: String, ownId: String): List<UserSummary>{
        val trim = query.trim()
        if (trim.isEmpty()) return emptyList()
        return chatRepo.searchByLogin(trim, ownId)
    }
    suspend fun getUserChats(userId: String): List<ChatSummary>{
        val rawChats = chatRepo.getRawUserChats(userId)

        val dmChatIds = rawChats
            .filter { it.type == "DM" }
            .map { it.id }

        val dmTitles = chatRepo.getDmTitles(
            currentUserId = userId,
            dmChatIds = dmChatIds
        )
        return rawChats.map { rawChat ->
            val displayTitle = if (rawChat.type == "DM") {
                dmTitles[rawChat.id] ?: rawChat.displayTitle
            } else {
                rawChat.displayTitle
            }

            ChatSummary(
                id = rawChat.id,
                displayTitle = displayTitle,
                type = rawChat.type
            )
        }
    }
}