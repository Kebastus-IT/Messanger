package org.messanger.project

import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import io.ktor.websocket.send

class EchoHandler {
    suspend fun handleEcho(session: DefaultWebSocketServerSession) = with(session) {
        send("connected")
        try {
            for (frame in incoming) {
                val text = (frame as? Frame.Text ?: continue).readText()
                send("echo: $text")
            }
        } finally {
            send("disconnected")
        }

    }
}