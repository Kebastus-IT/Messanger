package org.messanger.project


import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import io.ktor.server.websocket.webSocket
import org.messanger.project.ws.handleChatWs
import kotlin.time.Duration.Companion.seconds

fun Application.webSocketModule() {
    install(WebSockets){
        pingPeriod = 30.seconds
        timeout = 15.seconds
        maxFrameSize = 64 * 1024
        masking = false

    }

    routing {
        val echoHandler = EchoHandler()
        webSocket("/echo") {
            echoHandler.handleEcho(this)
        }
        webSocket("/ws"){
            val userId = "u1"
            handleChatWs(userId)
        }
    }
}

