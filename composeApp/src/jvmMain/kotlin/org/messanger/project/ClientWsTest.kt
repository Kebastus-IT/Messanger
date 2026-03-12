package org.messanger.project

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.messanger.project.protocol.ClientEvent
import org.messanger.project.protocol.Join
import org.messanger.project.protocol.SendMessage
import org.messanger.project.protocol.ServerEvent
import org.messanger.project.protocol.WsJson
import java.util.Scanner

fun main() = runBlocking {
    val client = HttpClient(CIO) { install(WebSockets) }
    val scanner = Scanner(System.`in`)

    client.webSocket(method = HttpMethod.Get, host = "127.0.0.1", port = 8080, path = "/ws") {
        val reader = launch {
            for (frame in incoming) {
                val t = frame as? Frame.Text ?: continue
                val event = WsJson.decodeFromString<ServerEvent>(t.readText())
                println("server event: $event")
            }
        }

        send(Frame.Text(WsJson.encodeToString<ClientEvent>(Join("room-general"))))

        // 3) консольный ввод → SendMessage
        while (true) {
            val line = scanner.nextLine()
            if (line == "/exit") break

            val msg = SendMessage(chatId = "room-general", text = line)
            send(Frame.Text(WsJson.encodeToString<ClientEvent>(msg)))
        }

        reader.cancel()
    }

    client.close()
}