package org.messanger.project

import io.ktor.server.websocket.*
import io.ktor.websocket.*

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