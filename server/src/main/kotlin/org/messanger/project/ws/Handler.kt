package org.messanger.project.ws

import io.ktor.server.websocket.*
import io.ktor.websocket.Frame
import org.messanger.project.protocol.*
import java.util.concurrent.ConcurrentHashMap

private val online = ConcurrentHashMap<String, DefaultWebSocketServerSession>()

private val chatMembers = mapOf(
    "room-general" to setOf("u1", "u2"),
    "dm-u1-u2" to setOf("u1", "u2")
)
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
                 val members = chatMembers[event.chatId]
                 if (members == null || userId !in members) {
                     sendEvent(ErrorEvent("IMPOSTER", "You are not a member of ${event.chatId}"))
                     continue
                 }
                     sendEvent(ChatMessage(event.chatId, "server", "joined ${event.chatId}"))

             }
                is SendMessage -> {
                    val members = chatMembers[event.chatId] ?: emptySet()
                    if (userId !in members) {
                        sendEvent(ErrorEvent("IMPOSTER", "You are not a member of ${event.chatId}"))
                        continue
                    }
                    for (m in members) {
                        online[m]?.sendEvent(ChatMessage(event.chatId, userId, event.text))
                    }
                }

            }


        }
    } finally {
        online.remove(userId)
    }
}