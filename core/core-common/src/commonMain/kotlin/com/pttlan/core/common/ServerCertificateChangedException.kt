package com.pttlan.core.common

/**
 * A LAN server answered with another certificate than the one trusted the first time (30.5). It can be someone
 * on the network posing as the server, or the host that reinstalled the app: the codes let the user compare with
 * the one the host shows before trusting the new one.
 */
class ServerCertificateChangedException(
    val host: String,
    val port: Int,
    val previousCode: String,
    val newCode: String,
) : IllegalStateException("O certificado deste servidor mudou")
