package org.messanger.project

import io.ktor.server.cio.*
import io.ktor.server.engine.*
import org.messanger.project.database.Db

fun main() {
    embeddedServer(CIO, 8080) {
        Db.init()
        webSocketModule()
    }.start(true)
}

