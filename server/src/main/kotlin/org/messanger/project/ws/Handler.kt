package org.messanger.project.ws

import io.ktor.server.application.log
import io.ktor.server.websocket.*
import io.ktor.websocket.Frame
import org.messanger.project.database.ChatRepository
import org.messanger.project.protocol.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet


class ChatWsHandler(private val chatRepo: ChatRepository) {
    private val online = ConcurrentHashMap<String, CopyOnWriteArraySet<DefaultWebSocketServerSession>>()

    suspend fun handle(userId: String, session: DefaultWebSocketServerSession) = with(session) {
        addSession(userId, this)
        val countOnline = online.size
        sendEvent(Connected(userId))

        try {
            for (frame in incoming) {
                val textFrame = frame as? Frame.Text ?: continue
                val event = runCatching { decodeClientEvent(textFrame) }
                    .getOrElse {
                        call.application.log.error("Failed to decode client WS event", it)
                        sendEvent(
                            ErrorEvent(
                                code = "BAD_JSON",
                                message = "Bad message"
                            )
                        )
                        continue
                    }
                when (event) {
                    is Join -> handleJoin(userId, event)
                    is SendMessage -> handleSend(userId, event)
                }


            }
        } finally {
            removeSession(userId, this)
        }
    }

    private fun addSession(userId: String, session: DefaultWebSocketServerSession) {
        online.compute(userId) { _, existing ->
            (existing ?: CopyOnWriteArraySet()).also { it.add(session) }
        }
    }

    private fun removeSession(userId: String, session: DefaultWebSocketServerSession) {
        online.compute(userId) { _, existing ->
            if (existing == null) return@compute null
            existing.remove(session)
            if (existing.isEmpty()) null else existing
        }
    }
    private suspend fun DefaultWebSocketServerSession.handleJoin(userId: String, event: Join) {
        val allowed = chatRepo.isMember(event.chatId, userId)
        if (!allowed) {
            sendEvent(ErrorEvent("NOT_A_MEMBER", "You are not a member of ${event.chatId}"))
            return
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
        sendEvent(RecentMessages(chatId = event.chatId, items = recentMessages))
    }
    private suspend fun DefaultWebSocketServerSession.handleSend(userId: String, event: SendMessage) {
        val text = event.text.trim()
        if (text.isEmpty()) {
            sendEvent(ErrorEvent("EMPTY_MESSAGE", "Message cannot be empty"))
            return
        }
        if (text.length > 4000) {
            sendEvent(ErrorEvent("MESSAGE_TOO_LONG", "Message must be 4000 characters or less"))
            return
        }
        val allowed = chatRepo.isMember(event.chatId, userId)
        if (!allowed) {
            sendEvent(ErrorEvent("NOT_A_MEMBER", "You are not a member of ${event.chatId}"))
            return
        }


        val messageId = chatRepo.saveMessage(event.chatId, userId, text)
        val memberIds = chatRepo.getChatMemberIds(event.chatId)
        val senderDisplayName = chatRepo.getUserDisplayName(userId) ?: userId

        val outgoing = ChatMessage(
            chatId = event.chatId,
            senderUserId = userId,
            senderDisplayName = senderDisplayName,
            text = text,
            serverMsgId = messageId
        )
        for (memberId in memberIds) {
            val sessions = online[memberId] ?: continue
            for (s in sessions) {
                try {
                    s.sendEvent(outgoing)
                } catch (e: Throwable) {
                    call.application.log.warn("Failed to deliver message to $memberId", e)
                }
            }
        }
    }
}
