package org.messanger.project.ws



import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import org.messanger.project.protocol.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

suspend fun DefaultWebSocketServerSession.sendEvent(event: ServerEvent) {
    val text = WsJson.encodeToString<ServerEvent>(event)
    send(Frame.Text(text))
}
fun decodeClientEvent(frame: Frame.Text): ClientEvent =
    WsJson.decodeFromString<ClientEvent>(frame.readText())