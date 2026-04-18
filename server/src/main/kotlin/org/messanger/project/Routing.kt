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
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respondText
import org.messanger.project.auth.PasswordHasher
import org.messanger.project.database.AuthRepository

fun Application.routingModule(authRepo: AuthRepository,
                              chatRepo: ChatRepository,
                              jwtService: JwtService,
                              passwordHasher: PasswordHasher) {
    install(ContentNegotiation) {
        json()
    }
    install(StatusPages) {
        exception<Throwable>{ call, cause ->
            call.application.log.error("Uncaught exception", cause)
            call.respondText(text ="500: Internal server error", status = HttpStatusCode.InternalServerError)
        }
    }
    install(WebSockets){
        pingPeriod = 30.seconds
        timeout = 15.seconds
        maxFrameSize = 64 * 1024
        masking = false

    }

    fun ApplicationCall.extractUserId(): String? {
        return principal<JWTPrincipal>()
            ?.payload
            ?.getClaim("userId")
            ?.asString()
    }
    routing {
        authRoutes(AuthRepository(),
            jwtService,
            passwordHasher)


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
                val query = call.request.queryParameters["login"].orEmpty().trim()
                if (query.length !in 1..32){
                    call.respond(HttpStatusCode.BadRequest, "Query must be 1-32 chars")
                    return@get
                }
                val users = chatRepo.findUser(
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
                val chatId = chatRepo.buildChatIdForDM(currentUserId,otherUserId)

                if (!chatRepo.chatExists(chatId)){
                    chatRepo.createDMChat(
                        chatId = chatId,
                        userA = currentUserId,
                        userB = otherUserId
                    )
                }
                val chat = chatRepo.getUserChatById(currentUserId, chatId)
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

                val chats = chatRepo.getUserChats(userId)
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
                handleChatWs(userId,chatRepo)
            }
        }


    }
}

