package org.messanger.project.connection

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import org.messanger.project.models.ChatSummary


class ChatsApi(
    private val client: HttpClient,
    private val baseUrl: String
) {
    suspend fun getChats(token: String): List<ChatSummary> {
        return client.get("$baseUrl/chats") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }.body()
    }
}