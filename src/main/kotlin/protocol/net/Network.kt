package com.github.hatoyuze.protocol.net

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class Network(val host: String,private val block: HttpClientConfig<CIOEngineConfig>.() -> Unit = {}) {
    private val net = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        this@Network.block(this)
    }

    suspend fun execute(url: String, block: HttpRequestBuilder.() -> Unit): HttpResponse {
        return net.request {
            block()
            url(this@Network.host + url)
        }
    }
}