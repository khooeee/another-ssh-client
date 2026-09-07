package com.anothersshclient

import android.app.Application
import com.anothersshclient.ssh.CryptoInit

class AnotherSshClientApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CryptoInit.ensureBouncyCastle()
    }
}
