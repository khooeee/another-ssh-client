package com.anothersshclient

import android.app.Application
import com.anothersshclient.ssh.CryptoInit

class SsherApp : Application() {
    override fun onCreate() {
        super.onCreate()
        CryptoInit.ensureBouncyCastle()
    }
}
