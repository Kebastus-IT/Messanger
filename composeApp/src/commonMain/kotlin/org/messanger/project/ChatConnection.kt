package org.messanger.project

import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.messanger.project.protocol.ChatMessage
import org.messanger.project.protocol.ClientEvent
import org.messanger.project.protocol.ErrorEvent
import org.messanger.project.protocol.Join
import org.messanger.project.protocol.RecentMessages
import org.messanger.project.protocol.SendMessage
import org.messanger.project.protocol.ServerEvent
import org.messanger.project.protocol.WsJson

class ChatConnection(
    private val client: HttpClient,
    private val baseUrl: String
) {
    private var session: WebSocketSession? = null
    private var listenJob: Job? = null

    suspend fun connect(
        token: String,
        chatId: String,
        scope: CoroutineScope,
        onEvent: (ServerEvent) -> Unit,
        onError: (String) -> Unit
    ) {
        val wsUrl = baseUrl
            .replace("http://", "ws://")
            .replace("https://", "wss://") + "/ws"

        try {
            val newSession = client.webSocketSession {
                url(wsUrl)
                header(HttpHeaders.Authorization, "Bearer $token")
            }

            session = newSession

            sendEvent(Join(chatId = chatId))

            listenJob = scope.launch {
                try {
                    for (frame in newSession.incoming) {
                        if (frame is Frame.Text) {
                            val text = frame.readText()
                            val event = WsJson.decodeFromString<ServerEvent>(text)
                            onEvent(event)
                        }
                    }
                } catch (e: Exception) {
                    onError(e.message ?: "WebSocket receive error")
                }
            }
        } catch (e: Exception) {
            onError(e.message ?: "WebSocket connection failed")
        }
    }

    suspend fun sendMessage(
        chatId: String,
        text: String
    ) {
        sendEvent(
            SendMessage(
                chatId = chatId,
                text = text
            )
        )
    }

    suspend fun disconnect() {
        listenJob?.cancel()
        listenJob = null
        session?.close()
        session = null
    }

    private suspend fun sendEvent(event: ClientEvent) {
        val currentSession = session ?: return
        val jsonText = WsJson.encodeToString(ClientEvent.serializer(), event)
        currentSession.send(Frame.Text(jsonText))
    }
}