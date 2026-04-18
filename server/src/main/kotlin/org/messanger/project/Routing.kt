package org.messanger.project


import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import org.messanger.project.auth.authRoutes
import org.messanger.project.models.CreateDmRequest
import org.messanger.project.services.AuthService
import org.messanger.project.services.ChatService
import org.messanger.project.ws.ChatWsHandler
import kotlin.time.Duration.Companion.seconds

fun Application.routingModule(authService: AuthService,
                              chatService: ChatService,
                              wsHandler: ChatWsHandler) {
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
        authRoutes(authService)


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
                if (query.length !in 1..32){
                    call.respond(HttpStatusCode.BadRequest, "Query must be 1-32 chars")
                    return@get
                }
                val users = chatService.searchUsers(query, currentUserId)
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

                val chat = chatService.getOrCreateDm(currentUserId, otherUserId)
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

                val chats = chatService.getUserChats(userId)
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
                wsHandler.handle(userId, this)
            }
        }


    }
}

