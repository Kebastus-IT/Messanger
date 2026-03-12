package org.messanger.project.protocol

import kotlinx.serialization.json.Json

val WsJson = Json {
    ignoreUnknownKeys = true
    classDiscriminator = "#type"
    encodeDefaults = true
}