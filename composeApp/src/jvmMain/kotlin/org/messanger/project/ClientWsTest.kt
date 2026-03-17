package org.messanger.project

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.websocket.Frame
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import io.ktor.serialization.kotlinx.json.json
import io.ktor.websocket.readText
import org.messanger.project.models.AuthResponse
import org.messanger.project.models.LoginRequest
import org.messanger.project.protocol.ClientEvent
import org.messanger.project.protocol.Join
import org.messanger.project.protocol.SendMessage
import org.messanger.project.protocol.ServerEvent
import org.messanger.project.protocol.WsJson
import java.util.Scanner

suspend fun login(
    client: HttpClient,
    login: String,
    password: String
): AuthResponse {
    return client.post("http://127.0.0.1:8080/login") {
        contentType(ContentType.Application.Json)
        setBody(
            LoginRequest(
                login = login,
                password = password
            )
        )
    }.body()
}

fun main() = runBlocking {
    val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json()
        }
        install(WebSockets)
    }

    val scanner = Scanner(System.`in`)

    print("login: ")
    val loginValue = "leonid"

    print("password: ")
    val passwordValue = "dev-hash-u1"

    val auth = try {
        login(client, loginValue, passwordValue)
    } catch (e: Exception) {
        println("login failed: ${e.message}")
        client.close()
        return@runBlocking
    }

    println("logged in as ${auth.login}, userId=${auth.userId}")
    println("token received: ${auth.token.take(30)}...")

    client.webSocket(
        method = HttpMethod.Get,
        host = "127.0.0.1",
        port = 8080,
        path = "/ws",
        request = {
            header(HttpHeaders.Authorization, "Bearer ${auth.token}")
        }
    ) {
        val reader = launch {
            for (frame in incoming) {
                val t = frame as? Frame.Text ?: continue
                val event = WsJson.decodeFromString<ServerEvent>(t.readText())
                println("server event: $event")
            }
        }

        send(Frame.Text(WsJson.encodeToString<ClientEvent>(Join("room-general"))))

        while (true) {
            val line = scanner.nextLine()
            if (line == "/exit") break

            val msg = SendMessage(
                chatId = "room-general",
                text = line
            )
            send(Frame.Text(WsJson.encodeToString<ClientEvent>(msg)))
        }

        reader.cancel()
    }

    client.close()
}