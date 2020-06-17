package com.google.firebase.nongmsauth.api.service

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Adds API Key param and Content-Type and Accept-Encoding headers to every request.
 */
class FirebaseKeyInterceptor(private val apiKey: String) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val originalUrl = request.url

        val newUrl = originalUrl.newBuilder()
            .addQueryParameter("key", this.apiKey)
            .build()

        val requestBuilder = request.newBuilder()
            .header("Content-Type", "application/json")
            .header("Accept-Encoding", "identity")  // needed because the response is not compressed when the header says it is. See https://github.com/square/okio/issues/299
            .url(newUrl)

        return chain.proceed(requestBuilder.build())
    }

}
