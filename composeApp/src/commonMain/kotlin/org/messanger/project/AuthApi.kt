package org.messanger.project

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
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