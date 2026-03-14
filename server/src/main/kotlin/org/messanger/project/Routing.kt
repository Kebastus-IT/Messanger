package org.messanger.project


import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import org.messanger.project.auth.JwtService
import org.messanger.project.auth.authRoutes
import org.messanger.project.ws.handleChatWs
import kotlin.time.Duration.Companion.seconds

fun Application.routingModule() {
    install(ContentNegotiation) {
        json()
    }
    install(WebSockets){
        pingPeriod = 30.seconds
        timeout = 15.seconds
        maxFrameSize = 64 * 1024
        masking = false

    }

    val jwtSecret = environment.config.property("jwt.secret").getString()
    val jwtIssuer = environment.config.property("jwt.issuer").getString()
    val jwtAudience = environment.config.property("jwt.audience").getString()

    val jwtService = JwtService(
        secret = jwtSecret,
        issuer = jwtIssuer,
        audience = jwtAudience
    )
    routing {
        authRoutes(jwtService)

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

