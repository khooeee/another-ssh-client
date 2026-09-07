package com.ssher

import android.app.Application
import com.ssher.ssh.CryptoInit

class SsherApp : Application() {
    override fun onCreate() {
        super.onCreate()
        CryptoInit.ensureBouncyCastle()
    }
}
