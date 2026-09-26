package com.pttlan.core.network

import com.pttlan.core.common.ServerCertificateChangedException
import kotlin.concurrent.Volatile

private const val CODE_LENGTH = 8
private const val CODE_GROUP = 4

/** Where the trusted fingerprints are kept, by `host:port`. */
interface PinStore {
    operator fun get(key: String): String?

    operator fun set(
        key: String,
        value: String,
    )
}

class InMemoryPinStore : PinStore {
    private val pins = mutableMapOf<String, String>()

    override fun get(key: String): String? = pins[key]

    override fun set(
        key: String,
        value: String,
    ) {
        pins[key] = value
    }
}

/** The start of a SHA-256 fingerprint, "BA78-16BF": enough to compare by eye or read aloud. */
fun certificateCode(fingerprint: String): String =
    fingerprint
        .take(CODE_LENGTH)
        .uppercase()
        .chunked(CODE_GROUP)
        .joinToString("-")

/** SHA-256 of [bytes]. */
expect fun sha256(bytes: ByteArray): ByteArray

/** Hex SHA-256, for certificate fingerprints. */
@OptIn(ExperimentalStdlibApi::class)
fun sha256Hex(bytes: ByteArray): String = sha256(bytes).toHexString()

/**
 * Trust on first use for LAN servers (30.5). Their certificates are self-signed, so no authority vouches for them:
 * the first one a host shows is kept, and a different one later is refused until the user trusts it. Servers on
 * the internet keep the system validation and never get here.
 */
class CertificatePins(
    private val store: PinStore = InMemoryPinStore(),
) {
    /** Fingerprints refused per host, waiting for the user to trust them or not. */
    @Volatile
    private var changed = mapOf<String, String>()

    /** Called during the TLS handshake with the fingerprint of the certificate the server showed. */
    fun verify(
        host: String,
        port: Int,
        fingerprint: String,
    ): Boolean {
        val key = key(host, port)
        val trusted = store[key]
        if (trusted == null) store[key] = fingerprint
        val ok = trusted == null || trusted == fingerprint
        changed = if (ok) changed - key else changed + (key to fingerprint)
        return ok
    }

    /** What changed on the last refused handshake with this host, if it was refused for its certificate. */
    fun changeFor(
        host: String,
        port: Int,
    ): ServerCertificateChangedException? {
        val key = key(host, port)
        val newFingerprint = changed[key]
        val previous = store[key]
        return if (newFingerprint == null || previous == null) {
            null
        } else {
            ServerCertificateChangedException(host, port, certificateCode(previous), certificateCode(newFingerprint))
        }
    }

    /** The user compared the codes and trusts the certificate the host shows now. */
    fun trustChanged(
        host: String,
        port: Int,
    ) {
        val key = key(host, port)
        val newFingerprint = changed[key] ?: return
        store[key] = newFingerprint
        changed = changed - key
    }

    fun codeFor(
        host: String,
        port: Int,
    ): String? = store[key(host, port)]?.let(::certificateCode)

    private fun key(
        host: String,
        port: Int,
    ) = "$host:$port"
}
