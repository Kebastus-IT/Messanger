package org.messanger.project.connection

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.header
import io.ktor.client.request.url
import io.ktor.http.HttpHeaders
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.messanger.project.protocol.ClientEvent
import org.messanger.project.protocol.Join
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