package com.github.zliofficial

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.request
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpMethod
import io.ktor.util.KtorExperimentalAPI
import io.ktor.utils.io.jvm.javaio.toInputStream
import java.io.File
import java.io.InputStream

object ImageUtil {
    @KtorExperimentalAPI
    suspend fun getInputStreamFromUrl(url: String): InputStream {
        return HttpClient(CIO).use { client ->
            client.request<HttpResponse> {
                url(url)
                method = HttpMethod.Get
            }.content.toInputStream()
        }
    }
}