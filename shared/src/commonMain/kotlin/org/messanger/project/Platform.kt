package org.messanger.project

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform