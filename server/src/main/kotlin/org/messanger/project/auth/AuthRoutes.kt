package org.messanger.project.auth

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import org.messanger.project.database.AuthRepository
import java.util.UUID

fun Route.authRoutes(jwtService: JwtService) {

    post("/register") {
        val request = call.receive<RegisterRequest>()

        if (AuthRepository.existsByLogin(request.login)) {
            call.respond(
                HttpStatusCode.Conflict,
                ErrorResponse(
                    code = "LOGIN_TAKEN",
                    message = "Login is already taken"
                )
            )
            return@post
        }

        val passwordHash = PasswordHasher.hash(request.password)

        val user = AuthRepository.createUser(
            id = UUID.randomUUID().toString(),
            login = request.login,
            displayName = request.displayName,
            passwordHash = passwordHash
        )

        val token = jwtService.createToken(user)

        call.respond(
            HttpStatusCode.Created,
            AuthResponse(
                token = token,
                userId = user.id,
                login = user.login,
                displayName = user.displayName
            )
        )
    }

    post("/login") {
        val request = call.receive<LoginRequest>()

        val user = AuthRepository.findByLogin(request.login)
        if (user == null) {
            call.respond(
                HttpStatusCode.Unauthorized,
                ErrorResponse(
                    code = "INVALID_CREDENTIALS",
                    message = "Invalid login or password"
                )
            )
            return@post
        }

        val passwordOk = PasswordHasher.verify(
            password = request.password,
            passwordHash = user.passwordHash
        )

        if (!passwordOk) {
            call.respond(
                HttpStatusCode.Unauthorized,
                ErrorResponse(
                    code = "INVALID_CREDENTIALS",
                    message = "Invalid login or password"
                )
            )
            return@post
        }

        val token = jwtService.createToken(user)

        call.respond(
            HttpStatusCode.OK,
            AuthResponse(
                token = token,
                userId = user.id,
                login = user.login,
                displayName = user.displayName
            )
        )
    }
}