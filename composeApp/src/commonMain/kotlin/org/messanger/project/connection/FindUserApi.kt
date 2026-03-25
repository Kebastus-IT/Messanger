package org.messanger.project.connection

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import org.messanger.project.models.UserSummary

class FindUserApi(
    private val client: HttpClient,
    private val baseUrl: String
){
    suspend fun findUser(
        token : String,
        loginQuery: String
    ): List<UserSummary>{
    return client.get("$baseUrl/users/find") {
        header(HttpHeaders.Authorization, "Bearer $token")
        parameter("login", loginQuery)
    }.body()
    }
}