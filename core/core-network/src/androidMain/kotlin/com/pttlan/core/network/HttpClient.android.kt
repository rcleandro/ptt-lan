package com.pttlan.core.network

import com.pttlan.core.common.network.isLocalNetwork
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import okhttp3.internal.tls.OkHostnameVerifier
import java.net.Socket
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLEngine
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509ExtendedTrustManager
import javax.net.ssl.X509TrustManager

@android.annotation.SuppressLint("CustomX509TrustManager", "TrustAllX509TrustManager")
actual fun createPlatformHttpClient(): HttpClient =
    HttpClient(OkHttp) {
        engine {
            val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
            trustManagerFactory.init(null as KeyStore?)
            val defaultTrustManager = trustManagerFactory.trustManagers.first { it is X509TrustManager } as X509TrustManager

            val conditionalTrustManager =
                object : X509ExtendedTrustManager() {
                    override fun checkClientTrusted(
                        chain: Array<out X509Certificate>?,
                        authType: String?,
                    ) = defaultTrustManager.checkClientTrusted(chain, authType)

                    override fun checkClientTrusted(
                        chain: Array<out X509Certificate>?,
                        authType: String?,
                        socket: Socket?,
                    ) = defaultTrustManager.checkClientTrusted(chain, authType)

                    override fun checkClientTrusted(
                        chain: Array<out X509Certificate>?,
                        authType: String?,
                        engine: SSLEngine?,
                    ) = defaultTrustManager.checkClientTrusted(chain, authType)

                    override fun checkServerTrusted(
                        chain: Array<out X509Certificate>?,
                        authType: String?,
                    ) = defaultTrustManager.checkServerTrusted(chain, authType)

                    override fun checkServerTrusted(
                        chain: Array<out X509Certificate>?,
                        authType: String?,
                        socket: Socket?,
                    ) {
                        val host = socket?.inetAddress?.hostName
                        if (host != null && isLocalNetwork(host)) return
                        defaultTrustManager.checkServerTrusted(chain, authType)
                    }

                    override fun checkServerTrusted(
                        chain: Array<out X509Certificate>?,
                        authType: String?,
                        engine: SSLEngine?,
                    ) {
                        val host = engine?.peerHost
                        if (host != null && isLocalNetwork(host)) return
                        defaultTrustManager.checkServerTrusted(chain, authType)
                    }

                    override fun getAcceptedIssuers(): Array<X509Certificate> = defaultTrustManager.acceptedIssuers
                }

            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, arrayOf(conditionalTrustManager), SecureRandom())

            config {
                sslSocketFactory(sslContext.socketFactory, conditionalTrustManager)
                hostnameVerifier { hostname, session ->
                    if (isLocalNetwork(hostname)) true else OkHostnameVerifier.verify(hostname, session)
                }
            }
        }
    }
