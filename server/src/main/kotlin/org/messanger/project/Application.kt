package org.messanger.project

import io.ktor.server.application.Application
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import org.messanger.project.database.Db

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    Db.init(environment.config)
    routingModule()
}
