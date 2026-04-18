package org.messanger.project.ws

import io.ktor.server.application.log
import io.ktor.server.websocket.*
import io.ktor.websocket.Frame
import org.messanger.project.database.ChatRepository
import org.messanger.project.protocol.*
import java.util.concurrent.ConcurrentHashMap

private val online = ConcurrentHashMap<String, DefaultWebSocketServerSession>()


suspend fun DefaultWebSocketServerSession.handleChatWs(userId: String, chatRepo: ChatRepository) {
    online[userId] = this
    val countOnline = online.size
    sendEvent(Connected(userId))

    try {
        for(frame in incoming) {
           val textFrame = frame as? Frame.Text ?: continue
            val event = runCatching { decodeClientEvent(textFrame) }
                .getOrElse {
                    call.application.log.error("Failed to decode client WS event",it)
                    sendEvent(ErrorEvent(
                        code ="BAD_JSON",
                        message = "Bad message"))
                    continue
                }
            when(event) {
             is Join -> {
                     val allowed = chatRepo.isMember(event.chatId,userId)
                 if (!allowed) {
                     sendEvent(ErrorEvent("NOT_A_MEMBER", "You are not a member of ${event.chatId}"))
                     continue
                 }
                 sendEvent(JoinedChat(event.chatId))

                 val recentMessages = chatRepo
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
                    val text = event.text.trim()
                    if (text.isEmpty()) {
                        sendEvent(ErrorEvent("EMPTY_MESSAGE", "Message cannot be empty"))
                        continue
                    }
                    if(text.length > 4000) {
                        sendEvent(ErrorEvent("MESSAGE_TOO_LONG", "Message must be 4000 characters or less"))
                        continue
                    }
                    val allowed = chatRepo.isMember(event.chatId,userId)
                    if (!allowed) {
                        sendEvent(ErrorEvent("NOT_A_MEMBER", "You are not a member of ${event.chatId}"))
                        continue
                    }



                    val messageId = chatRepo.saveMessage(event.chatId, userId, text)
                    val memberIds = chatRepo.getChatMemberIds(event.chatId)
                    val senderDisplayName = chatRepo.getUserDisplayName(userId) ?: userId

                    for (memberId in memberIds) {
                        online[memberId]?.sendEvent(
                            ChatMessage(
                                chatId = event.chatId,
                                senderUserId = userId,
                                senderDisplayName = senderDisplayName,
                                text = text,
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