package org.messanger.project.connection

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import org.messanger.project.models.AuthResponse
import org.messanger.project.models.LoginRequest
import org.messanger.project.models.RegisterRequest

class AuthApi(
    private val client: HttpClient,
    private val baseUrl: String
) {
    suspend fun login(
        login: String,
        password: String
    ): AuthResponse {
        return client.post("$baseUrl/login") {
            contentType(ContentType.Application.Json)
            setBody(
                LoginRequest(
                    login = login,
                    password = password
                )
            )
        }.body()
    }

    suspend fun register(
        login: String,
        displayName: String,
        password: String
    ): AuthResponse {
        return client.post("$baseUrl/register") {
            contentType(ContentType.Application.Json)
            setBody(
                RegisterRequest(
                    login = login,
                    displayName = displayName,
                    password = password
                )
            )
        }.body()
    }
}