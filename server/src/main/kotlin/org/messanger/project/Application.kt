package org.messanger.project

import io.ktor.server.application.*
import io.ktor.server.cio.*
import org.messanger.project.services.ChatService
import org.messanger.project.services.JwtService
import org.messanger.project.auth.PasswordHasher
import org.messanger.project.database.AuthRepository
import org.messanger.project.database.ChatRepository
import org.messanger.project.database.initDatabase
import org.messanger.project.services.AuthService
import org.messanger.project.ws.ChatWsHandler

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module(){
    initDatabase(environment.config)
    val jwtSecret = environment.config.property("jwt.secret").getString()
    val jwtAudience = environment.config.property("jwt.audience").getString()
    val jwtIssuer = environment.config.property("jwt.issuer").getString()
    val jwtRealm = environment.config.property("jwt.realm").getString()
    configureSecurity(jwtSecret, jwtIssuer, jwtAudience, jwtRealm)
    val passwordHasher = PasswordHasher()
    val authRepo = AuthRepository()
    val chatRepo = ChatRepository()
    val chatService = ChatService(chatRepo)
    val jwt = JwtService(
        secret = jwtSecret,
        audience = jwtAudience,
        issuer = jwtIssuer
    )
    val authService = AuthService(authRepo, passwordHasher, jwt)
    val wsHandler = ChatWsHandler(chatRepo)

    routingModule(authService, chatService, wsHandler)
}
