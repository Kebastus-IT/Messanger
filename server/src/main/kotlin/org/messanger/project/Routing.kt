package org.messanger.project


import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.*
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.CloseReason
import io.ktor.websocket.close
import org.messanger.project.auth.JwtService
import org.messanger.project.auth.authRoutes
import org.messanger.project.database.ChatRepository
import org.messanger.project.models.CreateDmRequest
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
    configureSecurity()

    val jwtSecret = environment.config.property("jwt.secret").getString()
    val jwtIssuer = environment.config.property("jwt.issuer").getString()
    val jwtAudience = environment.config.property("jwt.audience").getString()

    val jwtService = JwtService(
        secret = jwtSecret,
        issuer = jwtIssuer,
        audience = jwtAudience
    )

    fun ApplicationCall.extractUserId(): String? {
        return principal<JWTPrincipal>()
            ?.payload
            ?.getClaim("userId")
            ?.asString()
    }
    routing {
        authRoutes(jwtService)

        val echoHandler = EchoHandler()

        webSocket("/echo") {
            echoHandler.handleEcho(this)
        }
        authenticate("auth-jwt") {
            get("/users/find") {
                val currentUserId = call.extractUserId()

                if (currentUserId.isNullOrBlank()) {
                    call.respond(
                        HttpStatusCode.Unauthorized,
                        "Missing userId in token"
                    )
                    return@get
                }
                val query = call.request.queryParameters["login"].orEmpty()
                val users = ChatRepository.findUser(
                    query = query,
                    ownId = currentUserId
                )
                call.respond(users)
            }
            post("/chats/dm"){
                val currentUserId = call.extractUserId()

                if (currentUserId.isNullOrBlank()) {
                    call.respond(HttpStatusCode.Unauthorized,
                        "Missing userId in token"
                    )
                    return@post
                }
                val request = call.receive<CreateDmRequest>()
                val otherUserId = request.otherUserId
                if(otherUserId == currentUserId){
                    call.respond(HttpStatusCode.BadRequest,
                        "Cannot create DM with yourself"
                    )
                    return@post
                }
                val chatId = ChatRepository.buildChatIdForDM(currentUserId,otherUserId)

                if (!ChatRepository.chatExists(chatId)){
                    ChatRepository.createDMChat(
                        chatId = chatId,
                        userA = currentUserId,
                        userB = otherUserId
                    )
                }
                val chat = ChatRepository.getUserChatById(currentUserId, chatId)
                if (chat == null){
                    call.respond(HttpStatusCode.InternalServerError,
                        "Failed to load DM chat"
                    )
                    return@post
                }
                call.respond(chat)

            }
            get("/chats") {
                val userId = call.extractUserId()

                if (userId.isNullOrBlank()) {
                    call.respond(
                        HttpStatusCode.Unauthorized,
                        "Missing userId in token"
                    )
                    return@get
                }

                val chats = ChatRepository.getUserChats(userId)
                call.respond(chats)
            }
            webSocket("/ws"){
                val userId = call.extractUserId()

                if (userId.isNullOrBlank()) {
                    close(
                        CloseReason(
                                CloseReason.Codes.VIOLATED_POLICY,
                            "Missing userId in token"
                        )
                    )
                    return@webSocket
                }
                handleChatWs(userId)
            }
        }


    }
}

