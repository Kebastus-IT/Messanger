package org.messanger.project.auth

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import org.messanger.project.database.AuthRepository
import org.messanger.project.models.AuthResponse
import org.messanger.project.models.ErrorResponse
import org.messanger.project.models.LoginRequest
import org.messanger.project.models.RegisterRequest
import java.util.UUID

fun Route.authRoutes(jwtService: JwtService) {

    post("/register") {
        val request = call.receive<RegisterRequest>()
        val login = request.login.trim()
        val displayName = request.displayName.trim()
        val password = request.password

        if (login.length !in 3..32 || !login.matches(Regex("[a-zA-Z0-9_]+$"))){
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse(
                    code = "INVALID_LOGIN",
                    message = "Login must be 3-32 characters, letters/digits/underscore only"
            ))
            return@post
        }
        if (displayName.length !in 1..64){
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse(
                    code = "INVALID_DISPLAY_NAME",
                    message = "Name must be between 1-64 characters"
            ))
            return@post
        }
        if (password.length !in 8..128){
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse(
                    code = "INVALID_PASSWORD",
                    message = "Password must be between 8-128 characters"
            ))
            return@post
        }
        if (AuthRepository.existsByLogin(login)) {
            call.respond(
                HttpStatusCode.Conflict,
                ErrorResponse(
                    code = "LOGIN_TAKEN",
                    message = "Login is already taken"
            ))
            return@post
        }

        val passwordHash = PasswordHasher.hash(password)

        val user = AuthRepository.createUser(
            id = UUID.randomUUID().toString(),
            login = login,
            displayName = displayName,
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
        if (request.login.isBlank() || request.password.isBlank()){
            call.respond(HttpStatusCode.BadRequest,
                ErrorResponse(
                    code = "INVALID_CREDENTIALS",
                    message = "Login and password required"
                ))
            return@post
        }
        val user = AuthRepository.findByLogin(request.login)
        if (user == null) {
            call.respond(
                HttpStatusCode.Unauthorized,
                ErrorResponse(
                    code = "INVALID_CREDENTIALS",
                    message = "Invalid login or password"
                ))
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