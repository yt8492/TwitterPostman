package com.github.zliofficial

import twitter4j.TwitterException
import twitter4j.TwitterFactory
import twitter4j.auth.AccessToken

fun main() {
//    val twitter = TwitterFactory.getSingleton()
//    val requestToken = twitter.getOAuthRequestToken("oob")
//    var accessToken: AccessToken? = null
//    while (accessToken == null) {
//        println(requestToken.authorizationURL)
//        try {
//            val pin = readLine()
//            accessToken = twitter.getOAuthAccessToken(requestToken, pin)
//            println(accessToken.screenName)
//        } catch (te: TwitterException) {
//            if (te.statusCode == 401) {
//                println("Unable to get the access token")
//            } else {
//                te.printStackTrace()
//            }
//        }
//    }
//    twitter.updateStatus(readLine())
    val input = readLine() ?: return
    try {
        TwitterUtil.post(input, listOf())
    } catch (e: Exception) {
        e.printStackTrace()
        println(TwitterUtil.getAuthorizationUrl())
        val pin = readLine() ?: return
        TwitterUtil.setPin(pin)
        TwitterUtil.post(input, listOf())
    }
}