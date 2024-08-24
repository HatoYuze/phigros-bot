package com.github.hatoyuze.protocol.net

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.util.*

/**
 * A tool for phigros network api
 *
 * *Only automatically add some headers*
 * */
class PhigrosFeature {
    companion object : HttpClientPlugin<Unit, PhigrosFeature> {
        override val key: AttributeKey<PhigrosFeature> = AttributeKey("PhigrosFeature")

        override fun prepare(block: Unit.() -> Unit): PhigrosFeature = PhigrosFeature()

        override fun install(plugin: PhigrosFeature, scope: HttpClient) {
            // Add headers
            scope.requestPipeline.intercept(HttpRequestPipeline.Before) {
                with(context.headers) {
                    append("X-LC-Id","rAK3FfdieFob2Nn8Am")
                    append("X-LC-Key", "Qr9AEqtuoSVS3zeD6iVbM4ZC0AtkJcQ89tywVyi0")
                    append("User-Agent","LeanCloud-CSharp-SDK/1.0.3")
                    append("Accept","application/json")
                }
            }

        }

    }
}

