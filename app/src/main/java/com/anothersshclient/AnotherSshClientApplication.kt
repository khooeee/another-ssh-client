package com.anothersshclient

import android.app.Application
import com.anothersshclient.session.SessionManager
import com.anothersshclient.ssh.CryptoInit

class AnotherSshClientApplication : Application() {
    lateinit var sessionManager: SessionManager
        private set

    override fun onCreate() {
        super.onCreate()
        CryptoInit.ensureBouncyCastle()
        sessionManager = SessionManager()
    }
}
