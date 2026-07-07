package com.pttlan.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

actual fun createPlatformHttpClient(): HttpClient = HttpClient(OkHttp) {
    engine {
        val trustAllCert = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
        }

        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, arrayOf(trustAllCert), SecureRandom())

        config {
            sslSocketFactory(sslContext.socketFactory, trustAllCert)
            hostnameVerifier { _, _ -> true }
        }
    }
}
