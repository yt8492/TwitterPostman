package com.github.zliofficial

import twitter4j.StatusUpdate
import twitter4j.TwitterFactory
import twitter4j.auth.AccessToken
import twitter4j.auth.RequestToken
import java.io.File

object TwitterUtil {
    private const val FILE_NAME_ACCESS_TOKEN = "AccessToken.dat"
    private const val FILE_NAME_REQUEST_TOKEN = "RequestToken.dat"

    private val twitter = TwitterFactory.getSingleton().apply {
        val accessToken = getAccessToken()
        if (accessToken != null) {
            oAuthAccessToken = accessToken
        }
    }
    private val requestToken = getOrCreateRequestToken()

    fun getAuthorizationUrl(): String {
        return requestToken.authorizationURL
    }

    fun setPin(pin: String) {
        val accessToken = twitter.getOAuthAccessToken(requestToken, pin)
        saveAccessToken(accessToken)
    }

    fun post(text: String, medias: List<Media> = listOf()) {
        val status = if (medias.isEmpty()) {
            StatusUpdate(text)
        } else if (medias.size == 1) {
            val media = medias.first()
            StatusUpdate(text).media(media.name, media.body)
        } else {
            val mediaIds = medias.take(4).map {
                twitter.uploadMedia(it.name, it.body).mediaId
            }.toLongArray()
            StatusUpdate(text).apply {
                setMediaIds(*mediaIds)
            }
        }
        twitter.updateStatus(status)
    }

    private fun getOrCreateRequestToken(): RequestToken {
        val file = File(FILE_NAME_REQUEST_TOKEN)
        return if (file.exists()) {
            file.useLines {
                val lines = it.toList()
                if (lines.size == 2) {
                    val (token, tokenSecret) = lines
                    RequestToken(token, tokenSecret)
                } else {
                    file.delete()
                    file.createNewFile()
                    val requestToken = twitter.getOAuthRequestToken("oob")
                    file.bufferedWriter().use {
                        it.appendln(requestToken.token)
                        it.appendln(requestToken.tokenSecret)
                    }
                    requestToken
                }
            }
        } else {
            file.createNewFile()
            val requestToken = twitter.getOAuthRequestToken("oob")
            file.bufferedWriter().use {
                it.appendln(requestToken.token)
                it.appendln(requestToken.tokenSecret)
            }
            requestToken
        }
    }

    private fun getAccessToken(): AccessToken? {
        val file = File(FILE_NAME_ACCESS_TOKEN)
        return if (file.exists()) {
            file.useLines {
                val lines = it.toList()
                if (lines.size == 2) {
                    val (token, tokenSecret) = lines
                    AccessToken(token, tokenSecret)
                } else {
                    null
                }
            }
        } else {
            null
        }
    }

    private fun saveAccessToken(accessToken: AccessToken) {
        val file = File(FILE_NAME_ACCESS_TOKEN)
        if (file.exists()) {
            file.delete()
        }
        file.createNewFile()
        file.bufferedWriter().use {
            it.appendln(accessToken.token)
            it.appendln(accessToken.tokenSecret)
        }
    }
}