package org.messanger.project.services

import org.messanger.project.database.ChatRepository
import org.messanger.project.models.ChatSummary
import org.messanger.project.models.UserSummary

class ChatService(private val chatRepo: ChatRepository) {
    suspend fun getOrCreateDm(currentUserId: String, otherUserId: String): ChatSummary?{
        val chatId = buildChatIdForDM(currentUserId, otherUserId)
        if (!chatRepo.chatExists(chatId)){
            chatRepo.createDMChat(
                chatId = chatId,
                userA = currentUserId,
                userB = otherUserId
            )
        }
        return chatRepo.getUserChatById(currentUserId, chatId)
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