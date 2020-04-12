package com.github.zliofficial

import com.github.seratch.jslack.api.model.Message
import com.github.seratch.jslack.api.model.event.AppMentionEvent
import com.github.seratch.jslack.lightning.context.builtin.DefaultContext
import com.vdurmont.emoji.EmojiParser
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors

@KtorExperimentalAPI
class MentionRouter {

    private val dispatcher = Executors.newCachedThreadPool().asCoroutineDispatcher()

    fun route(
        event: AppMentionEvent,
        context: DefaultContext
    ): Job = CoroutineScope(dispatcher).launch {
        println("MentionRouter start")
        val client = context.client()
        if (event.text.matches(loginPhrase)) {
            println("Login start")
            val authorizationUrl = TwitterUtil.getAuthorizationUrl()
            withContext(Dispatchers.IO) {
                client.chatPostMessage {
                    it.token(context.botToken)
                    it.channel(event.channel)
                    it.text("ログインURL: $authorizationUrl")
                    it.threadTs(event.threadTs ?: event.ts)
                }
            }
            println("Login success")
            return@launch
        } else if (event.text.matches(logoutPhrase)) {
            println("Logout start")
            TwitterUtil.logout()
            withContext(Dispatchers.IO) {
                client.chatPostMessage {
                    it.token(context.botToken)
                    it.channel(event.channel)
                    it.text("ログアウトしました。")
                    it.threadTs(event.threadTs ?: event.ts)
                }
            }
            println("Logout success")
            return@launch
        } else if (event.text.matches(pinPhrase)) {
            println("Set pin start")
            try {
                val pin = """\d+""".toRegex().findAll(event.text).lastOrNull()?.value ?: return@launch
                TwitterUtil.setPin(pin)
                withContext(Dispatchers.IO) {
                    client.chatPostMessage {
                        it.token(context.botToken)
                        it.channel(event.channel)
                        it.text("ログインに成功しました。")
                        it.threadTs(event.threadTs ?: event.ts)
                    }
                }
                println("Set pin success")
            } catch (e: Exception) {
                withContext(Dispatchers.IO) {
                    client.chatPostMessage {
                        it.token(context.botToken)
                        it.channel(event.channel)
                        it.text("ログインに失敗しました。\n${e.message}")
                        it.threadTs(event.threadTs ?: event.ts)
                    }
                }
                println("Set pin failed")
            }
            return@launch
        }
        if (event.threadTs == null) {
            withContext(Dispatchers.IO) {
                client.chatPostMessage {
                    it.token(context.botToken)
                    it.channel(event.channel)
                    it.text("ツイートしたい内容のメッセージのスレッドに書いて下さい。")
                }
            }
        } else if (event.text.matches(postPhrase)) {
            println("Post tweet start")
            val threadMessages = withContext(Dispatchers.IO) {
                client.channelsHistory {
                    it.channel(event.channel)
                }.messages.filter {
                    it.ts == event.threadTs
                }
            }
            val threadRoot = threadMessages[0]
            val parsedMessage = EmojiParser.parseToUnicode(threadRoot.text)
            threadRoot.text = parsedMessage
            if (parsedMessage.length > 140) {
                withContext(Dispatchers.IO) {
                    client.chatPostMessage {
                        it.token(context.botToken)
                        it.channel(event.channel)
                        it.text("140文字以上です。")
                        it.threadTs(event.threadTs)
                    }
                }
                println("Tweet length over 140")
                return@launch
            }
            try {
                post(threadRoot, context.botToken)
                withContext(Dispatchers.IO) {
                    client.chatPostMessage {
                        it.token(context.botToken)
                        it.channel(event.channel)
                        it.text("${threadRoot.text}と投稿しました。")
                        it.threadTs(event.threadTs)
                    }
                }
                println("Post tweet success")
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.IO) {
                    client.chatPostMessage {
                        it.token(context.botToken)
                        it.channel(event.channel)
                        it.text("ツイートに失敗しました。再ログインしてください。")
                        it.threadTs(event.threadTs)
                    }
                }
                println("Post tweet failed")
            }
        } else if (event.text.contains(" echo ")) {
            val splitText = event.text.split(" ")
            val echoIndex = splitText.indexOf("echo")
            val text = splitText.drop(echoIndex + 1).joinToString(" ")
            withContext(Dispatchers.IO) {
                client.chatPostMessage {
                    it.token(context.botToken)
                    it.channel(event.channel)
                    it.text(text)
                    it.threadTs(event.threadTs)
                }
            }
        }
    }

    private suspend fun post(message: Message, token: String) {
        val medias = message.files?.map {
            Media(it.name, ImageUtil.getInputStreamFromUrl(it.urlPrivate, token))
        } ?: listOf()

        TwitterUtil.post(message.text, medias)
    }

    companion object {
        private val postPhrase = listOf(
            "投稿",
            "とうこう",
            "呟く",
            "つぶやく",
            "呟いて",
            "つぶやいて",
            "ツイート",
            "Tweet",
            "tweet"
        ).joinToString("|").let {
            ".*($it).*"
        }.toRegex()

        private val loginPhrase = listOf(
            "ログイン",
            "login",
            "Login"
        ).joinToString("|").let {
            ".*($it).*"
        }.toRegex()

        private val logoutPhrase = listOf(
            "ログアウト",
            "logout",
            "Logout"
        ).joinToString("|").let {
            ".*($it).*"
        }.toRegex()

        private val pinPhrase = listOf(
            "pin",
            "Pin",
            "ピン"
        ).joinToString("|").let {
            ".*($it).*"
        }.toRegex()
    }
}