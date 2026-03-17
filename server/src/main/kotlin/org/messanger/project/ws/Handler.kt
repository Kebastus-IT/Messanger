package org.messanger.project.ws

import io.ktor.server.websocket.*
import io.ktor.websocket.Frame
import org.messanger.project.database.ChatRepository
import org.messanger.project.protocol.*
import java.util.concurrent.ConcurrentHashMap

private val online = ConcurrentHashMap<String, DefaultWebSocketServerSession>()


suspend fun DefaultWebSocketServerSession.handleChatWs(userId: String) {
    online[userId] = this
    sendEvent(Connected(userId))

    try {
        for(frame in incoming) {
           val textFrame = frame as? Frame.Text ?: continue
            val event = runCatching { decodeClientEvent(textFrame) }
                .getOrElse {
                    sendEvent(ErrorEvent("BAD_JSON", it.localizedMessage ?: "Bad message"))
                    continue
                }
            when(event) {
             is Join -> {
                     val allowed = ChatRepository.isMember(event.chatId,userId)
                 if (!allowed) {
                     sendEvent(ErrorEvent("IMPOSTER", "You are not a member of ${event.chatId}"))
                     continue
                 }
                 sendEvent(JoinedChat(event.chatId))

                 val recentMessages = ChatRepository
                     .getRecentMessages(event.chatId, limit = 50)
                     .map {
                         ChatMessage(
                             chatId = it.chatId,
                             senderUserId = it.senderUserId,
                             senderDisplayName = it.senderDisplayName,
                             text = it.text,
                             serverMsgId = it.id
                         )
                     }

                 sendEvent(
                     RecentMessages(
                         chatId = event.chatId,
                         items = recentMessages
                     )
                 )
             }
                is SendMessage -> {
                    val messageId = ChatRepository.saveMessage(event.chatId, userId, event.text)
                    val memberIds = ChatRepository.getChatMemberIds(event.chatId)
                    val senderDisplayName = ChatRepository.getUserDisplayName(userId) ?: userId

                    for (memberId in memberIds) {
                        online[memberId]?.sendEvent(
                            ChatMessage(
                                chatId = event.chatId,
                                senderUserId = userId,
                                senderDisplayName = senderDisplayName,
                                text = event.text,
                                serverMsgId = messageId
                            )
                        )
                    }
                }

            }


        }
    } finally {
        online.remove(userId)
    }
}