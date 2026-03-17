package org.messanger.project

import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import org.messanger.project.screens.AuthScreen
import org.messanger.project.screens.ChatScreen
import org.messanger.project.screens.ChatsScreen
import org.messanger.project.protocol.WsJson
import org.messanger.project.models.ChatSummary
import io.ktor.client.plugins.websocket.*

@Composable
@Preview
fun App() {
    val httpClient = remember {
        HttpClient {
            install(ContentNegotiation) {
                json(WsJson)
            }
            install(WebSockets)
        }
    }

    val baseUrl = "http://localhost:8080"

    val authApi = remember {
        AuthApi(
            client = httpClient,
            baseUrl = baseUrl
        )
    }

    val chatsApi = remember {
        ChatsApi(
            client = httpClient,
            baseUrl = baseUrl
        )
    }

    val chatConnection = remember {
        ChatConnection(
            client = httpClient,
            baseUrl = baseUrl
        )
    }

    var session by remember { mutableStateOf<UserSession?>(null) }
    var selectedChat by remember { mutableStateOf<ChatSummary?>(null) }

    when {
        session == null -> {
            AuthScreen(
                authApi = authApi,
                onAuthSuccess = { newSession ->
                    session = newSession
                }
            )
        }

        selectedChat == null -> {
            ChatsScreen(
                token = session!!.token,
                chatsApi = chatsApi,
                onChatSelected = { chat ->
                    selectedChat = chat
                },
                onLogout = {
                    session = null
                    selectedChat = null
                }
            )
        }

        else -> {
            ChatScreen(
                session = session!!,
                chat = selectedChat!!,
                chatConnection = chatConnection,
                onBack = {
                    selectedChat = null
                }
            )
        }
    }
}

