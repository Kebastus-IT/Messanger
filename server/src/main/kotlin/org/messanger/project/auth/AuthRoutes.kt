package org.messanger.project.auth

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.messanger.project.models.AuthResponse
import org.messanger.project.models.ErrorResponse
import org.messanger.project.models.LoginRequest
import org.messanger.project.models.RegisterRequest
import org.messanger.project.services.AuthService
import org.messanger.project.services.LoginResult
import org.messanger.project.services.RegisterResult

fun Route.authRoutes(authService: AuthService) {

    post("/register") {
        val request = call.receive<RegisterRequest>()
        when (val result = authService.register(request)) {
                is RegisterResult.Success -> call.respond(HttpStatusCode.Created, AuthResponse(
                    token = result.token,
                    userId = result.userId,
                    login = result.login,
                    displayName = result.displayName
                ))

                RegisterResult.InvalidPassword -> call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse(
                        code = "INVALID_PASSWORD",
                        message = "Password must be between 8-128 characters"
                    ))

                RegisterResult.InvalidLogin -> call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse(
                        code = "INVALID_LOGIN",
                        message = "Login must be 3-32 characters, letters/digits/underscore only"
                    ))

                RegisterResult.InvalidDisplayName -> call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse(
                        code = "INVALID_DISPLAY_NAME",
                        message = "Name must be between 1-64 characters"
                    ))

                RegisterResult.LoginTaken -> call.respond(
                    HttpStatusCode.Conflict,
                    ErrorResponse(
                        code = "LOGIN_TAKEN",
                        message = "Login is already taken"
                    ))
            }
    }

    post("/login") {
        val request = call.receive<LoginRequest>()
        when(val result = authService.login(request)) {
            is LoginResult.Success -> call.respond(HttpStatusCode.OK, AuthResponse(
                token = result.token,
                userId = result.userId,
                login = result.login,
                displayName = result.displayName
            ))
            LoginResult.InvalidCredentials -> call.respond(HttpStatusCode.Unauthorized, ErrorResponse(
                code = "INVALID_CREDENTIALS",
                message = "Invalid login or password"
            ))
        }
    }
}