package com.github.zliofficial

import com.github.seratch.jslack.api.model.event.AppMentionEvent
import com.github.seratch.jslack.lightning.App
import com.github.seratch.jslack.lightning.jetty.SlackAppServer
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.runBlocking
import org.eclipse.jetty.util.ssl.SslContextFactory

@KtorExperimentalAPI
fun main() {
    val app = App()
    val mentionRouter = MentionRouter()

    app.event(AppMentionEvent::class.java) { event, context ->
        println(event.event)
        runBlocking {
            mentionRouter.route(event.event, context).join()
        }
        context.ack()
    }
    val server= SlackAppServer(app, 8080)
    println("server start")
    server.start()
}