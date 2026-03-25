package org.messanger.project

import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import com.russhwolf.settings.Settings
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import org.messanger.project.screens.AuthScreen
import org.messanger.project.screens.ChatScreen
import org.messanger.project.screens.ChatsScreen
import org.messanger.project.protocol.WsJson
import org.messanger.project.models.ChatSummary
import io.ktor.client.plugins.websocket.*
import org.messanger.project.connection.AuthApi
import org.messanger.project.connection.ChatConnection
import org.messanger.project.connection.ChatsApi
import org.messanger.project.connection.CreateChatApi
import org.messanger.project.connection.FindUserApi
import org.messanger.project.platform.BASE_URL

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

    val baseUrl = BASE_URL

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

    val findUserApi = remember {
        FindUserApi(
            client = httpClient,
            baseUrl =baseUrl
        )
    }
    val createChatApi = remember {
        CreateChatApi(
            client = httpClient,
            baseUrl =baseUrl
        )
    }

    val chatConnection = remember {
        ChatConnection(
            client = httpClient,
            baseUrl = baseUrl
        )
    }
    val settings = remember {
        Settings()
    }

    val sessionStorage = remember {
        SessionStorage(settings)
    }

    var session by remember {
        mutableStateOf(sessionStorage.loadSession())
    }
    var selectedChat by remember { mutableStateOf<ChatSummary?>(null) }

    when {
        session == null -> {
            AuthScreen(
                authApi = authApi,
                onAuthSuccess = { newSession ->
                    sessionStorage.saveSession(newSession)
                    session = newSession
                }
            )
        }

        selectedChat == null -> {
            ChatsScreen(
                token = session!!.token,
                chatsApi = chatsApi,
                findUserApi = findUserApi,
                createChatApi = createChatApi,
                onChatSelected = { chat ->
                    selectedChat = chat
                },
                onLogout = {
                    sessionStorage.clearSession()
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

