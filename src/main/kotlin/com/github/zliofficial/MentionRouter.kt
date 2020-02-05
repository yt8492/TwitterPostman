package com.github.zliofficial

import com.github.seratch.jslack.api.model.Message
import com.github.seratch.jslack.api.model.event.AppMentionEvent
import com.github.seratch.jslack.lightning.App
import com.github.seratch.jslack.lightning.context.builtin.DefaultContext
import com.vdurmont.emoji.EmojiParser
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.*
import twitter4j.TwitterException
import java.util.concurrent.Executors

@KtorExperimentalAPI
class MentionRouter(private val app: App) {

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
                    it.text(authorizationUrl)
                    it.threadTs(event.threadTs ?: event.ts)
                }
            }
            return@launch
        } else if (event.text.matches("""\d+""".toRegex())) {
            try {
                val pin = """\d+""".toRegex().find(event.text)?.value ?: return@launch
                TwitterUtil.setPin(pin)
                withContext(Dispatchers.IO) {
                    client.chatPostMessage {
                        it.token(context.botToken)
                        it.channel(event.channel)
                        it.text("ログインに成功しました")
                        it.threadTs(event.threadTs ?: event.ts)
                    }
                }
            } catch (te: TwitterException) {
                withContext(Dispatchers.IO) {
                    client.chatPostMessage {
                        it.token(context.botToken)
                        it.channel(event.channel)
                        it.text("ログインに失敗しました")
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
                    it.text("ツイートしたい内容のメッセージのスレッドに書いて下さい")
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
            if (parsedMessage.length > 140) {
                withContext(Dispatchers.IO) {
                    client.chatPostMessage {
                        it.token(context.botToken)
                        it.channel(event.channel)
                        it.text("140文字以上です")
                        it.threadTs(event.threadTs)
                    }
                }
                return@launch
            }

        }
    }

    private suspend fun post(message: Message) {
        val imageStreams = message.files.map {
            ImageUtil.getInputStreamFromUrl(it.urlPrivate)
        }

    }

    companion object {
        private val postPhrase = listOf(
            "投稿",
            "とうこう",
            "呟く",
            "つぶやく",
            "呟いて",
            "つぶやいて",
            "Tweet",
            "tweet"
        ).joinToString("|").toRegex()

        private val loginPhrase = listOf(
            "ログイン",
            "login",
            "Login"
        ).joinToString("|").toRegex()
    }
}