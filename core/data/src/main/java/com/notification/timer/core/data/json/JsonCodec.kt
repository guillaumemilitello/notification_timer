package com.notification.timer.core.data.json

import kotlinx.serialization.json.Json

object JsonCodec {
    val json =
        Json {
            ignoreUnknownKeys = true
            prettyPrint = true
            encodeDefaults = true
        }
}
