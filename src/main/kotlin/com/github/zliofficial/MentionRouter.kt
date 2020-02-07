package com.github.zliofficial

import com.github.seratch.jslack.api.model.Message
import com.github.seratch.jslack.api.model.event.AppMentionEvent
import com.github.seratch.jslack.lightning.context.builtin.DefaultContext
import com.vdurmont.emoji.EmojiParser
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.*
import java.util.concurrent.Executors

@KtorExperimentalAPI
class MentionRouter {

    private val dispatcher = Executors.newCachedThreadPool().asCoroutineDispatcher()

    fun route(
        event: AppMentionEvent,
        context: DefaultContext
    ): Job = CoroutineScope(dispatcher).launch {
        val client = context.client()
        if (event.text.matches(loginPhrase)) {
            val authorizationUrl = TwitterUtil.getAuthorizationUrl()
            withContext(Dispatchers.IO) {
                client.chatPostMessage {
                    it.token(context.botToken)
                    it.channel(event.channel)
                    it.text("ログインURL :$authorizationUrl")
                    it.threadTs(event.threadTs ?: event.ts)
                }
            }
            return@launch
        } else if (event.text.matches(pinPhrase)) {
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
            } catch (e: Exception) {
                withContext(Dispatchers.IO) {
                    client.chatPostMessage {
                        it.token(context.botToken)
                        it.channel(event.channel)
                        it.text("ログインに失敗しました。")
                        it.threadTs(event.threadTs ?: event.ts)
                    }
                }
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

        private val pinPhrase = listOf(
            "pin",
            "Pin",
            "ピン"
        ).joinToString("|").let {
            ".*($it).*"
        }.toRegex()
    }
}