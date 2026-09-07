package com.anothersshclient.ssh

import java.security.Security
import org.bouncycastle.jce.provider.BouncyCastleProvider

/**
 * Android ships a truncated BouncyCastle "BC" provider that lacks X25519.
 * sshj needs the full provider, so replace the platform one at startup.
 */
object CryptoInit {
    @Volatile
    private var ready = false

    @Synchronized
    fun ensureBouncyCastle() {
        if (ready) return

        val existing = Security.getProvider(BouncyCastleProvider.PROVIDER_NAME)
        if (existing == null || existing.javaClass != BouncyCastleProvider::class.java) {
            Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME)
            Security.insertProviderAt(BouncyCastleProvider(), 1)
        }
        ready = true
    }
}
