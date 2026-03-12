package org.messanger.project

import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.messanger.project.DataBase.Db

fun main() {
    embeddedServer(CIO, 8080) {
        Db.init()
        webSocketModule()
    }.start(true)
}

