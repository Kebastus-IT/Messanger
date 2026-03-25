package org.messanger.project.connection

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import org.messanger.project.models.ChatSummary
import org.messanger.project.models.CreateDmRequest

class CreateChatApi (
    private val client: HttpClient,
    private val baseUrl: String
) {
        suspend fun getOrCreateChat(
            token : String,
            otherUserId: String
        ): ChatSummary{
            return client.post("$baseUrl/chats/dm") {
                header(HttpHeaders.Authorization, "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(
                    CreateDmRequest(
                        otherUserId = otherUserId
                    )
                )
            }.body()
        }
    }
